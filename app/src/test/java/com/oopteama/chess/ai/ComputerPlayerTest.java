package com.oopteama.chess.ai;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Rules;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI(ComputerPlayer)와 메이트 계산(MateSolver)을 검증합니다.
 */
class ComputerPlayerTest {

    @Test
    void computerFindsMatesAndFreeMaterialForBothSides() {
        ComputerPlayer ai = new ComputerPlayer(new Random(42));

        Board whiteMateInOne = Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1");
        Move move = ai.chooseMove(whiteMateInOne, Difficulty.EASY).orElseThrow();
        assertTrue(Rules.isCheckmate(whiteMateInOne.apply(move)));

        // 흑 차례에도 점수 계산 방향이 맞는지 확인 (흑은 점수가 작은 쪽을 고름)
        Board blackMateInOne = Board.fromFen("3r2k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1");
        assertEquals(Move.of("d8d1"), ai.chooseMove(blackMateInOne, Difficulty.EASY).orElseThrow());

        // 퀸을 희생하는 2수 메이트는 3수 앞을 보는 중급부터 찾습니다.
        Board mateInTwo = Board.fromFen("r5k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1");
        assertEquals(Move.of("e5e8"), ai.chooseMove(mateInTwo, Difficulty.MEDIUM).orElseThrow());

        Board freeQueen = Board.fromFen("4k3/8/8/3q4/8/8/8/3QK3 w - - 0 1");
        assertEquals(Move.of("d1d5"), ai.chooseMove(freeQueen, Difficulty.EASY).orElseThrow());
    }

    @Test
    void cancelledSearchReturnsNothing() {
        ComputerPlayer ai = new ComputerPlayer();
        assertTrue(ai.chooseMove(Board.initial(), Difficulty.HARD, () -> true).isEmpty());
    }

    @Test
    void mateSolverFindsForcedMateAndBestDefense() {
        MateSolver solver = new MateSolver();
        Board mateInTwo = Board.fromFen("r5k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1");
        assertEquals(Move.of("e5e8"), solver.findMateIn(mateInTwo, 2).orElseThrow());
        assertTrue(solver.findMateIn(mateInTwo, 1).isEmpty());

        Board afterSacrifice = mateInTwo.apply(Move.of("e5e8"));
        assertTrue(solver.isForcedMate(afterSacrifice, 1));
        assertEquals(Move.of("a8e8"), solver.bestDefense(afterSacrifice, 1).orElseThrow());
    }
}
