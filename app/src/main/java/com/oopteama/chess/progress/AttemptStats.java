package com.oopteama.chess.progress;

/**
 * 시도 횟수와 성공 횟수.
 *
 * <p>퍼즐의 {@code solved}는 정답 공개(REVEAL) 전에 끝까지 푼 횟수(HINT 허용),
 * 용어 퀴즈의 {@code solved}는 정답 답안 수다.
 *
 * @param attempted 시도 횟수(0 이상)
 * @param solved    성공 횟수(0 이상, 시도 이하)
 */
public record AttemptStats(int attempted, int solved) {

    /**
     * 범위를 검사한다.
     *
     * @throws IllegalArgumentException 음수이거나 성공이 시도보다 많을 때
     */
    public AttemptStats {
        if (attempted < 0 || solved < 0 || solved > attempted) {
            throw new IllegalArgumentException("0 ≤ 정답 ≤ 시도여야 합니다: %d,%d".formatted(attempted, solved));
        }
    }
}
