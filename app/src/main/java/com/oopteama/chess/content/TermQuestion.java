package com.oopteama.chess.content;

import java.util.List;
import java.util.Objects;

/**
 * 용어 4지선다 문항 하나(QUIZ-05). 파일의 {@code answer}는 1~4, 여기의 answerIndex는 0~3이다.
 *
 * @param id          문항 ID. 학습 기록의 정답 문항 목록에 저장된다
 * @param termId      연결된 용어집 ID
 * @param prompt      질문
 * @param choices     선택지 4개(파일의 option.1~option.4 순서)
 * @param answerIndex 정답 선택지의 위치(0~3)
 * @param explanation 해설
 */
public record TermQuestion(String id, String termId, String prompt, List<String> choices,
                           int answerIndex, String explanation) {

    /** 선택지 개수. */
    private static final int CHOICE_COUNT = 4;

    /**
     * 선택지는 불변 사본으로 바꾼다.
     *
     * @throws IllegalArgumentException 선택지가 4개가 아니거나 answerIndex가 0~3 밖일 때
     */
    public TermQuestion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(termId, "termId");
        Objects.requireNonNull(prompt, "prompt");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        Objects.requireNonNull(explanation, "explanation");
        if (choices.size() != CHOICE_COUNT) {
            throw new IllegalArgumentException("선택지는 %d개여야 합니다: %d개".formatted(CHOICE_COUNT, choices.size()));
        }
        if (answerIndex < 0 || answerIndex >= CHOICE_COUNT) {
            throw new IllegalArgumentException("answerIndex는 0~%d입니다: %d".formatted(CHOICE_COUNT - 1, answerIndex));
        }
    }
}
