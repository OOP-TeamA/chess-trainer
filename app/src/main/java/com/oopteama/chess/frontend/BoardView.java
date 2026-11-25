package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.MoveAnalyzer;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece;
import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.engine.Piece.Type;
import com.oopteama.chess.backend.service.Settings;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * 모든 화면(레슨·퍼즐·AI 대전·로컬 대전)이 공용으로 사용하는 체스판 컴포넌트입니다.
 * 클릭 이동, 드래그 앤 드롭, 벡터 기물 렌더링, 합법 수 표시, 힌트 화살표, 프로모션 선택,
 * 기물 이동 애니메이션을 지원합니다.
 */
public final class BoardView extends JPanel {

    /** 기물이 출발 칸에서 도착 칸까지 미끄러지는 시간(ms). */
    private static final int MOVE_ANIMATION_MS = 250;

    /** 애니메이션 중 미끄러지는 기물 하나 (캐슬링은 킹과 룩 두 개). */
    private record Slide(Piece piece, int from, int to) {
    }

    /**
     * 한 수의 이동 애니메이션. 재생 중에는 수를 둔 뒤 국면(after)을 그리되 미끄러지는 기물의
     * 도착 칸은 비워 두고, 잡힌 기물은 제자리에서 서서히 사라지게 합니다.
     */
    private record MoveAnimation(Move move, Board after, List<Slide> slides, Piece captured, int capturedSquare) {
        boolean hides(int sq) {
            for (Slide slide : slides) {
                if (slide.to() == sq) {
                    return true;
                }
            }
            return false;
        }
    }

    private final Settings settings;
    private final BiFunction<Board, Move, MoveAnalyzer.Evaluation> movePreviewer;
    private Board board = Board.initial();
    private List<Move> legalMoves = board.legalMoves();
    private boolean flipped;
    private boolean interactive = true;
    private Side allowedSide; // null이면 현재 board.turn() 허용

    private Move lastMove;
    private Move arrowMove;
    private Color arrowColor = Theme.BOARD_ACCENT;
    private List<Integer> highlightSquares = List.of();
    private int wrongSquare = -1;
    private Timer wrongTimer;

    private final Map<Move, MoveAnalyzer.Evaluation> evalCache = new HashMap<>();

    private int selectedSquare = -1;
    private int hoverSquare = -1;
    private MoveAnalyzer.Evaluation hoverEval;
    private int dragSquare = -1;
    private Point dragPoint;
    private boolean dragging;
    private boolean deselectOnRelease; // 이미 선택한 기물을 다시 누른 뒤 끌지 않고 놓으면 선택 해제
    private Consumer<Move> moveHandler;
    private BiConsumer<Integer, Integer> illegalMoveHandler;

    // 맨 앞이 재생 중인 수, 그 뒤는 대기 중인 수 (예: 내 수가 끝나기 전에 도착한 AI 응수)
    private final Deque<MoveAnimation> animations = new ArrayDeque<>();
    private final Timer animationTimer = new Timer(15, e -> onAnimationTick());
    private long animationStartNanos;
    private Move droppedMove; // 드래그로 놓은 수는 이미 도착 칸에 있으므로 다시 미끄러지지 않음

