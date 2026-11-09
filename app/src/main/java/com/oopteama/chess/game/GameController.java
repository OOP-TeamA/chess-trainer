package com.oopteama.chess.game;

import com.oopteama.chess.ai.BlunderCheck;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 대국 진행의 상태 기계. 실수 알림 확인을 기다리는 상태가 명시적으로 있다(G9). EDT 전용.
 *
 * <pre>
 * start → WAITING_HUMAN | WAITING_AI
 * WAITING_HUMAN → (사용자 수) → CHECKING_BLUNDER(알림 켜짐) | WAITING_AI(꺼짐) | ENDED
 * CHECKING_BLUNDER → WAITING_BLUNDER_DECISION(실수) | WAITING_AI(정상)
 * WAITING_BLUNDER_DECISION → proceedAfterBlunder → WAITING_AI / undoTurn → WAITING_HUMAN
 * WAITING_AI → (AI 수) → WAITING_HUMAN | ENDED
 * </pre>
 */
public final class GameController {

    public enum State { WAITING_HUMAN, CHECKING_BLUNDER, WAITING_BLUNDER_DECISION, WAITING_AI, ENDED }

    private final Game game;
    private final Player white;
    private final Player black;
    private final Optional<BlunderCheck> blunders;   // 비어 있으면 실수 알림 꺼짐
    private final Async async;

    private State state;
    private boolean started;
    private boolean disposed;
    /** 턴이 바뀔 때마다 증가. 늦게 도착한 콜백(취소 이전에 예약된 것)을 걸러낸다. */
    private int turnToken;

    private Async.Task blunderTask;
    private Async.Task hintTask;
    private BlunderListener blunderListener;
    private Consumer<State> stateListener;

    public GameController(Game game, Player white, Player black,
                          Optional<BlunderCheck> blunders, Async async) {
        this.game = Objects.requireNonNull(game);
        this.white = Objects.requireNonNull(white);
        this.black = Objects.requireNonNull(black);
        this.blunders = Objects.requireNonNull(blunders);
        this.async = Objects.requireNonNull(async);
    }

    /** start() 이후에만 호출할 수 있다. 그 전에는 IllegalStateException. */
    public State state() {
        if (!started) throw new IllegalStateException("start() 전");
        return state;
    }

    public void start() {
        if (started) throw new IllegalStateException("이미 시작함");
        started = true;
        startTurn();
    }

    /**
     * AI-05. 사용자 차례가 될 때까지 되돌린다. 계산 중이면 먼저 취소.
     * 사용자 차례에서 누르면 AI의 수와 내 수, 두 수를 되돌린다.
     * 되돌려도 사용자 차례에 닿을 수 없으면(AI의 첫 수뿐 등) 아무 일도 하지 않는다.
     */
    public void undoTurn() {
        requireActive();
        int plies = pliesToUndo();
        if (plies > game.history().moves().size()) return;
        cancelAll();
        game.undo(plies);
        startTurn();
    }

    /** 사용자 기권. 이미 끝났으면 IllegalStateException. */
    public void resign() {
        requireActive();
        if (state == State.ENDED) throw new IllegalStateException("이미 끝난 대국");
        Side loser = humanSide().orElse(game.sideToMove());
        cancelAll();
        game.resign(loser);
        setState(State.ENDED);
    }

    /**
     * AI-06. WAITING_HUMAN에서만. 최선 수를 onHint(EDT)로 전달한다.
     * 대국에 AiPlayer가 없으면 IllegalStateException.
     */
    public void hint(Consumer<Move> onHint) {
        requireActive();
        Objects.requireNonNull(onHint);
        if (state != State.WAITING_HUMAN) throw new IllegalStateException("사용자 차례에서만 힌트 가능: " + state);
        AiPlayer ai = aiPlayer().orElseThrow(() -> new IllegalStateException("AI 플레이어 없음"));

        cancelHint();
        int token = turnToken;
        Async.Task t = ai.suggest(game.snapshot(), move -> {
            if (disposed || token != turnToken || state != State.WAITING_HUMAN) return;
            hintTask = null;
            onHint.accept(move);
        });
        if (token == turnToken && state == State.WAITING_HUMAN) hintTask = t;
    }

    /** WAITING_BLUNDER_DECISION에서 "그대로 두기". 되무르기는 undoTurn. */
    public void proceedAfterBlunder() {
        requireActive();
        if (state != State.WAITING_BLUNDER_DECISION) {
            throw new IllegalStateException("실수 알림 확인 대기 상태가 아님: " + state);
        }
        startTurn();
    }

    public void setBlunderListener(BlunderListener l) { this.blunderListener = l; }

    public void setStateListener(Consumer<State> l) { this.stateListener = l; }

