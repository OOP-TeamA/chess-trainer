package com.oopteama.chess.core;

import java.util.Optional;

/** 대국 결과와 그 사유. 기권 결과는 resignation()으로 만든다. */
public record GameResult(Outcome outcome, Reason reason) {

    public enum Outcome { WHITE_WINS, BLACK_WINS, DRAW }

    public enum Reason {
        CHECKMATE, STALEMATE, INSUFFICIENT_MATERIAL, FIFTY_MOVE, REPETITION, RESIGNATION
    }

    /** loser가 기권해서 상대가 이긴 결과. */
    public static GameResult resignation(Side loser) {
        Outcome outcome = loser == Side.WHITE ? Outcome.BLACK_WINS : Outcome.WHITE_WINS;
        return new GameResult(outcome, Reason.RESIGNATION);
    }

    /** 이긴 쪽. 무승부면 empty. */
    public Optional<Side> winner() {
        return switch (outcome) {
            case WHITE_WINS -> Optional.of(Side.WHITE);
            case BLACK_WINS -> Optional.of(Side.BLACK);
            case DRAW -> Optional.empty();
        };
    }

    /** "1-0", "0-1", "1/2-1/2" */
    public String pgnToken() {
        return switch (outcome) {
            case WHITE_WINS -> "1-0";
            case BLACK_WINS -> "0-1";
            case DRAW -> "1/2-1/2";
        };
    }
}

