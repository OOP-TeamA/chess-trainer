package com.oopteama.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class GameHistoryTest {

    private static Move move(String from, String to) {
        return new Move(Square.of(from), Square.of(to), MoveKind.QUIET);
    }

    @Test
    void startPositionCountsAsFirstOccurrence() {
        GameHistory h = new GameHistory(Fen.INITIAL, "A");
        assertEquals(1, h.occurrences("A"));
        assertEquals(0, h.occurrences("B"));
        assertEquals(Fen.INITIAL, h.startFen());
    }

    @Test
    void pushAndPopKeepCountsAndMovesInSync() {
        GameHistory h = new GameHistory(Fen.INITIAL, "A");
        h.push(move("g1", "f3"), "B");
        h.push(move("g8", "f6"), "C");
        h.push(move("f3", "g1"), "D");
        h.push(move("f6", "g8"), "A");
        assertEquals(2, h.occurrences("A"));
        assertEquals(4, h.moves().size());

        h.pop();
        assertEquals(1, h.occurrences("A"));
        assertEquals(1, h.occurrences("D"));
        h.pop();
        assertEquals(0, h.occurrences("D"));
        assertEquals(2, h.moves().size());
    }

    @Test
    void movesReturnsImmutableCopy() {
        GameHistory h = new GameHistory(Fen.INITIAL, "A");
        h.push(move("e2", "e4"), "B");
        List<Move> snapshot = h.moves();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(move("e7", "e5")));
        h.push(move("e7", "e5"), "C");
        assertEquals(1, snapshot.size());
    }

    @Test
    void popOnEmptyHistoryThrows() {
        GameHistory h = new GameHistory(Fen.INITIAL, "A");
        assertThrows(IllegalStateException.class, h::pop);
    }
}
