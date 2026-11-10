package com.oopteama.chess.content.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.core.rules.SandboxRules;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * RULE-02: 목표 5종의 판정, 정답 수, 기본 힌트.
 * 나이트 국면은 공용 샘플 docs/fixtures/first-demo/data/lessons/07_knight.txt의 세 단계와 같다.
 */
class StepGoalTest {

    private static final RuleSet SANDBOX = new SandboxRules();
    private static final RuleSet STANDARD = new StandardRules();

    /** 07_knight 1단계: 가운데 나이트 하나. */
    private static final String KNIGHT_CENTER = "8/8/8/3N4/8/8/8/8 w - - 0 1";
    /** 07_knight 2단계: 아군 폰에 둘러싸인 나이트. */
    private static final String KNIGHT_BOXED = "8/8/8/2PPP3/2PNP3/2PPP3/8/8 w - - 0 1";
    /** 룩으로 체크는 되지만 메이트는 안 되는 국면. */
    private static final String ROOK_CHECK = "6k1/8/8/8/8/8/8/R5K1 w - - 0 1";
    /** 백랭크 메이트 a1a8. */
    private static final String BACK_RANK = "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1";

    private static Square sq(String name) {
        return Square.of(name);
    }

    private static Set<Square> squares(String... names) {
        return Arrays.stream(names).map(Square::of).collect(Collectors.toSet());
    }

    /** start에서 uci 수를 두고 goal.accepts 결과를 돌려준다. start는 바꾸지 않는다. */
    private static boolean accepts(StepGoal goal, String fen, String uci, RuleSet rules) throws Exception {
        Position before = Fen.parse(fen);
        Move m = rules.fromUci(before, uci).orElseThrow(() -> new AssertionError("불법 수: " + uci));
        Position after = before.copy();
        after.make(m);
        boolean ok = goal.accepts(before, m, after, rules);
        assertEquals(fen, Fen.write(before), "before를 바꾸면 안 된다");
        return ok;
    }

    private static String uci(Optional<Move> m) {
        return m.map(Move::uci).orElse("");
    }

    @Test
    void anyMoveAcceptsEveryLegalMove() throws Exception {
        StepGoal goal = new AnyMoveGoal();
        Position start = Fen.parse(KNIGHT_CENTER);
        for (Move m : SANDBOX.legalMoves(start)) {
            assertTrue(accepts(goal, KNIGHT_CENTER, m.uci(), SANDBOX), m.uci());
        }
        assertTrue(goal.solution(start, SANDBOX).isPresent());
        assertEquals(squares("d5"), goal.defaultHint(start, SANDBOX));
        assertFalse(goal.acceptsSquare(sq("d5")));
        assertEquals(Set.of(), goal.solutionSquares());
    }

    @Test
    void moveGoalAcceptsOnlyListedMoves() throws Exception {
        StepGoal goal = StepGoal.parse("move:d4f5|d4f3");
        assertTrue(accepts(goal, KNIGHT_BOXED, "d4f5", SANDBOX));
        assertTrue(accepts(goal, KNIGHT_BOXED, "d4f3", SANDBOX));
        assertFalse(accepts(goal, KNIGHT_BOXED, "d4b5", SANDBOX));
        assertFalse(accepts(goal, KNIGHT_BOXED, "d5d6", SANDBOX));
    }

    @Test
    void moveGoalSolutionIsFirstLegalAlternativeAndHintShowsItsSquares() throws Exception {
        Position start = Fen.parse(KNIGHT_BOXED);
        // d4d5는 나이트가 갈 수 없으므로 건너뛴다
        StepGoal goal = StepGoal.parse("move:d4d5|d4f5");
        assertEquals("d4f5", uci(goal.solution(start, SANDBOX)));
        assertEquals(Optional.empty(), StepGoal.parse("move:d4d5").solution(start, SANDBOX));
        // 07_knight 2단계 파일의 hint=d4,f5와 같다
        assertEquals(squares("d4", "f5"), StepGoal.parse("move:d4f5").defaultHint(start, SANDBOX));
    }

