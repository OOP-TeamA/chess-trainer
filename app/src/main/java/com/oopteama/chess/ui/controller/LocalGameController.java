package com.oopteama.chess.ui.controller;

import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.screen.LocalGameScreen;

/**
 * 로컬 2인 대전 컨트롤러입니다. 한 화면에서 두 사람이 번갈아 둡니다.
 */
public final class LocalGameController extends GameController {

    private final LocalGameScreen screen;

    public LocalGameController(ChessBackend backend) {
        this(backend, new LocalGameScreen(backend.settings()));
    }

    /** 화면을 먼저 만들어 부모(GameController) 생성자에 넘기기 위한 보조 생성자입니다. */
    private LocalGameController(ChessBackend backend, LocalGameScreen screen) {
        super(backend, screen);
        this.screen = screen;
        screen.undoButton().addActionListener(e -> undo());
        screen.flipButton().addActionListener(e -> flipBoard());
        screen.resignButton().addActionListener(e -> resign());
        screen.newGameButton().addActionListener(e -> newGame());
        screen.showAutoFlip(backend.settings().autoFlipLocal());
        refreshScreen();
    }

    @Override
    protected String playerName(Side side) {
        return side == Side.WHITE ? "플레이어 1 (백)" : "플레이어 2 (흑)";
    }

    @Override
    protected boolean humanToMove() {
        return !game.isOver(); // 두 사람이 번갈아 두므로 대국이 끝나기 전에는 늘 사람 차례입니다.
    }

    @Override
    protected void onBoardMove(Move move) {
        if (game.isOver()) {
            return;
        }
        game.play(move);
        if (game.isOver()) {
            board.setInteractive(false);
        } else {
            flipForTurnIfEnabled();
        }
        refreshScreen();
    }

    private void undo() {
        if (game.undo()) {
            board.setInteractive(true);
            flipForTurnIfEnabled();
            refreshScreen();
        }
    }

    /** 지금 차례인 사람이 기권합니다. */
    private void resign() {
        if (!game.isOver()) {
            game.resign(game.turn());
            board.setInteractive(false);
            refreshScreen();
        }
    }

    private void newGame() {
        game.reset();
        boardPanel().setFlipped(false);
        board.setInteractive(true);
        refreshScreen();
    }

    /** 설정에서 켰으면 차례인 쪽이 아래에 오도록 보드를 뒤집습니다. */
    private void flipForTurnIfEnabled() {
        if (backend.settings().autoFlipLocal()) {
            boardPanel().setFlipped(game.turn() == Side.BLACK);
        }
    }

    @Override
    public void onDataChanged() {
        screen.showAutoFlip(backend.settings().autoFlipLocal());
        super.onDataChanged();
    }
}
