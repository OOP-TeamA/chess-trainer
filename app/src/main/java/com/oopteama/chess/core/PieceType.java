package com.oopteama.chess.core;

/** 기물 종류. */
public enum PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN;

    /** 교육용 기물 가치. 평가 함수(ai 패키지)는 이 값을 쓰지 않고 자체 값을 쓴다. */
    public int nominalValue() {
        return switch (this) {
            case KING -> 0;
            case QUEEN -> 9;
            case ROOK -> 5;
            case BISHOP -> 3;
            case KNIGHT -> 3;
            case PAWN -> 1;
        };
    }
}

