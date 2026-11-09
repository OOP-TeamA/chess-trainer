package com.oopteama.chess.ai;

import com.oopteama.chess.core.Move;

/** score는 차례인 쪽 기준 센티폰. 메이트는 MATE - ply. */
public record SearchResult(Move best, int score, int depthReached, long nodes) { }
