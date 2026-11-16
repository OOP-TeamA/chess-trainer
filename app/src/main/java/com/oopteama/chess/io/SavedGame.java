package com.oopteama.chess.io;

import com.oopteama.chess.notation.PgnGame;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 프로필별 저장 대국 하나(GAME-07). {@code saves/<id>/<game>.txt}의 PGN이다.
 *
 * @param file 저장 파일 경로
 * @param pgn  읽은 대국
 */
public record SavedGame(Path file, PgnGame pgn) {

    /** 모든 필드는 null 불가. */
    public SavedGame {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(pgn, "pgn");
    }
}
