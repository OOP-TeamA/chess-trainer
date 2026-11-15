package com.oopteama.chess.exercise;

import java.util.Objects;

/**
 * 용어 4지선다 문항 하나의 채점 결과(QUIZ-05). 화면은 이것으로 정답과 해설을 보여준다.
 *
 * @param questionId  채점한 문항 ID. {@code Progress.recordTermQuiz}에 그대로 넘긴다
 * @param correct     고른 선택지가 정답인지
 * @param answerIndex 정답 선택지의 위치(0~3)
 * @param explanation 해설
 */
public record TermAnswer(String questionId, boolean correct, int answerIndex, String explanation) {

    /** questionId와 explanation은 null 불가. */
    public TermAnswer {
        Objects.requireNonNull(questionId, "questionId");
        Objects.requireNonNull(explanation, "explanation");
    }
}
