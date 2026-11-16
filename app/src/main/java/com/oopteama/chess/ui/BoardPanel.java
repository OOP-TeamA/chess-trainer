package com.oopteama.chess.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;


public class BoardPanel extends JPanel {

    // =========================================
    // 체스판 표시 상태
    // =========================================

    public enum Mark {
        LAST_MOVE,
        SELECTED,
        TARGET,
        HINT,
        SOLUTION,
        WRONG
    }


    // =========================================
    // 체스판 화살표 종류
    // =========================================

    public enum ArrowKind {
        SOLUTION,
        THREAT,
        AI_HINT
    }


    public record Arrow(
            Square from,
            Square to,
            ArrowKind kind
    ) { }


    // =========================================
    // 내부 데이터
    // =========================================

    private Position position = Position.initial();

    private List<Move> legalMoves = List.of();

    private Optional<InputMode> inputMode =
            Optional.empty();

    private Optional<Square> checkSquare =
            Optional.empty();

    private boolean flipped = false;

    private Square selectedSquare;

    private Square wrongSquare;

    private Timer wrongTimer;

    private final Map<Square, EnumSet<Mark>> marks =
            new HashMap<>();

    private List<Arrow> arrows = List.of();


    // =========================================
    // 이벤트 리스너
    // =========================================

    private final List<Consumer<Move>> moveListeners =
            new ArrayList<>();

    private final List<Consumer<Square>> squareListeners =
            new ArrayList<>();


    // =========================================
    // 생성자
    // =========================================

