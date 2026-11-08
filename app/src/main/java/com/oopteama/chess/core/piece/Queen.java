package com.oopteama.chess.core.piece;

import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Side;

/** 퀸. */
public final class Queen extends SlidingPiece {

    /** 인스턴스는 PieceFactory가 12개만 만들어 공유한다. 다른 곳에서 직접 만들지 않는다. */
    public Queen(Side side) {
        super(side);
    }

    @Override
    public PieceType type() {
        return PieceType.QUEEN;
    }

    @Override
    protected int[][] directions() {
        return new int[][] {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
    }
}

