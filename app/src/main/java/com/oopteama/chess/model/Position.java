package com.oopteama.chess.model;

/**
 * 체스판의 한 칸입니다.
 * col 0~7은 a~h 파일(세로줄), row 0~7은 1~8 랭크(가로줄)입니다. 예) e4 = (col 4, row 3)
 */
public record Position(int col, int row) {

    public Position {
        if (!isInside(col, row)) {
            throw new IllegalArgumentException("체스판 밖의 칸입니다: (" + col + ", " + row + ")");
        }
    }

    /** (col, row)가 8×8 체스판 안에 있는지 확인합니다. */
    public static boolean isInside(int col, int row) {
        return col >= 0 && col < 8 && row >= 0 && row < 8;
    }

    /** "e4" 같은 칸 이름을 Position으로 바꿉니다. */
    public static Position of(String name) {
        if (name == null || name.length() != 2) {
            throw new IllegalArgumentException("잘못된 칸 이름: " + name);
        }
        int col = name.charAt(0) - 'a';
        int row = name.charAt(1) - '1';
        if (!isInside(col, row)) {
            throw new IllegalArgumentException("잘못된 칸 이름: " + name);
        }
        return new Position(col, row);
    }

    /** 이 칸에서 (dCol, dRow)만큼 떨어진 칸. 체스판 밖이면 null입니다. */
    public Position offset(int dCol, int dRow) {
        int c = col + dCol;
        int r = row + dRow;
        return isInside(c, r) ? new Position(c, r) : null;
    }

    /** 밝은 칸인지 여부 (a1은 어두운 칸입니다). */
    public boolean isLight() {
        return (col + row) % 2 == 1;
    }

    /** "e4" 형식의 칸 이름. */
    @Override
    public String toString() {
        return "" + (char) ('a' + col) + (char) ('1' + row);
    }
}
