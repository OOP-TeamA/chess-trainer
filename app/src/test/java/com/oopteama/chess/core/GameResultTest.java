package com.oopteama.chess.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameResultTest {

    @Test
    void resignationGivesTheWinToTheOtherSide() {
        GameResult whiteResigns = GameResult.resignation(Side.WHITE);
        assertEquals(Optional.of(Side.BLACK), whiteResigns.winner());
        assertEquals("0-1", whiteResigns.pgnToken());
        assertEquals(GameResult.Reason.RESIGNATION, whiteResigns.reason());

        assertEquals("1-0", GameResult.resignation(Side.BLACK).pgnToken());
    }

    @Test
    void drawHasNoWinner() {
        GameResult draw = new GameResult(GameResult.Outcome.DRAW, GameResult.Reason.STALEMATE);
        assertEquals(Optional.empty(), draw.winner());
        assertEquals("1/2-1/2", draw.pgnToken());
    }
}
