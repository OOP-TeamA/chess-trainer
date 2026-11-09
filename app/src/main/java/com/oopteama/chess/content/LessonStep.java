package com.oopteama.chess.content;

import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.Square;

import java.util.Objects;
import java.util.Set;

/**
 * 레슨의 한 단계(RULE-02). 설명을 읽고 보드 위 행동으로 목표를 이루면 끝난다(P1).
 *
 * @param fen       시작 국면
 * @param rules     적용할 규칙
 * @param text      설명
 * @param highlight 처음부터 강조할 칸. 없으면 빈 집합
 * @param goal      목표
 * @param hint      두 번째 오답에 강조할 칸. 비었으면 목표의 기본 힌트를 쓴다
 * @param fail      오답 메시지. 없으면 빈 문자열
 * @param success   정답 메시지. 없으면 빈 문자열
 */
public record LessonStep(String fen, RulesKind rules, String text, Set<Square> highlight,
                         StepGoal goal, Set<Square> hint, String fail, String success) {

    /** 모든 필드는 null 불가. 칸 집합은 불변 사본으로 바꾼다. */
    public LessonStep {
        Objects.requireNonNull(fen, "fen");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(text, "text");
        highlight = Set.copyOf(Objects.requireNonNull(highlight, "highlight"));
        Objects.requireNonNull(goal, "goal");
        hint = Set.copyOf(Objects.requireNonNull(hint, "hint"));
        Objects.requireNonNull(fail, "fail");
        Objects.requireNonNull(success, "success");
    }
}
