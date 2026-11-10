package com.oopteama.chess.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.ai.AlphaBetaSearch;
import com.oopteama.chess.ai.Blunder;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.ai.SearchLimits;
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
        public <T> Task run(Function<BooleanSupplier, T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
            T result;
            try {
                result = work.apply(() -> false);
            } catch (RuntimeException e) {
                onError.accept(e);
                return () -> { };
            }
            onDone.accept(result);
            return () -> { };
        }
    }

    /**
     * 작업을 보관했다가 테스트가 원할 때 실행하는 Async. "계산 중" 상태를 재현한다.
     * deliverCancelled가 true면 취소된 작업의 콜백도 부른다(SwingWorker.done()처럼 잘못 구현된 경우).
     */
    private static final class ManualAsync implements Async {
        private final boolean deliverCancelled;
        private final List<Job<?>> jobs = new ArrayList<>();

        ManualAsync(boolean deliverCancelled) { this.deliverCancelled = deliverCancelled; }

        ManualAsync() { this(false); }

        private static final class Job<T> implements Task {
            final Function<BooleanSupplier, T> work;
            final Consumer<T> onDone;
            final Consumer<Throwable> onError;
            boolean cancelled;

            Job(Function<BooleanSupplier, T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
                this.work = work;
                this.onDone = onDone;
                this.onError = onError;
            }

            @Override public void cancel() { cancelled = true; }
        }

        @Override
        public <T> Task run(Function<BooleanSupplier, T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
            Job<T> job = new Job<>(work, onDone, onError);
            jobs.add(job);
            return job;
        }

        int pending() { return jobs.size(); }

        Job<?> job(int i) { return jobs.get(i); }

        /** i번째 작업을 실행하고 (취소되지 않았거나 deliverCancelled면) onDone을 부른다. */
        void complete(int i) { completeJob(jobs.remove(i)); }

        /** 보관된 작업을 순서대로 모두 실행한다. 실행 중 새로 생긴 작업도 이어서 실행한다. */
        void completeAll() {
            while (!jobs.isEmpty()) complete(0);
        }

        /** i번째 작업이 작업 스레드에서 예외로 끝난 것처럼 onError를 부른다. */
        void fail(int i, RuntimeException e) {
            Job<?> job = jobs.remove(i);
            if (job.cancelled && !deliverCancelled) return;
            job.onError.accept(e);
        }

        private <T> void completeJob(Job<T> job) {
            T result = job.work.apply(() -> job.cancelled);
            if (job.cancelled && !deliverCancelled) return;
            job.onDone.accept(result);
        }
    }

    private static final StandardRules RULES = new StandardRules();

    private static AiGameFactory.Session session(AiSetup setup) throws Exception {
        return AiGameFactory.create(setup, new ImmediateAsync(), new Random(7));
    }

    private static AiGameFactory.Session session(AiSetup setup, Async async) throws Exception {
        return AiGameFactory.create(setup, async, new Random(7));
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

    // ------------------------------------------------------------------ 계약(명세 4.8)

    @Test
    void 첫_대국용_4인자_생성자로_대국을_진행한다() throws Exception {
        Game game = new Game(Position.initial(), RULES);
        HumanPlayer human = new HumanPlayer();
        AiPlayer ai = new AiPlayer(
            new AlphaBetaSearch(RULES, Difficulty.BEGINNER.evaluator(), new Random(7)),
            SearchLimits.of(Difficulty.BEGINNER), new ImmediateAsync());
        GameController c = new GameController(game, human, ai, new ImmediateAsync());

        c.start();
        human.submit(move(game, "e2e4"));

        assertEquals(2, game.history().moves().size());
        assertEquals(State.WAITING_HUMAN, c.state());
    }

    @Test
    void start_전에도_state를_조회할_수_있다() throws Exception {
        AiGameFactory.Session white = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER));
        assertEquals(State.WAITING_HUMAN, white.controller().state());

        AiGameFactory.Session black = session(AiSetup.standard(Side.BLACK, Difficulty.BEGINNER), new ManualAsync());
        assertEquals(State.WAITING_AI, black.controller().state());

        AiGameFactory.Session mated = session(new AiSetup(
            Side.WHITE, Difficulty.BEGINNER, "7k/5QQ1/8/8/8/8/8/K7 b - - 0 1", false));
        assertEquals(State.ENDED, mated.controller().state());

        white.controller().dispose();
        assertEquals(State.ENDED, white.controller().state());
    }

    // ------------------------------------------------------------------ AI-05: 계산 중 취소

    @Test
    void AI_계산_중_되무르기는_계산을_취소하고_사용자_차례로_돌아간다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER), async);
        List<Boolean> thinking = new ArrayList<>();
        s.ai().setThinkingListener(thinking::add);
        s.controller().start();

        s.human().submit(move(s.game(), "e2e4"));
        assertEquals(State.WAITING_AI, s.controller().state());
        assertEquals(List.of(true), thinking);

        s.controller().undoTurn();
        assertEquals(State.WAITING_HUMAN, s.controller().state());
        assertEquals(0, s.game().history().moves().size());
        assertEquals(List.of(true, false), thinking);

        async.completeAll();   // 보관된(취소된) 계산이 나중에 끝나도
        assertEquals(0, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());
    }

    @Test
    void AI_계산_중_화면을_떠나면_이후_계산_결과를_버린다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(AiSetup.standard(Side.BLACK, Difficulty.BEGINNER), async);
        s.controller().start();
        assertEquals(State.WAITING_AI, s.controller().state());

        s.controller().dispose();
        async.completeAll();

        assertEquals(0, s.game().history().moves().size());
        assertEquals(State.ENDED, s.controller().state());
    }

    @Test
    void 힌트_계산_중_사용자가_수를_두면_늦은_힌트는_전달하지_않는다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER), async);
        s.controller().start();
        List<Move> hints = new ArrayList<>();
        s.controller().hint(hints::add);
        assertEquals(1, async.pending());

        s.human().submit(move(s.game(), "e2e4"));   // 힌트 취소, AI 계산 시작
        async.completeAll();

        assertEquals(List.of(), hints);
        assertEquals(2, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());
    }

    @Test
    void 실수_알림_확인_대기_중에는_힌트를_거부하고_기권은_받는다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(new AiSetup(
            Side.WHITE, Difficulty.BEGINNER, "4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1", true), async);
        s.controller().start();
        s.human().submit(move(s.game(), "e1f2"));
        assertEquals(State.CHECKING_BLUNDER, s.controller().state());
        async.completeAll();
        assertEquals(State.WAITING_BLUNDER_DECISION, s.controller().state());

        assertThrows(IllegalStateException.class, () -> s.controller().hint(m -> { }));

        s.controller().resign();
        assertEquals(State.ENDED, s.controller().state());
        assertEquals(GameResult.Outcome.BLACK_WINS, s.game().result().orElseThrow().outcome());
        assertEquals(0, async.pending());
    }

    // ------------------------------------------------------------------ AiPlayer 늦은 콜백

    @Test
    void 취소된_이전_요청의_늦은_콜백은_새_요청을_지우지_않는다() throws Exception {
        ManualAsync async = new ManualAsync(true);   // 취소돼도 onDone을 부르는 잘못된 구현
        AiPlayer ai = new AiPlayer(
            new AlphaBetaSearch(RULES, Difficulty.BEGINNER.evaluator(), new Random(7)),
            SearchLimits.of(Difficulty.BEGINNER), async);
        List<Boolean> thinking = new ArrayList<>();
        ai.setThinkingListener(thinking::add);
        List<Move> moves = new ArrayList<>();

        ai.requestMove(Position.initial(), moves::add);   // 요청 A
        ai.requestMove(Position.initial(), moves::add);   // 요청 B (A 취소)
        async.complete(0);                                // A의 콜백이 늦게 도착

        assertEquals(List.of(), moves);
        assertEquals(List.of(true, false, true), thinking);   // B는 여전히 생각 중

        ai.cancel();                                      // B를 취소할 수 있어야 한다
        assertTrue(async.job(0).cancelled);
        async.complete(0);
        assertEquals(List.of(), moves);
        assertEquals(List.of(true, false, true, false), thinking);
    }

    // ------------------------------------------------------------------ Async 실패 콜백

    @Test
    void AI_탐색이_실패하면_생각_중을_끄고_알리며_되무르기로_계속할_수_있다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER), async);
        List<Boolean> thinking = new ArrayList<>();
        s.ai().setThinkingListener(thinking::add);
        s.controller().start();
        s.human().submit(move(s.game(), "e2e4"));

        assertThrows(IllegalStateException.class, () -> async.fail(0, new RuntimeException("탐색 버그")));
        assertEquals(List.of(true, false), thinking);

        s.controller().undoTurn();
        assertEquals(State.WAITING_HUMAN, s.controller().state());
        assertEquals(0, s.game().history().moves().size());
    }

    @Test
    void 실수_판정이_실패하면_실수_없음처럼_AI_차례로_넘어간다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(new AiSetup(
            Side.WHITE, Difficulty.BEGINNER, "4k3/8/8/3r4/8/8/8/3QK3 w - - 0 1", true), async);
        List<Blunder> seen = new ArrayList<>();
        s.controller().setBlunderListener(seen::add);
        s.controller().start();
        s.human().submit(move(s.game(), "e1f2"));

        async.fail(0, new RuntimeException("판정 버그"));
        assertEquals(State.WAITING_AI, s.controller().state());
        async.completeAll();

        assertEquals(List.of(), seen);
        assertEquals(2, s.game().history().moves().size());
        assertEquals(State.WAITING_HUMAN, s.controller().state());
    }

    @Test
    void 힌트가_실패해도_다시_요청할_수_있다() throws Exception {
        ManualAsync async = new ManualAsync();
        AiGameFactory.Session s = session(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER), async);
        s.controller().start();
        List<Move> hints = new ArrayList<>();

        s.controller().hint(hints::add);
        async.fail(0, new RuntimeException("힌트 버그"));
        assertEquals(State.WAITING_HUMAN, s.controller().state());

        s.controller().hint(hints::add);
        async.completeAll();
        assertEquals(1, hints.size());
    }
}
