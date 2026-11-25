package com.oopteama.chess.training;

import com.oopteama.chess.model.Side;

import java.util.List;

/**
 * 레슨 하나(기본 튜토리얼 또는 유명 오프닝)입니다. 여러 단계(LessonStep)로 이루어집니다.
 *
 * @param playerSide 학습자가 두는 진영 (보드를 이 진영 기준으로 보여 줌)
 */
public record Lesson(String id, Category category, String title, String summary,
                     Side playerSide, List<LessonStep> steps) {

    /** 레슨 종류. */
    public enum Category {
        TUTORIAL("기본 튜토리얼"),
        OPENING("유명 오프닝");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
