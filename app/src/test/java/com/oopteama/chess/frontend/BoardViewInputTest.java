package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.service.Settings;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 레슨·퍼즐처럼 불법 이동 안내를 켠 체스판에서 클릭 선택과 안내 호출 조건을 검증합니다.
 * 테스트 본문은 EDT에서 실행합니다.
 */
class BoardViewInputTest {

    private static final int SIZE = 480;

    private final List<String> illegal = new ArrayList<>();
    private final List<Move> moves = new ArrayList<>();

    @Test
    void clickingOwnPieceChangesSelectionInsteadOfReportingIllegalMove() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view = newBoardView();

            click(view, "e2");
            click(view, "d1"); // 움직일 수 없는 아군 퀸: 안내 없이 선택만 해제
            assertTrue(illegal.isEmpty());
            click(view, "e4");
            assertTrue(moves.isEmpty(), "선택이 해제됐으므로 e4 클릭은 수가 되지 않아야 합니다.");

            drag(view, "e2", "d2"); // 드래그로 아군 기물 위에 놓은 것은 이동 시도로 안내
            assertEquals(List.of("e2-d2"), illegal);
        });
    }

    @Test
    void illegalClickKeepsSelectionSoNextClickCanMove() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view = newBoardView();

            click(view, "e2");
            click(view, "e5");
            assertEquals(List.of("e2-e5"), illegal);
            click(view, "e4");
            assertEquals(List.of(Move.fromUci("e2e4")), moves);
        });
    }

    @Test
    void clickingSelectedPieceAgainDeselectsButDragStillMoves() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view = newBoardView();

            click(view, "g1");
            click(view, "g1");
            click(view, "f3");
            assertTrue(moves.isEmpty(), "다시 눌러 선택을 해제했으므로 f3 클릭은 수가 되지 않아야 합니다.");
            assertTrue(illegal.isEmpty());

            click(view, "g1");
            drag(view, "g1", "f3"); // 이미 선택한 기물을 끌어다 놓아도 이동
            assertEquals(List.of(Move.fromUci("g1f3")), moves);
        });
    }

    private BoardView newBoardView() {
        Theme.install();
        BoardView view = new BoardView(new Settings(), null);
        view.setSize(SIZE, SIZE);
        view.setBoard(Board.initial());
        view.setMoveHandler(moves::add);
        view.setIllegalMoveHandler((from, to) -> illegal.add(Board.formatSquare(from) + "-" + Board.formatSquare(to)));
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
}
