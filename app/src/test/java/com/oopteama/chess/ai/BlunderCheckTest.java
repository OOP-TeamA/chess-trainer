package com.oopteama.chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** AI-10 실수 알림 판정. */
class BlunderCheckTest {

    private static final StandardRules RULES = new StandardRules();
    /** 백 퀸 d1, 흑 룩 d5(보호 없음). 최선 수는 Qxd5. */
    private static final String FREE_ROOK = "4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1";

    private static BlunderCheck check() {
        return new BlunderCheck(new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(1)), 200);
    }

    @Test
    void 퀸을_내주는_수는_실수로_판정한다() throws Exception {
        Position pos = Fen.parse(FREE_ROOK);
        Optional<Blunder> bad = check().check(pos, RULES.fromUci(pos, "e1f2").orElseThrow(), () -> false);

        assertTrue(bad.isPresent());
        assertTrue(bad.get().lossCp() >= 200, "손실: " + bad.get().lossCp());
        assertEquals("d5d1", bad.get().refutation().uci());
    }

    @Test
    void 최선_수는_실수가_아니고_국면을_바꾸지_않는다() throws Exception {
        Position pos = Fen.parse(FREE_ROOK);
        assertTrue(check().check(pos, RULES.fromUci(pos, "d1d5").orElseThrow(), () -> false).isEmpty());
        assertEquals(FREE_ROOK, Fen.write(pos));
    }

    @Test
    void 취소되면_빈_값() throws Exception {
        Position pos = Fen.parse(FREE_ROOK);
        assertTrue(check().check(pos, RULES.fromUci(pos, "e1f2").orElseThrow(), () -> true).isEmpty());
    }

    @Test
    void 잘못된_인자는_거부한다() throws Exception {
        AlphaBetaSearch search = new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(1));
        assertThrows(IllegalArgumentException.class, () -> new BlunderCheck(search, 0));

        Position pos = Fen.parse(FREE_ROOK);
        Position other = Fen.parse("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1");
        assertThrows(IllegalArgumentException.class,
            () -> check().check(pos, RULES.fromUci(other, "e2e4").orElseThrow(), () -> false));
    }
}
