package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.ChessAI.Difficulty;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.service.ChessBackend;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 대전 화면: 모든 탐색·힌트·전적 기록을 ChessBackend 퍼사드를 통해서만 수행합니다.
 */
public final class AiGamePanel extends AbstractGamePanel {

    private final Random random = new Random();
    private final JComboBox<Difficulty> diffCombo = new JComboBox<>(Difficulty.values());
    private final JComboBox<String> sideCombo = new JComboBox<>(new String[]{"백으로 플레이", "흑으로 플레이", "무작위 진영"});

    private Difficulty currentDifficulty = Difficulty.EASY;
    private Side humanSide = Side.WHITE;
    private boolean resultRecorded;

    private AtomicBoolean activeCancel = new AtomicBoolean(false);
    private boolean aiThinking;

    public AiGamePanel(ChessBackend backend) {
        super(backend);
        diffCombo.setSelectedItem(Difficulty.EASY);
        initLayout();
        startNewGame();
    }

    @Override
    protected JComponent buildTopBar() {
        JPanel bar = Theme.card(10);
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        JButton newBtn = Theme.primaryButton("새 대국 시작");
        newBtn.addActionListener(e -> startNewGame());

        bar.add(Theme.muted("난이도"));
        bar.add(diffCombo);
        bar.add(Theme.muted("내 진영"));
        bar.add(sideCombo);
        bar.add(newBtn);
        return bar;
    }

    @Override
    protected JComponent buildControlBar() {
        JPanel controls = new JPanel(new GridLayout(2, 2, 8, 8));
        controls.setOpaque(false);

        JButton hintBtn = Theme.secondaryButton("힌트");
        JButton undoBtn = Theme.secondaryButton("무르기");
        JButton flipBtn = Theme.secondaryButton("보드 뒤집기");
        JButton resignBtn = Theme.dangerButton("기권");

        hintBtn.addActionListener(e -> requestHint());
        undoBtn.addActionListener(e -> undoTurn());
        flipBtn.addActionListener(e -> {
            boardView.setFlipped(!boardView.isFlipped());
            refreshBoardUi();
        });
        resignBtn.addActionListener(e -> resignGame());

        controls.add(hintBtn);
        controls.add(undoBtn);
        controls.add(flipBtn);
        controls.add(resignBtn);
        return controls;
    }

    @Override
    protected String playerName(Side side) {
        return side == humanSide ? backend.activeProfile().name() : "AI (" + currentDifficulty.label() + ")";
    }

    private void startNewGame() {
        cancelAiTask();
        currentDifficulty = (Difficulty) diffCombo.getSelectedItem();
        humanSide = switch (sideCombo.getSelectedIndex()) {
            case 1 -> Side.BLACK;
            case 2 -> random.nextBoolean() ? Side.WHITE : Side.BLACK;
            default -> Side.WHITE;
        };
        resultRecorded = false;
        game.reset();

        boardView.setArrow(null, Theme.INFO);
        boardView.setFlipped(humanSide == Side.BLACK);
        boardView.setAllowedSide(humanSide);
        boardView.setInteractive(true);
        refreshBoardUi();

        if (game.turn() != humanSide) {
            triggerAiTurn();
        }
    }

    @Override
    protected void onBoardMove(Move move) {
        if (aiThinking || game.isOver() || game.turn() != humanSide) {
            return;
        }
        game.play(move);
        boardView.setArrow(null, Theme.INFO);
        refreshBoardUi();

        if (game.isOver()) {
            recordResultIfNeeded();
            boardView.setInteractive(false);
        } else {
            triggerAiTurn();
        }
    }

    private void triggerAiTurn() {
        cancelAiTask();
        aiThinking = true;
        boardView.setInteractive(false);
        updateStatusBanner("AI(" + currentDifficulty.label() + ") 생각 중...", Theme.INFO);

        Board snapshot = game.board();
        Difficulty diff = currentDifficulty;
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        this.activeCancel = cancelFlag;

        new SwingWorker<Optional<Move>, Void>() {
            @Override
            protected Optional<Move> doInBackground() {
                return backend.chooseAiMove(snapshot, diff, cancelFlag::get);
            }

            @Override
            protected void done() {
                if (cancelFlag.get()) return;
                aiThinking = false;
                try {
                    Optional<Move> reply = get();
                    if (reply.isPresent() && !game.isOver()) {
                        game.play(reply.get());
                    }
                } catch (Exception ignored) {
                }
                refreshBoardUi();
                if (game.isOver()) {
                    recordResultIfNeeded();
                    boardView.setInteractive(false);
                } else {
                    boardView.setInteractive(true);
                }
            }
        }.execute();
    }

    private void requestHint() {
        if (aiThinking || game.isOver() || game.turn() != humanSide) return;
        Board snapshot = game.board();
        updateStatusBanner("추천 수 계산 중...", Theme.INFO);
        new SwingWorker<Optional<Move>, Void>() {
            @Override
            protected Optional<Move> doInBackground() {
                return backend.computeHint(snapshot);
            }

            @Override
            protected void done() {
                try {
                    get().ifPresent(m -> boardView.setArrow(m, Theme.BOARD_INFO));
                } catch (Exception ignored) {
                }
                refreshBoardUi();
            }
        }.execute();
    }

    private void undoTurn() {
        boolean wasThinking = aiThinking;
        cancelAiTask();
        if (game.history().isEmpty()) return;
        if (wasThinking || game.turn() != humanSide) {
            game.undo(1);
        } else {
            if (humanSide == Side.BLACK && game.history().size() <= 1) return;
            game.undo(2);
        }
        boardView.setArrow(null, Theme.INFO);
        boardView.setInteractive(!game.isOver());
        refreshBoardUi();
    }

    private void resignGame() {
        if (game.isOver()) return;
        cancelAiTask();
        game.resign(humanSide);
        boardView.setInteractive(false);
        recordResultIfNeeded();
        refreshBoardUi();
    }

    private void recordResultIfNeeded() {
        if (resultRecorded || !game.isOver()) return;
        resultRecorded = true;
        Side winner = game.winner();
        int outcome = winner == null ? 0 : (winner == humanSide ? 1 : -1);
        backend.recordAiResult(currentDifficulty, outcome);
    }

    private void cancelAiTask() {
        activeCancel.set(true);
        aiThinking = false;
    }

    @Override
    public void onShow() {
        refreshBoardUi();
    }

    @Override
    public void onHide() {
        cancelAiTask();
    }
}
