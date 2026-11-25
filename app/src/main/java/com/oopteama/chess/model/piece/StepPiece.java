package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * 정해진 몇 칸으로만 한 번에 뛰어가는 기물(킹, 나이트)의 부모 클래스입니다.
 * 하위 클래스는 뛸 수 있는 칸의 거리만 알려 주면 됩니다.
 */
public abstract class StepPiece extends Piece {

    protected StepPiece(Side side) {
        super(side);
    }

    /** 뛸 수 있는 칸의 거리 목록. 각 거리는 {열 변화, 행 변화}입니다. */
    protected abstract int[][] offsets();

    @Override
    public List<Position> attackedSquares(Board board, Position from) {
        List<Position> result = new ArrayList<>();
        for (int[] offset : offsets()) {
            Position to = from.offset(offset[0], offset[1]);
            if (to != null) {
                result.add(to);
            }
        }
        return result;
    }
}
