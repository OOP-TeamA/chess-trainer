package com.oopteama.chess.ui;

/**
 * 왼쪽 메뉴에서 고를 수 있는 화면 목록입니다.
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

    /** 메뉴에 보여 줄 이름. */
    public String label() {
        return label;
    }
}
