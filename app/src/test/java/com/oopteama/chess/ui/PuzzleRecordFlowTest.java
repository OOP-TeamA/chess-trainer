package com.oopteama.chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.rules.StandardRules;
import com.oopteama.chess.exercise.FeedbackLadder;
import com.oopteama.chess.exercise.PuzzleExercise;
import com.oopteama.chess.exercise.Verdict;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.progress.AttemptStats;
import com.oopteama.chess.progress.Progress;
import com.oopteama.chess.progress.ProgressStore;
import com.oopteama.chess.progress.PuzzleState;
import com.oopteama.chess.progress.Recommender;
import com.oopteama.chess.progress.ThemeStat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 기능 명세 9장 "퍼즐 기록 기준": 빈 기록에서 같은 퍼즐을 HINT까지만 보고 완주 → REVEAL 후 완주 →
 * 공개 없이 재완주하면 시도·정답은 1,1 → 2,1 → 3,2이고 해결 상태·5점이 유지된다. 주제별 정답률은 2/3이며
 * 저장·재시작 후에도 같다. 중간 정답(Continue)에는 기록하지 않고 풀이 종료에 한 번만 기록한다.
 * 화면(프2 PuzzleScreen)이 할 기록 호출은 테스트의 View가 대신한다.
 */
class PuzzleRecordFlowTest {

    private static final String PROFILE = "default";
    private static final StandardRules RULES = new StandardRules();
    /** 1.Qxd8 Rxd8 2.Rxd8# 메이트인2. */
    private static final Puzzle MATE_IN_TWO = new Puzzle("p0003",
            new LinkedHashSet<>(List.of("메이트인2", "백랭크")),
            "3rr1k1/5ppp/8/8/8/8/3Q1PPP/3R2K1 w - - 0 1", List.of("d2d8", "e8d8", "d1d8"), "");

    @TempDir
    Path userDir;

    @Test
    void hintRevealAndCleanRunsRecordOncePerFinish() throws Exception {
        ProgressStore store = new ProgressStore(new DataPaths(userDir));
        Progress progress = store.load(PROFILE);
        List<AttemptStats> afterEachRun = new ArrayList<>();

        SwingUtilities.invokeAndWait(() -> {
            PuzzleExercise exercise = new PuzzleExercise(MATE_IN_TWO);
            RecordingScreen screen = new RecordingScreen(progress);
            ExerciseController controller = new ExerciseController(exercise, new BoardPanel(), screen);
            screen.controller = controller;
            controller.start();

            // 1회차: 오답 2번(HINT)까지만 보고 완주 → 해결로 기록
            wrong(controller, exercise, "d2d7", "d2d6");
            assertEquals(FeedbackLadder.Stage.HINT, controller.stage());
            solve(controller, exercise);
            afterEachRun.add(progress.puzzleAttempts(MATE_IN_TWO.id()));

            // 2회차: 오답 3번(REVEAL) 뒤 완주 → 시도만 기록
            controller.restart();
            wrong(controller, exercise, "d2d7", "d2d6", "d2d5");
            solve(controller, exercise);
            afterEachRun.add(progress.puzzleAttempts(MATE_IN_TWO.id()));

            // 3회차: 공개 없이 완주
            controller.restart();
            solve(controller, exercise);
            afterEachRun.add(progress.puzzleAttempts(MATE_IN_TWO.id()));

            assertEquals(List.of(Verdict.Continue.class, Verdict.Solved.class,
                            Verdict.Continue.class, Verdict.Solved.class,
                            Verdict.Continue.class, Verdict.Solved.class),
                    screen.answers.stream().map(Object::getClass).toList());
        });

        assertEquals(List.of(new AttemptStats(1, 1), new AttemptStats(2, 1), new AttemptStats(3, 2)), afterEachRun);
        assertEquals(PuzzleState.SOLVED, progress.puzzleState(MATE_IN_TWO.id()));
        assertEquals(5, progress.score());
        assertThemeRatio(progress);

        store.save(PROFILE, progress);
        Progress restarted = store.load(PROFILE);
        assertEquals(new AttemptStats(3, 2), restarted.puzzleAttempts(MATE_IN_TWO.id()));
        assertEquals(5, restarted.score());
        assertThemeRatio(restarted);
    }

    private static void assertThemeRatio(Progress progress) {
        List<ThemeStat> stats = new Recommender(List.of(), List.of(MATE_IN_TWO), progress).themeStats();
        assertEquals(List.of(new ThemeStat("메이트인2", 3, 2), new ThemeStat("백랭크", 3, 2)), stats);
        assertEquals(2.0 / 3.0, stats.get(0).ratio(), 1e-9);
    }

    private static Move uci(PuzzleExercise exercise, String uci) {
        return RULES.fromUci(exercise.position(), uci).orElseThrow();
    }

    private static void wrong(ExerciseController controller, PuzzleExercise exercise, String... moves) {
        for (String m : moves) {
            controller.onMove(uci(exercise, m));
        }
    }

    private static void solve(ExerciseController controller, PuzzleExercise exercise) {
        while (!controller.solved()) {
            controller.onMove(exercise.solution().orElseThrow());
        }
    }

    /** 프2의 PuzzleScreen이 할 일: 풀이가 끝났을 때만 공개 사용 여부로 한 번 기록한다. */
    private static final class RecordingScreen implements ExerciseController.View {
        private final Progress progress;
        ExerciseController controller;
        final List<Verdict> answers = new ArrayList<>();

        RecordingScreen(Progress progress) {
            this.progress = progress;
        }

        @Override
        public void showPrompt(String prompt) {
        }

        @Override
        public void showVerdict(Verdict verdict, FeedbackLadder.Stage stage) {
            if (verdict instanceof Verdict.Wrong) {
                return;
            }
            answers.add(verdict);
            if (verdict instanceof Verdict.Solved) {
                progress.recordPuzzle(MATE_IN_TWO.id(), !controller.revealUsed());
            }
        }

        @Override
        public void setNextEnabled(boolean enabled) {
        }
    }

    @Test
    void puzzleRecordIsSavedWithTheSpecKey() throws IOException {
        ProgressStore store = new ProgressStore(new DataPaths(userDir));
        Progress progress = store.load(PROFILE);
        progress.recordPuzzle(MATE_IN_TWO.id(), true);
        store.save(PROFILE, progress);
        String saved = Files.readString(new DataPaths(userDir).profileFile(PROFILE));
        assertTrue(saved.contains("puzzle.attempt.p0003=1,1"), saved);
    }
}
