package com.oopteama.chess.progress;

import java.util.Objects;

/**
 * 프로그램 전체 설정(SYS-04). 프로필과 무관하며 {@code prefs.txt} 한 파일에 저장한다.
 *
 * @param boardTheme       보드 색상 테마 이름(예: {@code walnut}). 사용 가능한 이름 목록은 화면(ui) 몫이다
 * @param coordinates      보드 좌표 표시
 * @param legalMoves       이동 가능 칸 표시
 * @param lastMove         마지막 수 강조
 * @param blunderAlert     AI 대국의 실수 경고
 * @param unlockAllLessons 레슨 전체 잠금 해제
 */
public record Prefs(String boardTheme, boolean coordinates, boolean legalMoves,
                    boolean lastMove, boolean blunderAlert, boolean unlockAllLessons) {

    /** 기본 보드 테마(기능 명세 6장 {@code prefs.txt} 기본값). */
    public static final String DEFAULT_BOARD_THEME = "walnut";

    private static final Prefs DEFAULTS = new Prefs(DEFAULT_BOARD_THEME, true, true, true, true, true);

    /**
     * 테마 이름을 검사한다. 파일의 한 줄 값으로 그대로 저장·복원되도록
     * 빈 이름, 앞뒤 공백, 제어 문자(줄바꿈 포함)를 거부한다.
     *
     * @throws IllegalArgumentException 테마 이름이 규칙에 맞지 않을 때
     */
    public Prefs {
        checkBoardTheme(boardTheme);
    }

    /** 테마 이름 규칙 검사. {@link PrefsStore}가 파일 값을 읽을 때도 쓴다. */
    static String checkBoardTheme(String boardTheme) {
        Objects.requireNonNull(boardTheme, "boardTheme");
        if (boardTheme.isBlank() || !boardTheme.equals(boardTheme.strip())
                || boardTheme.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("잘못된 보드 테마 이름: '" + boardTheme + "'");
        }
        return boardTheme;
    }

    /**
     * 기능 명세 6장의 기본값: 테마 {@code walnut}, 나머지 항목은 모두 켬.
     *
     * @return 기본 설정
     */
    public static Prefs defaults() {
        return DEFAULTS;
    }
}
