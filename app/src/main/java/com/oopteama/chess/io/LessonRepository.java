package com.oopteama.chess.io;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.goal.CheckGoal;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.content.goal.MateGoal;
import com.oopteama.chess.content.goal.MoveGoal;
import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.core.rules.StandardRules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 레슨 파일 하나를 레슨 하나로 읽는다(RULE-01, RULE-02). 파일은 {@code @lesson <id>} 1개와 {@code @step} 1개 이상이다.
 *
 * <p>로드 시점 검증으로 체험형 원칙(P1)을 강제한다. 하나라도 어기면 레슨 전체를 건너뛰고
 * {@code 파일명:행번호: 설명}을 경고 로그에 남긴다.
 * <ul>
 *   <li>{@code @lesson}: ID는 영문 소문자·숫자·{@code -}·{@code _}. title, summary, 양의 정수 order 필수</li>
 *   <li>{@code @step}: fen, text, goal 필수. rules는 standard(기본값) 또는 sandbox. highlight·hint는 칸 목록</li>
 *   <li>standard 단계의 국면은 {@link StandardRules#validate}를 통과해야 한다</li>
 *   <li>체크·메이트 목표는 상대 킹이 국면에 있어야 한다(G7)</li>
 *   <li>이동 목표는 이룰 수 있어야 한다. {@code move:}는 적힌 모든 수가 합법이어야 하고,
 *       그 밖의 이동 목표는 정답 수가 하나 이상 있어야 한다</li>
 * </ul>
 * 목록의 배치는 {@code lessons/index.txt} 순서를 따르므로 이 저장소는 파일 하나만 다룬다.
 */
public final class LessonRepository extends BlockRepository<Lesson> {

    private static final Logger LOG = Logger.getLogger(LessonRepository.class.getName());

    private static final String LESSON = "lesson";
    private static final String STEP = "step";
    /** 레슨 ID. 학습 기록에 쉼표 목록으로 저장되므로 구분자로 쓰이는 문자를 막는다. */
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final StandardRules STANDARD = new StandardRules();

    @Override
    protected List<Lesson> assemble(List<Block> blocks) {
        try {
            return List.of(lesson(blocks));
        } catch (MalformedDataException e) {
            LOG.warning(e.getMessage() + " (레슨을 건너뜁니다)");
            return List.of();
        }
    }

    private static Lesson lesson(List<Block> blocks) throws MalformedDataException {
        if (blocks.isEmpty()) {
            throw new MalformedDataException("레슨 파일에 읽을 블록이 없습니다", "<unknown>", 0);
        }
        Block head = null;
        List<Block> stepBlocks = new ArrayList<>();
        for (Block b : blocks) {
            switch (b.type()) {
                case LESSON -> {
                    if (head != null) {
                        throw error(b, "@lesson 블록은 파일에 하나만 둡니다");
                    }
                    head = b;
                }
                case STEP -> stepBlocks.add(b);
                default -> throw error(b, "레슨 파일에 쓸 수 없는 블록입니다: "
                        + (b.type().isEmpty() ? "@lesson 앞의 키" : "@" + b.type()));
            }
        }
        if (head == null) {
            throw error(blocks.get(0), "@lesson 블록이 없습니다");
        }
        String id = head.id();
        if (!ID.matcher(id).matches()) {
            throw error(head, "레슨 ID는 영문 소문자·숫자·-·_로 씁니다: '" + id + "'");
        }
        String where = "레슨 " + id;
        String title = head.require("title");
        String summary = head.require("summary");
        int order = positiveInt(head, where, "order");
        if (stepBlocks.isEmpty()) {
            throw error(head, where + ": @step 블록이 하나 이상 있어야 합니다");
        }
        List<LessonStep> steps = new ArrayList<>();
        for (int i = 0; i < stepBlocks.size(); i++) {
            steps.add(step(stepBlocks.get(i), "%s %d단계".formatted(where, i + 1)));
        }
        return new Lesson(id, order, title, summary, steps);
    }

    private static LessonStep step(Block b, String where) throws MalformedDataException {
        String fen = b.require("fen");
        Position start;
        try {
            start = Fen.parse(fen);
        } catch (FenFormatException e) {
            throw error(b, where + ": fen 형식 오류: " + e.getMessage());
        }
        RulesKind kind = rulesKind(b, where);
        String text = b.require("text");
        String spec = b.require("goal");
        StepGoal goal;
        try {
            goal = StepGoal.parse(spec);
        } catch (MalformedDataException e) {
            throw error(b, where + ": " + e.getMessage());
        }
        Set<Square> highlight = squares(b, where, "highlight");
        Set<Square> hint = squares(b, where, "hint");
        checkPlayable(b, where, start, kind, goal);
        return new LessonStep(fen, kind, text, highlight, goal, hint, b.get("fail", ""), b.get("success", ""));
    }

    /** 국면이 규칙에 맞고 목표를 실제로 이룰 수 있는지 확인한다(P1, G7). */
    private static void checkPlayable(Block b, String where, Position start, RulesKind kind, StepGoal goal)
            throws MalformedDataException {
        if (kind == RulesKind.STANDARD) {
            List<String> problems = STANDARD.validate(start);
            if (!problems.isEmpty()) {
                throw error(b, where + ": 표준 규칙에 맞지 않는 국면입니다(교육용이면 rules=sandbox): "
                        + String.join(" ", problems));
            }
        }
        if ((goal instanceof CheckGoal || goal instanceof MateGoal)
                && start.kingSquare(start.sideToMove().opposite()).isEmpty()) {
            throw error(b, where + ": 체크·메이트 목표인데 상대 킹이 국면에 없습니다");
        }
        RuleSet rules = kind.rules();
        if (goal instanceof MoveGoal move) {
            for (String uci : move.uci()) {
                if (rules.fromUci(start, uci).isEmpty()) {
                    throw error(b, where + ": 정답 수 " + uci + "는 이 국면에서 둘 수 없습니다");
                }
            }
        } else if (goal.inputMode() == InputMode.MOVE && goal.solution(start, rules).isEmpty()) {
            throw error(b, where + ": 이 국면에는 목표를 이루는 수가 없습니다");
        }
    }

    private static RulesKind rulesKind(Block b, String where) throws MalformedDataException {
        String value = b.get("rules", "standard");
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "standard" -> RulesKind.STANDARD;
            case "sandbox" -> RulesKind.SANDBOX;
            default -> throw error(b, where + ": rules는 standard 또는 sandbox입니다: '" + value + "'");
        };
    }

    private static Set<Square> squares(Block b, String where, String key) throws MalformedDataException {
        Set<Square> out = new LinkedHashSet<>();
        for (String name : b.list(key)) {
            try {
                out.add(Square.of(name.toLowerCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw error(b, where + ": " + key + "의 칸 이름이 잘못됐습니다: '" + name + "'");
            }
        }
        return out;
    }

    private static int positiveInt(Block b, String where, String key) throws MalformedDataException {
        String value = b.require(key);
        try {
            int n = Integer.parseInt(value);
            if (n > 0) {
                return n;
            }
        } catch (NumberFormatException e) {
            // 아래에서 같은 메시지로 처리한다
        }
        throw error(b, where + ": " + key + "는 양의 정수여야 합니다: '" + value + "'");
    }

    /** 블록 위치를 붙인 형식 오류. */
    private static MalformedDataException error(Block b, String message) {
        return new MalformedDataException("%s:%d: %s".formatted(b.source(), b.line(), message), b.source(), b.line());
    }
}
