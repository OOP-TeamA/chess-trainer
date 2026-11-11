package com.oopteama.chess.game;

import com.oopteama.chess.ai.AlphaBetaSearch;
import com.oopteama.chess.ai.SearchLimits;
import com.oopteama.chess.ai.SearchResult;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;

import java.util.Objects;
import java.util.function.Consumer;

/** AI-04: 탐색은 Async 작업 스레드에서, 자기 소유의 Position 사본으로만 실행한다. */
public final class AiPlayer implements Player {

    private final AlphaBetaSearch search;
    private final SearchLimits limits;
    private final Async async;

    private Consumer<Boolean> thinkingListener = thinking -> { };
    private Consumer<SearchResult> resultListener = r -> { };
    private Async.Task task;
    private boolean busy;
    /**
     * 요청 번호. 새 요청과 취소 때마다 증가한다. 콜백은 자기 번호가 최신일 때만 상태를 바꾸므로,
     * 취소된 뒤 늦게 도착한 이전 요청의 콜백(Async 구현 실수 등)이 새 요청을 지우지 못한다.
     */
    private int request;

    public AiPlayer(AlphaBetaSearch search, SearchLimits limits, Async async) {
        this.search = Objects.requireNonNull(search);
        this.limits = Objects.requireNonNull(limits);
        this.async = Objects.requireNonNull(async);
    }

    /** "생각 중" 표시용. true = 시작, false = 끝, 취소 또는 실패. */
    public void setThinkingListener(Consumer<Boolean> thinking) {
        this.thinkingListener = Objects.requireNonNull(thinking);
    }

    /** 디버깅·테스트용(계약 밖): 탐색이 끝날 때마다 깊이, 점수, 노드 수를 받는다. EDT에서 호출. */
    void setResultListener(Consumer<SearchResult> l) {
        this.resultListener = Objects.requireNonNull(l);
    }

    /**
     * 탐색이 실패하면(작업 스레드 예외) 생각 중 표시를 끄고 IllegalStateException으로 알린다.
     * 순수 계산의 예외는 버그이므로 숨기지 않는다. 대국은 되무르기·기권으로 계속할 수 있다.
     */
    @Override
    public void requestMove(Position snapshot, Consumer<Move> onMove) {
        requestMove(snapshot, null, onMove);
    }

    /** GameController 전용: 대국 이력 사본을 함께 넘겨 이미 나온 국면으로 돌아가는 수를 피한다. */
    void requestMove(Position snapshot, GameHistory history, Consumer<Move> onMove) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(onMove);
        cancel();

        Position own = snapshot.copy();   // 작업 스레드가 쓸 전용 사본
        int id = ++request;
        busy = true;
        thinkingListener.accept(true);
        Async.Task t = async.run(
            cancelled -> search.search(own, history, limits, cancelled),
            result -> {
                if (id != request) return;
                finish();
                resultListener.accept(result);
                onMove.accept(result.best());
            },
            error -> {
                if (id != request) return;
                finish();
                throw new IllegalStateException("AI 탐색 실패", error);
            });
        if (id == request && busy) task = t;   // 즉시 실행 구현에서는 위 콜백이 이미 끝났을 수 있다
    }

    @Override
    public void cancel() {
        request++;
        if (task != null) task.cancel();
        task = null;
        if (busy) {
            busy = false;
            thinkingListener.accept(false);
        }
    }

    private void finish() {
        busy = false;
        task = null;
        thinkingListener.accept(false);
    }

    /**
     * AI-06 힌트용(GameController 전용). 사용자 쪽 최선 수를 계산한다.
     * 생각 중 표시는 켜지 않고, 입문 난이도의 무작위 선택도 끈다(힌트는 항상 최선 수).
     * 늦은 콜백은 GameController가 turnToken으로 거른다.
     */
    Async.Task suggest(Position snapshot, GameHistory history, Consumer<Move> onHint, Consumer<Throwable> onError) {
        Position own = snapshot.copy();
        SearchLimits hintLimits = new SearchLimits(limits.depth(), limits.timeLimitMillis(), 1);
        return async.run(
            cancelled -> search.search(own, history, hintLimits, cancelled),
            result -> onHint.accept(result.best()),
            onError);
    }
}
