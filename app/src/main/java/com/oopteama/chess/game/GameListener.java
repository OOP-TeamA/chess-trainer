package com.oopteama.chess.game;

import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;

/** 모든 콜백은 EDT에서 호출. after는 사본이라 받은 쪽이 바꿔도 대국에 영향 없음. */
public interface GameListener {
    void moveMade(Move move, Position after);
    void moveUndone(Move move, Position after);
    void gameEnded(GameResult result);
}