    public BoardView(Settings settings, BiFunction<Board, Move, MoveAnalyzer.Evaluation> movePreviewer) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.movePreviewer = movePreviewer;
        setBackground(Theme.BG);
        setOpaque(true);
        setPreferredSize(new Dimension(520, 520));
        setMinimumSize(new Dimension(320, 320));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePress(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragSquare >= 0) {
                    clearHover();
                    dragPoint = e.getPoint();
                    dragging = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleRelease(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handleHover(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverSquare >= 0 || hoverEval != null) {
                    clearHover();
                    repaint();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ---- 공개 API ----

    /**
     * 표시할 국면을 바꿉니다. 직전 국면에서 합법 수 한 개로 도달한 국면이면 그 수를 애니메이션으로 보여 주고,
     * 무르기·새 대국·다음 퍼즐처럼 한 수로 설명되지 않는 변화는 바로 보여 줍니다.
     */
    public void setBoard(Board board) {
        Board previous = this.board;
        List<Move> previousLegalMoves = this.legalMoves;
        this.board = Objects.requireNonNull(board, "board");
        this.legalMoves = board.legalMoves();
        this.selectedSquare = -1;
        this.dragSquare = -1;
        this.dragging = false;
        this.evalCache.clear();
        clearHover();
        queueMoveAnimation(previous, previousLegalMoves, board);
        repaint();
    }

    public void setLastMove(Move move) {
        this.lastMove = move;
        repaint();
    }

    public void setArrow(Move move, Color color) {
        this.arrowMove = move;
        this.arrowColor = color != null ? color : Theme.BOARD_ACCENT;
        repaint();
    }

    public void setHighlightSquares(List<Integer> squares) {
        this.highlightSquares = squares == null ? List.of() : List.copyOf(squares);
        repaint();
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) {
            selectedSquare = -1;
            dragSquare = -1;
            dragging = false;
            clearHover();
        }
        repaint();
    }

    public void setAllowedSide(Side side) {
        this.allowedSide = side;
    }

    public void setMoveHandler(Consumer<Move> moveHandler) {
        this.moveHandler = moveHandler;
    }

    void setIllegalMoveHandler(BiConsumer<Integer, Integer> handler) {
        illegalMoveHandler = handler;
    }

    public void flashWrong(int square) {
        this.wrongSquare = square;
        repaint();
        if (wrongTimer != null) {
            wrongTimer.stop();
        }
        wrongTimer = new Timer(450, e -> {
            wrongSquare = -1;
            repaint();
        });
        wrongTimer.setRepeats(false);
        wrongTimer.start();
    }

    // ---- 입력 처리 ----

    private boolean canInteractNow() {
        return interactive
                && moveHandler != null
                && (allowedSide == null || board.turn() == allowedSide);
    }

    private void clearHover() {
        hoverSquare = -1;
        hoverEval = null;
    }

    private void handleHover(Point p) {
        if (!canInteractNow() || selectedSquare < 0 || dragging || !settings.showMovePreview()) {
            if (hoverEval != null) {
                clearHover();
                repaint();
            }
            return;
        }
        int sq = squareAt(p);
        if (sq == hoverSquare) {
            return;
        }
        hoverSquare = sq;
        hoverEval = null;
        if (sq >= 0) {
            List<Move> candidates = findMatchingMoves(selectedSquare, sq);
            if (!candidates.isEmpty()) {
                Move previewMove = candidates.stream()
                        .filter(m -> m.promotion() == Type.QUEEN)
                        .findFirst()
                        .orElse(candidates.getFirst());
                if (movePreviewer != null) {
                    hoverEval = evalCache.computeIfAbsent(previewMove, m -> movePreviewer.apply(board, m));
                }
            }
        }
        repaint();
    }

    private void handlePress(Point p) {
        if (!canInteractNow()) {
            return;
        }
        clearHover();
        deselectOnRelease = false;
        int sq = squareAt(p);
        if (sq < 0) {
            selectedSquare = -1;
            repaint();
            return;
        }
        boolean pressedSelected = sq == selectedSquare;
        if (selectedSquare >= 0 && tryExecuteMove(selectedSquare, sq, false)) {
            return;
        }
        if (hasLegalMovesFrom(sq)) {
            selectedSquare = sq;
            dragSquare = sq;
            dragPoint = p;
            dragging = false;
            deselectOnRelease = pressedSelected;
        } else {
            selectedSquare = -1;
            dragSquare = -1;
        }
        repaint();
    }

    private void handleRelease(Point p) {
        if (!dragging || dragSquare < 0) {
            if (deselectOnRelease) {
                selectedSquare = -1;
                repaint();
            }
            deselectOnRelease = false;
            dragSquare = -1;
            dragging = false;
            return;
        }
        deselectOnRelease = false;
        int from = dragSquare;
        dragSquare = -1;
        dragging = false;
        int to = squareAt(p);
        if (to >= 0 && to != from) {
            tryExecuteMove(from, to, true);
        }
        repaint();
    }

    private boolean tryExecuteMove(int from, int to, boolean dropped) {
        List<Move> candidates = findMatchingMoves(from, to);
        if (candidates.isEmpty()) {
            // 클릭으로 아군 기물을 누른 것은 이동 시도가 아니라 선택 변경으로 봄 (드래그로 놓은 경우만 안내)
            boolean ownPiece = board.pieceAt(to).is(board.turn());
            if (illegalMoveHandler != null && from != to && (dropped || !ownPiece)) {
                // 선택은 유지해 안내 카드를 닫은 뒤에도 이동 가능 칸이 계속 보이게 함
                dragSquare = -1;
                illegalMoveHandler.accept(from, to);
                repaint();
                return true;
            }
            return false;
        }
        Move chosen;
        if (candidates.size() == 1) {
            chosen = candidates.getFirst();
        } else {
            Type promo = askPromotion(board.turn());
            if (promo == null) {
                selectedSquare = -1;
                repaint();
                return true;
            }
            chosen = candidates.stream()
                    .filter(m -> m.promotion() == promo)
                    .findFirst()
                    .orElse(candidates.getFirst());
        }
        selectedSquare = -1;
        droppedMove = dropped ? chosen : null;
        try {
            moveHandler.accept(chosen);
        } finally {
            droppedMove = null;
        }
        return true;
    }

    private List<Move> findMatchingMoves(int from, int to) {
        List<Move> matches = new ArrayList<>();
        for (Move m : legalMoves) {
            if (m.from() == from && m.to() == to) {
                matches.add(m);
            }
        }
        // 편의 기능: 킹 선택 후 아군 룩을 클릭해도 캐슬링으로 인식
        if (matches.isEmpty() && board.pieceAt(from).type() == Type.KING && board.pieceAt(to).is(board.turn(), Type.ROOK)) {
            int target = to > from ? from + 2 : from - 2;
            for (Move m : legalMoves) {
                if (m.from() == from && m.to() == target) {
                    matches.add(m);
                }
            }
        }
        return matches;
    }

    private boolean hasLegalMovesFrom(int sq) {
        for (Move m : legalMoves) {
            if (m.from() == sq) {
                return true;
            }
        }
        return false;
    }

    private Type askPromotion(Side side) {
        Type[] options = {Type.QUEEN, Type.ROOK, Type.BISHOP, Type.KNIGHT};
        String[] labels = {"퀸 (Q)", "룩 (R)", "비숍 (B)", "나이트 (N)"};
        int choice = JOptionPane.showOptionDialog(
                this,
                side.label() + " 폰이 승진할 기물을 선택하세요.",
                "폰 승진",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                labels,
                labels[0]
        );
        return choice >= 0 && choice < options.length ? options[choice] : null;
    }

    // ---- 기물 이동 애니메이션 ----

    private void queueMoveAnimation(Board previous, List<Move> previousLegalMoves, Board next) {
        // 같은 국면을 다시 그리는 경우(힌트 표시·화면 갱신 등)에는 재생 중인 애니메이션을 그대로 둠
        if (previous == next || previous.toFen().equals(next.toFen())) {
            return;
        }
        Move move = findMoveBetween(previous, previousLegalMoves, next);
        if (move == null || move.equals(droppedMove)) {
            stopAnimations();
            return;
        }
        if (animations.size() >= 2) {
            // 수가 연달아 밀려 들어오면 대기 중인 애니메이션은 건너뛰고 마지막 수만 보여 줌
            animations.clear();
        }
        animations.addLast(buildAnimation(previous, move, next));
        if (animations.size() == 1) {
            animationStartNanos = System.nanoTime();
            animationTimer.start();
        }
    }

    /**
     * before 국면의 합법 수 중 after 국면을 만드는 수를 찾습니다. 한 수로 설명되지 않으면 null입니다.
     */
    private static Move findMoveBetween(Board before, List<Move> beforeLegalMoves, Board after) {
        String afterFen = null;
        for (Move move : beforeLegalMoves) {
            Piece mover = before.pieceAt(move.from());
            Piece landed = move.promotion() == null ? mover : Piece.of(mover.side(), move.promotion());
            if (after.pieceAt(move.to()) != landed || !after.pieceAt(move.from()).isEmpty()) {
                continue;
            }
            if (afterFen == null) {
                afterFen = after.toFen();
            }
            if (before.applyMove(move).toFen().equals(afterFen)) {
                return move;
            }
        }
        return null;
    }

    private static MoveAnimation buildAnimation(Board before, Move move, Board after) {
        List<Slide> slides = new ArrayList<>(2);
        // 승진은 폰이 미끄러진 뒤 도착 칸에서 승진한 기물로 바뀜
        slides.add(new Slide(before.pieceAt(move.from()), move.from(), move.to()));
        Piece captured = Piece.EMPTY;
        int capturedSquare = -1;
        switch (before.classifyMove(move)) {
            case CASTLE_SHORT -> addRookSlide(slides, before, move.from() + 3, move.from() + 1);
            case CASTLE_LONG -> addRookSlide(slides, before, move.from() - 4, move.from() - 1);
            case CAPTURE -> {
                capturedSquare = move.to();
                captured = before.pieceAt(capturedSquare);
            }
            case EN_PASSANT -> {
                capturedSquare = move.to() + (before.turn() == Side.WHITE ? -8 : 8);
                captured = before.pieceAt(capturedSquare);
            }
            default -> {
            }
        }
        return new MoveAnimation(move, after, List.copyOf(slides), captured, capturedSquare);
    }

    private static void addRookSlide(List<Slide> slides, Board before, int rookFrom, int rookTo) {
        Piece rook = before.pieceAt(rookFrom);
        if (!rook.isEmpty()) {
            slides.add(new Slide(rook, rookFrom, rookTo));
        }
    }

    private void onAnimationTick() {
        if (!animations.isEmpty()
                && System.nanoTime() - animationStartNanos >= MOVE_ANIMATION_MS * 1_000_000L) {
            animations.removeFirst();
            animationStartNanos = System.nanoTime(); // 대기 중이던 다음 수를 이어서 재생
        }
        if (animations.isEmpty()) {
            animationTimer.stop();
        }
        repaint();
    }

    private void stopAnimations() {
        animations.clear();
        animationTimer.stop();
    }

    /** 재생 진행도 0..1 (ease-out cubic: 빠르게 출발해 도착 칸에서 부드럽게 멈춤). */
    private double animationProgress() {
        double t = Math.min(1.0, (System.nanoTime() - animationStartNanos) / (MOVE_ANIMATION_MS * 1_000_000.0));
        double rest = 1.0 - t;
        return 1.0 - rest * rest * rest;
    }

    /** 재생 중인 수와 대기 중인 수 (재생 순서). */
    List<Move> pendingAnimationMoves() {
        return animations.stream().map(MoveAnimation::move).toList();
    }

    // ---- 좌표 변환 ----

    private int[] boardMetrics() {
        int pad = 10;
        int size = Math.max(64, Math.min(getWidth(), getHeight()) - pad * 2);
        int cell = size / 8;
        int boardPx = cell * 8;
        int x0 = (getWidth() - boardPx) / 2;
        int y0 = (getHeight() - boardPx) / 2;
        return new int[]{x0, y0, cell, boardPx};
    }

    private int squareAt(Point p) {
        int[] m = boardMetrics();
        int col = (p.x - m[0]) / m[2];
        int row = (p.y - m[1]) / m[2];
        if (p.x < m[0] || p.y < m[1] || col < 0 || col >= 8 || row < 0 || row >= 8) {
            return -1;
        }
        int file = flipped ? 7 - col : col;
        int rank = flipped ? row : 7 - row;
        return Board.square(file, rank);
    }

    private int visualCol(int sq) {
        return flipped ? 7 - Board.file(sq) : Board.file(sq);
    }

    private int visualRow(int sq) {
        return flipped ? Board.rank(sq) : 7 - Board.rank(sq);
    }

    // ---- 렌더링 ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int[] m = boardMetrics();
        int x0 = m[0], y0 = m[1], cell = m[2], boardPx = m[3];

        // 외곽 프레임
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x0 - 6, y0 - 6, boardPx + 12, boardPx + 12, 12, 12);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(x0 - 6, y0 - 6, boardPx + 11, boardPx + 11, 12, 12);

        Color light = new Color(settings.boardTheme().lightRgb());
        Color dark = new Color(settings.boardTheme().darkRgb());

        // 이동 애니메이션 중에는 재생 중인 수 기준으로 그림 (뒤에 대기 중인 수가 먼저 보이지 않도록)
        MoveAnimation animation = animations.peekFirst();
        Board shown = animation != null ? animation.after() : board;
        Move shownLastMove = animation != null ? animation.move() : lastMove;
        int checkedKingSq = shown.isInCheck() ? shown.findKing(shown.turn()) : -1;

        // 1. 64칸 배경 + 강조 오버레이
        for (int sq = 0; sq < 64; sq++) {
            int x = x0 + visualCol(sq) * cell;
            int y = y0 + visualRow(sq) * cell;
            boolean isLight = ((Board.file(sq) + Board.rank(sq)) & 1) == 1;
            g2.setColor(isLight ? light : dark);
            g2.fillRect(x, y, cell, cell);

            if (settings.highlightLastMove() && shownLastMove != null
                    && (sq == shownLastMove.from() || sq == shownLastMove.to())) {
                g2.setColor(new Color(245, 200, 66, 105));
                g2.fillRect(x, y, cell, cell);
            }
            if (highlightSquares.contains(sq)) {
                g2.setColor(new Color(52, 211, 153, 85));
                g2.fillRect(x, y, cell, cell);
                g2.setColor(new Color(52, 211, 153, 190));
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawRect(x + 2, y + 2, cell - 4, cell - 4);
            }
            if (sq == selectedSquare) {
                g2.setColor(new Color(46, 184, 114, 130));
                g2.fillRect(x, y, cell, cell);
            }
            if (sq == checkedKingSq) {
                g2.setPaint(new RadialGradientPaint(
                        x + cell / 2f, y + cell / 2f, cell * 0.55f,
                        new float[]{0f, 1f},
                        new Color[]{new Color(239, 68, 68, 220), new Color(239, 68, 68, 0)}
                ));
                g2.fillRect(x, y, cell, cell);
            }
            if (sq == wrongSquare) {
                g2.setColor(new Color(248, 113, 113, 165));
                g2.fillRect(x, y, cell, cell);
            }
        }

        // 2. 좌표 표기 (테두리 칸 모서리 안쪽)
        if (settings.showCoords()) {
            Font coordFont = Theme.bold(Math.max(10f, cell * 0.17f));
            g2.setFont(coordFont);
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < 8; i++) {
                int file = flipped ? 7 - i : i;
                int rank = flipped ? i : 7 - i;
                // 왼쪽 열 숫자
                boolean leftLight = ((Board.file(Board.square(flipped ? 7 : 0, rank)) + rank) & 1) == 1;
                g2.setColor(leftLight ? dark : light);
                g2.drawString(String.valueOf(rank + 1), x0 + 4, y0 + i * cell + fm.getAscent() + 2);
                // 아래쪽 행 알파벳
                boolean botLight = ((file + (flipped ? 7 : 0)) & 1) == 1;
                g2.setColor(botLight ? dark : light);
                String fLabel = String.valueOf((char) ('a' + file));
                g2.drawString(fLabel, x0 + (i + 1) * cell - fm.stringWidth(fLabel) - 4, y0 + boardPx - 4);
            }
        }