    /** 화면 이탈: 계산 취소, 리스너 해제. 이후 호출은 모두 무시되거나 IllegalStateException. */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        cancelAll();
        blunderListener = null;
        stateListener = null;
        if (started) state = State.ENDED;
    }

    // ------------------------------------------------------------------ 진행

    /** 현재 국면의 차례인 플레이어에게 수를 요청한다. */
    private void startTurn() {
        if (disposed) return;
        if (game.result().isPresent()) {
            setState(State.ENDED);
            return;
        }
        Side side = game.sideToMove();
        Player player = player(side);
        int token = ++turnToken;
        setState(player instanceof HumanPlayer ? State.WAITING_HUMAN : State.WAITING_AI);
        // 즉시 실행 Async에서는 아래 호출 안에서 onMove까지 끝날 수 있다. 상태는 미리 정해 둔다.
        player.requestMove(game.snapshot(), move -> onMove(token, side, move));
    }

    private void onMove(int token, Side side, Move move) {
        if (disposed || token != turnToken) return;
        Player player = player(side);

        if (!game.legalMoves().contains(move)) {
            // 화면 계약 위반(legalMoves()에서 고르지 않은 수). 사용자는 다시 입력받고 AI라면 버그다.
            if (player instanceof HumanPlayer) {
                player.requestMove(game.snapshot(), m -> onMove(token, side, m));
                return;
            }
            throw new IllegalStateException("AI가 합법이 아닌 수를 냄: " + move);
        }

        cancelHint();
        Position before = (blunders.isPresent() && player instanceof HumanPlayer) ? game.snapshot() : null;
        game.make(move);   // 리스너(화면, 기록)가 여기서 호출된다
        if (disposed || token != turnToken) return;

        if (game.result().isPresent()) {
            setState(State.ENDED);
        } else if (before != null) {
            checkBlunder(before, move);
        } else {
            startTurn();
        }
    }

    /** 사용자 수 직후 작업 스레드에서 실수 여부를 판정. 정상이면 곧바로 AI 차례로. */
    private void checkBlunder(Position before, Move played) {
        int token = ++turnToken;
        setState(State.CHECKING_BLUNDER);
        BlunderCheck check = blunders.orElseThrow();
        Async.Task t = async.run(
            cancelled -> check.check(before, played, cancelled),
            found -> {
                if (disposed || token != turnToken) return;
                blunderTask = null;
                if (found.isPresent()) {
                    setState(State.WAITING_BLUNDER_DECISION);   // AI는 확인을 받을 때까지 기다린다(G9)
                    if (blunderListener != null) blunderListener.blunderDetected(found.get());
                } else {
                    startTurn();
                }
            });
        if (token == turnToken && state == State.CHECKING_BLUNDER) blunderTask = t;
    }

    // ------------------------------------------------------------------ 보조

    private int pliesToUndo() {
        boolean whiteHuman = white instanceof HumanPlayer;
        boolean blackHuman = black instanceof HumanPlayer;
        if (whiteHuman && blackHuman) return 1;                 // 둘이서 두는 대국: 한 수씩
        if (!whiteHuman && !blackHuman) throw new IllegalStateException("사용자가 없는 대국");
        Side human = whiteHuman ? Side.WHITE : Side.BLACK;
        return game.sideToMove() == human ? 2 : 1;              // 내 차례면 AI 수 + 내 수, 아니면 내 수만
    }

    private Optional<Side> humanSide() {
        boolean whiteHuman = white instanceof HumanPlayer;
        boolean blackHuman = black instanceof HumanPlayer;
        if (whiteHuman == blackHuman) return Optional.empty();
        return Optional.of(whiteHuman ? Side.WHITE : Side.BLACK);
    }

    private Optional<AiPlayer> aiPlayer() {
        if (white instanceof AiPlayer a) return Optional.of(a);
        if (black instanceof AiPlayer a) return Optional.of(a);
        return Optional.empty();
    }

    private Player player(Side side) { return side == Side.WHITE ? white : black; }

    private void requireActive() {
        if (!started) throw new IllegalStateException("start() 전");
        if (disposed) throw new IllegalStateException("dispose() 후");
    }

    /** 진행 중인 모든 계산과 입력 대기를 취소하고, 이미 예약된 콜백을 무효화한다. */
    private void cancelAll() {
        turnToken++;
        if (blunderTask != null) { blunderTask.cancel(); blunderTask = null; }
        cancelHint();
        white.cancel();
        black.cancel();
    }

    private void cancelHint() {
        if (hintTask != null) { hintTask.cancel(); hintTask = null; }
    }

    private void setState(State next) {
        if (state == next) return;
        state = next;
        if (stateListener != null) stateListener.accept(next);
    }
}
