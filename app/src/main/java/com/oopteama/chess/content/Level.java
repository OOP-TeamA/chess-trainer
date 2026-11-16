package com.oopteama.chess.content;

import java.util.Objects;

/**
 * 명국 기보의 난이도(GAME-01). PGN의 팀 정의 태그 {@code [Level "입문"]}과 UCI 대안 블록의
 * {@code level=입문} 값은 {@link #label()}의 한국어 표시명이다.
 */
public enum Level {
    INTRO("입문"),
    BEGINNER("초급"),
    INTERMEDIATE("중급"),
    ADVANCED("상급");

    /** AI 난이도({@code ai.Difficulty})의 표시명과 맞추기 위해 받아들이는 상급의 다른 이름. */
    private static final String ADVANCED_ALIAS = "고급";

    private final String label;

    Level(String label) {
        this.label = label;
    }

    /**
     * 화면과 파일에 쓰는 한국어 표시명.
     *
     * @return "입문", "초급", "중급", "상급"
     */
    public String label() {
        return label;
    }

    /**
     * 한국어 표시명으로 난이도를 찾는다. 앞뒤 공백은 무시하고 "고급"은 상급으로 읽는다.
     *
     * @param ko 표시명(예: {@code "입문"})
     * @return 난이도
     * @throws IllegalArgumentException 알 수 없는 표시명일 때
     */
    public static Level fromLabel(String ko) {
        String s = Objects.requireNonNull(ko, "ko").strip();
        for (Level level : values()) {
            if (level.label.equals(s)) {
                return level;
            }
        }
        if (s.equals(ADVANCED_ALIAS)) {
            return ADVANCED;
        }
        throw new IllegalArgumentException("알 수 없는 난이도입니다(입문, 초급, 중급, 상급): '" + ko + "'");
    }
}
