package com.oopteama.chess.data;

/**
 * 앱 설정: 체스판 색 테마와 화면 표시 옵션입니다.
 */
public final class Settings {

    /** 체스판 색 테마 (밝은 칸 색, 어두운 칸 색). */
    public enum BoardTheme {
        WALNUT("월넛 우드", 0xEAD4AE, 0xA6754B),
        FOREST("토너먼트 그린", 0xEEEED2, 0x769656),
        SLATE("모던 슬레이트", 0xDEE3E6, 0x7D94A8);

        private final String label;
        private final int lightRgb;
        private final int darkRgb;

        BoardTheme(String label, int lightRgb, int darkRgb) {
            this.label = label;
            this.lightRgb = lightRgb;
            this.darkRgb = darkRgb;
        }

        public String label() {
            return label;
        }

        public int lightRgb() {
            return lightRgb;
        }

        public int darkRgb() {
            return darkRgb;
        }

        /** 저장된 이름으로 테마를 찾습니다. 모르는 이름이면 기본 테마(월넛)입니다. */
        public static BoardTheme parse(String name) {
            for (BoardTheme theme : values()) {
                if (theme.name().equalsIgnoreCase(name)) {
                    return theme;
                }
            }
            return WALNUT;
        }
    }

    private BoardTheme boardTheme = BoardTheme.WALNUT;
    private boolean showCoords = true;        // 체스판 좌표(a~h, 1~8) 표시
    private boolean showLegalMoves = true;    // 선택한 기물이 갈 수 있는 칸 표시
    private boolean showMoveInsight = true;   // 갈 수 있는 칸에 마우스를 올리면 수 설명 (퍼즐·AI·로컬 대전)
    private boolean highlightLastMove = true; // 직전에 둔 수 강조
    private boolean autoFlipLocal = false;    // 로컬 대전에서 차례마다 보드 뒤집기

    public BoardTheme boardTheme() {
        return boardTheme;
    }

    public void setBoardTheme(BoardTheme boardTheme) {
        if (boardTheme != null) {
            this.boardTheme = boardTheme;
        }
    }

    public boolean showCoords() {
        return showCoords;
    }

    public void setShowCoords(boolean showCoords) {
        this.showCoords = showCoords;
    }

    public boolean showLegalMoves() {
        return showLegalMoves;
    }

    public void setShowLegalMoves(boolean showLegalMoves) {
        this.showLegalMoves = showLegalMoves;
    }

    public boolean showMoveInsight() {
        return showMoveInsight;
    }

    public void setShowMoveInsight(boolean showMoveInsight) {
        this.showMoveInsight = showMoveInsight;
    }

    public boolean highlightLastMove() {
        return highlightLastMove;
    }

    public void setHighlightLastMove(boolean highlightLastMove) {
        this.highlightLastMove = highlightLastMove;
    }

    public boolean autoFlipLocal() {
        return autoFlipLocal;
    }

    public void setAutoFlipLocal(boolean autoFlipLocal) {
        this.autoFlipLocal = autoFlipLocal;
    }
}
