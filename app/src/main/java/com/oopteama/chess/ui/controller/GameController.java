package com.oopteama.chess.ui.controller;

import com.oopteama.chess.model.Game;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.screen.GameScreen;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.List;

/**
 * 대국 화면 컨트롤러의 공통 부모 클래스입니다.
 * 대국(Game)을 들고 있고, 대국 상태를 화면(선수 띠, 상태 문구, 기보)에 옮기는 일을 함께 처리합니다.
 * 수를 두었을 때 무엇을 할지는 AiGameController, LocalGameController가 각자 정합니다.
 */
public abstract class GameController implements ScreenController {

    protected final ChessBackend backend;
    protected final Game game;
    protected final BoardController board;
    private final GameScreen screen;

    protected GameController(ChessBackend backend, GameScreen screen) {
        this.backend = backend;
        this.screen = screen;
        this.game = backend.newGame();
        this.board = new BoardController(screen.boardPanel());
        board.setMoveListener(this::onBoardMove);
    }

    /** 사람이 체스판에서 수를 두었을 때 할 일. */
    protected abstract void onBoardMove(Move move);

    /** 선수 띠에 보여 줄 이름. */
    protected abstract String playerName(Side side);

    /** 지금 대국 상태를 화면에 다시 그립니다. */
    protected final void refreshScreen() {
        BoardPanel panel = screen.boardPanel();
        panel.setBoard(game.board());
        Game.PlayedMove last = game.lastMove();
        panel.setLastMove(last == null ? null : last.move());

        Side top = panel.isFlipped() ? Side.WHITE : Side.BLACK;
        Side bottom = top.opposite();
        screen.showPlayers(sideMark(top) + " " + playerName(top), capturedText(top),
                sideMark(bottom) + " " + playerName(bottom), capturedText(bottom));
        screen.showStatus(game.statusText(), statusColor());
        screen.showMoveList(moveListText());
    }

    /** 상태 문구만 잠깐 바꿉니다 (예: "AI 생각 중..."). */
    protected final void showStatus(String text, Color color) {
        screen.showStatus(text, color);
    }

    protected final BoardPanel boardPanel() {
        return screen.boardPanel();
    }

    /** 보드를 위아래로 뒤집습니다. */
    protected final void flipBoard() {
        boardPanel().setFlipped(!boardPanel().isFlipped());
        refreshScreen();
    }

    private Color statusColor() {
        return switch (game.status()) {
            case CHECK -> Theme.WARNING;
            case CHECKMATE, RESIGNED_WHITE, RESIGNED_BLACK -> Theme.SUCCESS;
            case STALEMATE, DRAW_INSUFFICIENT, DRAW_FIFTY_MOVE, DRAW_REPETITION -> Theme.INFO;
            default -> Theme.TEXT;
        };
    }

    /** 기보를 "1. e4 e5" 형식의 줄로 만듭니다. */
    private String moveListText() {
        List<Game.PlayedMove> history = game.history();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i += 2) {
            String white = history.get(i).san();
            String black = i + 1 < history.size() ? history.get(i + 1).san() : "";
            sb.append(String.format("%2d. %-9s %s%n", i / 2 + 1, white, black));
        }
        return sb.toString();
    }

    private String capturedText(Side capturer) {
        StringBuilder sb = new StringBuilder();
        for (Piece piece : game.capturedBy(capturer)) {
            sb.append(piece.symbol()).append(' ');
        }
        return sb.toString().trim();
    }

    private static String sideMark(Side side) {
        return side == Side.WHITE ? "○" : "●";
    }

    // ---- ScreenController ----

    @Override
    public JComponent view() {
        return screen;
    }

    @Override
    public void onDataChanged() {
        boardPanel().repaint();
    }

    @Override
    public String leaveWarning() {
        return !game.isOver() && !game.history().isEmpty()
                ? "진행 중인 대국이 있습니다. 화면을 이동하시겠습니까?"
                : null;
    }
}
