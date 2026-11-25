package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Side;

/** 나이트: L자(두 칸 + 옆으로 한 칸)로 뛰며, 다른 기물을 뛰어넘을 수 있습니다. */
public class Knight extends StepPiece {

    private static final int[][] OFFSETS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    public Knight(Side side) {
        super(side);
    }

    @Override
    protected int[][] offsets() {
        return OFFSETS;
    }

    @Override
    public String name() {
        return "나이트";
    }

    @Override
    public char symbol() {
        return 'N';
    }

    @Override
    public int value() {
        return 320;
    }
}
