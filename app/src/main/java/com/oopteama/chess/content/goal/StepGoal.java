package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 학습 단계 하나의 목표(RULE-02). 레슨 단계와 용어 직접 해보기가 함께 쓴다.
 *
 * <p>구현은 불변이며 상태가 없다. 입력 수의 합법성은 호출하는 쪽(exercise)이 먼저 확인한다.
 * 파일의 {@code goal=} 값은 {@link #parse(String)}로 만든다.
 */
public interface StepGoal {

    /**
     * 사용자가 제출할 입력의 종류.
     *
     * @return 수를 두면 {@link InputMode#MOVE}, 칸을 고르면 {@link InputMode#SQUARE}
     */
    InputMode inputMode();

    /**
     * 수가 목표를 이뤘는지 판정한다. 칸 선택 목표는 항상 false다.
     *
     * @param before 수를 두기 전 국면(변경하지 않는다)
     * @param move   before에서 합법인 수
     * @param after  move를 둔 뒤의 국면(변경하지 않는다)
     * @param rules  단계의 규칙
     * @return 정답이면 true
     */
    boolean accepts(Position before, Move move, Position after, RuleSet rules);

    /**
     * 고른 칸이 정답인지 판정한다. 이동 목표는 항상 false다.
     *
     * @param square 사용자가 고른 칸
     * @return 정답 칸이면 true
     */
    default boolean acceptsSquare(Square square) {
        return false;
    }

    /**
     * 정답 공개 단계에서 보여줄 수. 칸 선택 목표이거나 정답 수가 없으면 empty.
     *
     * @param start 단계의 시작 국면(변경하지 않는다)
     * @param rules 단계의 규칙
     * @return start에서 합법인 정답 수
     */
    Optional<Move> solution(Position start, RuleSet rules);

    /**
     * 칸 선택 목표의 모든 정답 칸. 이동 목표는 빈 집합.
     *
     * @return 불변 집합
     */
    default Set<Square> solutionSquares() {
        return Set.of();
    }

    /**
     * 힌트 단계에서 강조할 칸. 파일에 {@code hint=}가 있으면 호출한 쪽이 그것을 우선한다.
     *
     * @param start 단계의 시작 국면(변경하지 않는다)
     * @param rules 단계의 규칙
     * @return 불변 집합. 줄 힌트가 없으면 빈 집합
     */
    Set<Square> defaultHint(Position start, RuleSet rules);

    /**
     * 목표 문자열을 읽는다. 문법: {@code any-move | move:d4f5|d4f3 | square:e4|e5 | check | mate}.
     *
     * <p>대소문자와 {@code |} 앞뒤 공백은 무시하고, 같은 대안이 반복되면 하나로 합친다.
     * 수는 UCI 표기({@code e2e4}, 승진은 {@code e7e8q})로 쓴다. 국면을 모르므로 수의 합법성은 검사하지 않는다.
     *
     * @param spec 파일의 {@code goal=} 값
     * @return 목표
     * @throws MalformedDataException 비었거나 문법에 맞지 않을 때. 파일 위치를 모르므로 source는
     *                                {@code "<input>"}, line은 0이며 저장소가 블록 위치를 붙여 다시 던진다
     */
    static StepGoal parse(String spec) throws MalformedDataException {
        if (spec == null || spec.isBlank()) {
            throw malformed("목표가 비어 있습니다", spec);
        }
        String s = spec.strip().toLowerCase(Locale.ROOT);
        try {
            if (s.startsWith("move:")) {
                return new MoveGoal(alternatives(s.substring("move:".length()), spec));
            }
            if (s.startsWith("square:")) {
                Set<Square> squares = new LinkedHashSet<>();
                for (String name : alternatives(s.substring("square:".length()), spec)) {
                    squares.add(Square.of(name));
                }
                return new SquareGoal(squares);
            }
        } catch (IllegalArgumentException e) {
            throw malformed(e.getMessage(), spec);
        }
        return switch (s) {
            case "any-move" -> new AnyMoveGoal();
            case "check" -> new CheckGoal();
            case "mate" -> new MateGoal();
            default -> throw malformed("알 수 없는 목표 형식입니다(any-move, move:, square:, check, mate)", spec);
        };
    }

    /** {@code |}로 나눈 대안. 빈 항목은 버리고 중복은 합친다. 하나도 없으면 오류. */
    private static List<String> alternatives(String body, String spec) throws MalformedDataException {
        Set<String> out = new LinkedHashSet<>();
        for (String part : body.split("\\|")) {
            String item = part.strip();
            if (!item.isEmpty()) {
                out.add(item);
            }
        }
        if (out.isEmpty()) {
            throw malformed("접두어 뒤에 값이 없습니다", spec);
        }
        return new ArrayList<>(out);
    }

    private static MalformedDataException malformed(String reason, String spec) {
        return new MalformedDataException("잘못된 목표 '%s': %s".formatted(spec, reason), "<input>", 0);
    }
}
