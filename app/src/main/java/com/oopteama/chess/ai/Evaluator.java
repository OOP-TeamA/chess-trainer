package com.oopteama.chess.ai;

import com.oopteama.chess.core.Position;

/** 차례인 쪽 기준 점수(센티폰). 양수면 차례인 쪽이 유리. */
public interface Evaluator {
    int evaluate(Position pos);
}
