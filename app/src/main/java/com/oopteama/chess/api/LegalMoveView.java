package com.oopteama.chess.api;

/**
 * UI 보드에서 합법 수 하이라이트와 승진 다이얼로그 여부를 판단하기 위한 최소 이동 정보.
 * 칸 인덱스는 0(a1) ~ 63(h8) 기준이다.
 */
public record LegalMoveView(
        int from,
        int to,
        boolean capture,
        boolean promotion
) {

    public LegalMoveView {
        checkSquare(from);
        checkSquare(to);
        if (from == to) {
            throw new IllegalArgumentException("from and to must differ: " + from);
        }
    }

    private static void checkSquare(int square) {
        if (square < 0 || square >= 64) {
            throw new IllegalArgumentException("Square index must be 0..63: " + square);
        }
    }
}
