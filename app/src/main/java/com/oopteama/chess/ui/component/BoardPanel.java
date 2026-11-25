package com.oopteama.chess.ui.component;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.ui.Theme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;

/**
 * 체스판을 그리는 화면 부품입니다. 레슨·퍼즐·AI 대전·로컬 대전 화면이 함께 씁니다.
 * 무엇을 그릴지(국면, 선택한 칸, 힌트 화살표, 드래그 중인 기물 등)만 기억하고 그립니다.
 * 마우스 입력 처리는 BoardController가 맡습니다.
 */
public final class BoardPanel extends JPanel {

    private static final Color LAST_MOVE_COLOR = new Color(245, 200, 66, 105);
    private static final Color HINT_FILL = new Color(52, 211, 153, 85);
    private static final Color HINT_BORDER = new Color(52, 211, 153, 190);
    private static final Color SELECTED_COLOR = new Color(46, 184, 114, 130);
    private static final Color WRONG_COLOR = new Color(248, 113, 113, 165);
    private static final Color MOVE_MARK_COLOR = new Color(18, 24, 34, 95);

    private final Settings settings;
    private final MoveAnimator animator = new MoveAnimator(this::repaint);
    private Board board = Board.initial();
    private boolean flipped;

    private Move lastMove;                        // 직전에 둔 수
    private Move arrowMove;                       // 힌트 화살표
    private Color arrowColor = Theme.BOARD_ACCENT;
    private List<Position> highlights = List.of(); // 힌트로 강조할 칸
    private Position wrongSquare;                 // 잘못 둔 칸 (잠깐 빨갛게)
    private Timer wrongTimer;

    private Position selected;                    // 선택한 칸
    private List<Position> targets = List.of();   // 선택한 기물이 갈 수 있는 칸
    private Position dragFrom;                    // 끌고 있는 기물의 원래 칸
    private Point dragPoint;                      // 끌고 있는 기물을 그릴 마우스 위치
    private boolean dragging;
    private Move droppedMove;                     // 드래그로 놓은 수 (다시 미끄러지지 않게)

