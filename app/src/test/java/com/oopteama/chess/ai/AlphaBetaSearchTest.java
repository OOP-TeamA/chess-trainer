package com.oopteama.chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** ai가 core(Position, StandardRules)와 맞물려 동작하는지 확인하는 연결 테스트. */
class AlphaBetaSearchTest {

    private static final StandardRules RULES = new StandardRules();

    private static AlphaBetaSearch search() {
        return new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(1));
    }

    @Test
    void 한_수_메이트를_찾고_국면을_원상태로_돌려놓는다() throws Exception {
        String fen = "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1";
        Position pos = Fen.parse(fen);

        SearchResult r = search().search(pos, new SearchLimits(2, 0, 1), () -> false);

        assertEquals("a1a8", r.best().uci());
        assertTrue(r.score() >= AlphaBetaSearch.MATE - 10, "메이트 점수: " + r.score());
        assertEquals(fen, Fen.write(pos));
        assertEquals(0, pos.undoDepth());
    }

    @Test
    void 공짜_기물을_잡는다() throws Exception {
        Position pos = Fen.parse("4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1");
        SearchResult r = search().search(pos, new SearchLimits(3, 0, 1), () -> false);
        assertEquals("d1d5", r.best().uci());
    }
}
