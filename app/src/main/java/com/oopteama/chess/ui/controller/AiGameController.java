package com.oopteama.chess.ui.controller;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.screen.AiGameScreen;

import javax.swing.SwingWorker;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 대전 컨트롤러입니다. 사람이 수를 두면 AI가 백그라운드 스레드에서 응수를 계산해 둡니다.
 * (계산 중에도 화면이 멈추지 않도록 SwingWorker를 씁니다.)
 */
public final class AiGameController extends GameController {

    private final AiGameScreen screen;
    private final Random random = new Random();
    private Difficulty difficulty = Difficulty.EASY;
    private Side humanSide = Side.WHITE;
    private boolean resultRecorded;
    private boolean aiThinking;
    private AtomicBoolean cancelFlag = new AtomicBoolean(false); // 진행 중인 AI 계산을 멈추는 신호

    public AiGameController(ChessBackend backend) {
        this(backend, new AiGameScreen(backend.settings()));
    }

    /** 화면을 먼저 만들어 부모(GameController) 생성자에 넘기기 위한 보조 생성자입니다. */
    private AiGameController(ChessBackend backend, AiGameScreen screen) {
        super(backend, screen);
        this.screen = screen;
        screen.newGameButton().addActionListener(e -> startNewGame());
        screen.hintButton().addActionListener(e -> requestHint());
        screen.undoButton().addActionListener(e -> undoTurn());
        screen.flipButton().addActionListener(e -> flipBoard());
        screen.resignButton().addActionListener(e -> resign());
        startNewGame();
    }

    @Override
    protected String playerName(Side side) {
        return side == humanSide ? backend.activeProfile().name() : "AI (" + difficulty.label() + ")";
    }

    private void startNewGame() {
        cancelAiThinking();
        difficulty = (Difficulty) screen.difficultyBox().getSelectedItem();
        humanSide = switch (screen.sideBox().getSelectedIndex()) {
            case 1 -> Side.BLACK;
            case 2 -> random.nextBoolean() ? Side.WHITE : Side.BLACK;
            default -> Side.WHITE;
        };
        resultRecorded = false;
        game.reset();
        boardPanel().setArrow(null, null);
        boardPanel().setFlipped(humanSide == Side.BLACK);
        board.setAllowedSide(humanSide);
        board.setInteractive(true);
        refreshScreen();
        if (game.turn() != humanSide) {
            startAiTurn();
        }
    }

    @Override
    protected void onBoardMove(Move move) {
        if (aiThinking || game.isOver() || game.turn() != humanSide) {
            return;
        }
        game.play(move);
        boardPanel().setArrow(null, null);
        refreshScreen();
        if (game.isOver()) {
            finishGame();
        } else {
            startAiTurn();
        }
    }

    /** AI 수 계산을 백그라운드에서 시작하고, 끝나면 그 수를 둡니다. */
    private void startAiTurn() {
        cancelAiThinking();
        aiThinking = true;
        board.setInteractive(false);
        showStatus("AI(" + difficulty.label() + ") 생각 중...", Theme.INFO);

        Board position = game.board();
        Difficulty level = difficulty;
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelFlag = cancelled;
        new SwingWorker<Optional<Move>, Void>() {
            @Override
            protected Optional<Move> doInBackground() {
                return backend.chooseAiMove(position, level, cancelled::get); // 백그라운드 스레드
            }

            @Override
            protected void done() { // 화면 스레드
                if (cancelled.get()) {
                    return;
                }
                aiThinking = false;
                try {
                    get().ifPresent(move -> {
                        if (!game.isOver()) {
                            game.play(move);
                        }
                    });
                } catch (Exception ignored) {
                    // 계산이 실패하면 수를 두지 않고 사람 차례로 돌아갑니다.
                }
                refreshScreen();
                if (game.isOver()) {
                    finishGame();
                } else {
                    board.setInteractive(true);
                }
            }
        }.execute();
    }

    /** 추천 수를 백그라운드에서 계산해 화살표로 보여 줍니다. */
    private void requestHint() {
        if (aiThinking || game.isOver() || game.turn() != humanSide) {
            return;
        }
        Board position = game.board();
        showStatus("추천 수 계산 중...", Theme.INFO);
        new SwingWorker<Optional<Move>, Void>() {
            @Override
            protected Optional<Move> doInBackground() {
                return backend.computeHint(position);
            }

            @Override
            protected void done() {
                try {
                    get().ifPresent(move -> boardPanel().setArrow(move, Theme.BOARD_INFO));
                } catch (Exception ignored) {
                    // 추천 수를 못 구하면 화살표 없이 둡니다.
                }
                refreshScreen();
            }
        }.execute();
    }

    /** 무르기: 내 수와 AI 수를 함께 되돌립니다 (AI가 생각 중이면 내 수만). */
    private void undoTurn() {
        boolean wasThinking = aiThinking;
        cancelAiThinking();
        if (game.history().isEmpty()) {
            return;
        }
        if (wasThinking || game.turn() != humanSide) {
            game.undo(1);
        } else {
            if (humanSide == Side.BLACK && game.history().size() <= 1) {
                return; // 흑으로 둘 때 AI의 첫 수는 무르지 않습니다.
            }
            game.undo(2);
        }
        boardPanel().setArrow(null, null);
        board.setInteractive(!game.isOver());
        refreshScreen();
    }

    private void resign() {
        if (game.isOver()) {
            return;
        }
        cancelAiThinking();
        game.resign(humanSide);
        finishGame();
        refreshScreen();
    }

    /** 대국이 끝나면 입력을 막고 전적을 한 번만 기록합니다. */
    private void finishGame() {
        board.setInteractive(false);
        if (resultRecorded || !game.isOver()) {
            return;
        }
        resultRecorded = true;
        Side winner = game.winner();
        int outcome = winner == null ? 0 : (winner == humanSide ? 1 : -1);
        backend.recordAiResult(difficulty, outcome);
    }

    private void cancelAiThinking() {
        cancelFlag.set(true);
        aiThinking = false;
    }

    @Override
    public void onShow() {
        refreshScreen();
        // 다른 화면에 가 있는 동안 멈춘 AI 차례를 이어서 둡니다.
        if (!game.isOver() && game.turn() != humanSide && !aiThinking) {
            startAiTurn();
        }
    }

    /** 화면을 떠나면 AI 계산을 멈춥니다. 돌아오면 onShow가 다시 시작합니다. */
    @Override
    public void onHide() {
        cancelAiThinking();
    }
}
