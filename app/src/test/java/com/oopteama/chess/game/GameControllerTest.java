package com.oopteama.chess.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.ai.Blunder;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.StandardRules;
import com.oopteama.chess.game.GameController.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** game이 core·ai와 맞물려 사람 대 AI 대국을 진행하는지 확인하는 연결 테스트. */
class GameControllerTest {

    /** 호출 즉시 같은 스레드에서 실행하는 테스트용 Async. */
    private static final class ImmediateAsync implements Async {
        @Override
        public <T> Task run(Function<BooleanSupplier, T> work, Consumer<T> onDone) {
            onDone.accept(work.apply(() -> false));
            return () -> { };
        }
    }

    private static final StandardRules RULES = new StandardRules();

    private static AiGameFactory.Session session(AiSetup setup) throws Exception {
        return AiGameFactory.create(setup, new ImmediateAsync(), new Random(7));
    }

    private static Move move(Game game, String uci) {
        Position p = game.snapshot();
        return RULES.fromUci(p, uci).orElseThrow();
    }

    @Test
    void 사용자_수_뒤에_AI가_응수하고_되무르기와_기권이_동작한다() throws Exception {
        AiGameFactory.Session s = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER));
        s.controller().start();
        assertEquals(State.WAITING_HUMAN, s.controller().state());

        s.human().submit(move(s.game(), "e2e4"));
        assertEquals(2, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());

        s.controller().undoTurn();
        assertEquals(0, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());

        s.controller().resign();
        assertEquals(State.ENDED, s.controller().state());
        assertEquals(GameResult.Outcome.BLACK_WINS, s.game().result().orElseThrow().outcome());
    }

    @Test
    void 사용자가_흑이면_AI가_먼저_둔다() throws Exception {
        AiGameFactory.Session s = session(AiSetup.standard(Side.BLACK, Difficulty.INTRO));
        s.controller().start();
        assertEquals(1, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());
    }

    @Test
    void 실수_알림은_확인을_기다리고_되무르기로_돌아간다() throws Exception {
        AiGameFactory.Session s = session(new AiSetup(
            Side.WHITE, Difficulty.BEGINNER, "4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1", true));
        List<Blunder> seen = new ArrayList<>();
        s.controller().setBlunderListener(seen::add);
        s.controller().start();

        s.human().submit(move(s.game(), "e1f2"));
        assertEquals(State.WAITING_BLUNDER_DECISION, s.controller().state());
        assertEquals(1, seen.size());
        assertEquals(1, s.game().history().moves().size());   // AI는 아직 두지 않았다

        s.controller().undoTurn();
        assertEquals(0, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());
    }

    @Test
    void 힌트는_사용자_쪽_합법_수를_준다() throws Exception {
        AiGameFactory.Session s = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER));
        s.controller().start();
        List<Move> hints = new ArrayList<>();
        s.controller().hint(hints::add);
        assertEquals(1, hints.size());
        assertTrue(s.game().legalMoves().contains(hints.get(0)));
    }
}
