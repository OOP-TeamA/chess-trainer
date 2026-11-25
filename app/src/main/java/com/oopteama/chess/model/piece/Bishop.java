package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Side;

/** 비숍: 대각선으로 원하는 만큼 움직입니다. */
public class Bishop extends SlidingPiece {

    private static final int[][] DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public Bishop(Side side) {
        super(side);
    }

    @Override
    protected int[][] directions() {
        return DIRECTIONS;
    }

    @Override
    public String name() {
        return "비숍";
    }

    @Override
    public char symbol() {
        return 'B';
    }

    @Override
    public int value() {
        return 330;
    }
}
