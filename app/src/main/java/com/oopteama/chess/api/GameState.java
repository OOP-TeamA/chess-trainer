package com.oopteama.chess.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 대국 화면이 표시할 전체 상태 스냅샷. */
public record GameState(
        BoardSnapshot board,
        List<String> sanHistory,
        boolean canUndo,
        boolean inCheck,
        Optional<GameOutcomeView> result
) {

    public GameState {
        Objects.requireNonNull(board, "board");
        sanHistory = List.copyOf(Objects.requireNonNull(sanHistory, "sanHistory"));
        Objects.requireNonNull(result, "result");
    }
}
