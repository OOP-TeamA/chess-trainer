package com.oopteama.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SquareTest {

    @Test
    void parsesAndPrintsNames() {
        Square e4 = Square.of("e4");
        assertEquals(4, e4.file());
        assertEquals(3, e4.rank());
        assertEquals("e4", e4.name());
        assertEquals(28, e4.index());
        assertEquals(e4, Square.ofIndex(28));
    }

    @Test
    void allSixtyFourSquaresRoundTrip() {
        for (int i = 0; i < 64; i++) {
            Square sq = Square.ofIndex(i);
            assertEquals(i, sq.index());
            assertEquals(sq, Square.of(sq.name()));
        }
    }

    @Test
    void offsetReturnsEmptyOutsideTheBoard() {
        assertEquals(Optional.of(Square.of("b3")), Square.of("a1").offset(1, 2));
        assertEquals(Optional.empty(), Square.of("a1").offset(-1, 0));
        assertEquals(Optional.empty(), Square.of("h8").offset(0, 1));
    }

    @Test
    void invalidSquaresAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Square.of("i1"));
        assertThrows(IllegalArgumentException.class, () -> Square.of("a9"));
        assertThrows(IllegalArgumentException.class, () -> Square.of("E4"));
        assertThrows(IllegalArgumentException.class, () -> Square.of("e"));
        assertThrows(IllegalArgumentException.class, () -> Square.of(null));
        assertThrows(IllegalArgumentException.class, () -> new Square(8, 0));
        assertThrows(IllegalArgumentException.class, () -> Square.ofIndex(64));
        assertThrows(IllegalArgumentException.class, () -> Square.ofIndex(-1));
    }

    @Test
    void lightAndDarkSquares() {
        assertFalse(Square.of("a1").isLight());
        assertTrue(Square.of("h1").isLight());
        assertTrue(Square.of("a8").isLight());
        assertFalse(Square.of("h8").isLight());
    }
}
