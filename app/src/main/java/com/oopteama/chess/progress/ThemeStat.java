package com.oopteama.chess.progress;

import java.util.Objects;

/**
 * 퍼즐 주제 하나의 누적 시도·정답(QUIZ-03). 그 주제에 속한 퍼즐별 {@code puzzle.attempt} 횟수를 합산한다.
 *
 * @param theme     주제(예: "포크")
 * @param attempted 시도 횟수 합(0 이상)
 * @param solved    정답 횟수 합(0 이상, attempted 이하)
 */
public record ThemeStat(String theme, int attempted, int solved) {

    /**
     * @throws IllegalArgumentException 횟수가 음수이거나 정답이 시도보다 많을 때
     */
    public ThemeStat {
        Objects.requireNonNull(theme, "theme");
        if (attempted < 0 || solved < 0 || solved > attempted) {
            throw new IllegalArgumentException("잘못된 횟수: 시도 %d, 정답 %d".formatted(attempted, solved));
        }
    }

    /**
     * 정답률.
     *
     * @return 정답 ÷ 시도(0~1). 시도가 없으면 0
     */
    public double ratio() {
        return attempted == 0 ? 0.0 : (double) solved / attempted;
    }
}
