package com.oopteama.chess.content;

import java.util.List;
import java.util.Objects;

/**
 * 레슨 하나(RULE-01). 목록의 배치는 {@code lessons/index.txt} 순서를 따르고 order는 표시용 번호다.
 *
 * @param id      레슨 ID. 학습 기록의 완료 목록에 저장된다
 * @param order   표시 순서 번호(양의 정수)
 * @param title   제목
 * @param summary 한 줄 요약
 * @param steps   단계. 하나 이상
 */
public record Lesson(String id, int order, String title, String summary, List<LessonStep> steps) {

    /**
     * 단계 목록은 불변 사본으로 바꾼다.
     *
     * @throws IllegalArgumentException order가 양수가 아니거나 단계가 없을 때
     */
    public Lesson {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (order <= 0) {
            throw new IllegalArgumentException("order는 양의 정수여야 합니다: " + order);
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("단계가 없습니다: " + id);
        }
    }
}
