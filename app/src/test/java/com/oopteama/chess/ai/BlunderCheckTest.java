package com.oopteama.chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** AI-10 실수 알림 판정. */
class BlunderCheckTest {

    private static final StandardRules RULES = new StandardRules();
    /** 백 퀸 d1, 흑 룩 d5(보호 없음). 최선 수는 Qxd5. */
    private static final String FREE_ROOK = "4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1";

    /** GameController 조립과 같은 구성: 고급 평가 함수, 임계값 200. */
    private static BlunderCheck advancedCheck() {
        return new BlunderCheck(new AlphaBetaSearch(RULES, Difficulty.ADVANCED.evaluator(), new Random(1)), 200);
    }

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

    /** 리뷰에서 확인한 정상 사례: 퀸을 공짜로 내주는 수는 더 깊게 확인해도 실수다. */
    @Test
    void 퀸을_공짜로_내주는_수는_확인_탐색_뒤에도_실수다() throws Exception {
        Position pos = Fen.parse(FREE_ROOK);
        Optional<Blunder> bad = advancedCheck().check(pos, RULES.fromUci(pos, "d1d4").orElseThrow(), () -> false);
        assertTrue(bad.isPresent());
        assertTrue(bad.get().lossCp() >= 900, "손실: " + bad.get().lossCp());
    }

    /** G15: 이미 크게 불리한 국면에서 최선 수는 점수가 나빠도 경고하지 않는다. */
    @Test
    void 불리한_국면의_최선_수는_실수가_아니다() throws Exception {
        Position pos = Fen.parse("r3k3/8/8/3q4/8/8/8/R3K3 w Qq - 0 1");
        assertTrue(advancedCheck().check(pos, RULES.fromUci(pos, "a1a8").orElseThrow(), () -> false).isEmpty());
    }

    /**
     * 희생 뒤 조용한 수로 끝나는 메이트는 실수가 아니다. 오페라 게임 16.Qb8+!! Nxb8 17.Rd8#.
     * 용어집 "희생" 직접 해보기의 정답(move:b3b8)과 같은 국면이다.
     */
    @Test
    void 메이트로_이어지는_희생_수는_실수가_아니다() throws Exception {
        Position pos = Fen.parse("4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 0 16");
        assertTrue(advancedCheck().check(pos, RULES.fromUci(pos, "b3b8").orElseThrow(), () -> false).isEmpty());
    }

    /** 실제 대국 수순: 오페라 게임의 백 수 17개(희생 3번 포함)는 모두 경고하지 않는다. */
    @Test
    void 오페라_게임의_백_수는_모두_실수가_아니다() throws Exception {
        List<String> moves = List.of(
            "e2e4", "e7e5", "g1f3", "d7d6", "d2d4", "c8g4", "d4e5", "g4f3", "d1f3", "d6e5",
            "f1c4", "g8f6", "f3b3", "d8e7", "b1c3", "c7c6", "c1g5", "b7b5", "c3b5", "c6b5",
            "c4b5", "b8d7", "e1c1", "a8d8", "d1d7", "d8d7", "h1d1", "e7e6", "b5d7", "f6d7",
            "b3b8", "d7b8", "d1d8");
        BlunderCheck check = advancedCheck();
        Position pos = Position.initial();
        for (int i = 0; i < moves.size(); i++) {
            var m = RULES.fromUci(pos, moves.get(i)).orElseThrow();
            if (i % 2 == 0) {
                Optional<Blunder> b = check.check(pos, m, () -> false);
                assertTrue(b.isEmpty(), (i / 2 + 1) + "." + moves.get(i) + " 오탐: " + b);
            }
            pos.make(m);
        }
        assertTrue(RULES.legalMoves(pos).isEmpty(), "17.Rd8# 메이트로 끝난다");
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
