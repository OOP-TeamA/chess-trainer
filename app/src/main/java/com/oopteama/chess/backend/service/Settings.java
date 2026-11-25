package com.oopteama.chess.backend.service;

/**
 * 체스판 테마와 표시 옵션을 보관하는 앱 설정 모델입니다. (AWT/Swing 비의존)
 */
public final class Settings {

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

        public static BoardTheme parse(String name) {
            if (name != null) {
                for (BoardTheme t : values()) {
                    if (t.name().equalsIgnoreCase(name)) {
                        return t;
                    }
                }
            }
            return WALNUT;
        }
    }

    private BoardTheme boardTheme = BoardTheme.WALNUT;
    private boolean showCoords = true;
    private boolean showLegalMoves = true;
    private boolean highlightLastMove = true;
    private boolean showMovePreview = true;
    private boolean autoFlipLocal = false;

    public BoardTheme boardTheme() {
        return boardTheme;
    }

    public void setBoardTheme(BoardTheme boardTheme) {
        if (boardTheme != null) this.boardTheme = boardTheme;
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

    public boolean highlightLastMove() {
        return highlightLastMove;
    }

    public void setHighlightLastMove(boolean highlightLastMove) {
        this.highlightLastMove = highlightLastMove;
    }

    public boolean showMovePreview() {
        return showMovePreview;
    }

    public void setShowMovePreview(boolean showMovePreview) {
        this.showMovePreview = showMovePreview;
    }

    public boolean autoFlipLocal() {
        return autoFlipLocal;
    }

    public void setAutoFlipLocal(boolean autoFlipLocal) {
        this.autoFlipLocal = autoFlipLocal;
    }
}
