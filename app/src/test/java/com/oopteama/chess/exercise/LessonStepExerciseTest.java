package com.oopteama.chess.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** RULE-02·RULE-03: 목표 하나짜리 연습의 채점, 오답 직전 국면 복귀, 힌트·정답 공개 자료. */
class LessonStepExerciseTest {

    /** 07_knight 2단계와 같은 국면. */
    private static final String BOXED = "8/8/8/2PPP3/2PNP3/2PPP3/8/8 w - - 0 1";
    private static final String CENTER = "8/8/8/3N4/8/8/8/8 w - - 0 1";
    private static final String BACK_RANK = "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1";

    private static LessonStep step(String fen, RulesKind rules, String goal, Set<Square> hint,
                                   String fail, String success) throws Exception {
        return new LessonStep(fen, rules, "설명", Set.of(), StepGoal.parse(goal), hint, fail, success);
    }

    private static Exercise exercise(String fen, String goal) throws Exception {
        return new LessonStepExercise(step(fen, RulesKind.SANDBOX, goal, Set.of(), "틀림", "맞음"));
    }

    private static Move move(Exercise ex, String uci) {
        return ex.rules().fromUci(ex.position(), uci).orElseThrow(() -> new AssertionError("불법 수: " + uci));
    }

    private static String fen(Exercise ex) {
        return Fen.write(ex.position());
    }

    @Test
    void wrongMoveKeepsPositionBeforeTheMistakeAndAllowsRetry() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        assertEquals(new Verdict.Wrong("틀림"), ex.submit(move(ex, "d4b5")));
        assertEquals(BOXED, fen(ex));
        assertEquals(new Verdict.Wrong("틀림"), ex.submit(move(ex, "d5d6")));
        assertEquals(BOXED, fen(ex));
        assertEquals(new Verdict.Solved("맞음"), ex.submit(move(ex, "d4f5")));
    }

    @Test
    void solvedShowsTheMoveAndLocksInput() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        Move right = move(ex, "d4f5");
        ex.submit(right);
        assertEquals("8/8/8/2PPPN2/2P1P3/2PPP3/8/8 b - - 1 1", fen(ex));
        assertEquals(List.of(), ex.legalMoves());
        assertEquals(Optional.empty(), ex.solution());
        assertThrows(IllegalStateException.class, () -> ex.submit(right));
    }

    @Test
    void resetReturnsToStartAndUnlocks() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        ex.submit(move(ex, "d4f5"));
        ex.reset();
        assertEquals(BOXED, fen(ex));
        assertTrue(ex.legalMoves().size() > 0);
        assertInstanceOf(Verdict.Solved.class, ex.submit(move(ex, "d4f5")));
    }

    @Test
    void rejectsMovesThatAreNotLegalHere() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        Move notLegal = move(exercise(CENTER, "any-move"), "d5c7");
        assertThrows(IllegalArgumentException.class, () -> ex.submit(notLegal));
        assertEquals(BOXED, fen(ex));
    }

    @Test
    void positionIsADefensiveCopy() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        Position copy = ex.position();
        copy.make(ex.rules().fromUci(copy, "d4f5").orElseThrow());
        assertEquals(BOXED, fen(ex));
    }

    @Test
    void solutionAndHintSupportTheLadder() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        assertEquals(InputMode.MOVE, ex.inputMode());
        assertEquals("d4f5", ex.solution().orElseThrow().uci());   // REVEAL 화살표
        assertEquals(Set.of(), ex.solutionSquares());
        // 파일에 hint가 없으면 목표의 기본 힌트(출발·도착 칸)
        assertEquals(Set.of(Square.of("d4"), Square.of("f5")), ex.hintSquares());
    }

    @Test
    void fileHintTakesPriorityOverDefaultHint() throws Exception {
        Set<Square> fileHint = Set.of(Square.of("d4"));
        Exercise ex = new LessonStepExercise(step(BOXED, RulesKind.SANDBOX, "move:d4f5", fileHint, "", ""));
        assertEquals(fileHint, ex.hintSquares());
    }

    @Test
    void blankMessagesFallBackToDefaults() throws Exception {
        Exercise ex = new LessonStepExercise(step(BOXED, RulesKind.SANDBOX, "move:d4f5", Set.of(), "", " "));
        assertEquals(new Verdict.Wrong(GoalExercise.DEFAULT_FAIL), ex.submit(move(ex, "d4b5")));
        assertEquals(new Verdict.Solved(GoalExercise.DEFAULT_SUCCESS), ex.submit(move(ex, "d4f5")));
    }

    @Test
    void squareExerciseGradesSquaresOnly() throws Exception {
        Exercise ex = exercise(CENTER, "square:c7");
        assertEquals(InputMode.SQUARE, ex.inputMode());
        assertEquals(List.of(), ex.legalMoves(), "칸 선택 중에는 이동 칸 표시로 답을 알려주지 않는다");
        assertThrows(IllegalStateException.class, () -> ex.submit(move(exercise(CENTER, "any-move"), "d5c7")));
        assertEquals(new Verdict.Wrong("틀림"), ex.submitSquare(Square.of("e7")));
        assertEquals(Optional.empty(), ex.solution());
        assertEquals(Set.of(Square.of("c7")), ex.solutionSquares());
        assertEquals(new Verdict.Solved("맞음"), ex.submitSquare(Square.of("c7")));
        assertThrows(IllegalStateException.class, () -> ex.submitSquare(Square.of("c7")));
        assertEquals(CENTER, fen(ex));
    }

    @Test
    void moveExerciseRejectsSquareInput() throws Exception {
        Exercise ex = exercise(BOXED, "move:d4f5");
        assertThrows(IllegalStateException.class, () -> ex.submitSquare(Square.of("f5")));
    }

    @Test
    void mateStepUsesStandardRules() throws Exception {
        Exercise ex = new LessonStepExercise(step(BACK_RANK, RulesKind.STANDARD, "mate", Set.of(), "", "메이트!"));
        assertEquals(RulesKind.STANDARD.rules(), ex.rules());
        assertInstanceOf(Verdict.Wrong.class, ex.submit(move(ex, "a1a7")));
        assertEquals("a1a8", ex.solution().orElseThrow().uci());
        assertEquals(new Verdict.Solved("메이트!"), ex.submit(move(ex, "a1a8")));
    }

    @Test
    void verdictSwitchIsExhaustive() {
        Verdict v = new Verdict.Continue(new Move(Square.of("e2"), Square.of("e4"), MoveKind.DOUBLE_PUSH), "계속");
        String kind = switch (v) {
            case Verdict.Solved s -> "solved";
            case Verdict.Continue c -> "continue " + c.reply().uci();
            case Verdict.Wrong w -> "wrong";
        };
        assertEquals("continue e2e4", kind);
        assertThrows(NullPointerException.class, () -> new Verdict.Wrong(null));
    }
}
