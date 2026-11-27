package com.oopteama.chess.ui.controller;

import com.oopteama.chess.ai.MoveInsight;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.model.piece.Rook;
import com.oopteama.chess.ui.component.BoardPanel;

import javax.swing.JOptionPane;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 체스판(BoardPanel)의 마우스 입력을 처리하는 이벤트 핸들러입니다.
 * <ul>
 *   <li>클릭: 기물을 눌러 선택한 뒤 갈 칸을 누르면 수가 됩니다. 선택한 기물을 다시 누르면 선택이 풀립니다.</li>
 *   <li>드래그: 기물을 끌어서 갈 칸에 놓으면 수가 됩니다.</li>
 *   <li>호버: 선택한(또는 끌고 있는) 기물이 갈 수 있는 칸에 마우스를 올리면 그 수의 설명을 보여 줍니다.</li>
 * </ul>
 * 수가 정해지면 MoveListener에게, 규칙상 갈 수 없는 칸이면 IllegalMoveListener에게 알립니다.
 */
public final class BoardController {

    /** 사용자가 수를 두었을 때 알림을 받습니다. */
    @FunctionalInterface
    public interface MoveListener {
        void onMove(Move move);
    }

    /** 규칙상 갈 수 없는 칸으로 옮기려 했을 때 알림을 받습니다. */
    @FunctionalInterface
    public interface IllegalMoveListener {
        void onIllegalMove(Position from, Position to);
    }

    private final BoardPanel panel;
    private MoveListener moveListener;
    private IllegalMoveListener illegalMoveListener;
    private boolean interactive = true;
    private Side allowedSide;          // 이 진영만 둘 수 있음 (null이면 지금 차례인 쪽)
    private boolean deselectOnRelease; // 이미 선택한 기물을 다시 눌렀다가 끌지 않고 놓으면 선택 해제

    private Board insightBoard;                         // 수 설명을 계산한 국면
    private Map<Move, MoveInsight> insights = Map.of(); // 그 국면의 수 설명 (설정이 꺼져 있으면 비어 있음)
    private Point hoverPoint;                           // 마지막 마우스 위치 (설명이 늦게 도착하면 다시 확인)

