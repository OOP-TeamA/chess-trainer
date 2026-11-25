package com.oopteama.chess.model;

/**
 * 체스의 두 진영입니다. 백이 먼저 둡니다.
 */
public enum Side {
    WHITE("백"),
    BLACK("흑");

    private final String label;

    Side(String label) {
        this.label = label;
    }

    /** 화면에 보여 줄 이름 ("백" / "흑"). */
    public String label() {
        return label;
    }

    /** 상대 진영. */
    public Side opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** 폰이 앞으로 나아가는 방향: 백은 위쪽(+1), 흑은 아래쪽(-1). */
    public int forward() {
        return this == WHITE ? 1 : -1;
    }

    /** 기물이 처음 놓이는 맨 뒷줄: 백은 0(1랭크), 흑은 7(8랭크). */
    public int homeRow() {
        return this == WHITE ? 0 : 7;
    }
}
