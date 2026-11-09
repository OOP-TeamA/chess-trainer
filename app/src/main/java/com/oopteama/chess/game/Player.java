package com.oopteama.chess.game;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;

import java.util.function.Consumer;

public interface Player {
    /** 블로킹하지 않는다. 수가 정해지면 onMove를 UI 스레드에서 한 번 호출. */
    void requestMove(Position snapshot, Consumer<Move> onMove);

    /** 진행 중인 요청을 취소. 취소된 요청의 onMove는 호출되지 않는다. */
    void cancel();
}
