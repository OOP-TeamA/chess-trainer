package com.oopteama.chess.ai;

import com.oopteama.chess.core.Move;

/** lossCp: 최선 수 대비 점수 손실. refutation: 상대의 최선 응수(null 아님). */
public record Blunder(Move played, int lossCp, Move refutation) { }
