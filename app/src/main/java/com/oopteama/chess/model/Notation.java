package com.oopteama.chess.model;

import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;

/**
 * 수를 표준 기보 표기(SAN)로 바꿉니다.
 * 예) e4, Nf3, exd5, Nbd7, O-O, O-O-O, e8=Q, Qh5+, Rd8#
 */
public final class Notation {

    private Notation() {
    }

    /** before 국면에서 둔 move를 기보 표기로 바꿉니다. */
    public static String toSan(Board before, Move move) {
        String text;
        if (Rules.isCastling(before, move)) {
            text = move.to().col() > move.from().col() ? "O-O" : "O-O-O";
        } else {
            Piece piece = before.pieceAt(move.from());
            boolean capture = Rules.capturedPiece(before, move) != null;
            StringBuilder sb = new StringBuilder();
            if (piece instanceof Pawn) {
                if (capture) {
                    sb.append(move.from().toString().charAt(0)).append('x'); // 폰이 잡으면 출발 파일을 적음
                }
            } else {
                sb.append(piece.symbol()).append(disambiguation(before, move, piece));
                if (capture) {
                    sb.append('x');
                }
            }
            sb.append(move.to());
            if (move.isPromotion()) {
                sb.append('=').append(move.promotion());
            }
            text = sb.toString();
        }

        Board after = before.apply(move);
        if (Rules.isCheckmate(after)) {
            return text + "#";
        }
        return Rules.isInCheck(after) ? text + "+" : text;
    }

    /** 같은 종류의 기물 둘이 같은 칸으로 갈 수 있으면 출발 칸의 파일이나 랭크를 덧붙입니다. 예) Nbd7 */
    private static String disambiguation(Board before, Move move, Piece piece) {
        boolean ambiguous = false;
        boolean sameCol = false;
        boolean sameRow = false;
        for (Move other : Rules.legalMoves(before)) {
            if (!other.from().equals(move.from()) && other.to().equals(move.to())
                    && piece.equals(before.pieceAt(other.from()))) {
                ambiguous = true;
                sameCol |= other.from().col() == move.from().col();
                sameRow |= other.from().row() == move.from().row();
            }
        }
        if (!ambiguous) {
            return "";
        }
        String from = move.from().toString();
        if (!sameCol) {
            return from.substring(0, 1);
        }
        return sameRow ? from : from.substring(1, 2);
    }
}
