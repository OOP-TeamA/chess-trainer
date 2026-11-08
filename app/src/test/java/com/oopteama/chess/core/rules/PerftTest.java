package com.oopteama.chess.core.rules;

import static org.junit.jupiter.api.Assertions.fail;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Position;
import org.junit.jupiter.api.Test;

/**
 * CORE-03, CORE-04 수용 기준. 기준값은 체스 프로그래밍 커뮤니티에 공개된 perft 표준 국면.
 * 캐슬링, 앙파상, 프로모션, 핀 버그는 거의 전부 여기서 드러난다.
 */
class PerftTest {

    private static final RuleSet RULES = new StandardRules();

    private static void assertPerft(String name, String fen, long... expected) throws Exception {
        for (int depth = 1; depth <= expected.length; depth++) {
            long actual = Perft.count(RULES, Fen.parse(fen), depth);
            if (actual != expected[depth - 1]) {
                Position pos = Fen.parse(fen);
                fail(name + " 깊이 " + depth + ": 기대 " + expected[depth - 1] + ", 실제 " + actual
                    + "\ndivide(첫 수 -> 하위 노드 수): " + Perft.divide(RULES, pos, depth));
            }
        }
    }

    @Test
    void startingPosition() throws Exception {
        assertPerft("시작 국면", Fen.INITIAL, 20, 400, 8_902, 197_281);
    }

    @Test
    void kiwipete() throws Exception {
        assertPerft("Kiwipete",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            48, 2_039, 97_862, 4_085_603);
    }

    @Test
    void position3EnPassantAndPins() throws Exception {
        assertPerft("국면 3", "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            14, 191, 2_812, 43_238, 674_624);
    }

    @Test
    void position4PromotionsAndCastlingThroughCheck() throws Exception {
        assertPerft("국면 4", "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            6, 264, 9_467, 422_333);
    }

    @Test
    void position4Mirrored() throws Exception {
        assertPerft("국면 4 좌우대칭", "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1",
            6, 264, 9_467, 422_333);
    }

    @Test
    void position5() throws Exception {
        assertPerft("국면 5", "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            44, 1_486, 62_379, 2_103_487);
    }

    @Test
    void position6() throws Exception {
        assertPerft("국면 6", "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            46, 2_079, 89_890, 3_894_594);
    }
}
