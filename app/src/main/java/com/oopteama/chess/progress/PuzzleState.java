package com.oopteama.chess.progress;

/**
 * 퍼즐 하나의 풀이 상태. 기록 파일의 {@code puzzle.attempt.<id>=시도,정답}에서 정해진다(기능 명세 6장).
 */
public enum PuzzleState {
    /** 시도 0. */
    UNSEEN,
    /** 정답 1 이상: 정답 공개(REVEAL) 단계 전에 한 번 이상 끝까지 풀었다. */
    SOLVED,
    /** 시도는 있지만 정답 0. */
    FAILED
}
