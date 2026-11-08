package com.oopteama.chess.core.piece;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 킹. 캐슬링 수는 공격 판정이 필요해 RuleSet이 만든다. */
public final class King extends Piece {

    private static final int[][] OFFSETS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    /** 인스턴스는 PieceFactory가 12개만 만들어 공유한다. 다른 곳에서 직접 만들지 않는다. */
    public King(Side side) {
        super(side);
    }

    @Override
    public PieceType type() {
        return PieceType.KING;
    }

    /** 한 칸 이동만 포함. 캐슬링은 공격 판정이 필요해 RuleSet이 추가한다. */
    @Override
    public List<Move> pseudoLegalMoves(Position pos, Square from) {
        List<Move> moves = new ArrayList<>();
        for (int[] off : OFFSETS) {
            Optional<Square> maybe = from.offset(off[0], off[1]);
            if (maybe.isEmpty()) {
                continue;
            }
            Square to = maybe.get();
            Piece occupant = pos.at(to);
            if (occupant == null) {
                moves.add(new Move(from, to, MoveKind.QUIET));
            } else if (occupant.side() != side()) {
                moves.add(new Move(from, to, MoveKind.CAPTURE));
            }
        }
        return moves;
    }
}

