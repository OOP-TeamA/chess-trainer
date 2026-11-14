package com.oopteama.chess.ai;

/** 탐색 제한. timeLimitMillis 0 = 시간 제한 없음. */
public record SearchLimits(int depth, long timeLimitMillis, int randomTopN) {

    public SearchLimits {
        if (depth < 1) throw new IllegalArgumentException("depth >= 1: " + depth);
        if (timeLimitMillis < 0) throw new IllegalArgumentException("timeLimitMillis >= 0");
        if (randomTopN < 1) throw new IllegalArgumentException("randomTopN >= 1");
    }

    /** AI-09: 난이도별 시간 상한. 값은 실기기 측정 후 조정(초기값). */
    public static SearchLimits of(Difficulty d) {
        long millis = switch (d) {
            case INTRO -> 1_000;
            case BEGINNER -> 1_500;
            case INTERMEDIATE -> 3_000;
            case ADVANCED -> 5_000;
        };
        return new SearchLimits(d.depth(), millis, d.randomTopN());
    }
}
