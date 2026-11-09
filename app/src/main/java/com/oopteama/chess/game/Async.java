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

    /** work는 작업 스레드에서, onDone은 UI 스레드에서 호출. 취소된 작업의 onDone은 호출하지 않는다. */
    <T> Task run(Function<BooleanSupplier, T> work, Consumer<T> onDone);
}
