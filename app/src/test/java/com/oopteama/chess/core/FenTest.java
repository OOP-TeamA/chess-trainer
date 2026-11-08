package com.oopteama.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class FenTest {

    @Test
    void validFensRoundTrip() throws Exception {
        List<String> fens = List.of(
            Fen.INITIAL,
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "4k3/8/8/8/8/8/8/4K3 w - - 12 40");
        for (String fen : fens) {
            assertEquals(fen, Fen.write(Fen.parse(fen)));
        }
    }

    @Test
    void missingHalfmoveAndFullmoveUseDefaults() throws Exception {
        Position pos = Fen.parse("4k3/8/8/8/8/8/8/4K3 w - -");
        assertEquals(0, pos.halfmoveClock());
        assertEquals(1, pos.fullmoveNumber());
    }

    @Test
    void invalidFensReportTheBadField() {
        assertField(1, "");
        assertField(1, "8/8/8/8/8/8/8 w - - 0 1");        // 랭크 7개
        assertField(1, "9/8/8/8/8/8/8/8 w - - 0 1");      // 랭크 길이 9
        assertField(1, "08/8/8/8/8/8/8/8 w - - 0 1");     // 0은 숫자 칸 수가 아님
        assertField(1, "xxxxxxxx/8/8/8/8/8/8/8 w - - 0 1");
        assertField(2, "8/8/8/8/8/8/8/8 x - - 0 1");
        assertField(3, "8/8/8/8/8/8/8/8 w Z - 0 1");
        assertField(4, "8/8/8/8/8/8/8/8 w - e9 0 1");
        assertField(5, "8/8/8/8/8/8/8/8 w - - a 1");
        assertField(6, "8/8/8/8/8/8/8/8 w - - 0 b");
    }

    private static void assertField(int expectedField, String fen) {
        FenFormatException e = assertThrows(FenFormatException.class, () -> Fen.parse(fen));
        assertEquals(expectedField, e.field(), "FEN: \"" + fen + "\"");
    }
}
