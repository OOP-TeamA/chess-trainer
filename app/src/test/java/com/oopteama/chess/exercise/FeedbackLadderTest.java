package com.oopteama.chess.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oopteama.chess.exercise.FeedbackLadder.Stage;
import org.junit.jupiter.api.Test;

/** RULE-03, P2: 오답 1회 설명, 2회 힌트, 3회 이상 정답 공개. */
class FeedbackLadderTest {

    @Test
    void climbsOneStagePerWrongAndStaysAtReveal() {
        FeedbackLadder ladder = new FeedbackLadder();
        assertEquals(Stage.NONE, ladder.stage());
        assertEquals(0, ladder.wrongCount());
        assertEquals(Stage.EXPLAIN, ladder.recordWrong());
        assertEquals(Stage.HINT, ladder.recordWrong());
        assertEquals(Stage.REVEAL, ladder.recordWrong());
        assertEquals(Stage.REVEAL, ladder.recordWrong());
        assertEquals(Stage.REVEAL, ladder.stage());
        assertEquals(4, ladder.wrongCount());
    }

    @Test
    void resetStartsOver() {
        FeedbackLadder ladder = new FeedbackLadder();
        ladder.recordWrong();
        ladder.recordWrong();
        ladder.reset();
        assertEquals(Stage.NONE, ladder.stage());
        assertEquals(0, ladder.wrongCount());
        assertEquals(Stage.EXPLAIN, ladder.recordWrong());
    }
}
