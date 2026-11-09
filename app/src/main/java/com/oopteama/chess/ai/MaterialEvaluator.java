package com.oopteama.chess.ai;

import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

/** 기물 가치만 합산하는 평가. 입문·초급 난이도용. */
public final class MaterialEvaluator implements Evaluator {

    static int value(PieceType t) {
        return switch (t) {
            case PAWN -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK -> 500;
            case QUEEN -> 900;
            case KING -> 0;
        };
    }

    @Override
    public int evaluate(Position pos) {
        int score = 0;
        for (Square sq : pos.occupied(Side.WHITE)) score += value(pos.at(sq).type());
        for (Square sq : pos.occupied(Side.BLACK)) score -= value(pos.at(sq).type());
        return pos.sideToMove() == Side.WHITE ? score : -score;
    }
}
