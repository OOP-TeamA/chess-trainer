package com.oopteama.chess.io;

import com.oopteama.chess.content.GlossaryEntry;
import com.oopteama.chess.content.TermCategory;
import com.oopteama.chess.content.TermTry;
import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.StandardRules;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 용어집 파일의 {@code @term <id>} 블록 하나를 용어 하나로 읽는다(GLOS-06). 잘못된 용어는 그 용어만 건너뛰고
 * {@code 파일명:행번호: 설명}을 경고 로그에 남긴다(SYS-03).
 * <ul>
 *   <li>ID는 영문 소문자·숫자·{@code -}·{@code _}. ko, en, category, def 필수</li>
 *   <li>category는 한국어 표시명(기본, 규칙, 기보 표기, 전술, 전략, 오프닝, 엔드게임)</li>
 *   <li>기보 표기 분류가 아니면 fen과 highlight도 필수. fen은 문법만 확인한다</li>
 *   <li>직접 해보기는 try.fen, try.text, try.goal을 함께 적는다(try.success는 선택). 표준 규칙으로 두므로
 *       try.fen은 {@link StandardRules#validate}를 통과하고 목표를 이룰 수 있어야 한다</li>
 * </ul>
 * ID 중복은 {@link com.oopteama.chess.content.Glossary} 생성자가, related의 ID 존재는 콘텐츠 로드 테스트가 확인한다.
 */
public final class GlossaryRepository extends PerBlockRepository<GlossaryEntry> {

    private static final String TERM = "term";
    private static final String TRY_PREFIX = "try.";
    private static final StandardRules STANDARD = new StandardRules();

    @Override
    protected GlossaryEntry map(Block b) throws MalformedDataException {
        if (!b.type().equals(TERM)) {
            throw BlockChecks.error(b, "용어집 파일에 쓸 수 없는 블록입니다: "
                    + (b.type().isEmpty() ? "@term 앞의 키" : "@" + b.type()));
        }
        String id = BlockChecks.id(b, "용어");
        String where = "용어 " + id;
        String ko = b.require("ko");
        String en = b.require("en");
        TermCategory category = category(b, where);
        String definition = b.require("def");

        Optional<String> fen = Optional.of(b.get("fen", "")).filter(value -> !value.isEmpty());
        if (category != TermCategory.NOTATION) {
            b.require("fen");
            b.require("highlight");
        }
        if (fen.isPresent()) {
            BlockChecks.fen(b, where, "fen");
        }
        Set<Square> highlight = BlockChecks.squares(b, where, "highlight");
        if (fen.isEmpty() && !highlight.isEmpty()) {
            throw BlockChecks.error(b, where + ": fen 없이 highlight만 있습니다");
        }
        return new GlossaryEntry(id, ko, en, category, definition, fen, highlight,
                b.list("related"), b.list("aliases"), tryStep(b, where));
    }

    private static TermCategory category(Block b, String where) throws MalformedDataException {
        String label = b.require("category");
        for (TermCategory c : TermCategory.values()) {
            if (c.label().equals(label)) {
                return c;
            }
        }
        throw BlockChecks.error(b, where + ": 알 수 없는 분류입니다: '" + label + "'");
    }

    /** try.* 키가 하나도 없으면 empty. 하나라도 있으면 try.fen, try.text, try.goal이 모두 있어야 한다. */
    private static Optional<TermTry> tryStep(Block b, String where) throws MalformedDataException {
        if (b.sub(TRY_PREFIX).values().isEmpty()) {
            return Optional.empty();
        }
        String tryWhere = where + " 직접 해보기";
        String fen = b.require("try.fen");
        Position start = BlockChecks.fen(b, tryWhere, "try.fen");
        String text = b.require("try.text");
        StepGoal goal = BlockChecks.goal(b, tryWhere, "try.goal");
        List<String> problems = STANDARD.validate(start);
        if (!problems.isEmpty()) {
            throw BlockChecks.error(b, tryWhere + ": 표준 규칙에 맞지 않는 국면입니다: " + String.join(" ", problems));
        }
        Optional<String> unreachable = BlockChecks.unreachable(start, STANDARD, goal);
        if (unreachable.isPresent()) {
            throw BlockChecks.error(b, tryWhere + ": " + unreachable.get());
        }
        return Optional.of(new TermTry(fen, goal, text, b.get("try.success", "")));
    }
}
