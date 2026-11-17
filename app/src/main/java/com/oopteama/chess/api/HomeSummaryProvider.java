package com.oopteama.chess.api;

/** 홈 요약 정보 및 변경 알림을 제공하는 선택적 포트. */
public interface HomeSummaryProvider {

    HomeSummary current();

    default void addChangeListener(Runnable listener) {
    }

    default void removeChangeListener(Runnable listener) {
    }
}