    public BoardPanel() {

        setBackground(Theme.WINDOW);

        setOpaque(true);

        setPreferredSize(
                new Dimension(620, 620)
        );

        setMinimumSize(
                new Dimension(320, 320)
        );

        setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {

                // 누른 위치가 아니라 마우스를 놓은 위치를 기준으로 입력한다.
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleClick(e.getX(), e.getY());
                }
            }
        });
    }


    // =========================================
    // 체스판 데이터 설정
    // =========================================

    /**
     * 보드에 표시할 국면을 사본으로 설정한다.
     * <p>새 국면을 설정하면 이전 국면의 합법 수, 선택 상태,
     * 체크·오답 표시, 칸 강조 및 화살표를 초기화한다.
     * 따라서 이동 입력과 시각적 표시를 계속 사용하려면 호출자가
     * 이어서 {@link #setLegalMoves(List)}, {@link #setCheckSquare(Optional)},
     * {@link #mark(Collection, Mark)}, {@link #setArrows(List)} 등을 호출해야 한다.
     */
    public void setPosition(Position snapshot) {

        this.position = Objects.requireNonNull(
                snapshot
        ).copy();

        // 새 국면에 이전 국면의 입력 상태가 남지 않도록 초기화한다.
        legalMoves = List.of();
        selectedSquare = null;

        // 이전 국면의 시각적 표시를 초기화한다.
        checkSquare = Optional.empty();
        marks.clear();
        arrows = List.of();
        wrongSquare = null;

        // 진행 중인 오답 표시 타이머도 정리한다.
        if (wrongTimer != null) {
            wrongTimer.stop();
            wrongTimer = null;
        }

        repaint();
    }


    public void setCheckSquare(Optional<Square> king) {

        this.checkSquare = Objects.requireNonNull(king);

        repaint();
    }


    public void setLegalMoves(List<Move> moves) {

        this.legalMoves = List.copyOf(moves);

        selectedSquare = null;

        repaint();
    }


    public void setInputMode(Optional<InputMode> mode) {

        this.inputMode = Objects.requireNonNull(mode);

        if (
                mode.isEmpty()
                || mode.get() == InputMode.SQUARE
        ) {

            selectedSquare = null;
        }

        repaint();
    }


    public void setFlipped(boolean flipped) {

        this.flipped = flipped;

        selectedSquare = null;

        repaint();
    }


    // =========================================
    // 칸 강조 표시 관리
    // =========================================

    public void mark(
            Collection<Square> squares,
            Mark mark
    ) {

        Objects.requireNonNull(squares);
        Objects.requireNonNull(mark);

        for (Square square : squares) {

            marks.computeIfAbsent(
                    square,
                    key -> EnumSet.noneOf(Mark.class)
            ).add(mark);
        }

        repaint();
    }


    public void clearMarks() {

        marks.clear();

        selectedSquare = null;

        wrongSquare = null;

        if (wrongTimer != null) {

            wrongTimer.stop();

            wrongTimer = null;
        }

        repaint();
    }


    // =========================================
    // 오답 칸 표시
    // =========================================

    public void flashWrong(Square square) {

        Objects.requireNonNull(square);

        if (wrongTimer != null) {

            wrongTimer.stop();
        }

        wrongSquare = square;

        wrongTimer = new Timer(400, e -> {

            wrongSquare = null;

            wrongTimer = null;

            repaint();
        });

        wrongTimer.setRepeats(false);

        wrongTimer.start();

        repaint();
    }


    // =========================================
    // 화살표 설정
    // =========================================

    public void setArrows(List<Arrow> arrows) {

        this.arrows = List.copyOf(arrows);

        repaint();
    }


    // =========================================
    // 이벤트 리스너 등록 및 제거
    // =========================================

    public void addMoveListener(
            Consumer<Move> listener
    ) {

        addIdentity(moveListeners, listener);
    }


    public void addSquareListener(
            Consumer<Square> listener
    ) {

        addIdentity(squareListeners, listener);
    }


    public void removeMoveListener(
            Consumer<Move> listener
    ) {

        removeIdentity(moveListeners, listener);
    }


    public void removeSquareListener(
            Consumer<Square> listener
    ) {

        removeIdentity(squareListeners, listener);
    }


    private <T> void addIdentity(
            List<T> list,
            T listener
    ) {

        Objects.requireNonNull(listener);

        for (T existing : list) {

            if (existing == listener) {

                return;
            }
        }

        list.add(listener);
    }


    private <T> void removeIdentity(
            List<T> list,
            T listener
    ) {

        for (int i = 0; i < list.size(); i++) {

            if (list.get(i) == listener) {

                list.remove(i);

                return;
            }
        }
    }


    // =========================================
    // 마우스 클릭 처리
    // =========================================

    private void handleClick(int x, int y) {

        // 입력 모드가 비어 있으면 입력 잠금
        if (inputMode.isEmpty()) {

            return;
        }

        Optional<Square> clicked =
                getSquareAt(x, y);

        if (clicked.isEmpty()) {

            return;
        }

        Square square = clicked.get();

        // 칸 선택 모드
        if (inputMode.get() == InputMode.SQUARE) {

            for (
                    Consumer<Square> listener :
                    List.copyOf(squareListeners)
            ) {

                listener.accept(square);
            }

            return;
        }

        // 일반 이동 모드
        handleMoveClick(square);
    }


    // =========================================
    // 기물 이동 처리
    // =========================================

    private void handleMoveClick(Square square) {

        // 선택된 기물이 없는 경우
        if (selectedSquare == null) {

            selectSquare(square);

            return;
        }

        // 같은 칸을 다시 누르면 선택 해제
        if (selectedSquare.equals(square)) {

            selectedSquare = null;

            repaint();

            return;
        }

        // 선택한 칸에서 이동 가능한 수 검색
        List<Move> candidates = legalMoves.stream()
                .filter(move ->
                        move.from().equals(selectedSquare)
                                && move.to().equals(square)
                )
                .toList();

        if (!candidates.isEmpty()) {

            Move selectedMove = candidates.get(0);

            // 폰 승진 처리
            if (selectedMove.isPromotion()) {

                selectedMove = choosePromotion(candidates);

                if (selectedMove == null) {

                    return;
                }
            }

            // 이동 이벤트 전달
            for (
                    Consumer<Move> listener :
                    List.copyOf(moveListeners)
            ) {

                listener.accept(selectedMove);
            }

            selectedSquare = null;

            repaint();

            return;
        }

        // 다른 기물 선택
        selectSquare(square);
    }


    // =========================================
    // 기물 선택
    // =========================================

    private void selectSquare(Square square) {

        boolean canMove = legalMoves.stream()
                .anyMatch(move ->
                        move.from().equals(square)
                );

        selectedSquare = canMove ? square : null;

        repaint();
    }


    // =========================================
    // 폰 승진 선택
    // =========================================

    private Move choosePromotion(
            List<Move> candidates
    ) {

        PieceType[] types = {

                PieceType.QUEEN,
                PieceType.ROOK,
                PieceType.BISHOP,
                PieceType.KNIGHT
        };

        String[] names = {

                "퀸",
                "룩",
                "비숍",
                "나이트"
        };

        int choice = JOptionPane.showOptionDialog(

                this,

                "승진할 기물을 선택하세요.",

                "기물 승진",

                JOptionPane.DEFAULT_OPTION,

                JOptionPane.QUESTION_MESSAGE,

                null,

                names,

                names[0]
        );

        if (choice < 0) {

            return null;
        }

        PieceType selectedType = types[choice];

        return candidates.stream()
                .filter(move ->
                        move.promotion() == selectedType
                )
                .findFirst()
                .orElse(null);
    }

        // =========================================
    // 체스판 그리기
    // =========================================

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        // 체스판 영역 계산
        BoardBounds bounds = calculateBounds();

        int x = bounds.x();
        int y = bounds.y();
        int size = bounds.cellSize();

        // 체스판 그림자
        drawBoardShadow(
                g2,
                x,
                y,
                size
        );

        // 체스판 칸
        drawSquares(
                g2,
                x,
                y,
                size
        );

        // 칸 강조(마지막 수, 선택, 오답, 체크 등)
        drawMarks(
                g2,
                x,
                y,
                size
        );

        // 힌트 및 정답 화살표는 기물 아래에 그린다.
        drawArrows(
                g2,
                x,
                y,
                size
        );

        // 체스 기물
        drawPieces(
                g2,
                x,
                y,
                size
        );

        // 이동 가능 점과 잡기 가능 링은 기물 위에 표시한다.
        drawLegalTargets(
                g2,
                x,
                y,
                size
        );

        // 힌트 링도 기물 위에 표시한다.
        drawMarkOutlines(
                g2,
                x,
                y,
                size
        );

        // 좌표
        drawCoordinates(
                g2,
                x,
                y,
                size
        );

        g2.dispose();
    }


    // =========================================
    // 체스판 영역 계산
    // =========================================

    private record BoardBounds(
            int x,
            int y,
            int cellSize
    ) { }


    private BoardBounds calculateBounds() {

        int cellSize = Math.max(
                1,
                Math.min(
                        (getWidth() - 44) / 8,
                        (getHeight() - 44) / 8
                )
        );

        int boardSize = cellSize * 8;

        int boardX =
                (getWidth() - boardSize + 22) / 2;

        int boardY =
                (getHeight() - boardSize - 18) / 2;

        return new BoardBounds(
                boardX,
                boardY,
                cellSize
        );
    }


    // =========================================
    // 체스판 그림자
    // =========================================

    private void drawBoardShadow(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        int boardSize = size * 8;

        g2.setColor(
                new Color(0, 0, 0, 65)
        );

        g2.fillRoundRect(
                x - 3,
                y - 3,
                boardSize + 8,
                boardSize + 8,
                8,
                8
        );

        g2.setColor(Theme.LINE);

        g2.setStroke(
                new BasicStroke(1.5f)
        );

        g2.drawRect(
                x - 1,
                y - 1,
                boardSize + 1,
                boardSize + 1
        );
    }


    // =========================================
    // 체스판 칸 그리기
    // =========================================

    private void drawSquares(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        for (int row = 0; row < 8; row++) {

            for (int col = 0; col < 8; col++) {

                Square square = squareAt(row, col);

                g2.setColor(
                        square.isLight()
                                ? Theme.SQ_LIGHT
                                : Theme.SQ_DARK
                );

                g2.fillRect(
                        x + col * size,
                        y + row * size,
                        size,
                        size
                );
            }
        }
    }


    // =========================================
    // 칸 강조 표시
    // =========================================

    private void drawMarks(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        for (
                Map.Entry<Square, EnumSet<Mark>> entry :
                marks.entrySet()
        ) {

            Square square = entry.getKey();

            for (Mark mark : entry.getValue()) {

                switch (mark) {
                    case LAST_MOVE -> fillSquare(
                            g2, x, y, size, square, Theme.ACCENT, 0.52f
                    );
                    case SELECTED -> fillSquare(
                            g2, x, y, size, square, Theme.ACCENT, 0.75f
                    );
                    case TARGET -> fillSquare(
                            g2, x, y, size, square, Theme.OK, 0.32f
                    );
                    case HINT -> {
                        // HINT는 LAST_MOVE와 구분되도록 별도의 이중 링으로 그린다.
                    }
                    case SOLUTION -> fillSquare(
                            g2, x, y, size, square, Theme.OK, 0.45f
                    );
                    case WRONG -> fillSquare(
                            g2, x, y, size, square, Theme.BAD, 0.58f
                    );
                }
            }
        }

        // 체크 상태는 단색 덮개 대신 중앙이 진하고 가장자리로
        // 자연스럽게 사라지는 붉은 방사형 그라데이션으로 표시한다.
        if (checkSquare.isPresent()) {
            fillCheckGradient(g2, x, y, size, checkSquare.get());
        }

        // 오답 칸
        if (wrongSquare != null) {
            fillSquare(
                    g2, x, y, size, wrongSquare, Theme.BAD, 0.65f
            );
        }

        // 현재 선택된 칸은 명세의 선택 강조(황동색 75%)로 표시한다.
        if (selectedSquare != null) {
            fillSquare(
                    g2, x, y, size, selectedSquare, Theme.ACCENT, 0.75f
            );
        }
    }


    /** 힌트 칸을 황동 링과 밝은 바깥 테두리의 이중 원으로 표시한다. */
    private void drawMarkOutlines(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        StrokeState old = new StrokeState(g2);

        for (Map.Entry<Square, EnumSet<Mark>> entry : marks.entrySet()) {
            if (!entry.getValue().contains(Mark.HINT)) {
                continue;
            }

            Point point = squarePoint(x, y, size, entry.getKey());
            int outerInset = Math.max(4, size / 8);
            int outerDiameter = size - outerInset * 2;
            int innerInset = Math.max(7, size / 5);
            int innerDiameter = size - innerInset * 2;

            g2.setColor(new Color(0xFFF1C8));
            g2.setStroke(new BasicStroke(Math.max(2.2f, size / 28f)));
            g2.drawOval(
                    point.x + outerInset, point.y + outerInset,
                    outerDiameter, outerDiameter
            );

            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(Math.max(2.8f, size / 22f)));
            g2.drawOval(
                    point.x + innerInset, point.y + innerInset,
                    innerDiameter, innerDiameter
            );
        }

        old.restore(g2);
    }


    private void fillCheckGradient(
            Graphics2D g2,
            int x,
            int y,
            int size,
            Square square
    ) {

        Point point = squarePoint(x, y, size, square);
        float centerX = point.x + size / 2f;
        float centerY = point.y + size / 2f;
        float radius = Math.max(1f, size * 0.78f);

        Paint oldPaint = g2.getPaint();
        g2.setPaint(new RadialGradientPaint(
                centerX,
                centerY,
                radius,
                new float[]{0.0f, 0.55f, 1.0f},
                new Color[]{
                        new Color(Theme.BAD.getRed(), Theme.BAD.getGreen(), Theme.BAD.getBlue(), 155),
                        new Color(Theme.BAD.getRed(), Theme.BAD.getGreen(), Theme.BAD.getBlue(), 75),
                        new Color(Theme.BAD.getRed(), Theme.BAD.getGreen(), Theme.BAD.getBlue(), 0)
                }
        ));
        g2.fillRect(point.x, point.y, size, size);
        g2.setPaint(oldPaint);
    }


    /** Graphics2D 상태를 그리기 전 상태로 복원하기 위한 보조 타입. */
    private static final class StrokeState {
        private final java.awt.Stroke stroke;
        private final Color color;

        private StrokeState(Graphics2D g2) {
            stroke = g2.getStroke();
            color = g2.getColor();
        }

        private void restore(Graphics2D g2) {
            g2.setStroke(stroke);
            g2.setColor(color);
        }
    }


    private void fillSquare(
            Graphics2D g2,
            int x,
            int y,
            int size,
            Square square,
            Color color,
            float alpha
    ) {

        Point point = squarePoint(
                x,
                y,
                size,
                square
        );

        Composite old = g2.getComposite();

        g2.setComposite(
                AlphaComposite.SrcOver.derive(alpha)
        );

        g2.setColor(color);

        g2.fillRect(
                point.x,
                point.y,
                size,
                size
        );

        g2.setComposite(old);
    }


    // =========================================
    // 합법 수 표시
    // =========================================

    private void drawLegalTargets(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        if (
                selectedSquare == null
                || inputMode.isEmpty()
                || inputMode.get() != InputMode.MOVE
        ) {
            return;
        }

        java.awt.Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        for (Move move : legalMoves) {
            if (!move.from().equals(selectedSquare)) {
                continue;
            }

            Point center = squareCenter(x, y, size, move.to());

            if (move.isCapture()) {
                // 잡기 가능한 칸은 기물이 있거나(일반 잡기),
                // 앙파상처럼 목적지가 비어 있어도 링으로 구분한다.
                int outerDiameter = (int) (size * 0.72f);
                int innerDiameter = (int) (size * 0.62f);

                g2.setColor(new Color(255, 245, 225, 225));
                g2.setStroke(new BasicStroke(Math.max(2.0f, size / 28f)));
                g2.drawOval(
                        center.x - outerDiameter / 2,
                        center.y - outerDiameter / 2,
                        outerDiameter,
                        outerDiameter
                );

                g2.setColor(new Color(35, 29, 22, 230));
                g2.setStroke(new BasicStroke(Math.max(1.3f, size / 45f)));
                g2.drawOval(
                        center.x - innerDiameter / 2,
                        center.y - innerDiameter / 2,
                        innerDiameter,
                        innerDiameter
                );
            } else {
                g2.setColor(new Color(35, 29, 22, 145));
                int diameter = Math.max(5, size / 4);
                g2.fillOval(
                        center.x - diameter / 2,
                        center.y - diameter / 2,
                        diameter,
                        diameter
                );
            }
        }

        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

        // =========================================
    // 기물 그리기
    // =========================================

    private void drawPieces(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        // 논리 글꼴을 사용해 특정 운영체제의 글꼴에 의존하지 않도록 한다.
        Font pieceFont = new Font(
                Font.DIALOG,
                Font.PLAIN,
                Math.max(12, (int) (size * 0.78))
        );
        g2.setFont(pieceFont);
        FontMetrics metrics = g2.getFontMetrics();
        java.awt.Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square square = squareAt(row, col);
                Piece piece = position.at(square);

                if (piece == null) {
                    continue;
                }

                String symbol = pieceSymbol(piece);
                Point center = squareCenter(x, y, size, square);
                int textX = center.x - metrics.stringWidth(symbol) / 2;
                int textY = center.y
                        + (metrics.getAscent() - metrics.getDescent()) / 2;

                // 백 기물은 밝은 채움 + 짙은 외곽선, 흑 기물은 짙은 채움 + 밝은 외곽선.
                Color fill = piece.side() == Side.WHITE
                        ? new Color(0xFFF4E5)
                        : new Color(0x211810);
                Color outline = piece.side() == Side.WHITE
                        ? new Color(0x3A281B)
                        : new Color(0xF4E9D8);

                int outlineOffset = Math.max(1, size / 90);
                g2.setColor(outline);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        g2.drawString(
                                symbol,
                                textX + dx * outlineOffset,
                                textY + dy * outlineOffset
                        );
                    }
                }

                g2.setColor(fill);
                g2.drawString(symbol, textX, textY);
            }
        }

        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }


    // =========================================
    // 기물 문자 반환
    // =========================================

    private String pieceSymbol(Piece piece) {

        // 채워진 체스 기호를 사용하고, 진영별 색상과 외곽선으로 백·흑을 구분한다.
        return switch (piece.type()) {
            case KING -> "♚";
            case QUEEN -> "♛";
            case ROOK -> "♜";
            case BISHOP -> "♝";
            case KNIGHT -> "♞";
            case PAWN -> "♟";
        };
    }


    // =========================================
    // 화살표 그리기
    // =========================================

    private void drawArrows(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        for (Arrow arrow : arrows) {

            Point from = squareCenter(
                    x,
                    y,
                    size,
                    arrow.from()
            );

            Point to = squareCenter(
                    x,
                    y,
                    size,
                    arrow.to()
            );

            Color color = switch (arrow.kind()) {

                case SOLUTION -> Theme.ACCENT;

                case THREAT -> Theme.BAD;

                case AI_HINT -> Theme.INFO;
            };

            g2.setColor(color);

            g2.setStroke(
                    new BasicStroke(
                            Math.max(2, size / 18f),
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            g2.drawLine(
                    from.x,
                    from.y,
                    to.x,
                    to.y
            );

            double angle = Math.atan2(
                    to.y - from.y,
                    to.x - from.x
            );

            int arrowSize = size / 4;

            int x1 = (int) (
                    to.x
                            - arrowSize
                            * Math.cos(angle - Math.PI / 6)
            );

            int y1 = (int) (
                    to.y
                            - arrowSize
                            * Math.sin(angle - Math.PI / 6)
            );

            int x2 = (int) (
                    to.x
                            - arrowSize
                            * Math.cos(angle + Math.PI / 6)
            );

            int y2 = (int) (
                    to.y
                            - arrowSize
                            * Math.sin(angle + Math.PI / 6)
            );

            Polygon head = new Polygon();

            head.addPoint(to.x, to.y);

            head.addPoint(x1, y1);

            head.addPoint(x2, y2);

            g2.fillPolygon(head);
        }
    }


    // =========================================
    // 좌표 표시
    // =========================================

    private void drawCoordinates(
            Graphics2D g2,
            int x,
            int y,
            int size
    ) {

        g2.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        g2.setColor(Theme.MUTED);

        for (int i = 0; i < 8; i++) {

            int file = flipped ? 7 - i : i;

            int rank = flipped ? i + 1 : 8 - i;

            String fileText = String.valueOf(
                    (char) ('a' + file)
            );

            String rankText = String.valueOf(rank);

            FontMetrics metrics =
                    g2.getFontMetrics();

            // 아래쪽 파일 좌표
            int fileX =
                    x + i * size
                            + size
                            - metrics.stringWidth(fileText) - 4;

            int fileY =
                    y + size * 8 + 17;

            g2.drawString(
                    fileText,
                    fileX,
                    fileY
            );

            // 왼쪽 랭크 좌표
            int rankX = x - 17;

            int rankY =
                    y + i * size + 15;

            g2.drawString(
                    rankText,
                    rankX,
                    rankY
            );
        }
    }


    // =========================================
    // 보드 좌표 계산
    // =========================================

    private Square squareAt(int row, int col) {

        int file = flipped ? 7 - col : col;

        int rank = flipped ? row : 7 - row;

        return new Square(file, rank);
    }


    private Point squarePoint(
            int x,
            int y,
            int size,
            Square square
    ) {

        int col = flipped
                ? 7 - square.file()
                : square.file();

        int row = flipped
                ? square.rank()
                : 7 - square.rank();

        return new Point(
                x + col * size,
                y + row * size
        );
    }


    private Point squareCenter(
            int x,
            int y,
            int size,
            Square square
    ) {

        Point point = squarePoint(
                x,
                y,
                size,
                square
        );

        return new Point(
                point.x + size / 2,
                point.y + size / 2
        );
    }


    // =========================================
    // 마우스 좌표 -> 체스판 좌표
    // =========================================

    private Optional<Square> getSquareAt(
            int mouseX,
            int mouseY
    ) {

        BoardBounds bounds = calculateBounds();

        int x = bounds.x();

        int y = bounds.y();

        int size = bounds.cellSize();

        int boardSize = size * 8;

        if (
                mouseX < x
                        || mouseY < y
                        || mouseX >= x + boardSize
                        || mouseY >= y + boardSize
        ) {

            return Optional.empty();
        }

        int col = (mouseX - x) / size;

        int row = (mouseY - y) / size;

        return Optional.of(
                squareAt(row, col)
        );
    }


    // =========================================
    // 화면에서 제거될 때 타이머 정리
    // =========================================

    @Override
    public void removeNotify() {

        if (wrongTimer != null) {

            wrongTimer.stop();

            wrongTimer = null;
        }

        super.removeNotify();
    }
}
