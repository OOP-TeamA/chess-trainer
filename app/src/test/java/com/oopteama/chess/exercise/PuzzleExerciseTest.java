package com.oopteama.chess.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** QUIZ-01·02, G14: 퍼즐 채점, 상대 응수 자동 진행, 오답 직전 국면 유지, 메이트 대안 수 인정. */
class PuzzleExerciseTest {

    private static final StandardRules RULES = new StandardRules();

    /** 기능 명세 6장 예시. 백랭크 메이트인1, 정답 Rd8#. */
    static final Puzzle BACK_RANK = puzzle("p0001", "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "d1d8",
            "메이트인1", "백랭크");
    /** Rd8#와 Re8# 둘 다 메이트인 국면. 파일에는 Rd8만 적었다. */
    static final Puzzle TWO_MATES = puzzle("p0002", "7k/6pp/8/8/8/8/8/K2RR3 w - - 0 1", "d1d8", "메이트인1");
    /** 1.Qxd8 Rxd8 2.Rxd8# 메이트인2. */
    static final Puzzle MATE_IN_TWO = puzzle("p0003", "3rr1k1/5ppp/8/8/8/8/3Q1PPP/3R2K1 w - - 0 1",
            "d2d8 e8d8 d1d8", "메이트인2", "백랭크");
    /** 1.Nc7+ Ke7 2.Nxa8 나이트 포크. 메이트 주제가 아니므로 마지막 수도 적힌 수만 정답. */
    static final Puzzle FORK = puzzle("p0004", "r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", "b5c7 e8e7 c7a8", "포크");

    static Puzzle puzzle(String id, String fen, String solution, String... themes) {
        return new Puzzle(id, new LinkedHashSet<>(List.of(themes)), fen, List.of(solution.split(" ")), "");
    }

    private static Move uci(Exercise ex, String uci) {
        return RULES.fromUci(ex.position(), uci).orElseThrow(() -> new AssertionError("불법 수 " + uci));
    }

    @Test
    void mateInOneAcceptsTheMateAndLocksInput() {
        PuzzleExercise ex = new PuzzleExercise(BACK_RANK);
        assertEquals(InputMode.MOVE, ex.inputMode());
        assertEquals("백 차례입니다. 1수 만에 체크메이트하세요.", ex.prompt());
        assertEquals(Set.of(Square.of("d1")), ex.hintSquares());
        assertEquals(Optional.of(uci(ex, "d1d8")), ex.solution());

        Verdict wrong = ex.submit(uci(ex, "d1d7"));
        assertEquals(new Verdict.Wrong(PuzzleExercise.WRONG_MATE), wrong);
        assertEquals(BACK_RANK.fen(), Fen.write(ex.position()), "오답이면 국면을 바꾸지 않는다");

        Verdict solved = ex.submit(uci(ex, "d1d8"));
        assertEquals(new Verdict.Solved(PuzzleExercise.SOLVED_MATE), solved);
        assertEquals(List.of(), ex.legalMoves());
        assertEquals(Optional.empty(), ex.solution());
        assertEquals(1, ex.userMovesPlayed());
        assertThrows(IllegalStateException.class, () -> ex.submit(new Move(Square.of("g1"), Square.of("f1"),
                com.oopteama.chess.core.MoveKind.QUIET)));
    }

    @Test
    void anyMateIsAcceptedOnTheLastMoveOfAMateTheme() {
        PuzzleExercise viaFile = new PuzzleExercise(TWO_MATES);
        assertInstanceOf(Verdict.Solved.class, viaFile.submit(uci(viaFile, "d1d8")));

        PuzzleExercise viaOther = new PuzzleExercise(TWO_MATES);
        assertInstanceOf(Verdict.Solved.class, viaOther.submit(uci(viaOther, "e1e8")), "파일에 없는 다른 메이트도 정답");
    }

    @Test
    void mateInTwoPlaysTheReplyAndWrongMovesReturnToTheMoveBefore() {
        PuzzleExercise ex = new PuzzleExercise(MATE_IN_TWO);
        assertEquals("백 차례입니다. 2수 만에 체크메이트하세요.", ex.prompt());

        assertInstanceOf(Verdict.Wrong.class, ex.submit(uci(ex, "d2d7")));
        assertEquals(MATE_IN_TWO.fen(), Fen.write(ex.position()));

        Verdict cont = ex.submit(uci(ex, "d2d8"));
        Verdict.Continue c = assertInstanceOf(Verdict.Continue.class, cont);
        assertEquals("e8d8", c.reply().uci());
        String afterReply = Fen.write(ex.position());
        assertEquals(1, ex.userMovesPlayed());
        assertEquals(Set.of(Square.of("d1")), ex.hintSquares());
        assertEquals("d1d8", ex.solution().orElseThrow().uci());

        assertInstanceOf(Verdict.Wrong.class, ex.submit(uci(ex, "g2g3")));
        assertEquals(afterReply, Fen.write(ex.position()), "두 번째 수의 오답은 퍼즐 처음이 아니라 응수 뒤 국면으로(G14)");

        assertInstanceOf(Verdict.Solved.class, ex.submit(uci(ex, "d1d8")));
        assertEquals(2, ex.userMovesPlayed());
    }

    @Test
    void nonMateThemesNeedTheWrittenMoveEvenAtTheEnd() {
        PuzzleExercise ex = new PuzzleExercise(FORK);
        assertTrue(ex.prompt().contains("주제: 포크"), ex.prompt());
        assertInstanceOf(Verdict.Continue.class, ex.submit(uci(ex, "b5c7")));
        assertEquals(new Verdict.Wrong(PuzzleExercise.WRONG), ex.submit(uci(ex, "c7e6")));
        assertEquals(new Verdict.Solved(PuzzleExercise.SOLVED), ex.submit(uci(ex, "c7a8")));
    }

    @Test
    void resetStartsOver() {
        PuzzleExercise ex = new PuzzleExercise(MATE_IN_TWO);
        ex.submit(uci(ex, "d2d8"));
        ex.reset();
        assertEquals(MATE_IN_TWO.fen(), Fen.write(ex.position()));
        assertEquals(0, ex.userMovesPlayed());
        assertEquals("d2d8", ex.solution().orElseThrow().uci());
    }

    @Test
    void illegalInputIsRejected() {
        PuzzleExercise ex = new PuzzleExercise(BACK_RANK);
        Move illegal = new Move(Square.of("d1"), Square.of("e2"), com.oopteama.chess.core.MoveKind.QUIET);
        assertThrows(IllegalArgumentException.class, () -> ex.submit(illegal));
        assertThrows(IllegalStateException.class, () -> ex.submitSquare(Square.of("d8")));
    }

    @Test
    void constructorRejectsASolutionThatCannotBePlayed() {
        Puzzle broken = puzzle("bad", BACK_RANK.fen(), "d1d9", "백랭크");
        assertThrows(IllegalArgumentException.class, () -> new PuzzleExercise(broken));
        Puzzle blocked = puzzle("bad2", MATE_IN_TWO.fen(), "d1d8 e8d8 d2d8", "백랭크");
        assertThrows(IllegalArgumentException.class, () -> new PuzzleExercise(blocked));
    }
}
