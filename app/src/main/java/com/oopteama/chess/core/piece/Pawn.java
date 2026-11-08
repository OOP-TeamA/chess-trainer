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

/** 폰. 전진, 두 칸 전진, 대각선 잡기, 앙파상, 승진 수를 만든다. */
public final class Pawn extends Piece {

    private static final PieceType[] PROMOTIONS = {
        PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    /** 인스턴스는 PieceFactory가 12개만 만들어 공유한다. 다른 곳에서 직접 만들지 않는다. */
    public Pawn(Side side) {
        super(side);
    }

    @Override
    public PieceType type() {
        return PieceType.PAWN;
    }

    @Override
    public List<Move> pseudoLegalMoves(Position pos, Square from) {
        List<Move> moves = new ArrayList<>();
        int dir = side() == Side.WHITE ? 1 : -1;
        int startRank = side() == Side.WHITE ? 1 : 6;
        int promotionRank = side() == Side.WHITE ? 7 : 0;

        // 한 칸 전진 (+ 승진)
        Optional<Square> oneAhead = from.offset(0, dir);
        boolean oneAheadClear = false;
        if (oneAhead.isPresent() && pos.at(oneAhead.get()) == null) {
            oneAheadClear = true;
            addForwardOrPromotion(moves, from, oneAhead.get(), promotionRank, MoveKind.QUIET);
        }

        // 두 칸 전진 (시작 랭크에서만, 한 칸 전진이 비어 있어야)
        if (from.rank() == startRank && oneAheadClear) {
            Optional<Square> twoAhead = from.offset(0, 2 * dir);
            if (twoAhead.isPresent() && pos.at(twoAhead.get()) == null) {
                moves.add(new Move(from, twoAhead.get(), MoveKind.DOUBLE_PUSH));
            }
        }

        // 대각선 포획 (+ 앙파상, + 승진)
        for (int dFile : new int[] {-1, 1}) {
            Optional<Square> diag = from.offset(dFile, dir);
            if (diag.isEmpty()) {
                continue;
            }
            Square to = diag.get();
            Piece occupant = pos.at(to);
            if (occupant != null) {
                if (occupant.side() != side()) {
                    addForwardOrPromotion(moves, from, to, promotionRank, MoveKind.CAPTURE);
                }
            } else if (pos.enPassantTarget().isPresent() && pos.enPassantTarget().get().equals(to)) {
                moves.add(new Move(from, to, MoveKind.EN_PASSANT));
            }
        }

        return moves;
    }

    private void addForwardOrPromotion(List<Move> moves, Square from, Square to, int promotionRank, MoveKind kind) {
        if (to.rank() == promotionRank) {
            for (PieceType promo : PROMOTIONS) {
                moves.add(new Move(from, to, kind, promo));
            }
        } else {
            moves.add(new Move(from, to, kind));
        }
    }
}

