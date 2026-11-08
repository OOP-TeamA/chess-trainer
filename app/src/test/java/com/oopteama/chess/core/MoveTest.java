package com.oopteama.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoveTest {

    private static Square sq(String name) {
        return Square.of(name);
    }

    @Test
    void uciOfPlainAndPromotionMoves() {
        assertEquals("e2e4", new Move(sq("e2"), sq("e4"), MoveKind.DOUBLE_PUSH).uci());
        assertEquals("e7e8q", new Move(sq("e7"), sq("e8"), MoveKind.QUIET, PieceType.QUEEN).uci());
        assertEquals("d7c8n", new Move(sq("d7"), sq("c8"), MoveKind.CAPTURE, PieceType.KNIGHT).uci());
    }

    @Test
    void captureFlagCoversCaptureAndEnPassantOnly() {
        assertTrue(new Move(sq("e4"), sq("d5"), MoveKind.CAPTURE).isCapture());
        assertTrue(new Move(sq("e5"), sq("d6"), MoveKind.EN_PASSANT).isCapture());
        assertFalse(new Move(sq("e2"), sq("e3"), MoveKind.QUIET).isCapture());
        assertFalse(new Move(sq("e2"), sq("e4"), MoveKind.DOUBLE_PUSH).isCapture());
        assertFalse(new Move(sq("e1"), sq("g1"), MoveKind.CASTLE_SHORT).isCapture());
    }

    @Test
    void promotionOnlyToQueenRookBishopOrKnight() {
        assertFalse(new Move(sq("e2"), sq("e3"), MoveKind.QUIET).isPromotion());
        assertTrue(new Move(sq("a7"), sq("a8"), MoveKind.QUIET, PieceType.ROOK).isPromotion());
        assertThrows(IllegalArgumentException.class,
            () -> new Move(sq("a7"), sq("a8"), MoveKind.QUIET, PieceType.KING));
        assertThrows(IllegalArgumentException.class,
            () -> new Move(sq("a7"), sq("a8"), MoveKind.QUIET, PieceType.PAWN));
    }
}
