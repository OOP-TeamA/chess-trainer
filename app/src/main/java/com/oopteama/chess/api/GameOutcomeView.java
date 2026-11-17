package com.oopteama.chess.api;

import java.util.Objects;

/** 종료된 대국의 승패 및 종료 사유. */
public record GameOutcomeView(Winner winner, EndReason reason) {

    public enum Winner {
        WHITE,
        BLACK,
        DRAW
    }

    public enum EndReason {
        CHECKMATE,
        STALEMATE,
        INSUFFICIENT_MATERIAL,
        FIFTY_MOVE,
        REPETITION,
        RESIGNATION
    }

    public GameOutcomeView {
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(reason, "reason");
    }
}
