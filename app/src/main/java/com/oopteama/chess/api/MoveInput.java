package com.oopteama.chess.api;

import java.util.Objects;
import java.util.Optional;

/** UI 보드에서 사용자가 선택한 착수 요청. */
public record MoveInput(int from, int to, Optional<PieceKind> promotion) {

    public MoveInput {
        checkSquare(from);
        checkSquare(to);
        if (from == to) {
            throw new IllegalArgumentException("from and to must differ: " + from);
        }
        Objects.requireNonNull(promotion, "promotion");
        promotion.ifPresent(kind -> {
            if (kind == PieceKind.KING || kind == PieceKind.PAWN) {
                throw new IllegalArgumentException("Invalid promotion piece: " + kind);
            }
        });
    }

    public static MoveInput of(int from, int to) {
        return new MoveInput(from, to, Optional.empty());
    }

    public static MoveInput promote(int from, int to, PieceKind promotion) {
        return new MoveInput(from, to, Optional.of(Objects.requireNonNull(promotion, "promotion")));
    }

    private static void checkSquare(int square) {
        if (square < 0 || square >= 64) {
            throw new IllegalArgumentException("Square index must be 0..63: " + square);
        }
    }
}
