package com.oopteama.chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 보드 입력으로 캐슬링: 킹 → 도착 칸, 킹 → 같은 편 룩 둘 다 합법 캐슬링 수 하나를 내보낸다. */
class BoardPanelCastlingTest {

    private static final StandardRules RULES = new StandardRules();

    private final List<Move> emitted = new ArrayList<>();
    private List<Move> legal;

    private BoardPanel board(String fen) throws Exception {
        Position position = Fen.parse(fen);
        legal = RULES.legalMoves(position);
        BoardPanel board = new BoardPanel();
        board.setPosition(position);
        board.setLegalMoves(legal);
        board.setInputMode(Optional.of(InputMode.MOVE));
        board.addMoveListener(emitted::add);
        return board;
    }

    private Move legal(String uci) {
        return legal.stream().filter(m -> m.uci().equals(uci)).findFirst().orElseThrow();
    }

    private static void click(BoardPanel board, String... squares) {
        for (String s : squares) {
            board.handleMoveClick(Square.of(s));
        }
    }

    @Test
    void kingToDestinationSquareCastles() throws Exception {
        BoardPanel board = board("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        click(board, "e1", "g1");
        assertEquals(1, emitted.size());
        assertSame(legal("e1g1"), emitted.getFirst(), "합법 수 목록의 인스턴스를 그대로 내보낸다");
        assertEquals(MoveKind.CASTLE_SHORT, emitted.getFirst().kind());
    }

    @Test
    void kingThenOwnRookCastlesTowardThatRook() throws Exception {
        BoardPanel board = board("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        click(board, "e1", "h1");
        click(board, "e1", "a1");
        assertEquals(List.of(legal("e1g1"), legal("e1c1")), emitted);
        assertEquals(MoveKind.CASTLE_LONG, emitted.get(1).kind());
    }

    @Test
    void blackCastlesWithRookClickToo() throws Exception {
        BoardPanel board = board("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1");
        click(board, "e8", "a8");
        assertEquals(List.of(legal("e8c8")), emitted);
    }

    @Test
    void rookClickWithoutCastlingRightJustSelectsTheRook() throws Exception {
        BoardPanel board = board("r3k2r/8/8/8/8/8/8/R3K2R w Qkq - 0 1");
        click(board, "e1", "h1");
        assertTrue(emitted.isEmpty(), "킹사이드 권리가 없으면 캐슬링하지 않는다");
        click(board, "h2");
        assertEquals(List.of(legal("h1h2")), emitted, "대신 룩이 선택되어 다음 클릭으로 룩이 움직인다");
    }

    @Test
    void rookClickWhilePassingThroughCheckDoesNotCastle() throws Exception {
        // f1이 흑 룩(f8)에게 공격받아 킹사이드 캐슬링은 불법, 퀸사이드는 가능
        BoardPanel board = board("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        click(board, "e1", "h1");
        assertTrue(emitted.isEmpty());
        click(board, "e1", "a1");
        assertEquals(List.of(legal("e1c1")), emitted);
    }
}
