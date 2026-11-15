package com.oopteama.chess.game;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;

import java.util.Objects;
import java.util.function.Consumer;

/** 보드를 모른다. 화면이 BoardPanel의 수를 submit으로 넘겨 game→ui 의존을 끊는다(G2). */
public final class HumanPlayer implements Player {

    private Consumer<Move> pending;
    private Consumer<Boolean> inputListener = enabled -> { };

    /** 입력 열림(true)/닫힘(false) 통지. 화면이 보드 입력 잠금에 사용. */
    public void setInputListener(Consumer<Boolean> enabled) {
        this.inputListener = Objects.requireNonNull(enabled);
    }

    @Override
    public void requestMove(Position snapshot, Consumer<Move> onMove) {
        Objects.requireNonNull(onMove);
        this.pending = onMove;
        inputListener.accept(true);
    }

    /** 화면이 BoardPanel의 수를 전달. 대기 중이 아니면 무시. */
    public void submit(Move move) {
        Objects.requireNonNull(move);
        if (pending == null) return;
        Consumer<Move> callback = pending;
        pending = null;
        inputListener.accept(false);
        callback.accept(move);
    }

    @Override
    public void cancel() {
        if (pending == null) return;
        pending = null;
        inputListener.accept(false);
    }
}
