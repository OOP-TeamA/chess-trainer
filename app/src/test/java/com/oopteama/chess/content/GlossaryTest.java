package com.oopteama.chess.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** GLOS-01·02·05: 용어집의 조회, 링크 이름 해석, 검색. */
class GlossaryTest {

    private static final GlossaryEntry FORK = entry("fork", "포크", "Fork", TermCategory.TACTIC, "나이트 포크");
    private static final GlossaryEntry PIN = entry("pin", "핀", "Pin", TermCategory.TACTIC);
    private static final GlossaryEntry EN_PASSANT = entry("en-passant", "앙파상", "En passant", TermCategory.RULE);
    private static final GlossaryEntry OPPOSITION = entry("opposition", "오포지션", "Opposition", TermCategory.ENDGAME);
    private static final GlossaryEntry FEN = entry("fen", "FEN", "Forsyth–Edwards Notation", TermCategory.NOTATION);

    private final Glossary glossary = new Glossary(List.of(FORK, PIN, EN_PASSANT, OPPOSITION, FEN));

    private static GlossaryEntry entry(String id, String ko, String en, TermCategory category, String... aliases) {
        return new GlossaryEntry(id, ko, en, category, ko + "의 정의다.", Optional.empty(), Set.of(),
                List.of(), List.of(aliases), Optional.empty());
    }

    private static List<String> ids(List<GlossaryEntry> entries) {
        return entries.stream().map(GlossaryEntry::id).toList();
    }

    @Test
    void allIsSortedByKoreanName() {
        // 영문으로 시작하는 이름이 먼저, 그 뒤는 가나다순
        assertEquals(List.of("fen", "en-passant", "opposition", "fork", "pin"), ids(glossary.all()));
    }

    @Test
    void byIdMatchesExactIdOnly() {
        assertEquals(Optional.of(FORK), glossary.byId("fork"));
        assertEquals(Optional.empty(), glossary.byId("Fork"));
        assertEquals(Optional.empty(), glossary.byId("포크"));
    }

    @Test
    void resolveAcceptsIdKoEnAndAliasIgnoringCaseAndSpaces() {
        assertEquals(Optional.of(FORK), glossary.resolve("fork"));
        assertEquals(Optional.of(FORK), glossary.resolve("포크"));
        assertEquals(Optional.of(FORK), glossary.resolve("FORK"));
        assertEquals(Optional.of(FORK), glossary.resolve("나이트포크"));
        assertEquals(Optional.of(EN_PASSANT), glossary.resolve(" en  PASSANT "));
        assertEquals(Optional.of(EN_PASSANT), glossary.resolve("en-passant"));
        assertEquals(Optional.empty(), glossary.resolve("퀸 희생"));
        assertEquals(Optional.empty(), glossary.resolve(""));
    }

    @Test
    void duplicateIdKeepsFirstAndWarns() {
        GlossaryEntry second = entry("fork", "갈래", "Second", TermCategory.BASIC);
        try (TestLog log = TestLog.capture(Glossary.class)) {
            Glossary g = new Glossary(List.of(FORK, second));
            assertEquals(List.of(FORK), g.all());
            assertEquals(Optional.empty(), g.resolve("갈래"));
            assertEquals(1, log.messages().size());
            assertTrue(log.messages().get(0).contains("용어 ID 'fork'가 중복"), log.messages().get(0));
        }
    }

    @Test
    void nameClashGoesToIdFirstThenEarlierEntryAndWarns() {
        GlossaryEntry check = entry("check", "체크", "Check", TermCategory.RULE);
        GlossaryEntry thief = entry("thief", "도둑", "Thief", TermCategory.BASIC, "check", "핀");
        try (TestLog log = TestLog.capture(Glossary.class)) {
            Glossary g = new Glossary(List.of(thief, PIN, check));
            assertEquals(Optional.of(check), g.resolve("check")); // 다른 용어의 ID가 별칭보다 우선
            assertEquals(Optional.of(thief), g.resolve("핀"));     // 이름끼리는 먼저 나온 용어
            assertEquals(Optional.of(PIN), g.resolve("pin"));
            assertEquals(2, log.messages().size(), () -> String.join("\n", log.messages()));
        }
    }

    @Test
    void sameEntryMayRepeatItsOwnName() {
        try (TestLog log = TestLog.capture(Glossary.class)) {
            new Glossary(List.of(FEN)); // id "fen"과 ko "FEN"은 같은 이름
            assertEquals(List.of(), log.messages());
        }
    }

    @Test
    void searchMatchesPartOfKoreanOrEnglishName() {
        // GLOS-02 수용 기준: "fork", "포크", "포" 모두 포크를 찾음
        assertEquals(List.of("fork"), ids(glossary.search("fork", Optional.empty())));
        assertEquals(List.of("fork"), ids(glossary.search("포크", Optional.empty())));
        assertEquals(List.of("opposition", "fork"), ids(glossary.search("포", Optional.empty())));
        assertEquals(List.of("en-passant"), ids(glossary.search("EN PASS", Optional.empty())));
        assertEquals(List.of(), ids(glossary.search("스큐어", Optional.empty())));
    }

    @Test
    void searchDoesNotMatchIdOrAlias() {
        assertEquals(List.of(), ids(glossary.search("en-passant", Optional.empty())));
        assertEquals(List.of(), ids(glossary.search("나이트", Optional.empty())));
    }

    @Test
    void emptyQueryFiltersByCategoryOnly() {
        assertEquals(glossary.all(), glossary.search("", Optional.empty()));
        assertEquals(glossary.all(), glossary.search("   ", Optional.empty()));
        assertEquals(List.of("fork", "pin"), ids(glossary.search("", Optional.of(TermCategory.TACTIC))));
        assertEquals(List.of(), ids(glossary.search("", Optional.of(TermCategory.OPENING))));
    }

    @Test
    void queryAndCategoryCombine() {
        assertEquals(List.of("fork"), ids(glossary.search("포", Optional.of(TermCategory.TACTIC))));
        assertEquals(List.of(), ids(glossary.search("포크", Optional.of(TermCategory.RULE))));
    }

    @Test
    void choseongOnlyQueryMatchesInitialConsonants() {
        // GLOS-05: ㅍㅋ → 포크
        assertEquals(List.of("fork"), ids(glossary.search("ㅍㅋ", Optional.empty())));
        assertEquals(List.of("en-passant"), ids(glossary.search("ㅇㅍㅅ", Optional.empty())));
        assertEquals(List.of("en-passant", "opposition", "fork", "pin"), ids(glossary.search("ㅍ", Optional.empty())));
        assertEquals(List.of(), ids(glossary.search("ㅋㅍ", Optional.empty())));
        assertEquals(List.of(), ids(glossary.search("ㅍ크", Optional.empty()))); // 초성만일 때만 초성 비교
    }

    @Test
    void categoryLabelsMatchSpec() {
        assertEquals(List.of("기본", "규칙", "기보 표기", "전술", "전략", "오프닝", "엔드게임"),
                List.of(TermCategory.values()).stream().map(TermCategory::label).toList());
    }
}
