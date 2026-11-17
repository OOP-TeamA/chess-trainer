package com.oopteama.chess.api;

import java.util.function.Consumer;

/**
 * UI 대국 화면이 백엔드 대국 엔진에 접근하기 위한 최소 세션 계약.
 * 로컬 대국과 추후 AI 대국 모두 이 인터페이스로 다룰 수 있다.
 */
public interface GameSession {

    /** 현재 대국 상태를 반환한다. */
    GameState state();

    /** 수를 둔다. 성공 시 등록된 리스너에 새 상태를 알린다. */
    MoveAttemptResult playMove(MoveInput move);

    /** 마지막 한 수를 되돌린다. 성공 시 등록된 리스너에 새 상태를 알린다. */
    boolean undo();

    /** 현재 차례 진영이 기권한다. */
    void resign();

    /** 초기 국면으로 새 대국을 시작한다. */
    void newGame();

    /** 상태 변경 리스너를 등록한다. */
    void addListener(Consumer<GameState> listener);

    /** 상태 변경 리스너를 해제한다. */
    void removeListener(Consumer<GameState> listener);
}
