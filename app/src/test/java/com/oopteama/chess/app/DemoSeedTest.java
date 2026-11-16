package com.oopteama.chess.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.SavedGame;
import com.oopteama.chess.nav.Route;
import com.oopteama.chess.progress.AiRecord;
import com.oopteama.chess.progress.AttemptStats;
import com.oopteama.chess.progress.Progress;
import com.oopteama.chess.progress.ProgressStore;
import com.oopteama.chess.progress.PuzzleState;
import com.oopteama.chess.progress.Recommendation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 시연용 더미 기록 {@code demo/userdata}({@code :app:runDemo}가 쓰는 원본)가 번들 콘텐츠와 맞고 경고 없이 읽히는지 확인한다.
 * 원본은 건드리지 않도록 임시 폴더에 복사해서 읽는다.
 */
class DemoSeedTest {

    @TempDir
    Path userDir;

    /** Gradle 테스트의 작업 폴더는 app 모듈 폴더다. */
    private static Path seed() {
        for (Path candidate : List.of(Path.of("..", "demo", "userdata"), Path.of("demo", "userdata"))) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("demo/userdata를 찾지 못했습니다: " + Path.of("").toAbsolutePath());
    }

    private DataPaths copiedSeed() throws IOException {
        Path from = seed();
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path p : walk.toList()) {
                Path to = userDir.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(to);
                } else {
                    Files.copy(p, to);
                }
            }
        }
        return new DataPaths(userDir);
    }

    @Test
    void demoProfileLoadsWithoutWarningsAndShowsAPartlyDoneLearner() throws IOException {
        DataPaths paths = copiedSeed();
        AppContext context;
        try (TestLog log = TestLog.captureAll()) {
            context = AppContext.load(paths);
            assertEquals(List.of(), log.messages().stream().filter(m -> !m.startsWith("콘텐츠 로드:")).toList(),
                    "더미 기록은 경고 없이 읽혀야 한다");
        }

        assertEquals("demo", context.profiles().active().id());
        assertEquals("민준", context.profiles().active().name());

        Progress p = context.progress();
        assertEquals(Set.of("rook", "knight"), p.lessonsDone());
        assertEquals(PuzzleState.SOLVED, p.puzzleState("p0001"));
        assertEquals(PuzzleState.FAILED, p.puzzleState("p0007"));
        assertEquals(PuzzleState.UNSEEN, p.puzzleState("p0010"));
        assertEquals(new AttemptStats(20, 14), p.termQuizAttempts());
        assertEquals(new AiRecord(3, 1, 0), p.aiRecord(Difficulty.INTRO));
        assertEquals(new AiRecord(1, 2, 1), p.aiRecord(Difficulty.BEGINNER));
        assertEquals(OptionalDouble.of(0.86), p.bestGuess("legal-1750"));
        // 레슨 2개 ×10 + 해결 퍼즐 6개 ×5 + 정답 용어 문항 7개 ×1
        assertEquals(57, p.score());

        Recommendation next = context.recommender().next().orElseThrow();
        assertInstanceOf(Route.Lesson.class, next.route());
        assertEquals("castling", ((Route.Lesson) next.route()).lessonId(), "다음 추천은 아직 안 한 캐슬링 레슨");

        Set<String> savedNames = context.games().saved().stream()
                .map(SavedGame::file).map(f -> f.getFileName().toString()).collect(Collectors.toSet());
        assertEquals(Set.of("20261114-193012-intro.txt", "20261115-210544-beginner.txt"), savedNames);
    }

    @Test
    void everyDemoIdExistsInBundledContent() throws IOException {
        AppContext context = AppContext.load(copiedSeed());
        Set<String> lessonIds = context.lessons().stream().map(l -> l.id()).collect(Collectors.toSet());
        Set<String> puzzleIds = context.puzzles().stream().map(pz -> pz.id()).collect(Collectors.toSet());
        Set<String> questionIds = context.termQuestions().stream().map(q -> q.id()).collect(Collectors.toSet());
        Set<String> gameIds = context.games().bundled().stream().map(g -> g.id()).collect(Collectors.toSet());

        for (String line : Files.readAllLines(seed().resolve("profiles").resolve("demo.txt"))) {
            String key = line.substring(0, line.indexOf('='));
            String value = line.substring(line.indexOf('=') + 1);
            switch (key) {
                case "lesson.done" -> assertTrue(lessonIds.containsAll(List.of(value.split(","))), line);
                case "quiz.correct.unique" -> assertTrue(questionIds.containsAll(List.of(value.split(","))), line);
                default -> {
                    if (key.startsWith("puzzle.attempt.")) {
                        assertTrue(puzzleIds.contains(key.substring("puzzle.attempt.".length())), line);
                    } else if (key.startsWith("replay.best.")) {
                        assertTrue(gameIds.contains(key.substring("replay.best.".length())), line);
                    }
                }
            }
        }
    }

    @Test
    void otherProfilesLoadWithoutWarnings() throws IOException {
        ProgressStore store = new ProgressStore(copiedSeed());
        try (TestLog log = TestLog.captureAll()) {
            assertEquals(90, store.load("learner-2").score());
            assertEquals(18, store.load("learner-3").score());
            assertEquals(List.of(), log.messages());
        }
    }
}
