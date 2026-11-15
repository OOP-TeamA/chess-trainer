package com.oopteama.chess.ai;

/** 난이도. key()가 저장 파일 키의 단일 출처(G11). */
public enum Difficulty {
    INTRO("intro", "입문", 1, 3, new MaterialEvaluator()),
    BEGINNER("beginner", "초급", 2, 1, new MaterialEvaluator()),
    INTERMEDIATE("intermediate", "중급", 3, 1, new PstEvaluator()),
    ADVANCED("advanced", "고급", 4, 1, new PstEvaluator());

    private final String key;
    private final String label;
    private final int depth;
    private final int randomTopN;
    private final Evaluator evaluator;

    Difficulty(String key, String label, int depth, int randomTopN, Evaluator evaluator) {
        this.key = key;
        this.label = label;
        this.depth = depth;
        this.randomTopN = randomTopN;
        this.evaluator = evaluator;
    }

    public int depth() { return depth; }               // 1, 2, 3, 4
    public int randomTopN() { return randomTopN; }     // 3, 1, 1, 1
    public Evaluator evaluator() { return evaluator; } // 상태 없는 객체라 공유 가능
    public String label() { return label; }
    public String key() { return key; }
}
