package com.oopteama.chess.ui;

/**
 * 애플리케이션의 최상위 화면 식별자입니다.
 */
public enum ScreenId {
    HOME("홈"),
    LESSONS("레슨"),
    PUZZLES("퍼즐"),
    AI_GAME("AI 대전"),
    LOCAL_GAME("로컬 대전"),
    PROFILES("프로필"),
    SETTINGS("설정");

    private final String label;

    ScreenId(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
