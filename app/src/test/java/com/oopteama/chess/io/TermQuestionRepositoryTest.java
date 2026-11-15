package com.oopteama.chess.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.content.GlossaryEntry;
import com.oopteama.chess.content.TermCategory;
import com.oopteama.chess.content.TermQuestion;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** QUIZ-05: 용어 4지선다 문항 로드와 검증. */
class TermQuestionRepositoryTest {

    /** 정상 문항. 줄 번호: 1 @term-question. */
    private static final String Q1 = """
            @term-question q0001
            term=fork
            question=포크는 어떤 전술인가요?
            option.1=한 기물이 둘 이상을 동시에 공격
            option.2=킹을 움직여 룩을 보호
            option.3=폰이 두 칸 전진
            option.4=기물을 교환
            answer=1
            explanation=포크를 두면 상대는 둘 이상에 동시에 대응해야 합니다.
            """;

    private static final String Q2 = Q1.replace("q0001", "q0002").replace("term=fork", "term=pin")
            .replace("answer=1", "answer=4").replace("explanation=포크를 두면 상대는 둘 이상에 동시에 대응해야 합니다.\n", "");

    private static final Glossary GLOSSARY = new Glossary(List.of(term("fork", "포크"), term("pin", "핀")));

    private final TestLog log = TestLog.capture(TermQuestionRepository.class);

    @AfterEach
    void releaseLog() {
        log.close();
    }

    private static GlossaryEntry term(String id, String ko) {
        return new GlossaryEntry(id, ko, id, TermCategory.TACTIC, ko + "의 정의다.", Optional.empty(), Set.of(),
                List.of(), List.of(), Optional.empty());
    }

    private static List<TermQuestion> load(String text) throws IOException {
        return new TermQuestionRepository(GLOSSARY).loadAll(new StringReader(text), "q.txt");
    }

    private static List<String> ids(List<TermQuestion> questions) {
        return questions.stream().map(TermQuestion::id).toList();
    }

    @Test
    void readsQuestionAndConvertsAnswerToZeroBasedIndex() throws IOException {
        TermQuestion q = load(Q1).get(0);
        assertEquals("q0001", q.id());
        assertEquals("fork", q.termId());
        assertEquals("포크는 어떤 전술인가요?", q.prompt());
        assertEquals(List.of("한 기물이 둘 이상을 동시에 공격", "킹을 움직여 룩을 보호", "폰이 두 칸 전진", "기물을 교환"),
                q.choices());
        assertEquals(0, q.answerIndex());
        assertEquals("포크를 두면 상대는 둘 이상에 동시에 대응해야 합니다.", q.explanation());
        assertEquals(List.of(), log.messages());
    }

    @Test
    void missingOrEmptyExplanationFallsBackToTermDefinition() throws IOException {
        assertEquals("핀의 정의다.", load(Q2).get(0).explanation());
        assertEquals(3, load(Q2).get(0).answerIndex());
        assertEquals("포크의 정의다.", load(Q1.replaceFirst("explanation=.*", "explanation=")).get(0).explanation());
    }

    @Test
    void keepsFileOrderAndSkipsOnlyBrokenQuestions() throws IOException {
        String q3 = Q1.replace("q0001", "q0003");
        List<TermQuestion> loaded = load(Q2 + "\n" + Q1.replace("answer=1", "answer=5") + "\n" + q3);
        assertEquals(List.of("q0002", "q0003"), ids(loaded));
        assertEquals(1, log.messages().size());
        assertTrue(log.messages().get(0).startsWith("q.txt:10: 문항 q0001: answer는 1~4입니다: '5'"),
                log.messages().get(0));
    }