        // 3. 합법 수 점/링 표시
        if (settings.showLegalMoves() && selectedSquare >= 0) {
            for (Move move : legalMoves) {
                if (move.from() != selectedSquare) {
                    continue;
                }
                int tx = x0 + visualCol(move.to()) * cell;
                int ty = y0 + visualRow(move.to()) * cell;
                boolean capture = !board.pieceAt(move.to()).isEmpty()
                        || (board.pieceAt(move.from()).type() == Type.PAWN && move.to() == board.enPassant());
                g2.setColor(new Color(18, 24, 34, 95));
                if (capture) {
                    g2.setStroke(new BasicStroke(Math.max(3f, cell * 0.08f)));
                    g2.draw(new Ellipse2D.Double(tx + cell * 0.09, ty + cell * 0.09, cell * 0.82, cell * 0.82));
                } else {
                    double r = cell * 0.16;
                    g2.fill(new Ellipse2D.Double(tx + cell / 2.0 - r, ty + cell / 2.0 - r, r * 2, r * 2));
                }
            }
        }

        // 4. 기물 렌더링 (드래그 중인 기물은 맨 위에, 호버 프리뷰 관련 칸은 반투명 처리)
        MoveAnalyzer.Evaluation preview = (settings.showMovePreview() && !dragging) ? hoverEval : null;
        int dimRookFrom = -1;
        int dimEpCaptured = -1;
        if (preview != null) {
            Move.Kind kind = board.classifyMove(preview.move());
            if (kind == Move.Kind.CASTLE_SHORT) {
                dimRookFrom = preview.move().from() + 3;
            } else if (kind == Move.Kind.CASTLE_LONG) {
                dimRookFrom = preview.move().from() - 4;
            } else if (kind == Move.Kind.EN_PASSANT) {
                dimEpCaptured = preview.move().to() - (board.turn() == Side.WHITE ? 8 : -8);
            }
        }
        for (int sq = 0; sq < 64; sq++) {
            if ((dragging && sq == dragSquare) || (animation != null && animation.hides(sq))) {
                continue;
            }
            Piece piece = shown.pieceAt(sq);
            if (!piece.isEmpty()) {
                int x = x0 + visualCol(sq) * cell;
                int y = y0 + visualRow(sq) * cell;
                float alpha = 1.0f;
                if (preview != null) {
                    if (sq == preview.move().from() || sq == dimRookFrom) {
                        alpha = 0.30f;
                    } else if (sq == preview.move().to() || sq == dimEpCaptured) {
                        alpha = 0.38f;
                    }
                }
                drawPiece(g2, piece, x, y, cell, alpha);
            }
        }
        if (animation != null) {
            drawMoveAnimation(g2, animation, x0, y0, cell);
        }

