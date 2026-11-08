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
            } else if (canCaptureEnPassant(pos, from, to)) {
                moves.add(new Move(from, to, MoveKind.EN_PASSANT));
            }
        }

        return moves;
    }

    /**
     * 앙파상 조건: 도착 칸이 앙파상 타깃이고, 그 타깃이 이 폰이 갈 수 있는 랭크(백 6, 흑 3)에 있으며,
     * 잡힐 칸(출발 랭크, 도착 파일)에 상대 폰이 실제로 있어야 한다.
     * 잘못된 FEN으로 타깃만 있고 폰이 없거나 다른 기물이 있으면 그 기물이 지워지므로 만들지 않는다.
     */
    private boolean canCaptureEnPassant(Position pos, Square from, Square to) {
        Optional<Square> target = pos.enPassantTarget();
        if (target.isEmpty() || !target.get().equals(to)) {
            return false;
        }
        int expectedRank = side() == Side.WHITE ? 5 : 2;
        if (to.rank() != expectedRank) {
            return false;
        }
        Piece victim = pos.at(new Square(to.file(), from.rank()));
        return victim != null && victim.type() == PieceType.PAWN && victim.side() != side();
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