    @Test
    void duplicateIdKeepsFirstValidQuestion() throws IOException {
        // 같은 ID의 첫 블록이 잘못됐으면 다음 유효 블록이 남는다
        String broken = Q1.replace("term=fork", "term=nothing");
        String later = Q1.replace("포크는 어떤 전술인가요?", "나중 문항");
        List<TermQuestion> loaded = load(broken + "\n" + Q1 + "\n" + later);
        assertEquals(List.of("q0001"), ids(loaded));
        assertEquals("포크는 어떤 전술인가요?", loaded.get(0).prompt());
        assertEquals(2, log.messages().size(), () -> String.join("\n", log.messages()));
        assertTrue(log.messages().get(0).contains("용어집에 없는 용어 ID입니다: 'nothing'"), log.messages().get(0));
        assertTrue(log.messages().get(1).startsWith("q.txt:21: 문항 ID 'q0001'가 중복"), log.messages().get(1));
    }

    @Test
    void readsSharedFirstDemoSample() throws IOException {
        Glossary glossary;
        try (Reader in = Files.newBufferedReader(TestFixtures.firstDemo().resolve("glossary.txt"), StandardCharsets.UTF_8)) {
            glossary = new Glossary(new GlossaryRepository().loadAll(in, "glossary.txt"));
        }
        assertEquals(10, glossary.all().size());
        List<TermQuestion> questions;
        try (Reader in = Files.newBufferedReader(TestFixtures.firstDemo().resolve("term-quiz.txt"), StandardCharsets.UTF_8)) {
            questions = new TermQuestionRepository(glossary).loadAll(in, "term-quiz.txt");
        }
        assertEquals(List.of("q0001", "q0002", "q0003", "q0004", "q0005", "q0006", "q0007", "q0008", "q0009", "q0010"),
                ids(questions));
        // 파일의 answer 1,2,3,4,2,3,1,4,2,3
        assertEquals(List.of(0, 1, 2, 3, 1, 2, 0, 3, 1, 2), questions.stream().map(TermQuestion::answerIndex).toList());
        assertEquals(List.of(), log.messages());
    }

    static Stream<Arguments> brokenQuestions() {
        return Stream.of(
            Arguments.of("다른 블록", Q1.replace("@term-question", "@term"), "용어 퀴즈 파일에 쓸 수 없는 블록입니다: @term"),
            Arguments.of("ID 없음", Q1.replace("@term-question q0001", "@term-question"), "문항 ID는 영문 소문자·숫자·-·_로 씁니다: ''"),
            Arguments.of("ID에 쉼표", Q1.replace("q0001", "q1,q2"), "문항 ID는 영문 소문자·숫자·-·_로 씁니다: 'q1,q2'"),
            Arguments.of("term 없음", Q1.replace("term=fork\n", ""), "필수 키 'term'"),
            Arguments.of("용어집에 없는 term", Q1.replace("term=fork", "term=포크"), "문항 q0001: 용어집에 없는 용어 ID입니다: '포크'"),
            Arguments.of("question 없음", Q1.replace("question=포크는 어떤 전술인가요?\n", ""), "필수 키 'question'"),
            Arguments.of("선택지 3개", Q1.replace("option.4=기물을 교환\n", ""), "필수 키 'option.4'"),
            Arguments.of("선택지 비어 있음", Q1.replace("option.2=킹을 움직여 룩을 보호", "option.2="), "필수 키 'option.2'"),
            Arguments.of("선택지 5개", Q1 + "option.5=다섯째\n", "문항 q0001: 선택지는 option.1~option.4 네 개만 씁니다"),
            Arguments.of("answer 없음", Q1.replace("answer=1\n", ""), "필수 키 'answer'"),
            Arguments.of("answer 0", Q1.replace("answer=1", "answer=0"), "문항 q0001: answer는 1~4입니다: '0'"),
            Arguments.of("answer 숫자 아님", Q1.replace("answer=1", "answer=첫째"), "answer는 1~4입니다: '첫째'"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenQuestions")
    void brokenQuestionIsSkippedWithLocatedWarning(String name, String text, String expected) throws IOException {
        assertEquals(List.of(), load(text));
        assertEquals(1, log.messages().size(), () -> String.join("\n", log.messages()));
        assertTrue(log.messages().get(0).startsWith("q.txt:1: "), log.messages().get(0));
        assertTrue(log.messages().get(0).contains(expected), log.messages().get(0));
    }
}
