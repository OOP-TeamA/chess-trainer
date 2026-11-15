package com.oopteama.chess.content;

/** 용어 분류(GLOS-01). 용어집 파일의 {@code category=} 값은 {@link #label()}의 한국어 표시명이다. */
public enum TermCategory {
    BASIC("기본"),
    RULE("규칙"),
    NOTATION("기보 표기"),
    TACTIC("전술"),
    STRATEGY("전략"),
    OPENING("오프닝"),
    ENDGAME("엔드게임");

    private final String label;

    TermCategory(String label) {
        this.label = label;
    }

    /**
     * 화면과 파일에 쓰는 한국어 표시명.
     *
     * @return "기본", "규칙", "기보 표기", "전술", "전략", "오프닝", "엔드게임"
     */
    public String label() {
        return label;
    }
}
