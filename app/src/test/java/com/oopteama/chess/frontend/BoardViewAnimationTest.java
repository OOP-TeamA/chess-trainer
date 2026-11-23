package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece;
import com.oopteama.chess.backend.service.Settings;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 체스판 기물 이동 애니메이션이 어떤 국면 변화에서 재생되는지 검증합니다.
 * 테스트 본문은 EDT에서 실행되므로 Swing 타이머가 끼어들지 않아 대기열 상태가 결정적입니다.
 */
class BoardViewAnimationTest {

    private static final int SIZE = 480;

    @Test
    void clickedMoveSlidesWhileDroppedMoveSnapsIntoPlace() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view = newBoardView();
            Board[] position = {Board.initial()};
            view.setBoard(position[0]);
            view.setMoveHandler(move -> {
                position[0] = position[0].applyMove(move);
                view.setBoard(position[0]);
                view.setLastMove(move);
            });

            click(view, "e2");
            click(view, "e4");
            assertEquals(List.of(Move.fromUci("e2e4")), view.pendingAnimationMoves());
            paint(view);

            drag(view, "e7", "e5");
            assertEquals(Piece.BP, position[0].pieceAt("e5"));
            assertTrue(view.pendingAnimationMoves().isEmpty(), "드래그로 놓은 수는 다시 미끄러지지 않아야 합니다.");
        });
    }

    @Test
    void replyQueuesBehindCurrentMoveAndUnrelatedPositionSnaps() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view = newBoardView();
            Board start = Board.initial();
            Board afterE4 = start.applyMove(Move.fromUci("e2e4"));
            Board afterE5 = afterE4.applyMove(Move.fromUci("e7e5"));
            Board afterNf3 = afterE5.applyMove(Move.fromUci("g1f3"));

            view.setBoard(start);
            view.setBoard(afterE4);
            // 같은 국면을 다시 그리는 경우(힌트 표시 등)는 재생을 끊지 않음
            view.setBoard(Board.fromFen(afterE4.toFen()));
            assertEquals(List.of(Move.fromUci("e2e4")), view.pendingAnimationMoves());

            // 내 수가 끝나기 전에 도착한 AI 응수는 이어서 재생
            view.setBoard(afterE5);
            assertEquals(List.of(Move.fromUci("e2e4"), Move.fromUci("e7e5")), view.pendingAnimationMoves());
            paint(view);

            // 밀린 수가 더 쌓이면 마지막 수만 재생
            view.setBoard(afterNf3);
            assertEquals(List.of(Move.fromUci("g1f3")), view.pendingAnimationMoves());

            // 무르기·새 대국처럼 한 수로 설명되지 않는 변화는 바로 표시
            view.setBoard(start);
            assertTrue(view.pendingAnimationMoves().isEmpty());
        });
    }

    @Test
    void castlingEnPassantAndPromotionCaptureAnimateAndRender() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view = newBoardView();
            String[][] cases = {
                    {"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1"},
                    {"r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "e8c8"},
                    {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6"},
                    {"1n2k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7b8n"},
            };
            for (String[] c : cases) {
                Board before = Board.fromFen(c[0]);
                Move move = Move.fromUci(c[1]);
                view.setBoard(before);
                view.setBoard(before.applyMove(move));
                assertEquals(List.of(move), view.pendingAnimationMoves(), c[1]);
                view.setFlipped(false);
                paint(view);
                view.setFlipped(true);
                paint(view);
            }
        });
    }

    private static BoardView newBoardView() {
        Theme.install();
        BoardView view = new BoardView(new Settings(), null);
        view.setSize(SIZE, SIZE);
        return view;
    }

    private static void click(BoardView view, String square) {
        Point p = center(square);
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 1, false));
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, 0, 0, p.x, p.y, 1, false));
    }

    private static void drag(BoardView view, String from, String to) {
        Point a = center(from);
        Point b = center(to);
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, 0, 0, a.x, a.y, 1, false));
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_DRAGGED, 0, 0, b.x, b.y, 0, false));
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, 0, 0, b.x, b.y, 1, false));
    }

    /** 뒤집지 않은 SIZE×SIZE 보드에서 칸 중앙 좌표 (BoardView의 여백 10px 배치와 같은 계산). */
    private static Point center(String square) {
        int sq = Board.parseSquare(square);
        int cell = (SIZE - 20) / 8;
        int origin = (SIZE - cell * 8) / 2;
        return new Point(origin + Board.file(sq) * cell + cell / 2, origin + (7 - Board.rank(sq)) * cell + cell / 2);
    }

    private static void paint(BoardView view) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        view.printAll(g2);
        g2.dispose();
    }
}