    @Test
    void moveGoalDistinguishesPromotionPiece() throws Exception {
        String fen = "8/4P3/8/8/8/8/8/8 w - - 0 1";
        StepGoal goal = StepGoal.parse("move:e7e8q");
        assertTrue(accepts(goal, fen, "e7e8q", SANDBOX));
        assertFalse(accepts(goal, fen, "e7e8n", SANDBOX));
        Move solution = goal.solution(Fen.parse(fen), SANDBOX).orElseThrow();
        assertEquals(PieceType.QUEEN, solution.promotion());
    }

    @Test
    void squareGoalJudgesSelectedSquareOnly() throws Exception {
        // 07_knight 3단계
        StepGoal goal = StepGoal.parse("square:c7");
        Position start = Fen.parse(KNIGHT_CENTER);
        assertTrue(goal.acceptsSquare(sq("c7")));
        assertFalse(goal.acceptsSquare(sq("e7")));
        assertFalse(accepts(goal, KNIGHT_CENTER, "d5c7", SANDBOX));
        assertEquals(Optional.empty(), goal.solution(start, SANDBOX));
        assertEquals(squares("c7"), goal.solutionSquares());
        // 정답 칸으로 갈 수 있는 나이트를 가리킨다(파일의 hint=d5와 같음)
        assertEquals(squares("d5"), goal.defaultHint(start, SANDBOX));
    }

    @Test
    void squareGoalHintIsEmptyWhenNoPieceReachesTarget() throws Exception {
        StepGoal goal = StepGoal.parse("square:a1");
        assertEquals(Set.of(), goal.defaultHint(Fen.parse(KNIGHT_CENTER), SANDBOX));
    }

    @Test
    void checkGoalAcceptsAnyCheckIncludingMate() throws Exception {
        StepGoal goal = new CheckGoal();
        assertTrue(accepts(goal, ROOK_CHECK, "a1a8", STANDARD));
        assertFalse(accepts(goal, ROOK_CHECK, "a1a7", STANDARD));
        assertTrue(accepts(goal, BACK_RANK, "a1a8", STANDARD));
    }

    @Test
    void mateGoalRejectsCheckThatIsNotMate() throws Exception {
        StepGoal goal = new MateGoal();
        assertTrue(accepts(goal, BACK_RANK, "a1a8", STANDARD));
        assertFalse(accepts(goal, ROOK_CHECK, "a1a8", STANDARD));
        assertFalse(accepts(goal, BACK_RANK, "a1a7", STANDARD));
    }

    @Test
    void checkAndMateSolutionsComeFromOneMoveSearch() throws Exception {
        Position check = Fen.parse(ROOK_CHECK);
        Move m = new CheckGoal().solution(check, STANDARD).orElseThrow();
        Position after = check.copy();
        after.make(m);
        assertTrue(STANDARD.inCheck(after), m.uci());
        assertEquals(squares("a1"), new CheckGoal().defaultHint(check, STANDARD));

        Position mate = Fen.parse(BACK_RANK);
        assertEquals("a1a8", uci(new MateGoal().solution(mate, STANDARD)));
        assertEquals(squares("a1"), new MateGoal().defaultHint(mate, STANDARD));
        // 탐색이 시작 국면을 바꾸지 않는다
        assertEquals(ROOK_CHECK, Fen.write(check));
        assertEquals(BACK_RANK, Fen.write(mate));
    }

    @Test
    void checkAndMateHaveNoSolutionWithoutEnemyKing() throws Exception {
        // 상대 킹이 없는 교육용 국면(G7: 로더가 이런 단계를 거른다)
        Position start = Fen.parse(KNIGHT_CENTER);
        for (StepGoal goal : new StepGoal[] {new CheckGoal(), new MateGoal()}) {
            assertEquals(Optional.empty(), goal.solution(start, SANDBOX));
            assertEquals(Set.of(), goal.defaultHint(start, SANDBOX));
        }
    }

    @Test
    void checkGoalWorksUnderSandboxRules() throws Exception {
        // 백 킹이 없어도 흑 킹이 있으면 체크를 판정한다
        String fen = "4k3/8/8/8/8/8/8/R7 w - - 0 1";
        assertTrue(accepts(new CheckGoal(), fen, "a1e1", SANDBOX));
        assertFalse(accepts(new CheckGoal(), fen, "a1b1", SANDBOX));
    }
}
