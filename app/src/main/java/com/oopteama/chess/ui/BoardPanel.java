package com.oopteama.chess.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
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

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;


public class BoardPanel extends JPanel {

    // 표시 상태
    public enum Mark {
        LAST_MOVE,
        SELECTED,
        TARGET,
        HINT,
        SOLUTION,
        WRONG
    }

    // 화살표 종류
    public enum ArrowKind {
        SOLUTION,
        THREAT,
        AI_HINT
    }

    // 화살표 정보
    public record Arrow(
        Square from,
        Square to,
        ArrowKind kind
    ) { }


    // 내부 데이터
    private Position position = Position.initial();

    private List<Move> legalMoves = List.of();

    private Optional<InputMode> inputMode = Optional.empty();

    private Optional<Square> checkSquare = Optional.empty();

    private boolean flipped = false;

    private Square selectedSquare;

    private Square wrongSquare;

    private javax.swing.Timer wrongTimer;

    private final Map<Square, EnumSet<Mark>> marks =
        new HashMap<>();

    private List<Arrow> arrows = List.of();


    // 이벤트 리스너
    private final List<Consumer<Move>> moveListeners =
        new ArrayList<>();

    private final List<Consumer<Square>> squareListeners =
        new ArrayList<>();


    // 생성자
    public BoardPanel() {

        setBackground(Theme.WINDOW);

        setOpaque(true);

        setPreferredSize(new Dimension(640, 640));

        setMinimumSize(new Dimension(400, 400));


        // 체스판 클릭 이벤트
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                handleClick(e.getX(), e.getY());

            }
        });
    }


    // 체스판 데이터 설정
    public void setPosition(Position snapshot) {

        this.position = Objects.requireNonNull(snapshot).copy();

        // 국면이 바뀌면 이전 국면의 합법 수는 더 이상 유효하지 않음
        legalMoves = List.of();

        selectedSquare = null;

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

        if (mode.isEmpty() || mode.get() == InputMode.SQUARE) {
            selectedSquare = null;
        }

        repaint();
    }


    public void setFlipped(boolean flipped) {

        this.flipped = flipped;

        repaint();
    }


    // 칸 표시 관리
    public void mark(Collection<Square> squares, Mark mark) {

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
        }

        repaint();
    }


    // 오답 표시
    public void flashWrong(Square square) {

        if (wrongTimer != null) {
            wrongTimer.stop();
        }

        wrongSquare = square;

        wrongTimer = new javax.swing.Timer(400, e -> {

            wrongSquare = null;

            repaint();

        });

        wrongTimer.setRepeats(false);

        wrongTimer.start();

        repaint();
    }


    // 화살표 설정
    public void setArrows(List<Arrow> arrows) {

        this.arrows = List.copyOf(arrows);

        repaint();
    }


    // 이벤트 리스너 등록
    public void addMoveListener(Consumer<Move> listener) {

        addIdentity(moveListeners, listener);
    }


    public void addSquareListener(Consumer<Square> listener) {

        addIdentity(squareListeners, listener);
    }


    public void removeMoveListener(Consumer<Move> listener) {

        removeIdentity(moveListeners, listener);
    }


    public void removeSquareListener(Consumer<Square> listener) {

        removeIdentity(squareListeners, listener);
    }


    // 같은 객체의 리스너는 한 번만 등록
    private <T> void addIdentity(List<T> list, T listener) {

        Objects.requireNonNull(listener);

        for (T existing : list) {

            if (existing == listener) {
                return;
            }
        }

        list.add(listener);
    }


    // 동일한 객체의 리스너만 제거
    private <T> void removeIdentity(List<T> list, T listener) {

        for (int i = 0; i < list.size(); i++) {

            if (list.get(i) == listener) {

                list.remove(i);

                return;
            }
        }
    }


    // 클릭 이벤트 처리
    private void handleClick(int x, int y) {

        // 입력 모드가 비어 있으면 입력 잠금
        if (inputMode.isEmpty()) {
            return;
        }

        Optional<Square> clicked = getSquareAt(x, y);

        if (clicked.isEmpty()) {
            return;
        }

        Square square = clicked.get();


        // 칸 선택 모드
        if (inputMode.get() == InputMode.SQUARE) {

            for (Consumer<Square> listener :
                    List.copyOf(squareListeners)) {

                listener.accept(square);
            }

            return;
        }


        // 일반 이동 모드
        handleMoveClick(square);
    }


    // 이동 처리
    private void handleMoveClick(Square square) {

        // 아직 선택한 칸이 없는 경우
        if (selectedSquare == null) {

            selectSquare(square);

            return;
        }


        // 같은 칸을 다시 클릭하면 선택 해제
        if (selectedSquare.equals(square)) {

            selectedSquare = null;

            repaint();

            return;
        }


        // 선택한 칸에서 클릭한 칸으로 이동 가능한 수 검색
        List<Move> candidates = legalMoves.stream()
            .filter(move ->
                move.from().equals(selectedSquare)
                    && move.to().equals(square)
            )
            .toList();


        if (!candidates.isEmpty()) {

            Move selectedMove = candidates.get(0);


            // 승진 수라면 승진할 기물을 선택하는 대화상자 표시
            if (selectedMove.isPromotion()) {

                selectedMove = choosePromotion(candidates);

                if (selectedMove == null) {
                    return;
                }
            }


            // 이동 이벤트 전달
            for (Consumer<Move> listener :
                    List.copyOf(moveListeners)) {

                listener.accept(selectedMove);
            }

            selectedSquare = null;

            repaint();

            return;
        }


        // 다른 기물을 다시 선택할 수 있도록 처리
        selectSquare(square);
    }


    // 기물 선택
    private void selectSquare(Square square) {

        boolean canMove = legalMoves.stream()
            .anyMatch(move -> move.from().equals(square));

        selectedSquare = canMove ? square : null;

        repaint();
    }


    // 승진 선택
    private Move choosePromotion(List<Move> candidates) {

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


    // 화면 그리기
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );


        int cellSize = Math.max(
            1,
            Math.min(getWidth(), getHeight()) / 8
        );


        int boardSize = cellSize * 8;

        int boardX = (getWidth() - boardSize) / 2;

        int boardY = (getHeight() - boardSize) / 2;


        // 체스판 배경과 칸
        drawSquares(g2, boardX, boardY, cellSize);

        // 칸 표시
        drawMarks(g2, boardX, boardY, cellSize);

        // 합법 수 표시
        drawLegalTargets(g2, boardX, boardY, cellSize);

        // 화살표 표시
        drawArrows(g2, boardX, boardY, cellSize);

        // 기물 표시
        drawPieces(g2, boardX, boardY, cellSize);

        // 좌표 표시
        drawCoordinates(g2, boardX, boardY, cellSize);


        g2.dispose();
    }


    // 체스판 칸 그리기
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


    // 칸 강조 표시
    private void drawMarks(
        Graphics2D g2,
        int x,
        int y,
        int size
    ) {

        for (Map.Entry<Square, EnumSet<Mark>> entry :
                marks.entrySet()) {

            Square square = entry.getKey();

            for (Mark mark : entry.getValue()) {

                fillSquare(
                    g2,
                    x,
                    y,
                    size,
                    square,
                    markColor(mark),
                    0.45f
                );
            }
        }


        if (checkSquare.isPresent()) {

            fillSquare(
                g2,
                x,
                y,
                size,
                checkSquare.get(),
                Theme.BAD,
                0.55f
            );
        }


        if (wrongSquare != null) {

            fillSquare(
                g2,
                x,
                y,
                size,
                wrongSquare,
                Theme.BAD,
                0.65f
            );
        }


        if (selectedSquare != null) {

            fillSquare(
                g2,
                x,
                y,
                size,
                selectedSquare,
                Theme.ACCENT,
                0.55f
            );
        }
    }


    // 강조 색상
    private Color markColor(Mark mark) {

        return switch (mark) {

            case LAST_MOVE -> Theme.ACCENT;
            case SELECTED -> Theme.INFO;
            case TARGET -> Theme.OK;
            case HINT -> Theme.ACCENT;
            case SOLUTION -> Theme.OK;
            case WRONG -> Theme.BAD;
        };
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

        Point point = squarePoint(x, y, size, square);

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


    // 합법 수 표시
    private void drawLegalTargets(
        Graphics2D g2,
        int x,
        int y,
        int size
    ) {

        if (selectedSquare == null || inputMode.isEmpty()) {
            return;
        }

        if (inputMode.get() != InputMode.MOVE) {
            return;
        }


        for (Move move : legalMoves) {

            if (!move.from().equals(selectedSquare)) {
                continue;
            }

            Point center = squareCenter(
                x,
                y,
                size,
                move.to()
            );

            g2.setColor(
                new Color(30, 30, 30, 110)
            );

            int diameter = size / 4;

            g2.fillOval(
                center.x - diameter / 2,
                center.y - diameter / 2,
                diameter,
                diameter
            );
        }
    }


    // 기물 그리기
    private void drawPieces(
        Graphics2D g2,
        int x,
        int y,
        int size
    ) {

        for (int row = 0; row < 8; row++) {

            for (int col = 0; col < 8; col++) {

                Square square = squareAt(row, col);

                Piece piece = position.at(square);

                if (piece == null) {
                    continue;
                }


                String symbol = pieceSymbol(piece);

                Point center = squareCenter(
                    x,
                    y,
                    size,
                    square
                );


                Font pieceFont = new Font(
                    "Segoe UI Symbol",
                    Font.PLAIN,
                    (int) (size * 0.78)
                );

                g2.setFont(pieceFont);


                FontMetrics metrics = g2.getFontMetrics();

                int textX =
                    center.x - metrics.stringWidth(symbol) / 2;

                int textY =
                    center.y +
                    (metrics.getAscent() - metrics.getDescent()) / 2;


                g2.setColor(
                    piece.side() == Side.WHITE
                        ? new Color(0xFFF4E5)
                        : new Color(0x211810)
                );


                g2.drawString(
                    symbol,
                    textX,
                    textY
                );
            }
        }
    }


    // 기물 종류에 맞는 문자 반환
    private String pieceSymbol(Piece piece) {

        if (piece.side() == Side.WHITE) {

            return switch (piece.type()) {

                case KING -> "♔";
                case QUEEN -> "♕";
                case ROOK -> "♖";
                case BISHOP -> "♗";
                case KNIGHT -> "♘";
                case PAWN -> "♙";
            };
        }

        return switch (piece.type()) {

            case KING -> "♚";
            case QUEEN -> "♛";
            case ROOK -> "♜";
            case BISHOP -> "♝";
            case KNIGHT -> "♞";
            case PAWN -> "♟";
        };
    }


    // 화살표 그리기
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
                new BasicStroke(4)
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
                to.x - arrowSize * Math.cos(angle - Math.PI / 6)
            );

            int y1 = (int) (
                to.y - arrowSize * Math.sin(angle - Math.PI / 6)
            );

            int x2 = (int) (
                to.x - arrowSize * Math.cos(angle + Math.PI / 6)
            );

            int y2 = (int) (
                to.y - arrowSize * Math.sin(angle + Math.PI / 6)
            );


            Polygon head = new Polygon();

            head.addPoint(to.x, to.y);

            head.addPoint(x1, y1);

            head.addPoint(x2, y2);

            g2.fillPolygon(head);
        }
    }


    // 좌표 표시
    private void drawCoordinates(
        Graphics2D g2,
        int x,
        int y,
        int size
    ) {

        g2.setFont(
            Theme.body(Theme.FONT_12, Font.BOLD)
        );

        g2.setColor(Theme.MUTED);


        for (int i = 0; i < 8; i++) {

            int file = flipped ? 7 - i : i;

            int rank = flipped ? i + 1 : 8 - i;


            String fileText = String.valueOf(
                (char) ('a' + file)
            );

            String rankText = String.valueOf(rank);


            g2.drawString(
                fileText,
                x + i * size + size - 12,
                y + size * 8 + 16
            );

            g2.drawString(
                rankText,
                x - 17,
                y + i * size + 14
            );
        }
    }


    // 좌표 계산
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


    // 마우스 좌표를 체스판 좌표로 변환
    private Optional<Square> getSquareAt(int mouseX, int mouseY) {

        int size = Math.max(

            1,

            Math.min(
                (getWidth() - 48) / 8,
                (getHeight() - 48) / 8
            )
        );

        int boardSize = size * 8;

        int boardX = (getWidth() - boardSize) / 2;

        int boardY = (getHeight() - boardSize) / 2;


        if (
            mouseX < boardX ||
            mouseY < boardY ||
            mouseX >= boardX + boardSize ||
            mouseY >= boardY + boardSize
        ) {

            return Optional.empty();
        }


        int col = (mouseX - boardX) / size;

        int row = (mouseY - boardY) / size;


        return Optional.of(
            squareAt(row, col)
        );
    }
}
