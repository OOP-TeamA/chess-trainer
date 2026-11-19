package com.oopteama.chess.engine;

import com.oopteama.chess.engine.Piece.Type;

/**
 * 체스 한 수(좌표 + 선택적 승진)와 착수 기록(Record)을 표현합니다.
 */
public record Move(int from, int to, Type promotion) {

    public enum Kind {
        NORMAL,
        CAPTURE,
        EN_PASSANT,
        CASTLE_SHORT,
        CASTLE_LONG;

        public boolean isCapture() {
            return this == CAPTURE || this == EN_PASSANT;
        }
    }

    public record Record(
            Move move,
            Kind kind,
            Piece moved,
            Piece captured,
            boolean check,
            boolean checkmate,
            String san
    ) {
        public boolean isCapture() {
            return kind.isCapture();
        }
    }

    public Move {
        Board.checkSquare(from);
        Board.checkSquare(to);
        if (from == to) {
            throw new IllegalArgumentException("출발 칸과 도착 칸이 같습니다: " + Board.formatSquare(from));
        }
        if (promotion != null && !promotion.isPromotable()) {
            throw new IllegalArgumentException("승진할 수 없는 기물입니다: " + promotion);
        }
    }

    public Move(int from, int to) {
        this(from, to, null);
    }

    public static Move fromUci(String uci) {
        if (uci == null) {
            throw new IllegalArgumentException("UCI 문자열이 비어 있습니다.");
        }
        String s = uci.trim();
        if (s.length() != 4 && s.length() != 5) {
            throw new IllegalArgumentException("올바르지 않은 UCI 형식입니다: " + uci);
        }
        int from = Board.parseSquare(s.substring(0, 2));
        int to = Board.parseSquare(s.substring(2, 4));
        Type promo = s.length() == 5 ? Type.fromSymbol(s.charAt(4)) : null;
        return new Move(from, to, promo);
    }

    public String uci() {
        String base = Board.formatSquare(from) + Board.formatSquare(to);
        return promotion == null ? base : base + Character.toLowerCase(promotion.symbol());
    }
}
