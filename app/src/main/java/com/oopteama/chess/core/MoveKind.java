package com.oopteama.chess.core;

/** 수의 종류. 승진 여부는 Move.promotion으로 따로 나타낸다. */
public enum MoveKind {
    QUIET, CAPTURE, DOUBLE_PUSH, EN_PASSANT, CASTLE_SHORT, CASTLE_LONG
}

