package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Side;

/** 룩: 가로·세로 직선으로 원하는 만큼 움직입니다. */
public class Rook extends SlidingPiece {

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public Rook(Side side) {
        super(side);
    }

    @Override
    protected int[][] directions() {
        return DIRECTIONS;
    }

    @Override
    public String name() {
        return "룩";
    }

    @Override
    public char symbol() {
        return 'R';
    }

    @Override
    public int value() {
        return 500;
    }
}