        // 5. 호버 프리뷰 (고스트 · 내 공격 · 아군 수비 · 방어 이탈 · 상대 예상 최선 응수 · 평가 바)
        if (preview != null) {
            drawHoverPreview(g2, x0, y0, cell, boardPx, preview);
        }

        // 6. 힌트/정답 화살표
        if (arrowMove != null) {
            drawArrow(g2, x0, y0, cell, arrowMove.from(), arrowMove.to(), arrowColor, 0.14f, 0.34f, false);
        }

        // 7. 드래그 중인 기물
        if (dragging && dragSquare >= 0 && dragPoint != null) {
            Piece piece = board.pieceAt(dragSquare);
            if (!piece.isEmpty()) {
                drawPiece(g2, piece, dragPoint.x - cell / 2, dragPoint.y - cell / 2, cell, 1.0f);
            }
        }

        g2.dispose();
    }

    private void drawMoveAnimation(Graphics2D g2, MoveAnimation animation, int x0, int y0, int cell) {
        double t = animationProgress();
        // 잡힌 기물은 제자리에서 서서히 사라짐 (앙파상은 도착 칸이 아닌 옆 칸의 폰)
        if (!animation.captured().isEmpty() && t < 1.0) {
            int sq = animation.capturedSquare();
            drawPiece(g2, animation.captured(), x0 + visualCol(sq) * cell, y0 + visualRow(sq) * cell,
                    cell, (float) (1.0 - t));
        }
        for (Slide slide : animation.slides()) {
            double col = visualCol(slide.from()) + (visualCol(slide.to()) - visualCol(slide.from())) * t;
            double row = visualRow(slide.from()) + (visualRow(slide.to()) - visualRow(slide.from())) * t;
            drawPiece(g2, slide.piece(), x0 + (int) Math.round(col * cell), y0 + (int) Math.round(row * cell),
                    cell, 1.0f);
        }
    }

    private void drawHoverPreview(
            Graphics2D g2,
            int x0,
            int y0,
            int cell,
            int boardPx,
            MoveAnalyzer.Evaluation eval
    ) {
        Move move = eval.move();
        Color verdictColor = verdictColor(eval.verdict());

        // 1) 출발 → 도착 점선 궤적
        drawDashedLine(g2, x0, y0, cell, move.from(), move.to(), verdictColor, 165, 0.05f);

        // 2) 도착 칸 강조 + 고스트 기물
        int destX = x0 + visualCol(move.to()) * cell;
        int destY = y0 + visualRow(move.to()) * cell;
        g2.setColor(new Color(verdictColor.getRed(), verdictColor.getGreen(), verdictColor.getBlue(), 55));
        g2.fillRect(destX, destY, cell, cell);
        g2.setColor(new Color(verdictColor.getRed(), verdictColor.getGreen(), verdictColor.getBlue(), 225));
        g2.setStroke(new BasicStroke(2.6f));
        g2.drawRect(destX + 1, destY + 1, cell - 3, cell - 3);

        Piece movedAfter = eval.after().pieceAt(move.to());
        if (!movedAfter.isEmpty()) {
            drawPiece(g2, movedAfter, destX, destY, cell, 0.74f);
        }

        // 캐슬링 시 룩 고스트도 함께 표시
        Move.Kind kind = board.classifyMove(move);
        if (kind == Move.Kind.CASTLE_SHORT || kind == Move.Kind.CASTLE_LONG) {
            int rookTo = kind == Move.Kind.CASTLE_SHORT ? move.from() + 1 : move.from() - 1;
            Piece rook = eval.after().pieceAt(rookTo);
            if (!rook.isEmpty()) {
                drawPiece(g2, rook, x0 + visualCol(rookTo) * cell, y0 + visualRow(rookTo) * cell, cell, 0.74f);
            }
        }

        // 3) 도착 칸이 상대 공격을 받는 경우: 아군 수비선(파랑 점선) 표시로 교환 가능 여부 시각화
        if (!eval.attackersOfDest().isEmpty()) {
            for (int defSq : eval.defendersOfDest()) {
                drawDashedLine(g2, x0, y0, cell, defSq, move.to(), Theme.BOARD_INFO, 195, 0.055f);
            }
        }

        // 4) 도착 칸에서 내가 공격하는 상대 기물(포크·체크·일반 공격)
        int enemyKingSq = eval.check() ? eval.after().findKing(eval.after().turn()) : -1;
        Color attackColor = (eval.check() || eval.isFork()) ? new Color(0xF59E0B) : Theme.BOARD_ACCENT;
        for (int enemySq : eval.attackedEnemies()) {
            boolean isKing = enemySq == enemyKingSq;
            Color targetColor = isKing ? new Color(0xF97316) : attackColor;
            drawTargetRing(g2, x0, y0, cell, enemySq, targetColor);
            drawArrow(g2, x0, y0, cell, move.to(), enemySq, targetColor, 0.075f, 0.23f, false);
        }

        // 디스커버드 체크 공격선
        if (eval.check() && enemyKingSq >= 0) {
            drawTargetRing(g2, x0, y0, cell, enemyKingSq, new Color(0xF97316));
            for (int checkerSq : eval.checkingAttackers()) {
                if (checkerSq != move.to()) {
                    drawArrow(g2, x0, y0, cell, checkerSq, enemyKingSq, new Color(0xF97316), 0.075f, 0.23f, false);
                }
            }
        }

        // 5) 방어 이탈로 새로 위험해진 다른 아군 기물 경고 링
        for (int allySq : eval.newlyExposedAllies()) {
            drawTargetRing(g2, x0, y0, cell, allySq, Theme.DANGER);
        }

        // 6) 이후 전개: 상대의 최선 예상 응수(탈출·반격·포획) 빨간 점선 화살표
        Move reply = eval.opponentReply();
        if (reply != null) {
            Color replyColor = (eval.verdict() == MoveAnalyzer.Verdict.BLUNDER || eval.verdict() == MoveAnalyzer.Verdict.RISKY)
                    ? new Color(0xEF4444)
                    : new Color(0xFB7185);
            drawArrow(g2, x0, y0, cell, reply.from(), reply.to(), replyColor, 0.09f, 0.26f, true);
            drawTargetRing(g2, x0, y0, cell, reply.to(), replyColor);
        }

        // 7) 자동 회피 1줄 평가 요약 바 (등급 + 이유 + 예상 응수 SAN)
        drawEvalHud(g2, x0, y0, cell, boardPx, eval, verdictColor);
    }

    private static Color verdictColor(MoveAnalyzer.Verdict verdict) {
        return switch (verdict) {
            case MATE -> Theme.SUCCESS;
            case GOOD -> Theme.BOARD_ACCENT;
            case NEUTRAL -> Theme.BOARD_INFO;
            case RISKY -> Theme.WARNING;
            case BLUNDER -> Theme.DANGER;
        };
    }

    private void drawDashedLine(
            Graphics2D g2,
            int x0,
            int y0,
            int cell,
            int fromSq,
            int toSq,
            Color color,
            int alpha,
            float widthFactor
    ) {
        double fx = x0 + (visualCol(fromSq) + 0.5) * cell;
        double fy = y0 + (visualRow(fromSq) + 0.5) * cell;
        double tx = x0 + (visualCol(toSq) + 0.5) * cell;
        double ty = y0 + (visualRow(toSq) + 0.5) * cell;
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2.setStroke(new BasicStroke(
                Math.max(2f, cell * widthFactor),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1f,
                new float[]{cell * 0.12f, cell * 0.10f},
                0f
        ));
        g2.drawLine((int) fx, (int) fy, (int) tx, (int) ty);
    }

    private void drawTargetRing(Graphics2D g2, int x0, int y0, int cell, int sq, Color color) {
        int x = x0 + visualCol(sq) * cell;
        int y = y0 + visualRow(sq) * cell;
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
        g2.fillRect(x, y, cell, cell);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230));
        g2.setStroke(new BasicStroke(Math.max(2.4f, cell * 0.055f)));
        g2.draw(new Ellipse2D.Double(x + cell * 0.07, y + cell * 0.07, cell * 0.86, cell * 0.86));
    }

    private void drawEvalHud(
            Graphics2D g2,
            int x0,
            int y0,
            int cell,
            int boardPx,
            MoveAnalyzer.Evaluation eval,
            Color accent
    ) {
        String badgeText = eval.verdict().label();
        String replyPart = eval.opponentReplySan().isEmpty() ? "" : ("예상 응수 " + eval.opponentReplySan());
        String detailText = eval.reason();

        Font badgeFont = Theme.bold(Math.max(11f, Math.min(13f, cell * 0.21f)));
        Font bodyFont = Theme.font(Math.max(11f, Math.min(13f, cell * 0.20f)));
        FontMetrics bfm = g2.getFontMetrics(badgeFont);
        FontMetrics dfm = g2.getFontMetrics(bodyFont);

        int barH = Math.max(30, bfm.getHeight() + 12);
        int barW = boardPx - 16;
        int barX = x0 + 8;
        // 호버 칸이 보드 위쪽 2행에 있으면 배너를 하단에 배치해 기물을 가리지 않음
        boolean placeBottom = visualRow(eval.move().to()) <= 1;
        int barY = placeBottom ? (y0 + boardPx - barH - 8) : (y0 + 8);

        g2.setColor(new Color(14, 18, 26, 236));
        g2.fillRoundRect(barX, barY, barW, barH, 10, 10);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(barX, barY, barW, barH, 10, 10);

        // 좌측 등급 배지
        int pillW = bfm.stringWidth(badgeText) + 16;
        int pillH = barH - 8;
        int pillX = barX + 6;
        int pillY = barY + 4;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48));
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 8, 8);
        g2.setFont(badgeFont);
        g2.setColor(accent);
        g2.drawString(badgeText, pillX + 8, pillY + (pillH + bfm.getAscent() - bfm.getDescent()) / 2);

        // 우측 상대 예상 응수 배지
        int rightBound = barX + barW - 10;
        if (!replyPart.isEmpty()) {
            int rw = bfm.stringWidth(replyPart) + 14;
            int rx = barX + barW - rw - 6;
            g2.setColor(new Color(239, 68, 68, 42));
            g2.fillRoundRect(rx, pillY, rw, pillH, 8, 8);
            g2.setFont(badgeFont);
            g2.setColor(new Color(0xFCA5A5));
            g2.drawString(replyPart, rx + 7, pillY + (pillH + bfm.getAscent() - bfm.getDescent()) / 2);
            rightBound = rx - 8;
        }

        // 중앙 이유 텍스트 (너비 초과 시 말줄임)
        int textX = pillX + pillW + 10;
        int maxTextW = Math.max(40, rightBound - textX);
        g2.setFont(bodyFont);
        g2.setColor(Theme.TEXT);
        String clipped = clipText(detailText, dfm, maxTextW);
        g2.drawString(clipped, textX, barY + (barH + dfm.getAscent() - dfm.getDescent()) / 2);
    }

    private static String clipText(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ell = "…";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + ell) > maxWidth) {
            end--;
        }
        return end == 0 ? ell : text.substring(0, end) + ell;
    }

    private void drawArrow(
            Graphics2D g2,
            int x0,
            int y0,
            int cell,
            int fromSq,
            int toSq,
            Color color,
            float widthFactor,
            float headFactor,
            boolean dashed
    ) {
        double x1 = x0 + (visualCol(fromSq) + 0.5) * cell;
        double y1 = y0 + (visualRow(fromSq) + 0.5) * cell;
        double x2 = x0 + (visualCol(toSq) + 0.5) * cell;
        double y2 = y0 + (visualRow(toSq) + 0.5) * cell;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1) return;
        double ux = dx / len;
        double uy = dy / len;
        double head = cell * headFactor;
        double sx = x2 - ux * head;
        double sy = y2 - uy * head;

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 215));
        float strokeW = (float) (cell * widthFactor);
        if (dashed) {
            g2.setStroke(new BasicStroke(
                    strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{cell * 0.14f, cell * 0.11f}, 0f
            ));
        } else {
            g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g2.drawLine((int) x1, (int) y1, (int) sx, (int) sy);

        double px = -uy * (head * 0.65);
        double py = ux * (head * 0.65);
        Path2D headShape = new Path2D.Double();
        headShape.moveTo(x2, y2);
        headShape.lineTo(sx + px, sy + py);
        headShape.lineTo(sx - px, sy - py);
        headShape.closePath();
        g2.fill(headShape);
    }

    /**
     * 0..100 정규화 좌표계로 선명한 클래식 Staunton 벡터 기물을 그립니다.
     */
    private static void drawPiece(Graphics2D g2, Piece piece, int x, int y, int cell, float alpha) {
        Graphics2D pg = (Graphics2D) g2.create();
        if (alpha < 0.99f) {
            pg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        at.scale(cell / 100.0, cell / 100.0);
        pg.transform(at);

        boolean white = piece.side() == Side.WHITE;
        Color fill = white ? new Color(0xFCFBF7) : new Color(0x232730);
        Color stroke = white ? new Color(0x1E242E) : new Color(0xE8ECF2);
        Color detail = white ? new Color(0x475569) : new Color(0x94A3B8);

        // 바닥 그림자
        pg.setColor(new Color(0, 0, 0, 48));
        pg.fill(new Ellipse2D.Double(20, 82, 60, 11));

        Path2D body = pieceSilhouette(piece.type());
        pg.setColor(fill);
        pg.fill(body);
        pg.setColor(stroke);
        pg.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pg.draw(body);

        // 기물별 세부 강조선 (가독성 강화)
        pg.setColor(detail);
        pg.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (piece.type()) {
            case KING -> {
                pg.drawLine(50, 12, 50, 27);
                pg.drawLine(43, 19, 57, 19);
                pg.drawLine(32, 76, 68, 76);
            }
            case QUEEN -> {
                pg.drawLine(31, 75, 69, 75);
                pg.fill(new Ellipse2D.Double(19, 25, 6, 6));
                pg.fill(new Ellipse2D.Double(33, 19, 6, 6));
                pg.fill(new Ellipse2D.Double(47, 16, 6, 6));
                pg.fill(new Ellipse2D.Double(61, 19, 6, 6));
                pg.fill(new Ellipse2D.Double(75, 25, 6, 6));
            }
            case ROOK -> {
                pg.drawLine(33, 38, 67, 38);
                pg.drawLine(31, 74, 69, 74);
            }
            case BISHOP -> {
                pg.drawLine(44, 34, 56, 46);
                pg.drawLine(33, 74, 67, 74);
            }
            case KNIGHT -> pg.fill(new Ellipse2D.Double(37, 33, 5, 5));
            case PAWN -> pg.drawLine(34, 75, 66, 75);
        }
        pg.dispose();
    }

    private static Path2D pieceSilhouette(Type type) {
        Path2D p = new Path2D.Double();
        switch (type) {
            case PAWN -> {
                p.append(new Ellipse2D.Double(36, 20, 28, 28), false);
                p.moveTo(39, 46);
                p.lineTo(61, 46);
                p.lineTo(67, 72);
                p.lineTo(74, 84);
                p.lineTo(26, 84);
                p.lineTo(33, 72);
                p.closePath();
            }
            case ROOK -> {
                p.moveTo(27, 22);
                p.lineTo(37, 22); p.lineTo(37, 30);
                p.lineTo(45, 30); p.lineTo(45, 22);
                p.lineTo(55, 22); p.lineTo(55, 30);
                p.lineTo(63, 30); p.lineTo(63, 22);
                p.lineTo(73, 22); p.lineTo(73, 38);
                p.lineTo(66, 44); p.lineTo(68, 72);
                p.lineTo(76, 84); p.lineTo(24, 84);
                p.lineTo(32, 72); p.lineTo(34, 44);
                p.lineTo(27, 38);
                p.closePath();
            }
            case KNIGHT -> {
                p.moveTo(28, 84);
                p.lineTo(35, 68);
                p.curveTo(38, 56, 46, 54, 48, 46);
                p.lineTo(30, 50);
                p.lineTo(25, 42);
                p.lineTo(35, 26);
                p.lineTo(41, 16);
                p.lineTo(52, 22);
                p.curveTo(69, 25, 76, 44, 71, 70);
                p.lineTo(76, 84);
                p.closePath();
            }
            case BISHOP -> {
                p.append(new Ellipse2D.Double(45, 13, 10, 10), false);
                p.moveTo(50, 22);
                p.curveTo(66, 30, 68, 48, 59, 60);
                p.lineTo(66, 73);
                p.lineTo(75, 84);
                p.lineTo(25, 84);
                p.lineTo(34, 73);
                p.lineTo(41, 60);
                p.curveTo(32, 48, 34, 30, 50, 22);
                p.closePath();
            }
            case QUEEN -> {
                p.moveTo(25, 84);
                p.lineTo(31, 72);
                p.lineTo(21, 31);
                p.lineTo(36, 53);
                p.lineTo(36, 25);
                p.lineTo(46, 51);
                p.lineTo(50, 22);
                p.lineTo(54, 51);
                p.lineTo(64, 25);
                p.lineTo(64, 53);
                p.lineTo(79, 31);
                p.lineTo(69, 72);
                p.lineTo(75, 84);
                p.closePath();
            }
            case KING -> {
                p.moveTo(25, 84);
                p.lineTo(31, 72);
                p.curveTo(20, 55, 24, 32, 40, 32);
                p.lineTo(44, 28);
                p.lineTo(56, 28);
                p.lineTo(60, 32);
                p.curveTo(76, 32, 80, 55, 69, 72);
                p.lineTo(75, 84);
                p.closePath();
            }
        }
        return p;
    }
}
