package com.oopteama.chess.core;

/** 진영. */
public enum Side {
    WHITE, BLACK;

    /** 상대 진영. */
    public Side opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

