package com.oopteama.chess.core;

/**
 * promotion은 승진 수에서만 non-null이고 QUEEN, ROOK, BISHOP, KNIGHT만 허용.
 * 승진하면서 잡는 수는 kind = CAPTURE.
 */
public record Move(Square from, Square to, MoveKind kind, PieceType promotion) {

    public Move {
        if (promotion != null
                && promotion != PieceType.QUEEN
                && promotion != PieceType.ROOK
                && promotion != PieceType.BISHOP
                && promotion != PieceType.KNIGHT) {
            throw new IllegalArgumentException("승진은 Q/R/B/N만 허용: " + promotion);
        }
    }

    /** 승진 없는 수의 편의 생성자. */
    public Move(Square from, Square to, MoveKind kind) {
        this(from, to, kind, null);
    }

    /** CAPTURE 또는 EN_PASSANT면 true. */
    public boolean isCapture() {
        return kind == MoveKind.CAPTURE || kind == MoveKind.EN_PASSANT;
    }

    /** 승진 수면 true. */
    public boolean isPromotion() {
        return promotion != null;
    }

    /** "e2e4", "e7e8q" 형식. */
    public String uci() {
        String base = from.name() + to.name();
        if (promotion == null) {
            return base;
        }
        return base + promotionChar(promotion);
    }

    private static char promotionChar(PieceType type) {
        return switch (type) {
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            default -> throw new IllegalStateException("승진 불가 기물: " + type);
        };
    }
}