    public BoardController(BoardPanel panel) {
        this.panel = panel;
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onPress(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                panel.dragTo(e.getPoint());
                onHover(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onRelease(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                onHover(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverPoint = null;
                panel.showInsight(null);
            }
        };
        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);
    }

    public BoardPanel panel() {
        return panel;
    }

    public void setMoveListener(MoveListener listener) {
        this.moveListener = listener;
    }

    /** 레슨·퍼즐처럼 "왜 못 가는지" 알려 줄 화면에서만 등록합니다. */
    public void setIllegalMoveListener(IllegalMoveListener listener) {
        this.illegalMoveListener = listener;
    }

    /** false면 마우스 입력을 받지 않습니다 (상대 응수를 기다리는 중 등). */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) {
            panel.clearSelection();
            panel.endDrag();
        }
    }

    /** AI 대전처럼 한쪽 진영만 사람이 둘 때 그 진영을 정합니다. */
    public void setAllowedSide(Side side) {
        this.allowedSide = side;
    }

    private boolean canInteract() {
        return interactive && moveListener != null
                && (allowedSide == null || panel.board().turn() == allowedSide);
    }

    /**
     * position 국면의 수 설명을 받아 둡니다 (빈 Map이면 설명을 끔).
     * 마우스가 이미 갈 수 있는 칸 위에 있으면 바로 보여 줍니다.
     */
    public void setInsights(Board position, Map<Move, MoveInsight> insights) {
        this.insightBoard = position;
        this.insights = insights == null ? Map.of() : insights;
        if (hoverPoint != null) {
            onHover(hoverPoint);
        } else {
            panel.showInsight(null);
        }
    }

    /** 선택한(또는 끌고 있는) 기물이 갈 수 있는 칸에 마우스가 있으면 그 수의 설명을 보여 줍니다. */
    private void onHover(Point point) {
        hoverPoint = point;
        Position from = panel.isDragging() ? panel.dragFrom() : panel.selected();
        Position to = panel.positionAt(point);
        MoveInsight insight = null;
        // 지금 체스판의 국면을 계산한 설명일 때만 씁니다 (국면이 바뀐 뒤 늦게 도착한 설명은 무시).
        boolean overTarget = from != null && to != null && !to.equals(from);
        if (overTarget && canInteract() && panel.board() == insightBoard) {
            insight = insights.get(new Move(from, to));
            if (insight == null) {
                insight = insights.get(new Move(from, to, 'Q')); // 폰 승진은 퀸으로 둘 때를 설명합니다.
            }
        }
        panel.showInsight(insight);
    }

    // ---- 마우스 이벤트 처리 ----

    private void onPress(Point point) {
        if (!canInteract()) {
            return;
        }
        deselectOnRelease = false;
        Position square = panel.positionAt(point);
        if (square == null) {
            panel.clearSelection();
            return;
        }
        Position selected = panel.selected();
        if (selected != null && tryMove(selected, square, false)) {
            return;
        }
        List<Move> moves = Rules.legalMovesFrom(panel.board(), square);
        if (moves.isEmpty()) {
            panel.clearSelection();
            return;
        }
        List<Position> targets = new ArrayList<>();
        for (Move move : moves) {
            targets.add(move.to());
        }
        panel.select(square, targets);
        panel.startDrag(square, point);
        deselectOnRelease = square.equals(selected);
    }

    private void onRelease(Point point) {
        Position from = panel.dragFrom();
        boolean dragged = panel.isDragging();
        panel.endDrag();
        if (from == null || !dragged) {
            if (deselectOnRelease) {
                panel.clearSelection(); // 같은 기물을 다시 눌렀다 뗌 → 선택 해제
            }
            deselectOnRelease = false;
            return;
        }
        deselectOnRelease = false;
        Position to = panel.positionAt(point);
        if (to != null && !to.equals(from)) {
            tryMove(from, to, true);
        }
    }

    /**
     * from → to 수를 두어 봅니다. 수가 되었거나 안내를 했으면 true를 돌려줍니다.
     *
     * @param dropped 드래그로 놓았으면 true, 클릭이면 false
     */
    private boolean tryMove(Position from, Position to, boolean dropped) {
        List<Move> candidates = movesBetween(from, to);
        if (candidates.isEmpty()) {
            // 클릭으로 아군 기물을 누른 것은 이동 시도가 아니라 선택을 바꾸려는 것으로 봅니다.
            Piece target = panel.board().pieceAt(to);
            boolean ownPiece = target != null && target.side() == panel.board().turn();
            if (illegalMoveListener != null && (dropped || !ownPiece)) {
                illegalMoveListener.onIllegalMove(from, to); // 선택은 그대로 두어 갈 수 있는 칸이 계속 보이게 함
                return true;
            }
            return false;
        }
        Move chosen = candidates.size() == 1 ? candidates.getFirst() : askPromotion(candidates);
        panel.clearSelection();
        if (chosen == null) {
            return true; // 승진 선택 창을 닫음
        }
        panel.setDroppedMove(dropped ? chosen : null);
        try {
            moveListener.onMove(chosen);
        } finally {
            panel.setDroppedMove(null);
        }
        return true;
    }

    /** from에서 to로 가는 합법 수 (폰 승진이면 4개). 킹을 고른 뒤 아군 룩을 누르면 캐슬링으로 봅니다. */
    private List<Move> movesBetween(Position from, Position to) {
        Board board = panel.board();
        List<Move> result = new ArrayList<>();
        for (Move move : Rules.legalMovesFrom(board, from)) {
            if (move.to().equals(to)) {
                result.add(move);
            }
        }
        boolean kingToOwnRook = board.pieceAt(from) instanceof King
                && board.pieceAt(to) instanceof Rook rook && rook.side() == board.turn();
        if (result.isEmpty() && kingToOwnRook) {
            Position castleTarget = new Position(from.col() + (to.col() > from.col() ? 2 : -2), from.row());
            for (Move move : Rules.legalMovesFrom(board, from)) {
                if (move.to().equals(castleTarget)) {
                    result.add(move);
                }
            }
        }
        return result;
    }

    /** 폰 승진: 바꿀 기물을 고르는 창을 띄웁니다. 창을 닫으면 null입니다. */
    private Move askPromotion(List<Move> candidates) {
        char[] symbols = {'Q', 'R', 'B', 'N'};
        String[] labels = {"퀸 (Q)", "룩 (R)", "비숍 (B)", "나이트 (N)"};
        int choice = JOptionPane.showOptionDialog(panel,
                panel.board().turn().label() + " 폰이 승진할 기물을 선택하세요.", "폰 승진",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
        if (choice < 0) {
            return null;
        }
        for (Move move : candidates) {
            if (move.promotion() == symbols[choice]) {
                return move;
            }
        }
        return candidates.getFirst();
    }
}
