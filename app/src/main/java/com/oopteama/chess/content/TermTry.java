package com.oopteama.chess.content;

import com.oopteama.chess.content.goal.StepGoal;

import java.util.Objects;

/**
 * 용어의 직접 해보기(GLOS-07). 용어집 파일의 {@code try.*} 키에서 읽으며 표준 규칙으로 둔다.
 *
 * @param fen     시작 국면
 * @param goal    목표
 * @param text    문제 설명
 * @param success 정답 메시지. 없으면 빈 문자열
 */
public record TermTry(String fen, StepGoal goal, String text, String success) {

    /** 모든 필드는 null 불가. */
    public TermTry {
        Objects.requireNonNull(fen, "fen");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(success, "success");
    }
}
