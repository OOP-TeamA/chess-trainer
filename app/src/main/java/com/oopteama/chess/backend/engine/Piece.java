package com.oopteama.chess.backend.engine;

/**
 * 체스 진영(Side), 기물 종류(Type), 64칸 위 기물(Piece)을 정의합니다.
 */
public enum Piece {
    EMPTY('.', null, null),
    WP('P', Side.WHITE, Type.PAWN),
    WN('N', Side.WHITE, Type.KNIGHT),
    WB('B', Side.WHITE, Type.BISHOP),
    WR('R', Side.WHITE, Type.ROOK),
    WQ('Q', Side.WHITE, Type.QUEEN),
    WK('K', Side.WHITE, Type.KING),
    BP('p', Side.BLACK, Type.PAWN),
    BN('n', Side.BLACK, Type.KNIGHT),
    BB('b', Side.BLACK, Type.BISHOP),
    BR('r', Side.BLACK, Type.ROOK),
    BQ('q', Side.BLACK, Type.QUEEN),
    BK('k', Side.BLACK, Type.KING);

    public enum Side {
        WHITE('w', "백"),
        BLACK('b', "흑");

        private final char fenChar;
        private final String label;

        Side(char fenChar, String label) {
            this.fenChar = fenChar;
            this.label = label;
        }

        public Side opposite() {
            return this == WHITE ? BLACK : WHITE;
        }

        public char fenChar() {
            return fenChar;
        }

        public String label() {
            return label;
        }
    }

    public enum Type {
        PAWN('P', 100, "폰"),
        KNIGHT('N', 320, "나이트"),
        BISHOP('B', 330, "비숍"),
        ROOK('R', 500, "룩"),
        QUEEN('Q', 900, "퀸"),
        KING('K', 20000, "킹");

        private final char symbol;
        private final int value;
        private final String label;

        Type(char symbol, int value, String label) {
            this.symbol = symbol;
            this.value = value;
            this.label = label;
        }

        public char symbol() {
            return symbol;
        }

        public int value() {
            return value;
        }

        public String label() {
            return label;
        }

        public boolean isPromotable() {
            return this == QUEEN || this == ROOK || this == BISHOP || this == KNIGHT;
        }

        public static Type fromSymbol(char c) {
            return switch (Character.toUpperCase(c)) {
                case 'P' -> PAWN;
                case 'N' -> KNIGHT;
                case 'B' -> BISHOP;
                case 'R' -> ROOK;
                case 'Q' -> QUEEN;
                case 'K' -> KING;
                default -> throw new IllegalArgumentException("알 수 없는 기물 기호: " + c);
            };
        }
    }

    private final char fenChar;
    private final Side side;
    private final Type type;

    Piece(char fenChar, Side side, Type type) {
        this.fenChar = fenChar;
        this.side = side;
        this.type = type;
    }

    public char fenChar() {
        return fenChar;
    }

    public Side side() {
        return side;
    }

    public Type type() {
        return type;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean is(Side side) {
        return this.side == side;
    }

    public boolean is(Side side, Type type) {
        return this.side == side && this.type == type;
    }

    public static Piece of(Side side, Type type) {
        if (side == null || type == null) {
            return EMPTY;
        }
        return switch (side) {
            case WHITE -> switch (type) {
                case PAWN -> WP;
                case KNIGHT -> WN;
                case BISHOP -> WB;
                case ROOK -> WR;
                case QUEEN -> WQ;
                case KING -> WK;
            };
            case BLACK -> switch (type) {
                case PAWN -> BP;
                case KNIGHT -> BN;
                case BISHOP -> BB;
                case ROOK -> BR;
                case QUEEN -> BQ;
                case KING -> BK;
            };
        };
    }

    public static Piece fromFen(char c) {
        for (Piece p : values()) {
            if (p != EMPTY && p.fenChar == c) {
                return p;
            }
        }
        throw new IllegalArgumentException("알 수 없는 FEN 기물 문자: " + c);
    }
}
