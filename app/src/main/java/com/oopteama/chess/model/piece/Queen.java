package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Side;

/** 퀸: 가로·세로·대각선 8방향으로 원하는 만큼 움직입니다. */
public class Queen extends SlidingPiece {

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Queen(Side side) {
        super(side);
    }

    @Override
    protected int[][] directions() {
        return DIRECTIONS;
    }

    @Override
    public String name() {
        return "퀸";
    }

    @Override
    public char symbol() {
        return 'Q';
    }

    @Override
    public int value() {
        return 900;
    }
}
