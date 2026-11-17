package com.oopteama.chess.api;

import java.util.Objects;

/** 보드 한 칸에 놓인 기물의 UI 표시 정보. */
public record PieceView(SideColor side, PieceKind kind) {

    public PieceView {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(kind, "kind");
    }
}
