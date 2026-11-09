package com.oopteama.chess.exercise;

import com.oopteama.chess.content.LessonStep;

/** 레슨 단계 하나를 푸는 연습(RULE-02). 단계의 설명이 문제 설명이 된다. */
public final class LessonStepExercise extends GoalExercise {

    /**
     * @param step LessonRepository가 검증해 읽은 단계
     */
    public LessonStepExercise(LessonStep step) {
        super(step.fen(), step.rules().rules(), step.goal(), step.hint(),
                step.text(), step.fail(), step.success());
    }
}
