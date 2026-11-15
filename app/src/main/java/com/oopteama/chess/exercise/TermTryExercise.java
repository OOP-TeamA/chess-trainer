package com.oopteama.chess.exercise;

import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.TermTry;

import java.util.Set;

/** 용어의 직접 해보기를 푸는 연습(GLOS-07). 레슨 단계와 같은 채점을 쓰고 표준 규칙으로 둔다. */
public final class TermTryExercise extends GoalExercise {

    /**
     * @param tryStep GlossaryRepository가 검증해 읽은 직접 해보기. 힌트는 목표의 기본 힌트, 오답 메시지는 기본 문구를 쓴다
     */
    public TermTryExercise(TermTry tryStep) {
        super(tryStep.fen(), RulesKind.STANDARD.rules(), tryStep.goal(), Set.of(),
                tryStep.text(), "", tryStep.success());
    }
}
