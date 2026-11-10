package com.oopteama.chess.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import com.oopteama.chess.content.GlossaryEntry;
import com.oopteama.chess.content.TermCategory;
import com.oopteama.chess.content.TermTry;
import com.oopteama.chess.content.goal.MoveGoal;
import com.oopteama.chess.core.Square;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** GLOS-06·SYS-03: 용어집 파일 로드와 로드 시점 검증. */
class GlossaryRepositoryTest {

    /** 직접 해보기가 있는 정상 용어. 줄 번호: 1 @term. */
    private static final String FORK = """
            @term fork
            ko=포크
            en=Fork
            aliases=나이트 포크, 양수걸이
            category=전술
            def=한 기물이 상대 기물 둘 이상을 동시에 공격하는 수다.
            fen=r3k3/2N5/8/8/8/8/8/4K3 b - - 0 1
            highlight=c7,a8,e8
            related=pin,skewer
            try.fen=r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1
            try.text=나이트로 킹과 룩을 동시에 공격해 보세요.
            try.goal=move:b5c7
            try.success=나이트가 킹과 룩을 동시에 공격합니다.
            """;

    private static final String SAN = """
            @term san
            ko=표준 대수 표기
            en=Standard Algebraic Notation
            category=기보 표기
            def=개별 수를 나타내는 표기다.
            """;

    private final TestLog log = TestLog.capture(PerBlockRepository.class);

    @AfterEach
    void releaseLog() {
        log.close();
    }

    private static List<GlossaryEntry> load(String text) throws IOException {
        return new GlossaryRepository().loadAll(new StringReader(text), "g.txt");
    }

    @Test
    void readsAllFieldsOfATerm() throws IOException {
        GlossaryEntry fork = load(FORK).get(0);
        assertEquals("fork", fork.id());
        assertEquals("포크", fork.ko());
        assertEquals("Fork", fork.en());
        assertEquals(TermCategory.TACTIC, fork.category());
        assertEquals("한 기물이 상대 기물 둘 이상을 동시에 공격하는 수다.", fork.definition());
        assertEquals(Optional.of("r3k3/2N5/8/8/8/8/8/4K3 b - - 0 1"), fork.fen());
        assertEquals(Set.of(Square.of("c7"), Square.of("a8"), Square.of("e8")), fork.highlight());
        assertEquals(List.of("pin", "skewer"), fork.related());
        assertEquals(List.of("나이트 포크", "양수걸이"), fork.aliases());
        TermTry tryStep = fork.tryStep().orElseThrow();
        assertEquals("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", tryStep.fen());
        assertEquals(new MoveGoal(List.of("b5c7")), tryStep.goal());
        assertEquals("나이트로 킹과 룩을 동시에 공격해 보세요.", tryStep.text());
        assertEquals("나이트가 킹과 룩을 동시에 공격합니다.", tryStep.success());
        assertEquals(List.of(), log.messages());
    }

    @Test
    void notationTermNeedsNoExampleAndOptionalKeysDefaultToEmpty() throws IOException {
        GlossaryEntry san = load(SAN).get(0);
        assertEquals(TermCategory.NOTATION, san.category());
        assertEquals(Optional.empty(), san.fen());
        assertEquals(Set.of(), san.highlight());
        assertEquals(List.of(), san.related());
        assertEquals(List.of(), san.aliases());
        assertEquals(Optional.empty(), san.tryStep());
        assertEquals(List.of(), log.messages());
    }

    @Test
    void trySuccessIsOptional() throws IOException {
        GlossaryEntry fork = load(FORK.replace("try.success=나이트가 킹과 룩을 동시에 공격합니다.\n", "")).get(0);
        assertEquals("", fork.tryStep().orElseThrow().success());
    }

    @Test
    void brokenTermIsSkippedAndOthersStillLoad() throws IOException {
        // SYS-03 수용 기준: 용어 파일에 깨진 블록이 있어도 나머지 용어는 정상 표시
        List<GlossaryEntry> loaded = load(SAN + "\n" + FORK.replace("category=전술", "category=전슬") + "\n"
                + SAN.replace("@term san", "@term fen"));
        assertEquals(List.of("san", "fen"), loaded.stream().map(GlossaryEntry::id).toList());
        assertEquals(1, log.messages().size());
        assertTrue(log.messages().get(0).startsWith("g.txt:7: 용어 fork: 알 수 없는 분류입니다: '전슬'"),
                log.messages().get(0));
    }

