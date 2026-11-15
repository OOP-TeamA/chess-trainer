package com.oopteama.chess.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oopteama.chess.content.TermTry;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.content.goal.MoveGoal;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Square;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** GLOS-07: 용어 직접 해보기가 레슨 단계와 같은 방식으로 채점된다. */
class TermTryExerciseTest {

    private static final TermTry FORK = new TermTry("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1",
            new MoveGoal(List.of("b5c7")), "나이트로 킹과 룩을 동시에 공격해 보세요.", "나이트가 킹과 룩을 동시에 공격합니다.");

    private static Move move(Exercise exercise, String uci) {
        return exercise.rules().fromUci(exercise.position(), uci).orElseThrow();
    }

    @Test
    void forkTrySucceedsWithB5C7() {
        // GLOS-07 수용 기준: 포크 용어에서 b5c7을 두면 성공
        TermTryExercise exercise = new TermTryExercise(FORK);
        assertEquals("나이트로 킹과 룩을 동시에 공격해 보세요.", exercise.prompt());
        assertEquals(InputMode.MOVE, exercise.inputMode());
        assertEquals(Optional.of(move(exercise, "b5c7")), exercise.solution());

        Verdict wrong = exercise.submit(move(exercise, "b5a3"));
        assertEquals(new Verdict.Wrong(GoalExercise.DEFAULT_FAIL), wrong);
        assertEquals(FORK.fen(), Fen.write(exercise.position())); // 오답 직전 국면 그대로

        assertEquals(new Verdict.Solved("나이트가 킹과 룩을 동시에 공격합니다."), exercise.submit(move(exercise, "b5c7")));
    }

    @Test
    void hintComesFromGoalAndMissingSuccessUsesDefault() {
        TermTryExercise exercise = new TermTryExercise(new TermTry(FORK.fen(), FORK.goal(), FORK.text(), ""));
        assertEquals(Set.of(Square.of("b5"), Square.of("c7")), exercise.hintSquares());
        assertEquals(new Verdict.Solved(GoalExercise.DEFAULT_SUCCESS), exercise.submit(move(exercise, "b5c7")));
    }
}
