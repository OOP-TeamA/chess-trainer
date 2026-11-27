package com.oopteama.chess.ui.controller;

import com.oopteama.chess.ai.MoveInsight;
import com.oopteama.chess.data.Settings;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 체스판 마우스 입력(BoardController)을 검증합니다: 클릭 선택, 선택 해제, 드래그, 불법 이동 안내, 수 설명 호버.
 * 테스트 본문은 화면 스레드(EDT)에서 실행합니다.
 */
class BoardControllerTest {

    private static final int SIZE = 480;

    private final List<String> illegal = new ArrayList<>();
    private final List<Move> moves = new ArrayList<>();
    private BoardController controller;

    @Test
    void hoveringATargetOfTheSelectedPieceShowsThatMovesInsight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoard();
            MoveInsight e4 = new MoveInsight(Move.of("e2e4"), "e4", MoveInsight.Verdict.BEST, 0, null, List.of());
            controller.setInsights(panel.board(), Map.of(Move.of("e2e4"), e4));

            hover(panel, "e4");
            assertNull(panel.shownInsight(), "기물을 고르기 전에는 설명이 없어야 합니다.");
            click(panel, "e2");
            hover(panel, "e4");
            assertSame(e4, panel.shownInsight());
            hover(panel, "e5"); // 갈 수 없는 칸
            assertNull(panel.shownInsight());
            hover(panel, "e2"); // 고른 기물이 있는 칸
            assertNull(panel.shownInsight());

            controller.setInsights(Board.initial(), Map.of(Move.of("e2e4"), e4));
            hover(panel, "e4");
            assertNull(panel.shownInsight(), "다른 국면을 계산한 설명은 쓰지 않아야 합니다.");
        });
    }

    @Test
    void draggingOverATargetShowsTheInsightUntilThePieceIsDropped() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoard();
            MoveInsight nf3 = new MoveInsight(Move.of("g1f3"), "Nf3", MoveInsight.Verdict.GOOD, 20, null, List.of());
            controller.setInsights(panel.board(), Map.of(Move.of("g1f3"), nf3));

            Point from = center("g1");
            Point over = center("f3");
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, from.x, from.y, 1, false));
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, 0, from.x + 3, from.y, 0, false));
            assertNull(panel.shownInsight(), "아직 출발 칸 위에 있으면 설명이 없어야 합니다.");
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, 0, over.x, over.y, 0, false));
            assertSame(nf3, panel.shownInsight());
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, over.x, over.y, 1, false));
            assertNull(panel.shownInsight());
            assertEquals(List.of(Move.of("g1f3")), moves);
        });
    }

    @Test
    void clickingOwnPieceChangesSelectionInsteadOfReportingIllegalMove() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoard();

            click(panel, "e2");
            click(panel, "d1"); // 움직일 수 없는 아군 퀸: 안내 없이 선택만 해제
            assertTrue(illegal.isEmpty());
            click(panel, "e4");
            assertTrue(moves.isEmpty(), "선택이 해제됐으므로 e4 클릭은 수가 되지 않아야 합니다.");

            drag(panel, "e2", "d2"); // 드래그로 아군 기물 위에 놓은 것은 이동 시도로 안내
            assertEquals(List.of("e2-d2"), illegal);
        });
    }

    @Test
    void illegalClickKeepsSelectionSoNextClickCanMove() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoard();

            click(panel, "e2");
            click(panel, "e5");
            assertEquals(List.of("e2-e5"), illegal);
            click(panel, "e4");
            assertEquals(List.of(Move.of("e2e4")), moves);
        });
    }

    @Test
    void clickingSelectedPieceAgainDeselectsButDragStillMoves() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardPanel panel = newBoard();

            click(panel, "g1");
            click(panel, "g1");
            click(panel, "f3");
            assertTrue(moves.isEmpty(), "다시 눌러 선택을 해제했으므로 f3 클릭은 수가 되지 않아야 합니다.");
            assertTrue(illegal.isEmpty());

            click(panel, "g1");
            drag(panel, "g1", "f3"); // 이미 선택한 기물을 끌어다 놓아도 이동
            assertEquals(List.of(Move.of("g1f3")), moves);
        });
    }

    private BoardPanel newBoard() {
        Theme.install();
        BoardPanel panel = new BoardPanel(new Settings());
        panel.setSize(SIZE, SIZE);
        panel.setBoard(Board.initial());
        controller = new BoardController(panel);
        controller.setMoveListener(moves::add);
        controller.setIllegalMoveListener((from, to) -> illegal.add(from + "-" + to));
        return panel;
    }

    private static void hover(BoardPanel panel, String square) {
        Point p = center(square);
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, p.x, p.y, 0, false));
    }

    private static void click(BoardPanel panel, String square) {
        Point p = center(square);
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 1, false));
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, p.x, p.y, 1, false));
    }

    private static void drag(BoardPanel panel, String from, String to) {
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
}