    static Stream<Arguments> brokenTerms() {
        return Stream.of(
            Arguments.of("@term이 아닌 블록", FORK.replace("@term fork", "@lesson fork"), "용어집 파일에 쓸 수 없는 블록입니다: @lesson"),
            Arguments.of("ID 없음", FORK.replace("@term fork", "@term"), "g.txt:1: 용어 ID는 영문 소문자·숫자·-·_로 씁니다: ''"),
            Arguments.of("ID에 대문자", FORK.replace("@term fork", "@term Fork"), "용어 ID는 영문 소문자·숫자·-·_로 씁니다: 'Fork'"),
            Arguments.of("ko 없음", FORK.replace("ko=포크\n", ""), "필수 키 'ko'"),
            Arguments.of("en 비어 있음", FORK.replace("en=Fork", "en="), "필수 키 'en'"),
            Arguments.of("category 없음", FORK.replace("category=전술\n", ""), "필수 키 'category'"),
            Arguments.of("def 없음", FORK.replace("def=한 기물이 상대 기물 둘 이상을 동시에 공격하는 수다.\n", ""), "필수 키 'def'"),
            Arguments.of("기보 표기가 아닌데 fen 없음", FORK.replace("fen=r3k3/2N5/8/8/8/8/8/4K3 b - - 0 1\n", ""), "필수 키 'fen'"),
            Arguments.of("기보 표기가 아닌데 highlight 없음", FORK.replace("highlight=c7,a8,e8\n", ""), "필수 키 'highlight'"),
            Arguments.of("fen 형식 오류", FORK.replace("fen=r3k3/2N5", "fen=r3k3/2N9"), "용어 fork: fen 형식 오류"),
            Arguments.of("highlight 칸 오류", FORK.replace("highlight=c7,a8,e8", "highlight=c7,i9"), "highlight의 칸 이름이 잘못됐습니다: 'i9'"),
            Arguments.of("fen 없이 highlight만", SAN + "highlight=e4\n", "용어 san: fen 없이 highlight만 있습니다"),
            Arguments.of("try.goal 없음", FORK.replace("try.goal=move:b5c7\n", ""), "필수 키 'try.goal'"),
            Arguments.of("try.fen 없음", FORK.replace("try.fen=r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1\n", ""), "필수 키 'try.fen'"),
            Arguments.of("try.text 없음", FORK.replace("try.text=나이트로 킹과 룩을 동시에 공격해 보세요.\n", ""), "필수 키 'try.text'"),
            Arguments.of("try.goal 문법 오류", FORK.replace("try.goal=move:b5c7", "try.goal=jump"), "용어 fork 직접 해보기: 잘못된 목표 'jump'"),
            Arguments.of("try.fen 형식 오류", FORK.replace("try.fen=r3k3/8", "try.fen=r3k3/9"), "용어 fork 직접 해보기: try.fen 형식 오류"),
            Arguments.of("직접 해보기 국면에 킹 없음", FORK.replace("try.fen=r3k3/8/8/1N6/8/8/8/4K3", "try.fen=r3k3/8/8/1N6/8/8/8/8"),
                "용어 fork 직접 해보기: 표준 규칙에 맞지 않는 국면입니다"),
            Arguments.of("직접 해보기 정답 수를 둘 수 없음", FORK.replace("try.goal=move:b5c7", "try.goal=move:b5b6"),
                "용어 fork 직접 해보기: 정답 수 b5b6는 이 국면에서 둘 수 없습니다"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenTerms")
    void brokenTermIsSkippedWithLocatedWarning(String name, String text, String expected) throws IOException {
        assertEquals(List.of(), load(text));
        assertEquals(1, log.messages().size(), () -> String.join("\n", log.messages()));
        assertTrue(log.messages().get(0).startsWith("g.txt:1: "), log.messages().get(0));
        assertTrue(log.messages().get(0).contains(expected), log.messages().get(0));
    }
}