    public BoardPanel(Settings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        setBackground(Theme.BG);
        setOpaque(true);
        setPreferredSize(new Dimension(520, 520));
        setMinimumSize(new Dimension(320, 320));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ---- 보여 줄 내용 바꾸기 ----

    /** 보여 줄 국면을 바꿉니다. 수 하나로 바뀐 국면이면 이동 애니메이션을 보여 줍니다. */
    public void setBoard(Board board) {
        Board previous = this.board;
        this.board = Objects.requireNonNull(board, "board");
        selected = null;
        targets = List.of();
        dragFrom = null;
        dragging = false;
        animator.boardChanged(previous, board, droppedMove);
        repaint();
    }

    public Board board() {
        return board;
    }

    /** true면 흑이 아래쪽에 오도록 뒤집어 그립니다. */
    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setLastMove(Move move) {
        this.lastMove = move;
        repaint();
    }

    /** 힌트 화살표 (move가 null이면 지움). */
    public void setArrow(Move move, Color color) {
        this.arrowMove = move;
        this.arrowColor = color != null ? color : Theme.BOARD_ACCENT;
        repaint();
    }

    public void setHighlights(List<Position> squares) {
        this.highlights = squares == null ? List.of() : List.copyOf(squares);
        repaint();
    }

    /** 잘못 둔 칸을 잠깐 빨갛게 표시합니다. */
    public void flashWrong(Position square) {
        wrongSquare = square;
        repaint();
        if (wrongTimer != null) {
            wrongTimer.stop();
        }
        wrongTimer = new Timer(450, e -> {
            wrongSquare = null;
            repaint();
        });
        wrongTimer.setRepeats(false);
        wrongTimer.start();
    }

    // ---- 선택과 드래그 상태 (BoardController가 바꿈) ----

    /** square를 선택하고, 갈 수 있는 칸(targets)에 점을 찍습니다. */
    public void select(Position square, List<Position> targets) {
        this.selected = square;
        this.targets = List.copyOf(targets);
        repaint();
    }

    public void clearSelection() {
        selected = null;
        targets = List.of();
        repaint();
    }

    public Position selected() {
        return selected;
    }

    /** 마우스를 누른 칸의 기물을 끌 준비를 합니다 (아직 움직이지는 않음). */
    public void startDrag(Position from, Point point) {
        dragFrom = from;
        dragPoint = point;
        dragging = false;
    }

    /** 끌고 있는 기물을 point 위치에 그립니다. */
    public void dragTo(Point point) {
        if (dragFrom != null) {
            dragPoint = point;
            dragging = true;
            repaint();
        }
    }

    public void endDrag() {
        dragFrom = null;
        dragging = false;
        repaint();
    }

    public Position dragFrom() {
        return dragFrom;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** 드래그로 놓은 수를 알려 둡니다 (이 수로 국면이 바뀔 때는 애니메이션을 생략). */
    public void setDroppedMove(Move move) {
        this.droppedMove = move;
    }

    /** 재생 중이거나 대기 중인 이동 애니메이션 (테스트용). */
    List<Move> pendingAnimationMoves() {
        return animator.pendingMoves();
    }

    // ---- 화면 좌표 ↔ 칸 ----

    /** 마우스 위치에 있는 칸 (체스판 밖이면 null). */
    public Position positionAt(Point point) {
        int cell = cellSize();
        int left = boardLeft(cell);
        int top = boardTop(cell);
        if (point.x < left || point.y < top) {
            return null;
        }
        int screenCol = (point.x - left) / cell;
        int screenRow = (point.y - top) / cell;
        if (screenCol >= 8 || screenRow >= 8) {
            return null;
        }
        int col = flipped ? 7 - screenCol : screenCol;
        int row = flipped ? screenRow : 7 - screenRow;
        return new Position(col, row);
    }

    private int cellSize() {
        int size = Math.max(64, Math.min(getWidth(), getHeight()) - 20); // 바깥 여백 10px
        return size / 8;
    }

    private int boardLeft(int cell) {
        return (getWidth() - cell * 8) / 2;
    }

    private int boardTop(int cell) {
        return (getHeight() - cell * 8) / 2;
    }

    /** 칸의 화면상 x 좌표 (왼쪽 위 모서리). */
    private int xOf(Position square, int cell) {
        int screenCol = flipped ? 7 - square.col() : square.col();
        return boardLeft(cell) + screenCol * cell;
    }

    /** 칸의 화면상 y 좌표 (왼쪽 위 모서리). 흰 진영 기준으로 8랭크가 위쪽입니다. */
    private int yOf(Position square, int cell) {
        int screenRow = flipped ? square.row() : 7 - square.row();
        return boardTop(cell) + screenRow * cell;
    }

    // ---- 그리기 ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cell = cellSize();

        // 애니메이션 중에는 재생 중인 수 기준으로 그립니다 (뒤에 대기 중인 수가 먼저 보이지 않도록).
        MoveAnimator.Animation animation = animator.current();
        Board shown = animation != null ? animation.after() : board;
        Move shownLastMove = animation != null ? animation.move() : lastMove;

        drawFrame(g2, cell);
        drawSquares(g2, cell, shown, shownLastMove);
        if (settings.showCoords()) {
            drawCoordinates(g2, cell);
        }
        if (settings.showLegalMoves()) {
            drawMoveMarks(g2, cell);
        }
        drawPieces(g2, cell, shown, animation);
        if (animation != null) {
            drawAnimation(g2, cell, animation);
        }
        if (arrowMove != null) {
            drawArrow(g2, cell, arrowMove.from(), arrowMove.to(), arrowColor);
        }
        if (dragging && dragFrom != null && board.pieceAt(dragFrom) != null) {
            PiecePainter.draw(g2, board.pieceAt(dragFrom), dragPoint.x - cell / 2, dragPoint.y - cell / 2, cell, 1f);
        }
        g2.dispose();
    }

    private void drawFrame(Graphics2D g2, int cell) {
        int left = boardLeft(cell);
        int top = boardTop(cell);
        int size = cell * 8;
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(left - 6, top - 6, size + 12, size + 12, 12, 12);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(left - 6, top - 6, size + 11, size + 11, 12, 12);
    }

    /** 64칸 바탕색과 강조 표시(직전 수, 힌트, 선택, 체크, 오답). */
    private void drawSquares(Graphics2D g2, int cell, Board shown, Move shownLastMove) {
        Color light = new Color(settings.boardTheme().lightRgb());
        Color dark = new Color(settings.boardTheme().darkRgb());
        Position checkedKing = Rules.isInCheck(shown) ? shown.findKing(shown.turn()) : null;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position square = new Position(col, row);
                int x = xOf(square, cell);
                int y = yOf(square, cell);
                g2.setColor(square.isLight() ? light : dark);
                g2.fillRect(x, y, cell, cell);

                if (settings.highlightLastMove() && shownLastMove != null
                        && (square.equals(shownLastMove.from()) || square.equals(shownLastMove.to()))) {
                    g2.setColor(LAST_MOVE_COLOR);
                    g2.fillRect(x, y, cell, cell);
                }
                if (highlights.contains(square)) {
                    g2.setColor(HINT_FILL);
                    g2.fillRect(x, y, cell, cell);
                    g2.setColor(HINT_BORDER);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawRect(x + 2, y + 2, cell - 4, cell - 4);
                }
                if (square.equals(selected)) {
                    g2.setColor(SELECTED_COLOR);
                    g2.fillRect(x, y, cell, cell);
                }
                if (square.equals(checkedKing)) {
                    g2.setPaint(new RadialGradientPaint(x + cell / 2f, y + cell / 2f, cell * 0.55f,
                            new float[]{0f, 1f},
                            new Color[]{new Color(239, 68, 68, 220), new Color(239, 68, 68, 0)}));
                    g2.fillRect(x, y, cell, cell);
                }
                if (square.equals(wrongSquare)) {
                    g2.setColor(WRONG_COLOR);
                    g2.fillRect(x, y, cell, cell);
                }
            }
        }
    }

    /** 왼쪽 세로줄에 랭크 숫자, 맨 아래 줄에 파일 알파벳을 적습니다. */
    private void drawCoordinates(Graphics2D g2, int cell) {
        Color light = new Color(settings.boardTheme().lightRgb());
        Color dark = new Color(settings.boardTheme().darkRgb());
        g2.setFont(Theme.bold(Math.max(10f, cell * 0.17f)));
        FontMetrics fm = g2.getFontMetrics();
        int left = boardLeft(cell);
        int top = boardTop(cell);
        for (int i = 0; i < 8; i++) {
            int row = flipped ? i : 7 - i;   // 화면 i번째 줄의 랭크
            int col = flipped ? 7 - i : i;   // 화면 i번째 칸의 파일
            Position leftSquare = new Position(flipped ? 7 : 0, row);
            g2.setColor(leftSquare.isLight() ? dark : light); // 칸 색과 반대 색으로 써서 잘 보이게
            g2.drawString(String.valueOf(row + 1), left + 4, top + i * cell + fm.getAscent() + 2);

            Position bottomSquare = new Position(col, flipped ? 7 : 0);
            g2.setColor(bottomSquare.isLight() ? dark : light);
            String file = String.valueOf((char) ('a' + col));
            g2.drawString(file, left + (i + 1) * cell - fm.stringWidth(file) - 4, top + cell * 8 - 4);
        }
    }

    /** 선택한 기물이 갈 수 있는 칸: 빈 칸은 점, 잡을 수 있는 칸은 고리. */
    private void drawMoveMarks(Graphics2D g2, int cell) {
        if (selected == null) {
            return;
        }
        g2.setColor(MOVE_MARK_COLOR);
        for (Position target : targets) {
            int x = xOf(target, cell);
            int y = yOf(target, cell);
            boolean capture = !board.isEmpty(target)
                    || (board.pieceAt(selected) instanceof Pawn && target.equals(board.enPassant()));
            if (capture) {
                g2.setStroke(new BasicStroke(Math.max(3f, cell * 0.08f)));
                g2.draw(new Ellipse2D.Double(x + cell * 0.09, y + cell * 0.09, cell * 0.82, cell * 0.82));
            } else {
                double r = cell * 0.16;
                g2.fill(new Ellipse2D.Double(x + cell / 2.0 - r, y + cell / 2.0 - r, r * 2, r * 2));
            }
        }
    }

    private void drawPieces(Graphics2D g2, int cell, Board shown, MoveAnimator.Animation animation) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position square = new Position(col, row);
                boolean draggedAway = dragging && square.equals(dragFrom);
                boolean waitingForSlide = animation != null && animation.hides(square);
                Piece piece = shown.pieceAt(square);
                if (piece != null && !draggedAway && !waitingForSlide) {
                    PiecePainter.draw(g2, piece, xOf(square, cell), yOf(square, cell), cell, 1f);
                }
            }
        }
    }

    /** 잡힌 기물은 제자리에서 서서히 사라지고, 움직인 기물은 도착 칸으로 미끄러집니다. */
    private void drawAnimation(Graphics2D g2, int cell, MoveAnimator.Animation animation) {
        double t = animator.progress();
        if (animation.captured() != null && t < 1.0) {
            Position at = animation.capturedAt();
            PiecePainter.draw(g2, animation.captured(), xOf(at, cell), yOf(at, cell), cell, (float) (1.0 - t));
        }
        for (MoveAnimator.Slide slide : animation.slides()) {
            int x = (int) Math.round(xOf(slide.from(), cell) + (xOf(slide.to(), cell) - xOf(slide.from(), cell)) * t);
            int y = (int) Math.round(yOf(slide.from(), cell) + (yOf(slide.to(), cell) - yOf(slide.from(), cell)) * t);
            PiecePainter.draw(g2, slide.piece(), x, y, cell, 1f);
        }
    }

    private void drawArrow(Graphics2D g2, int cell, Position from, Position to, Color color) {
        double x1 = xOf(from, cell) + cell / 2.0;
        double y1 = yOf(from, cell) + cell / 2.0;
        double x2 = xOf(to, cell) + cell / 2.0;
        double y2 = yOf(to, cell) + cell / 2.0;
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length < 1) {
            return;
        }
        double ux = (x2 - x1) / length;
        double uy = (y2 - y1) / length;
        double head = cell * 0.34;
        double shaftEndX = x2 - ux * head;
        double shaftEndY = y2 - uy * head;

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 215));
        g2.setStroke(new BasicStroke(cell * 0.14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) x1, (int) y1, (int) shaftEndX, (int) shaftEndY);

        double px = -uy * head * 0.65;
        double py = ux * head * 0.65;
        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(x2, y2);
        arrowHead.lineTo(shaftEndX + px, shaftEndY + py);
        arrowHead.lineTo(shaftEndX - px, shaftEndY - py);
        arrowHead.closePath();
        g2.fill(arrowHead);
    }
}
