package com.oopteama.chess.game;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 작업 스레드 실행 추상화(G1). ai와 game이 Swing 없이 유지되도록 인터페이스만 여기에 둔다.
 * 구현: ui.SwingAsync(SwingWorker, D 담당) / 테스트 ImmediateAsync(호출 즉시 같은 스레드).
 */
public interface Async {

    interface Task { void cancel(); }

    /**
     * work는 작업 스레드에서 실행. 성공 onDone 또는 실패 onError 중 하나만 EDT에서 호출한다.
     * 취소된 작업에는 어느 콜백도 호출하지 않는다. 화면은 실패 콜백에서 계산 중 표시를 해제한다.
     * (SwingWorker는 취소돼도 done()을 부르므로 SwingAsync가 isCancelled()로 걸러야 한다.)
     */
    <T> Task run(Function<BooleanSupplier, T> work, Consumer<T> onDone, Consumer<Throwable> onError);
}
