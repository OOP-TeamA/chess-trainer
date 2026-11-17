package com.oopteama.chess.api;

import java.util.Optional;

/**
 * UI가 백엔드 기능에 접근하는 유일한 서비스 허브.
 * 아직 구현되지 않은 학습/퀴즈/기보 기능은 Optional.empty()로 비워 둘 수 있다.
 */
public interface AppServices {

    /** 로컬 2인 대국 세션을 생성한다. */
    GameSession createLocalGame();

    /** 홈 대시보드 요약 정보 제공자. 아직 미구현이면 empty. */
    default Optional<HomeSummaryProvider> homeSummary() {
        return Optional.empty();
    }

    /** 창 종료 직전에 백엔드 정리·저장이 필요할 때 호출된다. */
    default void saveOnClose() throws Exception {
    }
}
