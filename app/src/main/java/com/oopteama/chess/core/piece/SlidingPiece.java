package com.oopteama.chess.core.piece;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 룩, 비숍, 퀸처럼 한 방향으로 미끄러지는 기물의 공통 구현. 방향 목록만 정의하면 된다. */
public abstract class SlidingPiece extends Piece {

    protected SlidingPiece(Side side) {
        super(side);
    }

    /** Rook, Bishop, Queen은 이 방향 목록만 정의하면 된다. {dFile, dRank} 쌍들. */
    protected abstract int[][] directions();

    @Override
    public final List<Move> pseudoLegalMoves(Position pos, Square from) {
        List<Move> moves = new ArrayList<>();
        for (int[] dir : directions()) {
            int step = 1;
            Optional<Square> next = from.offset(dir[0] * step, dir[1] * step);
            while (next.isPresent()) {
                Square to = next.get();
                Piece occupant = pos.at(to);
                if (occupant == null) {
                    moves.add(new Move(from, to, MoveKind.QUIET));
                } else {
                    if (occupant.side() != side()) {
                        moves.add(new Move(from, to, MoveKind.CAPTURE));
                    }
                    break; // 막힌 칸(자기편이든 상대편이든) 뒤로는 더 못 감
                }
                step++;
                next = from.offset(dir[0] * step, dir[1] * step);
            }
        }
        return moves;
    }
}

