package com.oopteama.chess.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.StandardRules;
import com.oopteama.chess.notation.PgnGame;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** GAME-05: 오페라 게임으로 진영별 대기 지점, 채점, 해설 공개 시점, 정답률과 놓친 수를 확인한다. */
class GuessSessionTest {

    private static final StandardRules RULES = new StandardRules();
    /** 오페라 게임의 백 수는 17개(17. Rd8#로 끝남), 흑 수는 16개. */
    private static final int WHITE_MOVES = 17;
    private static final int BLACK_MOVES = 16;

    private static PgnGame opera;

    @BeforeAll
    static void readOpera() throws Exception {
        opera = ReplayModelTest.readBundledOpera();
    }

    private static Move uci(ReplayModel model, String uci) {
        return RULES.fromUci(model.position(), uci).orElseThrow(() -> new AssertionError("불법 수 " + uci));
    }

    @Test
    void whiteStopsAtTheStartAndBlackAfterTheFirstMove() {
        ReplayModel white = new ReplayModel(opera);
        GuessSession forWhite = new GuessSession(white, Side.WHITE);
        assertTrue(forWhite.awaitingGuess());
        assertEquals(0, white.ply());

        ReplayModel black = new ReplayModel(opera);
        GuessSession forBlack = new GuessSession(black, Side.BLACK);
        assertTrue(forBlack.awaitingGuess());
        assertEquals(1, black.ply(), "백의 첫 수 e4를 두고 흑 차례에서 멈춘다");
    }

    @Test
    void guessPlaysTheActualMoveRevealsTheCommentAndThenContinues() {
        ReplayModel model = new ReplayModel(opera);
        GuessSession session = new GuessSession(model, Side.BLACK);

        GuessResult first = session.guess(uci(model, "e7e5"));
        assertTrue(first.correct());
        assertEquals("e5", first.actual().san());
        assertEquals(2, model.ply(), "실제 수를 두고 멈춘다");
        assertFalse(session.awaitingGuess());

        session.continueToNext();
        assertEquals(3, model.ply(), "백의 Nf3를 두고 다음 흑 차례로 간다");
        assertTrue(session.awaitingGuess());

        GuessResult wrong = session.guess(uci(model, "b8c6"));
        assertFalse(wrong.correct());
        assertEquals("d6", wrong.actual().san());
        assertEquals(4, model.ply(), "틀려도 실제 수로 진행한다");
        assertEquals(List.of(3), session.missedPlies());
        assertEquals(0.5, session.ratio());
    }

    @Test
    void continueBeforeAnsweringAndGuessAfterAnsweringAreRejected() {
        ReplayModel model = new ReplayModel(opera);
        GuessSession session = new GuessSession(model, Side.WHITE);
        assertThrows(IllegalStateException.class, session::continueToNext);
        session.giveUp();
        assertThrows(IllegalStateException.class, () -> session.guess(uci(model, "e7e5")));
        assertThrows(IllegalStateException.class, session::giveUp);
    }

    @Test
    void wholeGameForWhiteCountsAttemptsRatioAndMissedPlies() {
        ReplayModel model = new ReplayModel(opera);
        GuessSession session = new GuessSession(model, Side.WHITE);
        int answered = 0;
        while (!session.finished()) {
            int ply = model.ply();
            assertEquals(Side.WHITE, model.position().sideToMove());
            if (ply == 18) {
                session.guess(uci(model, "c3d5"));     // 10. Nxb5! 대신 다른 수
            } else if (ply == 30) {
                session.giveUp();                      // 16. Qb8+!! 포기
            } else {
                assertTrue(session.guess(model.moveAfter().orElseThrow().move()).correct());
            }
            answered++;
            session.continueToNext();
        }
        assertEquals(WHITE_MOVES, answered);
        assertEquals(WHITE_MOVES, session.attempts());
        assertEquals(WHITE_MOVES - 2, session.correct());
        assertEquals(15.0 / 17.0, session.ratio(), 1e-9);
        assertEquals(List.of(18, 30), session.missedPlies());
        assertEquals(model.length(), model.ply());
        assertEquals(ReplayModelTest.OPERA_FINAL_FEN, Fen.write(model.position()));
    }

    @Test
    void blackSessionEndsAfterWhitesLastMove() {
        ReplayModel model = new ReplayModel(opera);
        GuessSession session = new GuessSession(model, Side.BLACK);
        while (!session.finished()) {
            session.guess(model.moveAfter().orElseThrow().move());
            session.continueToNext();
        }
        assertEquals(BLACK_MOVES, session.attempts());
        assertEquals(1.0, session.ratio());
        assertEquals(List.of(), session.missedPlies());
        assertEquals(model.length(), model.ply(), "남은 백의 Rd8#까지 두고 끝난다");
        session.continueToNext();
        assertTrue(session.finished());
    }

    @Test
    void sessionWithoutMovesForTheSideFinishesImmediately() {
        PgnGame empty = new PgnGame(Map.of(), Fen.INITIAL, List.of(), "*");
        GuessSession session = new GuessSession(new ReplayModel(empty), Side.WHITE);
        assertTrue(session.finished());
        assertFalse(session.awaitingGuess());
        assertEquals(0.0, session.ratio());
    }

    @Test
    void eachStepMovesTheModelOnceSoTheScreenRedrawsOnce() {
        ReplayModel model = new ReplayModel(opera);
        AtomicInteger redraws = new AtomicInteger();
        model.addChangeListener(redraws::incrementAndGet);
        GuessSession session = new GuessSession(model, Side.BLACK);
        assertEquals(1, redraws.get(), "시작: 0 → 1");
        session.giveUp();
        assertEquals(2, redraws.get(), "공개: 1 → 2");
        session.continueToNext();
        assertEquals(3, redraws.get(), "다음 대기: 2 → 3");
    }
}
