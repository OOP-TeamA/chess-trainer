package com.oopteama.chess;

import com.oopteama.chess.app.ChessApp;

/** Gradle {@code :app:run}과 실행 JAR의 진입점. 실제 시작 순서는 {@link ChessApp}이 맡는다. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ChessApp.main(args);
    }
}
