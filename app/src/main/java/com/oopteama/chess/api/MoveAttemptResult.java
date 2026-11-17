package com.oopteama.chess.api;

/** 착수 요청 결과. */
public record MoveAttemptResult(boolean accepted, String message) {

    public static MoveAttemptResult ok() {
        return new MoveAttemptResult(true, "");
    }

    public static MoveAttemptResult rejected(String message) {
        return new MoveAttemptResult(false, message == null ? "" : message);
    }
}
