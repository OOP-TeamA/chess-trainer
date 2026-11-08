package com.oopteama.chess.core.piece;

import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Side;

/** 비숍. */
public final class Bishop extends SlidingPiece {

    /** 인스턴스는 PieceFactory가 12개만 만들어 공유한다. 다른 곳에서 직접 만들지 않는다. */
    public Bishop(Side side) {
        super(side);
    }

    @Override
    public PieceType type() {
        return PieceType.BISHOP;
    }

    @Override
    protected int[][] directions() {
        return new int[][] { {1, 1}, {1, -1}, {-1, 1}, {-1, -1} };
    }
}

