package com.oopteama.chess.core.rules;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * StandardRules와 SandboxRules가 함께 쓰는 합법 수 계산. 패키지 밖에는 공개하지 않는다.
 * 두 규칙의 차이는 캐슬링 권리 플래그를 요구하는지 하나뿐이라 boolean으로 받는다.
 */
final class MoveGenerator {

    private static final int[][] KNIGHT = {
        {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}
    };
    private static final int[][] KING = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final int[][] ORTHOGONAL = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
    private static final int[][] DIAGONAL = { {1, 1}, {1, -1}, {-1, 1}, {-1, -1} };

    private MoveGenerator() {}

    static boolean isPiece(Piece piece, Side side, PieceType type) {
        return piece != null && piece.side() == side && piece.type() == type;
    }

    /**
     * 기물이 아니라 목표 칸에서 거꾸로 훑어 공격 여부를 본다.
     * 폰은 앞으로 가는 수가 공격이 아니고 빈 칸도 공격하므로 pseudoLegalMoves로는 판정할 수 없다.
     */
    static boolean isAttacked(Position pos, Square target, Side by) {
        int pawnRankOffset = by == Side.WHITE ? -1 : 1; // 백 폰은 target보다 한 랭크 아래에서 공격한다
        for (int dFile : new int[] {-1, 1}) {
            Optional<Square> from = target.offset(dFile, pawnRankOffset);
            if (from.isPresent() && isPiece(pos.at(from.get()), by, PieceType.PAWN)) {
                return true;
            }
        }
        return attackedByStepper(pos, target, KNIGHT, by, PieceType.KNIGHT)
            || attackedByStepper(pos, target, KING, by, PieceType.KING)
            || attackedBySlider(pos, target, ORTHOGONAL, by, PieceType.ROOK)
            || attackedBySlider(pos, target, DIAGONAL, by, PieceType.BISHOP);
    }

    private static boolean attackedByStepper(
            Position pos, Square target, int[][] offsets, Side by, PieceType type) {
        for (int[] off : offsets) {
            Optional<Square> from = target.offset(off[0], off[1]);
            if (from.isPresent() && isPiece(pos.at(from.get()), by, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean attackedBySlider(
            Position pos, Square target, int[][] directions, Side by, PieceType slider) {
        for (int[] dir : directions) {
            Optional<Square> next = target.offset(dir[0], dir[1]);
            while (next.isPresent()) {
                Piece p = pos.at(next.get());
                if (p != null) {
                    if (p.side() == by && (p.type() == slider || p.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break; // 처음 만난 기물이 막는다
                }
                next = next.get().offset(dir[0], dir[1]);
            }
        }
        return false;
    }

    static boolean inCheck(Position pos) {
        return kingAttacked(pos, pos.sideToMove());
    }

    /** side의 킹이 상대에게 공격받는가. 킹이 없으면 false. */
    private static boolean kingAttacked(Position pos, Side side) {
        Optional<Square> king = pos.kingSquare(side);
        return king.isPresent() && isAttacked(pos, king.get(), side.opposite());
    }

    static List<Move> legalMoves(Position pos, boolean requireCastlingRights) {
        Side us = pos.sideToMove();
        List<Move> pseudo = new ArrayList<>();
        for (Square from : pos.occupied(us)) {
            pseudo.addAll(pos.at(from).pseudoLegalMoves(pos, from));
        }
        addCastling(pos, requireCastlingRights, pseudo);

        Position work = pos.copy(); // 호출자의 Position은 건드리지 않는다
        List<Move> legal = new ArrayList<>(pseudo.size());
        for (Move m : pseudo) {
            work.make(m);
            if (!kingAttacked(work, us)) {
                legal.add(m);
            }
            work.unmake();
        }
        return List.copyOf(legal);
    }

    /** 킹이 제자리(e파일)에 있고 체크 중이 아니어야 하며, 지나는 칸과 도착 칸이 공격받지 않아야 한다. */
    private static void addCastling(Position pos, boolean requireRights, List<Move> out) {
        Side us = pos.sideToMove();
        int rank = us == Side.WHITE ? 0 : 7;
        Square kingSquare = new Square(4, rank);
        if (!isPiece(pos.at(kingSquare), us, PieceType.KING)) {
            return;
        }
        if (isAttacked(pos, kingSquare, us.opposite())) {
            return;
        }
        if (canCastle(pos, us, rank, true, requireRights)) {
            out.add(new Move(kingSquare, new Square(6, rank), MoveKind.CASTLE_SHORT));
        }
        if (canCastle(pos, us, rank, false, requireRights)) {
            out.add(new Move(kingSquare, new Square(2, rank), MoveKind.CASTLE_LONG));
        }
    }

    private static boolean canCastle(
            Position pos, Side us, int rank, boolean shortSide, boolean requireRights) {
        if (requireRights && !pos.canCastle(us, shortSide)) {
            return false;
        }
        int rookFile = shortSide ? 7 : 0;
        if (!isPiece(pos.at(new Square(rookFile, rank)), us, PieceType.ROOK)) {
            return false;
        }
        int[] empty = shortSide ? new int[] {5, 6} : new int[] {1, 2, 3};
        for (int file : empty) {
            if (pos.at(new Square(file, rank)) != null) {
                return false;
            }
        }
        int[] kingPath = shortSide ? new int[] {5, 6} : new int[] {3, 2}; // b1은 공격받아도 된다
        for (int file : kingPath) {
            if (isAttacked(pos, new Square(file, rank), us.opposite())) {
                return false;
            }
        }
        return true;
    }

    static String repetitionKey(Position pos, boolean requireCastlingRights) {
        String[] fields = Fen.write(pos).split(" ");
        String ep = fields[3];
        if (!ep.equals("-") && !hasLegalEnPassant(pos, requireCastlingRights)) {
            ep = "-";
        }
        return fields[0] + " " + fields[1] + " " + fields[2] + " " + ep;
    }

    private static boolean hasLegalEnPassant(Position pos, boolean requireCastlingRights) {
        for (Move m : legalMoves(pos, requireCastlingRights)) {
            if (m.kind() == MoveKind.EN_PASSANT) {
                return true;
            }
        }
        return false;
    }
}
