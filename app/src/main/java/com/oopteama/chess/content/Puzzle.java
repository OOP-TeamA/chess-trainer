package com.oopteama.chess.content;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 퍼즐 하나(QUIZ-01). {@code data/puzzles.txt}의 {@code @puzzle <id>} 블록 하나에 대응한다.
 *
 * <p>정답 수열은 사용자 수와 상대 응수를 번갈아 담는다. 사용자는 짝수 위치(0, 2, …)의 수를 두고
 * 홀수 위치의 수는 자동으로 두어진다. 메이트인2는 사용자, 상대, 사용자 3수다.
 *
 * @param id          퍼즐 ID. 학습 기록의 {@code puzzle.attempt.<id>}에 저장된다
 * @param themes      주제(파일의 {@code theme=} 쉼표 목록). 하나 이상, 파일 순서 유지
 * @param fen         시작 국면
 * @param solutionUci 정답 수열(UCI, 상대 응수 포함). 하나 이상, 사용자 수로 끝난다
 * @param explain     해설. {@code [[용어]]} 링크를 쓸 수 있다. 없으면 빈 문자열
 */
public record Puzzle(String id, Set<String> themes, String fen, List<String> solutionUci, String explain) {

    /** 메이트 주제의 접두어(메이트인1, 메이트인2 …). */
    private static final String MATE_THEME_PREFIX = "메이트";

    /**
     * 주제는 순서를 유지한 불변 집합, 정답 수열은 불변 목록으로 바꾼다.
     *
     * @throws IllegalArgumentException 주제나 정답 수열이 비었거나, 정답 수열이 상대 응수로 끝날 때(길이가 짝수)
     */
    public Puzzle {
        Objects.requireNonNull(id, "id");
        themes = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(themes, "themes")));
        Objects.requireNonNull(fen, "fen");
        solutionUci = List.copyOf(Objects.requireNonNull(solutionUci, "solutionUci"));
        Objects.requireNonNull(explain, "explain");
        if (themes.isEmpty()) {
            throw new IllegalArgumentException("퍼즐 주제가 없습니다: " + id);
        }
        if (solutionUci.isEmpty()) {
            throw new IllegalArgumentException("퍼즐 정답 수열이 없습니다: " + id);
        }
        if (solutionUci.size() % 2 == 0) {
            throw new IllegalArgumentException("정답 수열은 사용자 수로 끝나야 합니다(홀수 길이): " + id);
        }
    }

    /**
     * 메이트 주제인지. 마지막 수에서 메이트가 되는 어떤 수든 정답으로 받는 기준이다(QUIZ-02).
     *
     * @return 주제 중 "메이트"로 시작하는 항목이 있으면 true
     */
    public boolean isMateTheme() {
        return themes.stream().anyMatch(t -> t.startsWith(MATE_THEME_PREFIX));
    }

    /**
     * 사용자가 두어야 하는 수의 개수.
     *
     * @return 정답 수열 중 사용자 차례 수(메이트인2면 2)
     */
    public int userMoveCount() {
        return (solutionUci.size() + 1) / 2;
    }
}
