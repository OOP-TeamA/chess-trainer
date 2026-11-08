package com.oopteama.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Position.make/unmake와 기물별 pseudoLegalMoves 검증. RuleSet 없이 돌아간다. */
class PositionTest {

    private static List<Move> pseudoLegal(Position pos) {
        List<Move> all = new ArrayList<>();
        for (Square s : pos.occupied(pos.sideToMove())) {
            all.addAll(pos.at(s).pseudoLegalMoves(pos, s));
        }
        return all;
    }

    private static Move quiet(String from, String to, MoveKind kind) {
        return new Move(Square.of(from), Square.of(to), kind);
    }

    @Test
    void initialPositionMatchesInitialFen() {
        assertEquals(Fen.INITIAL, Fen.write(Position.initial()));
    }

    @Test
    void initialPositionHasTwentyPseudoLegalMoves() {
        assertEquals(20, pseudoLegal(Position.initial()).size());
    }

    @Test
    void knightAndPawnMovesFromInitialPosition() {
        Position pos = Position.initial();
        List<String> knight = pos.at(Square.of("b1")).pseudoLegalMoves(pos, Square.of("b1"))
            .stream().map(Move::uci).sorted().toList();
        assertEquals(List.of("b1a3", "b1c3"), knight);

        List<String> pawn = pos.at(Square.of("e2")).pseudoLegalMoves(pos, Square.of("e2"))
            .stream().map(Move::uci).sorted().toList();
        assertEquals(List.of("e2e3", "e2e4"), pawn);
    }

    @Test
    void randomPlayMakeUnmakeRestoresEveryPosition() throws Exception {
        Random rnd = new Random(42);
        for (int game = 0; game < 500; game++) {
            Position pos = Position.initial();
            Deque<String> before = new ArrayDeque<>();
            for (int ply = 0; ply < 200; ply++) {
                List<Move> moves = pseudoLegal(pos);
                if (moves.isEmpty()) {
                    break;
                }
                before.push(Fen.write(pos));
                pos.make(moves.get(rnd.nextInt(moves.size())));

                String fen = Fen.write(pos);
                assertEquals(fen, Fen.write(Fen.parse(fen)), "FEN 왕복 실패");
            }
            while (!before.isEmpty()) {
                pos.unmake();
                assertEquals(before.pop(), Fen.write(pos), "unmake 후 국면이 다름");
            }
            assertEquals(0, pos.undoDepth());
        }
    }

    @Test
    void whiteShortAndLongCastle() throws Exception {
        String start = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        Position pos = Fen.parse(start);

        pos.make(quiet("e1", "g1", MoveKind.CASTLE_SHORT));
        assertEquals("r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1", Fen.write(pos));
        pos.unmake();
        assertEquals(start, Fen.write(pos));

        pos.make(quiet("e1", "c1", MoveKind.CASTLE_LONG));
        assertEquals("r3k2r/8/8/8/8/8/8/2KR3R b kq - 1 1", Fen.write(pos));
        pos.unmake();
        assertEquals(start, Fen.write(pos));
    }

    @Test
    void blackShortCastleIncrementsFullmove() throws Exception {
        String start = "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1";
        Position pos = Fen.parse(start);
        pos.make(quiet("e8", "g8", MoveKind.CASTLE_SHORT));
        assertEquals("r4rk1/8/8/8/8/8/8/R3K2R w KQ - 1 2", Fen.write(pos));
        pos.unmake();
        assertEquals(start, Fen.write(pos));
    }

    @Test
    void capturingRookOnCornerRemovesThatCastlingRight() throws Exception {
        String start = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        Position pos = Fen.parse(start);
        pos.make(quiet("a1", "a8", MoveKind.CAPTURE));
        // 백 퀸사이드(룩이 움직임)와 흑 퀸사이드(룩이 잡힘) 권리가 함께 사라진다
        assertEquals("R3k2r/8/8/8/8/8/8/4K2R b Kk - 0 1", Fen.write(pos));
        pos.unmake();
        assertEquals(start, Fen.write(pos));
    }

    @Test
    void enPassantCaptureAndUndo() throws Exception {
        String start = "4k3/8/8/8/3p4/8/4P3/4K3 w - - 0 1";
        Position pos = Fen.parse(start);

        pos.make(quiet("e2", "e4", MoveKind.DOUBLE_PUSH));
        assertEquals("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1", Fen.write(pos));

        Move ep = pseudoLegal(pos).stream()
            .filter(m -> m.kind() == MoveKind.EN_PASSANT)
            .findFirst()
            .orElseThrow(() -> new AssertionError("앙파상 수가 생성되지 않음"));
        assertEquals("d4e3", ep.uci());

        pos.make(ep);
        assertEquals("4k3/8/8/8/8/4p3/8/4K3 w - - 0 2", Fen.write(pos));

        pos.unmake();
        assertEquals("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1", Fen.write(pos));
        pos.unmake();
        assertEquals(start, Fen.write(pos));
    }

    @Test
    void promotionGeneratesFourMovesAndUndoRestoresPawn() throws Exception {
        String start = "4k3/P7/8/8/8/8/8/4K3 w - - 0 1";
        Position pos = Fen.parse(start);

        List<String> promos = pseudoLegal(pos).stream()
            .filter(Move::isPromotion).map(Move::uci).sorted().toList();
        assertEquals(List.of("a7a8b", "a7a8n", "a7a8q", "a7a8r"), promos);

        Move toQueen = pseudoLegal(pos).stream()
            .filter(m -> "a7a8q".equals(m.uci())).findFirst().orElseThrow();
        pos.make(toQueen);
        assertEquals("Q3k3/8/8/8/8/8/8/4K3 b - - 0 1", Fen.write(pos));
        pos.unmake();
        assertEquals(start, Fen.write(pos));
    }

    @Test
    void unmakeWithoutMoveThrows() {
        assertThrows(IllegalStateException.class, () -> Position.initial().unmake());
    }

    @Test
    void copyDropsUndoStackAndIsIndependent() {
        Position pos = Position.initial();
        pos.make(quiet("e2", "e4", MoveKind.DOUBLE_PUSH));
        Position copy = pos.copy();

        assertEquals(0, copy.undoDepth());
        assertEquals(Fen.write(pos), Fen.write(copy));
        assertThrows(IllegalStateException.class, copy::unmake);

        copy.make(quiet("e7", "e5", MoveKind.DOUBLE_PUSH));
        assertEquals(1, pos.undoDepth());
        assertTrue(Fen.write(pos).startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b"));
    }
}
