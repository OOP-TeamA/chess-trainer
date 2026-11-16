package com.oopteama.chess.game;

import com.oopteama.chess.ai.Blunder;

public interface BlunderListener {
    /** EDT에서 호출. 이후 화면은 proceedAfterBlunder() 또는 undoTurn()으로 응답해야 한다. */
    void blunderDetected(Blunder blunder);
}
