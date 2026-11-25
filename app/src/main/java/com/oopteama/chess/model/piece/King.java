package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Side;

/**
 * 킹: 주변 8칸으로 한 칸씩 움직입니다.
 * 캐슬링은 킹과 룩이 함께 움직이는 특수 규칙이라 Rules에서 따로 처리합니다.
 */
public class King extends StepPiece {

    private static final int[][] OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public King(Side side) {
        super(side);
    }

    @Override
    protected int[][] offsets() {
        return OFFSETS;
    }

    @Override
    public String name() {
        return "킹";
    }

    @Override
    public char symbol() {
        return 'K';
    }

    @Override
    public int value() {
        return 20_000;
    }
}
