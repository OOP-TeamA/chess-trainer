package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * 다른 기물에 막힐 때까지 한 방향으로 계속 미끄러지는 기물(퀸, 룩, 비숍)의 부모 클래스입니다.
 * 하위 클래스는 움직일 방향만 알려 주면 됩니다.
 */
public abstract class SlidingPiece extends Piece {

    protected SlidingPiece(Side side) {
        super(side);
    }

    /** 움직일 수 있는 방향 목록. 각 방향은 {열 변화, 행 변화}입니다. */
    protected abstract int[][] directions();

    @Override
    public List<Position> attackedSquares(Board board, Position from) {
        List<Position> result = new ArrayList<>();
        for (int[] direction : directions()) {
            Position next = from.offset(direction[0], direction[1]);
            while (next != null) {
                result.add(next);
                if (!board.isEmpty(next)) {
                    break; // 기물에 막히면 그 칸까지만 공격합니다.
                }
                next = next.offset(direction[0], direction[1]);
            }
        }
        return result;
    }
}
