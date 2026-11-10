package com.oopteama.chess.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.exercise.FeedbackLadder.Stage;
import com.oopteama.chess.io.LessonRepository;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 11/13 첫 시연 절차(기능 명세 6장 공용 샘플)의 레슨 부분을 화면 없이 재현한다.
 * "d5f6, d4f5 순서로 두고 3단계에서 a1, a2, a3을 골라 오답 3회를 만듭니다.
 * 정답 칸 c7이 표시되면 선택해 완료합니다. 각 입력은 해당 단계에서 한 번만 채점됩니다."
 * 화면의 ExerciseController가 할 일(단계마다 사다리 초기화, 오답마다 recordWrong)은 테스트가 대신한다.
 */
class KnightLessonFlowTest {

    private static Lesson knightLesson() throws IOException {
        Path dir = Path.of("").toAbsolutePath();
        Path relative = Path.of("docs", "fixtures", "first-demo", "data", "lessons", "07_knight.txt");
        while (dir != null && !Files.isRegularFile(dir.resolve(relative))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("공용 샘플을 찾지 못했습니다: " + relative);
        }
        try (Reader in = Files.newBufferedReader(dir.resolve(relative), StandardCharsets.UTF_8)) {
            List<Lesson> lessons = new LessonRepository().loadAll(in, "07_knight.txt");
            assertEquals(1, lessons.size());
            return lessons.get(0);
        }
    }

    private static Move move(Exercise ex, String uci) {
        return ex.legalMoves().stream().filter(m -> m.uci().equals(uci)).findFirst()
            .orElseThrow(() -> new AssertionError("legalMoves에 없는 수: " + uci));
    }

    @Test
    void firstDemoLessonScenario() throws IOException {
        List<LessonStep> steps = knightLesson().steps();
        FeedbackLadder ladder = new FeedbackLadder();

        // 1단계 any-move: d5f6 한 번에 통과
        Exercise one = new LessonStepExercise(steps.get(0));
        Verdict v1 = one.submit(move(one, "d5f6"));
        assertEquals(new Verdict.Solved(steps.get(0).success()), v1);
        assertEquals(Stage.NONE, ladder.stage());

        // 2단계 move:d4f5: 한 번에 통과
        ladder.reset();
        Exercise two = new LessonStepExercise(steps.get(1));
        assertEquals(new Verdict.Solved(steps.get(1).success()), two.submit(move(two, "d4f5")));

        // 3단계 square:c7: a1, a2, a3 오답 3회 → 사다리 EXPLAIN, HINT, REVEAL
        ladder.reset();
        Exercise three = new LessonStepExercise(steps.get(2));
        Stage[] expected = {Stage.EXPLAIN, Stage.HINT, Stage.REVEAL};
        String[] wrong = {"a1", "a2", "a3"};
        for (int i = 0; i < wrong.length; i++) {
            Verdict v = three.submitSquare(Square.of(wrong[i]));
            assertEquals(new Verdict.Wrong(steps.get(2).fail()), v);
            assertEquals(expected[i], ladder.recordWrong());
        }
        // HINT에서 강조할 칸은 파일의 hint=d5, REVEAL에서 표시할 정답 칸은 c7
        assertEquals(Set.of(Square.of("d5")), three.hintSquares());
        assertEquals(Set.of(Square.of("c7")), three.solutionSquares());

        // 정답을 공개해도 사용자가 c7을 골라야 완료
        assertInstanceOf(Verdict.Solved.class, three.submitSquare(Square.of("c7")));
        assertEquals(3, ladder.wrongCount());
    }
}
