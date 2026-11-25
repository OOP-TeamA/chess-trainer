package com.oopteama.chess.ai;

/**
 * AI 난이도입니다. 몇 수 앞까지 읽는지(depth)에 따라 강해집니다.
 * 입문은 한 수 앞만 보고, 비슷하게 좋은 수 몇 개 중에서 무작위로 골라 일부러 실수도 합니다.
 */
public enum Difficulty {
    BEGINNER("입문", 1),
    EASY("초급", 2),
    MEDIUM("중급", 3),
    HARD("상급", 4);

    private final String label;
    private final int depth;

    Difficulty(String label, int depth) {
        this.label = label;
        this.depth = depth;
    }

    /** 화면에 보여 줄 이름. */
    public String label() {
        return label;
    }

    /** 몇 수(반수) 앞까지 읽는지. */
    public int depth() {
        return depth;
    }

    @Override
    public String toString() {
        return label;
    }
}
