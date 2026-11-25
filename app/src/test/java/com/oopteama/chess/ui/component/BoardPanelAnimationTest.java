package com.oopteama.chess.ui.component;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.controller.BoardController;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 체스판 기물 이동 애니메이션이 어떤 국면 변화에서 재생되는지 검증합니다.
 * 테스트 본문은 화면 스레드(EDT)에서 실행되므로 타이머가 끼어들지 않아 대기열 상태가 일정합니다.
 */
class BoardPanelAnimationTest {

    private static final int SIZE = 480;

    @Test
    void clickedMoveSlidesWhileDroppedMoveSnapsIntoPlace() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoardPanel();
            BoardController controller = new BoardController(panel);
            Board[] position = {Board.initial()};
            panel.setBoard(position[0]);
            controller.setMoveListener(move -> {
                position[0] = position[0].apply(move);
                panel.setBoard(position[0]);
                panel.setLastMove(move);
            });

            click(panel, "e2");
            click(panel, "e4");
            assertEquals(List.of(Move.of("e2e4")), panel.pendingAnimationMoves());
            paint(panel);

            drag(panel, "e7", "e5");
            assertInstanceOf(Pawn.class, position[0].pieceAt("e5"));
            assertEquals(Side.BLACK, position[0].pieceAt("e5").side());
            assertTrue(panel.pendingAnimationMoves().isEmpty(), "드래그로 놓은 수는 다시 미끄러지지 않아야 합니다.");
        });
    }

    @Test
    void replyQueuesBehindCurrentMoveAndUnrelatedPositionSnaps() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoardPanel();
            Board start = Board.initial();
            Board afterE4 = start.apply(Move.of("e2e4"));
            Board afterE5 = afterE4.apply(Move.of("e7e5"));
            Board afterNf3 = afterE5.apply(Move.of("g1f3"));

            panel.setBoard(start);
            panel.setBoard(afterE4);
            // 같은 국면을 다시 그리는 경우(힌트 표시 등)는 재생을 끊지 않음
            panel.setBoard(Board.fromFen(afterE4.toFen()));
            assertEquals(List.of(Move.of("e2e4")), panel.pendingAnimationMoves());

            // 내 수가 끝나기 전에 도착한 AI 응수는 이어서 재생
            panel.setBoard(afterE5);
            assertEquals(List.of(Move.of("e2e4"), Move.of("e7e5")), panel.pendingAnimationMoves());
            paint(panel);

            // 밀린 수가 더 쌓이면 마지막 수만 재생
            panel.setBoard(afterNf3);
            assertEquals(List.of(Move.of("g1f3")), panel.pendingAnimationMoves());

            // 무르기·새 대국처럼 한 수로 설명되지 않는 변화는 바로 표시
            panel.setBoard(start);
            assertTrue(panel.pendingAnimationMoves().isEmpty());
        });
    }

    @Test
    void castlingEnPassantAndPromotionCaptureAnimateAndRender() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoardPanel();
            String[][] cases = {
                    {"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1"},
                    {"r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "e8c8"},
                    {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "e5d6"},
                    {"1n2k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7b8n"},
            };
            for (String[] c : cases) {
                Board before = Board.fromFen(c[0]);
                Move move = Move.of(c[1]);
                panel.setBoard(before);
                panel.setBoard(before.apply(move));
                assertEquals(List.of(move), panel.pendingAnimationMoves(), c[1]);
                panel.setFlipped(false);
                paint(panel);
                panel.setFlipped(true);
                paint(panel);
            }
        });
    }

    private static BoardPanel newBoardPanel() {
        Theme.install();
        BoardPanel panel = new BoardPanel(new Settings());
        panel.setSize(SIZE, SIZE);
        return panel;
    }

    static void click(BoardPanel panel, String square) {
        Point p = center(square);
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 1, false));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, p.x, p.y, 1, false));
    }

    static void drag(BoardPanel panel, String from, String to) {
        Point a = center(from);
        Point b = center(to);
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, a.x, a.y, 1, false));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, 0, b.x, b.y, 0, false));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, b.x, b.y, 1, false));
    }

    /** 뒤집지 않은 SIZE×SIZE 체스판에서 칸 가운데 좌표 (BoardPanel의 여백 10px 배치와 같은 계산). */
    private static Point center(String square) {
        Position position = Position.of(square);
        int cell = (SIZE - 20) / 8;
        int origin = (SIZE - cell * 8) / 2;
        return new Point(origin + position.col() * cell + cell / 2, origin + (7 - position.row()) * cell + cell / 2);
    }

    private static void paint(BoardPanel panel) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.printAll(g2);
        g2.dispose();
    }
}
