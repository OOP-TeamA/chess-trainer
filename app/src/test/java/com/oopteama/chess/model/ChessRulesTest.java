package com.oopteama.chess.model;

import com.oopteama.chess.model.piece.Bishop;
import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Knight;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.model.piece.Queen;
import com.oopteama.chess.model.piece.Rook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 체스 규칙(model 패키지)을 검증합니다.
 */
class ChessRulesTest {

    /** 깊이 depth까지 가능한 모든 수순의 개수 (수 생성이 정확한지 검증하는 표준 방법). */
    private static long perft(Board board, int depth) {
        List<Move> moves = Rules.legalMoves(board);
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0;
        for (Move move : moves) {
            nodes += perft(board.apply(move), depth - 1);
        }
        return nodes;
    }

    @Test
    void moveCountsMatchStandardPerftNumbers() {
        Board initial = Board.initial();
        assertEquals(Fen.START, initial.toFen());
        assertEquals(20, perft(initial, 1));
        assertEquals(400, perft(initial, 2));
        assertEquals(8_902, perft(initial, 3));

        // 캐슬링·앙파상·승진이 모두 나오는 유명한 검증 국면 "Kiwipete"
        Board kiwipete = Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertEquals(48, perft(kiwipete, 1));
        assertEquals(2_039, perft(kiwipete, 2));
    }

    @Test
    void eachPieceMovesInItsOwnWay() {
        Board empty = Board.fromSandboxFen("8/8/8/8/8/8/8/8 w - - 0 1");
        Position d4 = Position.of("d4");
        // 같은 attackedSquares() 호출이지만 기물마다 결과가 다릅니다 (다형성).
        assertEquals(14, new Rook(Side.WHITE).attackedSquares(empty, d4).size());
        assertEquals(13, new Bishop(Side.WHITE).attackedSquares(empty, d4).size());
        assertEquals(27, new Queen(Side.WHITE).attackedSquares(empty, d4).size());
        assertEquals(8, new Knight(Side.WHITE).attackedSquares(empty, d4).size());
        assertEquals(8, new King(Side.WHITE).attackedSquares(empty, d4).size());
        assertEquals(2, new Pawn(Side.WHITE).attackedSquares(empty, d4).size());

        // 룩은 같은 편 기물을 뛰어넘지 못합니다.
        Board blocked = Board.fromSandboxFen("8/8/3P4/8/3R4/8/8/8 w - - 0 1");
        List<Position> rookTargets = blocked.pieceAt("d4").movableSquares(blocked, d4);
        assertTrue(rookTargets.contains(Position.of("d5")));
        assertFalse(rookTargets.contains(Position.of("d6")));
        assertFalse(rookTargets.contains(Position.of("d7")));
    }

    @Test
    void castlingEnPassantAndPromotionFollowTheRules() {
        // f8 룩이 f1을 공격하므로 킹 쪽 캐슬링은 안 되고, 퀸 쪽은 됩니다.
        Board castle = Board.fromFen("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        List<String> moves = Rules.legalMoves(castle).stream().map(Move::toString).toList();
        assertFalse(moves.contains("e1g1"));
        assertTrue(moves.contains("e1c1"));
        Board castled = castle.apply(Move.of("e1c1"));
        assertInstanceOf(King.class, castled.pieceAt("c1"));
        assertInstanceOf(Rook.class, castled.pieceAt("d1"));

        Game enPassant = new Game(Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"));
        Game.PlayedMove capture = enPassant.play(Move.of("e5d6"));
        assertInstanceOf(Pawn.class, capture.captured());
        assertEquals(Side.BLACK, capture.captured().side());
        assertNull(enPassant.board().pieceAt("d5"));
        assertEquals("exd6", capture.san());

        Game promotion = new Game(Board.fromFen("7k/1P6/8/8/8/8/8/6K1 w - - 0 1"));
        Game.PlayedMove promoted = promotion.play(Move.of("b7b8q"));
        Piece queen = promotion.board().pieceAt("b8");
        assertInstanceOf(Queen.class, queen);
        assertEquals(Side.WHITE, queen.side());
        assertEquals("b8=Q+", promoted.san());
    }

    @Test
    void operaGameEndsInCheckmateWithCorrectNotation() {
        String[] moves = {
                "e2e4", "e7e5", "g1f3", "d7d6", "d2d4", "c8g4", "d4e5", "g4f3",
                "d1f3", "d6e5", "f1c4", "g8f6", "f3b3", "d8e7", "b1c3", "c7c6",
                "c1g5", "b7b5", "c3b5", "c6b5", "c4b5", "b8d7", "e1c1", "a8d8",
                "d1d7", "d8d7", "h1d1", "e7e6", "b5d7", "f6d7", "b3b8", "d7b8",
                "d1d8"
        };
        List<String> expectedSan = List.of(
                "e4", "e5", "Nf3", "d6", "d4", "Bg4", "dxe5", "Bxf3",
                "Qxf3", "dxe5", "Bc4", "Nf6", "Qb3", "Qe7", "Nc3", "c6",
                "Bg5", "b5", "Nxb5", "cxb5", "Bxb5+", "Nbd7", "O-O-O", "Rd8",
                "Rxd7", "Rxd7", "Rd1", "Qe6", "Bxd7+", "Nxd7", "Qb8+", "Nxb8",
                "Rd8#"
        );
        Game game = new Game();
        for (String move : moves) {
            game.play(Move.of(move));
        }
        assertEquals(expectedSan, game.history().stream().map(Game.PlayedMove::san).toList());
        assertEquals(Game.Status.CHECKMATE, game.status());
        assertEquals(Side.WHITE, game.winner());

        assertTrue(game.undo());
        assertEquals(Game.Status.PLAYING, game.status());
        assertFalse(game.isOver());
    }

    @Test
    void drawRulesAreDetected() {
        assertEquals(Game.Status.STALEMATE, new Game(Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")).status());

        assertTrue(Rules.isInsufficientMaterial(Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")));
        assertTrue(Rules.isInsufficientMaterial(Board.fromFen("4k3/8/8/8/8/8/4N3/4K3 w - - 0 1")));
        assertTrue(Rules.isInsufficientMaterial(Board.fromFen("4k3/8/3b4/8/8/8/5B2/4K3 w - - 0 1")));
        assertFalse(Rules.isInsufficientMaterial(Board.fromFen("4k3/8/2b5/8/8/8/5B2/4K3 w - - 0 1")));

        Game repetition = new Game();
        for (int i = 0; i < 2; i++) {
            for (String move : new String[]{"g1f3", "g8f6", "f3g1", "f6g8"}) {
                repetition.play(Move.of(move));
            }
        }
        assertEquals(3, repetition.repetitionCount());
        assertEquals(Game.Status.DRAW_REPETITION, repetition.status());
    }

    @Test
    void sandboxBoardsWorkWithoutKings() {
        Board knightOnly = Board.fromSandboxFen("8/8/8/3N4/8/8/8/8 w - - 0 1");
        assertEquals(8, Rules.legalMoves(knightOnly).size());
        Board moved = knightOnly.apply(Move.of("d5e7"));
        assertInstanceOf(Knight.class, moved.pieceAt("e7"));
        assertNull(moved.pieceAt("d5"));
    }
}
