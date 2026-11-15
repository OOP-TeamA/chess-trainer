package com.oopteama.chess.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oopteama.chess.content.TermLinks.Segment.Link;
import com.oopteama.chess.content.TermLinks.Segment.Text;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** RULE-06·GLOS-04: 설명 글의 [[용어]] 링크 해석과 HTML 변환. */
class TermLinksTest {

    private final Glossary glossary = new Glossary(List.of(
            entry("pin", "핀", "Pin"),
            entry("sacrifice", "희생", "Sacrifice", "퀸 희생")));

    private static GlossaryEntry entry(String id, String ko, String en, String... aliases) {
        return new GlossaryEntry(id, ko, en, TermCategory.TACTIC, "정의", Optional.empty(), Set.of(),
                List.of(), List.of(aliases), Optional.empty());
    }

    @Test
    void plainTextIsOneSegment() {
        assertEquals(List.of(new Text("링크 없는 글")), TermLinks.parse("링크 없는 글"));
        assertEquals(List.of(), TermLinks.parse(""));
    }

    @Test
    void bareLinkUsesLabelAsTarget() {
        assertEquals(List.of(new Text("f3 나이트를 "), new Link("핀", "핀"), new Text("한다.")),
                TermLinks.parse("f3 나이트를 [[핀]]한다."));
    }

    @Test
    void pipeSeparatesTargetAndLabel() {
        assertEquals(List.of(new Link("sacrifice", "퀸 희생"), new Text(". 나이트가 d7을 비운다.")),
                TermLinks.parse("[[sacrifice|퀸 희생]]. 나이트가 d7을 비운다."));
        assertEquals(List.of(new Link("pin", "핀")), TermLinks.parse("[[ pin | 핀 ]]"));
        assertEquals(List.of(new Link("a", "b|c")), TermLinks.parse("[[a|b|c]]"));
    }

    @Test
    void adjacentLinksStaySeparate() {
        assertEquals(List.of(new Link("핀", "핀"), new Link("희생", "희생")), TermLinks.parse("[[핀]][[희생]]"));
    }

    @Test
    void malformedBracketsStayAsText() {
        assertEquals(List.of(new Text("닫히지 않은 [[핀")), TermLinks.parse("닫히지 않은 [[핀"));
        assertEquals(List.of(new Text("빈 [[]] 괄호")), TermLinks.parse("빈 [[]] 괄호"));
        assertEquals(List.of(new Text("[[|표시문]] [[대상|]]")), TermLinks.parse("[[|표시문]] [[대상|]]"));
        assertEquals(List.of(new Text("닫기만 ]] 있음")), TermLinks.parse("닫기만 ]] 있음"));
    }

    @Test
    void innermostOpeningBracketPairsWithClose() {
        assertEquals(List.of(new Text("[[앞 "), new Link("핀", "핀")), TermLinks.parse("[[앞 [[핀]]"));
    }

    @Test
    void htmlLinksOnlyResolvedTermsByEntryId() {
        assertEquals("f3 나이트를 <a href=\"term:pin\">핀</a>한다.",
                TermLinks.toHtml("f3 나이트를 [[핀]]한다.", glossary));
        assertEquals("<a href=\"term:sacrifice\">퀸 희생</a>", TermLinks.toHtml("[[퀸 희생]]", glossary));
        assertEquals("<a href=\"term:sacrifice\">과감한 수</a>", TermLinks.toHtml("[[sacrifice|과감한 수]]", glossary));
        assertEquals("없는 용어", TermLinks.toHtml("[[없는 용어]]", glossary));
        assertEquals("표시문", TermLinks.toHtml("[[nothing|표시문]]", glossary));
    }

    @Test
    void htmlEscapesTextAndKeepsLineBreaks() {
        assertEquals("a &lt; b &amp; c &gt; d &quot;e&quot;<br>둘째 줄",
                TermLinks.toHtml("a < b & c > d \"e\"\n둘째 줄", glossary));
        assertEquals("<a href=\"term:pin\">&lt;핀&gt;</a>", TermLinks.toHtml("[[pin|<핀>]]", glossary));
    }
}
