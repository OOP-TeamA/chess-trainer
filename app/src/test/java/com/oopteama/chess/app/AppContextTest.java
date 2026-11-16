package com.oopteama.chess.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.SandboxRules;
import com.oopteama.chess.core.rules.StandardRules;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.progress.AttemptStats;
import com.oopteama.chess.progress.Progress;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 첫 시연 조립: 번들 콘텐츠 로드, 기본 프로필 기록, 기록 변경 시 자동 저장과 재실행 후 유지. */
class AppContextTest {

    @TempDir
    Path userDir;

    @Test
    void loadsBundledContentForAnEmptyUserFolder() throws IOException {
        AppContext context;
        try (TestLog log = TestLog.captureAll()) {
            context = AppContext.load(new DataPaths(userDir));
            assertEquals(List.of(), log.messages().stream().filter(m -> !m.startsWith("콘텐츠 로드:")).toList(),
                    "번들 콘텐츠와 빈 사용자 폴더는 경고 없이 읽혀야 한다");
        }

        assertTrue(context.lessons().stream().anyMatch(l -> l.id().equals("knight")), "나이트 레슨");
        assertTrue(context.glossary().byId("fork").isPresent(), "용어 fork");
        assertTrue(context.termQuestions().size() >= 10, "문항 수: " + context.termQuestions().size());
        assertTrue(context.standardRules() instanceof StandardRules);
        assertTrue(context.sandboxRules() instanceof SandboxRules);
        assertEquals(20, context.standardRules().legalMoves(Position.initial()).size());
        assertEquals(0, context.progress().score());
        assertEquals(userDir.toAbsolutePath().normalize(),
                context.paths().profilesIndexFile().getParent().getParent());
    }

    @Test
    void progressChangesAreSavedAutomaticallyAndSurviveRestart() throws IOException {
        DataPaths paths = new DataPaths(userDir);
        AppContext first = AppContext.load(paths);
        Progress progress = first.progress();
        Path file = paths.profileFile("default");
        assertFalse(Files.exists(file), "아무것도 하지 않았으면 기록 파일을 만들지 않는다");

        progress.markLessonDone("knight");
        assertTrue(Files.exists(file), "레슨 완료 즉시 저장");
        progress.recordTermQuiz("q0001", true);
        progress.recordTermQuiz("q0008", false);

        AppContext restarted = AppContext.load(paths);
        assertEquals(Set.of("knight"), restarted.progress().lessonsDone());
        assertEquals(new AttemptStats(2, 1), restarted.progress().termQuizAttempts());
        assertEquals(11, restarted.progress().score());
    }

    @Test
    void saveProgressWritesTheActiveProfileOnDemand() throws IOException {
        DataPaths paths = new DataPaths(userDir);
        AppContext context = AppContext.load(paths);
        context.saveProgress();
        assertTrue(Files.exists(paths.profileFile("default")));
        assertSame(context.progress(), context.progress(), "같은 기록 객체를 계속 돌려준다");
    }
}
