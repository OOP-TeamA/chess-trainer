package com.oopteama.chess.ai;

import com.oopteama.chess.core.Move;

/**
 * score는 best를 둔 경우의 점수(차례인 쪽 기준 센티폰, 메이트는 MATE - ply).
 * randomTopN &gt; 1로 최선이 아닌 수를 골랐다면 그 수의 점수다.
 * depthReached 0 = 취소·시간 초과로 깊이 1도 끝내지 못함(best는 정렬상 첫 수, score 0).
 */
public record SearchResult(Move best, int score, int depthReached, long nodes) { }
