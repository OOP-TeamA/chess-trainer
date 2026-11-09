package com.oopteama.chess.game;

import com.oopteama.chess.ai.AlphaBetaSearch;
import com.oopteama.chess.ai.SearchLimits;
import com.oopteama.chess.ai.SearchResult;
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

    public AiPlayer(AlphaBetaSearch search, SearchLimits limits, Async async) {
        this.search = Objects.requireNonNull(search);
        this.limits = Objects.requireNonNull(limits);
        this.async = Objects.requireNonNull(async);
    }

    /** "생각 중" 표시용. true = 시작, false = 끝 또는 취소. */
    public void setThinkingListener(Consumer<Boolean> thinking) {
        this.thinkingListener = Objects.requireNonNull(thinking);
    }

    /** 디버깅용: 탐색이 끝날 때마다 깊이, 점수, 노드 수를 받는다. EDT에서 호출. */
    public void setResultListener(Consumer<SearchResult> l) {
        this.resultListener = Objects.requireNonNull(l);
    }

    @Override
    public void requestMove(Position snapshot, Consumer<Move> onMove) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(onMove);
        cancel();

        Position own = snapshot.copy();   // 작업 스레드가 쓸 전용 사본
        busy = true;
        thinkingListener.accept(true);
        Async.Task t = async.run(
            cancelled -> search.search(own, limits, cancelled),
            result -> {
                busy = false;
                task = null;
                thinkingListener.accept(false);
                resultListener.accept(result);
                onMove.accept(result.best());
            });
        if (busy) task = t;   // 즉시 실행 구현에서는 위 onDone이 이미 끝났을 수 있다
    }

    @Override
    public void cancel() {
        if (task != null) task.cancel();
        task = null;
        if (busy) {
            busy = false;
            thinkingListener.accept(false);
        }
    }

    /**
     * AI-06 힌트용(GameController 전용). 사용자 쪽 최선 수를 계산한다.
     * 생각 중 표시는 켜지 않고, 입문 난이도의 무작위 선택도 끈다(힌트는 항상 최선 수).
     */
    Async.Task suggest(Position snapshot, Consumer<Move> onHint) {
        Position own = snapshot.copy();
        SearchLimits hintLimits = new SearchLimits(limits.depth(), limits.timeLimitMillis(), 1);
        return async.run(
            cancelled -> search.search(own, hintLimits, cancelled),
            result -> onHint.accept(result.best()));
    }
}
