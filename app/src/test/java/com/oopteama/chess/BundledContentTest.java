package com.oopteama.chess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.content.GlossaryEntry;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.content.TermCategory;
import com.oopteama.chess.content.TermLinks;
import com.oopteama.chess.content.TermQuestion;
import com.oopteama.chess.content.TermTry;
import com.oopteama.chess.exercise.TermTryExercise;
import com.oopteama.chess.exercise.Verdict;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.GlossaryRepository;
import com.oopteama.chess.io.LessonRepository;
import com.oopteama.chess.io.TermQuestionRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 전체 콘텐츠 로드(기능 명세 9장): JAR에 들어갈 {@code data/}의 레슨·용어·용어 퀴즈가 경고 없이 읽히고,
 * 서로 가리키는 ID와 [[용어]] 링크의 대상이 모두 있는지 확인한다. 퍼즐과 기보 재생은 해당 로더가 생기면 더한다.
 */
class BundledContentTest {

    /** 첫 시연에 필요한 최소 문항 수(QUIZ-05). */
    private static final int QUIZ_SET_SIZE = 10;

    @TempDir
    Path userDir;

    private DataPaths paths;
    private TestLog log;

    @BeforeEach
    void setUp() {
        paths = new DataPaths(userDir);
        log = TestLog.captureAll();
    }

    @AfterEach
    void releaseLog() {
        log.close();
    }

    private Glossary glossary() throws IOException {
        try (Reader in = paths.openResource("data/glossary.txt")) {
            return new Glossary(new GlossaryRepository().loadAll(in, "glossary.txt"));
        }
    }

    private List<Lesson> lessons() throws IOException {
        List<Lesson> out = new ArrayList<>();
        for (String name : paths.index("data/lessons")) {
            try (Reader in = paths.openResource("data/lessons/" + name)) {
                List<Lesson> loaded = new LessonRepository().loadAll(in, name);
                assertEquals(1, loaded.size(), name + "에서 레슨 하나를 읽어야 합니다");
                out.addAll(loaded);
            }
        }
        return out;
    }

    private List<TermQuestion> questions(Glossary glossary) throws IOException {
        try (Reader in = paths.openResource("data/term-quiz.txt")) {
            return new TermQuestionRepository(glossary).loadAll(in, "term-quiz.txt");
        }
    }

    private void assertNoWarnings() {
        assertEquals(List.of(), log.messages(), () -> String.join("\n", log.messages()));
    }

    /** 글 안의 링크가 모두 용어집에서 해석되는지 확인한다. */
    private static void assertLinksResolve(Glossary glossary, String where, String text) {
        for (TermLinks.Segment segment : TermLinks.parse(text)) {
            if (segment instanceof TermLinks.Segment.Link link) {
                assertTrue(glossary.resolve(link.target()).isPresent(),
                        () -> where + ": 용어집에 없는 링크 [[" + link.target() + "]]");
            }
        }
    }

    @Test
    void glossaryLoadsWithoutWarningsAndReferencesExist() throws IOException {
        Glossary glossary = glossary();
        assertTrue(glossary.all().size() >= 20, "용어 수: " + glossary.all().size());
        for (GlossaryEntry e : glossary.all()) {
            for (String related : e.related()) {
                assertTrue(glossary.byId(related).isPresent(), () -> e.id() + ": 없는 related ID '" + related + "'");
            }
            // GLOS-06 수용 기준: 기보 표기 분류를 제외한 모든 항목에 예시 FEN
            assertTrue(e.category() == TermCategory.NOTATION || e.fen().isPresent(), e.id());
            assertLinksResolve(glossary, "용어 " + e.id(), e.definition());
        }
        assertNoWarnings();
    }

    @Test
    void everyTermTryIsSolvedByItsRevealedSolution() throws IOException {
        for (GlossaryEntry e : glossary().all()) {
            if (e.tryStep().isEmpty()) {
                continue;
            }
            TermTry tryStep = e.tryStep().get();
            TermTryExercise exercise = new TermTryExercise(tryStep);
            Verdict verdict = exercise.submit(exercise.solution().orElseThrow(() -> new AssertionError(e.id())));
            assertInstanceOf(Verdict.Solved.class, verdict, e.id());
            assertLinksResolve(glossary(), "용어 " + e.id() + " 직접 해보기", tryStep.text() + tryStep.success());
        }
        assertNoWarnings();
    }

    @Test
    void lessonsLoadInIndexOrderWithUniqueIds() throws IOException {
        Glossary glossary = glossary();
        List<Lesson> lessons = lessons();
        assertTrue(!lessons.isEmpty(), "레슨이 하나 이상 있어야 합니다");
        assertEquals(lessons.size(), lessons.stream().map(Lesson::id).distinct().count(), "레슨 ID 중복");
        for (Lesson lesson : lessons) {
            for (LessonStep step : lesson.steps()) {
                assertLinksResolve(glossary, "레슨 " + lesson.id(), step.text() + step.fail() + step.success());
            }
        }
        assertNoWarnings();
    }

    @Test
    void termQuizHasAFullSetLinkedToGlossary() throws IOException {
        Glossary glossary = glossary();
        List<TermQuestion> questions = questions(glossary);
        assertTrue(questions.size() >= QUIZ_SET_SIZE, "문항 수: " + questions.size());
        for (TermQuestion q : questions) {
            assertTrue(glossary.byId(q.termId()).isPresent(), q.id());
            assertLinksResolve(glossary, "문항 " + q.id(), q.prompt() + q.explanation());
        }
        assertNoWarnings();
    }

    @Test
    void gameCommentLinksResolve() throws IOException {
        Glossary glossary = glossary();
        List<String> games = paths.index("data/games");
        assertTrue(!games.isEmpty(), "기보가 하나 이상 있어야 합니다");
        for (String name : games) {
            try (BufferedReader in = new BufferedReader(paths.openResource("data/games/" + name))) {
                assertLinksResolve(glossary, "기보 " + name, in.lines().collect(Collectors.joining("\n")));
            }
        }
        assertNoWarnings();
    }
}
