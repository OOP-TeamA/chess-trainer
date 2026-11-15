package com.oopteama.chess.progress;

/**
 * 난이도 하나의 AI 대전 전적(사용자 기준). 저장 키는 {@code ai.record.<난이도 key>=승,패,무}.
 *
 * @param wins   사용자가 이긴 대국 수(0 이상)
 * @param losses 사용자가 진 대국 수(기권 포함, 0 이상)
 * @param draws  무승부 수(0 이상)
 */
public record AiRecord(int wins, int losses, int draws) {

    /**
     * 범위를 검사한다.
     *
     * @throws IllegalArgumentException 음수가 있을 때
     */
    public AiRecord {
        if (wins < 0 || losses < 0 || draws < 0) {
            throw new IllegalArgumentException("전적은 0 이상이어야 합니다: %d,%d,%d".formatted(wins, losses, draws));
        }
    }
}
