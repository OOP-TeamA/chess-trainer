package com.oopteama.chess.content.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Square;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** RULE-02: 목표 문자열 문법 any-move | move:d4f5|d4f3 | square:e4|e5 | check | mate. */
class StepGoalParseTest {

    @Test
    void parsesEveryGoalKind() throws Exception {
        assertEquals(new AnyMoveGoal(), StepGoal.parse("any-move"));
        assertEquals(new MoveGoal(List.of("d4f5", "d4f3")), StepGoal.parse("move:d4f5|d4f3"));
        assertEquals(new SquareGoal(Set.of(Square.of("e4"), Square.of("e5"))), StepGoal.parse("square:e4|e5"));
        assertEquals(new CheckGoal(), StepGoal.parse("check"));
        assertEquals(new MateGoal(), StepGoal.parse("mate"));
    }

    @Test
    void inputModeFollowsGoalKind() throws Exception {
        assertEquals(InputMode.MOVE, StepGoal.parse("any-move").inputMode());
        assertEquals(InputMode.MOVE, StepGoal.parse("move:d4f5").inputMode());
        assertEquals(InputMode.SQUARE, StepGoal.parse("square:c7").inputMode());
        assertEquals(InputMode.MOVE, StepGoal.parse("check").inputMode());
        assertEquals(InputMode.MOVE, StepGoal.parse("mate").inputMode());
    }

    @Test
    void ignoresCaseSpacesEmptyAndDuplicateAlternatives() throws Exception {
        assertEquals(new MoveGoal(List.of("d4f5", "d4f3")), StepGoal.parse("  MOVE: D4F5 | | d4f3 | d4f5 "));
        assertEquals(new SquareGoal(Set.of(Square.of("c7"))), StepGoal.parse("Square:c7|C7"));
        assertEquals(new MateGoal(), StepGoal.parse(" Mate "));
    }

    @Test
    void keepsPromotionPiece() throws Exception {
        assertEquals(new MoveGoal(List.of("e7e8q", "e7e8n")), StepGoal.parse("move:e7e8q|e7e8n"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "jump", "move", "move:", "move:|", "move:d4", "move:z9f5", "move:e7e8k",
        "move:d4f5x", "square:", "square:i9", "square:e44", "any-move:e4", "check:e4", "mate!"})
    void rejectsMalformedSpecWithInputSource(String spec) {
        MalformedDataException e = assertThrows(MalformedDataException.class, () -> StepGoal.parse(spec));
        assertEquals("<input>", e.source());
        assertEquals(0, e.line());
        assertTrue(e.getMessage().contains("목표"), e.getMessage());
    }

    @Test
    void goalsCopyTheirInput() {
        assertThrows(UnsupportedOperationException.class,
            () -> new MoveGoal(List.of("d4f5")).uci().add("d4f3"));
        assertThrows(UnsupportedOperationException.class,
            () -> new SquareGoal(Set.of(Square.of("c7"))).solutionSquares().add(Square.of("e7")));
    }
}
