package com.oopteama.chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** ai가 core(Position, StandardRules)와 맞물려 동작하는지 확인하는 연결 테스트. AI-02 수용 기준 포함. */
class AlphaBetaSearchTest {

    private static final StandardRules RULES = new StandardRules();

    private static AlphaBetaSearch search() {
        return new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(1));
    }

    private static SearchResult run(String fen, int depth) throws Exception {
        return search().search(Fen.parse(fen), new SearchLimits(depth, 0, 1), () -> false);
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

    /** AI-02: 메이트인1을 깊이 1에서. 말단 정지 탐색이 stand-pat보다 종료 판정을 먼저 해야 한다. */
    @ParameterizedTest
    @CsvSource({
        "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1, a1a8",
        "r5k1/8/8/8/8/8/5PPP/6K1 b - - 0 1, a8a1",
    })
    void 깊이_1에서_한_수_메이트를_찾는다(String fen, String mate) throws Exception {
        SearchResult r = run(fen, 1);
        assertEquals(mate, r.best().uci());
        assertEquals(AlphaBetaSearch.MATE - 1, r.score());
    }

    /** AI-02: 메이트인2를 깊이 3에서. 1.Nf6+ gxf6 2.Bxf7# (메이트인1은 없는 국면). */
    @Test
    void 깊이_3에서_두_수_메이트를_찾는다() throws Exception {
        SearchResult r = run("r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 1", 3);
        assertEquals("d5f6", r.best().uci());
        assertEquals(AlphaBetaSearch.MATE - 3, r.score());
    }

    /** 실제 대국(StandardRules.checkEnd)처럼 메이트가 50수 규칙보다 먼저다. */
    @Test
    void 반수_시계가_다_돼도_메이트를_먼저_본다() throws Exception {
        SearchResult r = run("6k1/5ppp/8/8/8/8/8/R5K1 w - - 99 50", 2);
        assertEquals("a1a8", r.best().uci());
        assertEquals(AlphaBetaSearch.MATE - 1, r.score());
    }

    @Test
    void 공짜_기물을_잡는다() throws Exception {
        Position pos = Fen.parse("4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1");
        SearchResult r = search().search(pos, new SearchLimits(3, 0, 1), () -> false);
        assertEquals("d1d5", r.best().uci());
    }

    @Test
    void 이미_취소된_요청은_탐색하지_않는다() throws Exception {
        String fen = Fen.INITIAL;
        Position pos = Fen.parse(fen);
        AtomicInteger calls = new AtomicInteger();

        SearchResult r = search().search(pos, new SearchLimits(1, 0, 1), () -> {
            calls.incrementAndGet();
            return true;
        });

        assertTrue(calls.get() > 0, "취소 공급자를 확인해야 한다");
        assertEquals(0, r.depthReached());
        assertTrue(RULES.legalMoves(pos).contains(r.best()), "대비책도 합법 수");
        assertEquals(fen, Fen.write(pos));
        assertEquals(0, pos.undoDepth());
    }

    @Test
    void 도중에_취소되면_직전에_끝낸_깊이의_결과를_돌려준다() throws Exception {
        String fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
        Position pos = Fen.parse(fen);

        // 깊이 2까지 끝내는 동안 취소 공급자가 몇 번 불리는지 센다(탐색은 결정적).
        AtomicInteger callsToDepth2 = new AtomicInteger();
        SearchResult depth2 = search().search(pos, new SearchLimits(2, 0, 1), () -> {
            callsToDepth2.incrementAndGet();
            return false;
        });

        // 같은 탐색을 깊이 6으로 시작하고, 깊이 3을 시작하는 검사부터 취소한다.
        AtomicInteger calls = new AtomicInteger();
        SearchResult r = search().search(pos, new SearchLimits(6, 0, 1),
            () -> calls.incrementAndGet() > callsToDepth2.get());

        assertEquals(2, r.depthReached());
        assertEquals(depth2.best(), r.best());
        assertEquals(depth2.score(), r.score());
        assertEquals(fen, Fen.write(pos));
        assertEquals(0, pos.undoDepth());
    }

    @Test
    void 시간이_다_되면_끝낸_깊이까지의_결과를_돌려준다() throws Exception {
        Position pos = Fen.parse("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3");
        long t0 = System.nanoTime();
        SearchResult r = search().search(pos, new SearchLimits(30, 50, 1), () -> false);
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertTrue(r.depthReached() >= 1 && r.depthReached() < 30, "깊이: " + r.depthReached());
        assertTrue(ms < 1_000, "시간 제한을 넘김: " + ms + "ms");
        assertEquals(0, pos.undoDepth());
    }

    /** 입문 난이도의 무작위 선택: 같은 시드면 같은 결과, 점수는 최선 수가 아니라 고른 수의 점수. */
    @Test
    void 무작위_선택은_시드로_재현되고_고른_수의_점수를_돌려준다() throws Exception {
        String fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";
        SearchResult best = new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(0))
            .search(Fen.parse(fen), new SearchLimits(2, 0, 1), () -> false);

        boolean pickedWorse = false;
        for (long seed = 0; seed < 20; seed++) {
            SearchLimits limits = new SearchLimits(2, 0, 3);
            SearchResult a = new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(seed))
                .search(Fen.parse(fen), limits, () -> false);
            SearchResult b = new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(seed))
                .search(Fen.parse(fen), limits, () -> false);
            assertEquals(a, b);

            if (a.best().equals(best.best())) {
                assertEquals(best.score(), a.score());
            } else {
                assertTrue(a.score() <= best.score(), a + " vs " + best);
                pickedWorse |= a.score() < best.score();
            }
        }
        assertTrue(pickedWorse, "최선보다 낮은 수를 고른 경우가 있어야 점수 구분을 확인할 수 있다");
    }

    /**
     * 이긴 끝내기를 반복 무승부 없이 메이트로 끝낸다(AI 백, 수비 흑은 PstEvaluator 깊이 2).
     * 실제 대국처럼 대국 이력을 넘겨 이미 나온 국면으로 돌아가지 않게 한다.
     * 시간 제한은 실행 환경마다 다르므로 끄고 난이도의 깊이·평가 함수만 쓴다.
     */
    @ParameterizedTest
    @CsvSource({
        "8/8/8/4k3/8/8/8/3QK3 w - - 0 1, BEGINNER",
        "8/8/8/4k3/8/8/8/3QK3 w - - 0 1, INTERMEDIATE",
        "8/8/8/4k3/8/8/8/3QK3 w - - 0 1, ADVANCED",
        "8/8/3k4/8/8/8/8/Q3K3 w - - 0 1, BEGINNER",
        "8/8/3k4/8/8/8/8/Q3K3 w - - 0 1, INTERMEDIATE",
        "8/8/3k4/8/8/8/8/Q3K3 w - - 0 1, ADVANCED",
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1, BEGINNER",
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1, INTERMEDIATE",
        "4k3/8/8/8/8/8/8/R3K3 w - - 0 1, ADVANCED",
    })
    void 이긴_끝내기를_메이트로_끝낸다(String fen, Difficulty d) throws Exception {
        AlphaBetaSearch ai = new AlphaBetaSearch(RULES, d.evaluator(), new Random(1));
        AlphaBetaSearch defender = new AlphaBetaSearch(RULES, new PstEvaluator(), new Random(1));
        SearchLimits aiLimits = new SearchLimits(d.depth(), 0, d.randomTopN());
        SearchLimits defLimits = new SearchLimits(2, 0, 1);

        Position pos = Fen.parse(fen);
        GameHistory history = new GameHistory(fen, RULES.repetitionKey(pos));
        Optional<GameResult> end = Optional.empty();
        for (int ply = 0; ply < 200 && end.isEmpty(); ply++) {
            boolean aiTurn = ply % 2 == 0;
            SearchResult r = (aiTurn ? ai : defender)
                .search(pos.copy(), history, aiTurn ? aiLimits : defLimits, () -> false);
            pos.make(r.best());
            history.push(r.best(), RULES.repetitionKey(pos));
            end = RULES.checkEnd(pos, history);
        }

        assertTrue(end.isPresent(), "200반수 안에 끝나지 않음: " + Fen.write(pos));
        assertEquals(GameResult.Outcome.WHITE_WINS, end.get().outcome(), end.get() + " " + Fen.write(pos));
        assertEquals(GameResult.Reason.CHECKMATE, end.get().reason());
    }
}
