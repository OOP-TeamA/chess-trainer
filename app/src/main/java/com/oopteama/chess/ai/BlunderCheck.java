package com.oopteama.chess.ai;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * AI-10 실수 알림. "평가가 떨어졌는가"가 아니라 "최선 수 대비 손실"로 판정해
 * 이미 불리한 국면에서 모든 수가 실수로 나오는 오탐을 막는다(G15).
 */
public final class BlunderCheck {

    private static final int CHECK_DEPTH = 2;   // 시퀀스 1: search(depth 2)

    private final AlphaBetaSearch search;
    private final int thresholdCp;

    /** 기본 임계값은 200(기물 가치 2). */
    public BlunderCheck(AlphaBetaSearch search, int thresholdCp) {
        this.search = Objects.requireNonNull(search);
        if (thresholdCp <= 0) throw new IllegalArgumentException("thresholdCp > 0");
        this.thresholdCp = thresholdCp;
    }

    /**
     * before에서 최선 수 점수 대비 played의 점수 손실이 threshold 이상이면 Blunder.
     * 작업 스레드에서 실행하며, 취소되면 빈 값. before는 내부에서 다시 복사해 쓴다.
     */
    public Optional<Blunder> check(Position before, Move played, BooleanSupplier cancelled) {
        Objects.requireNonNull(before);
        Objects.requireNonNull(played);
        Objects.requireNonNull(cancelled);

        RuleSet rules = search.rules();
        Position pos = before.copy();
        List<Move> legal = rules.legalMoves(pos);
        if (!legal.contains(played)) throw new IllegalArgumentException("합법 수가 아님: " + played);
        if (legal.size() == 1) return Optional.empty();            // 고를 수가 없었다

        // 1) 최선 수의 점수
        SearchResult best = search.search(pos, new SearchLimits(CHECK_DEPTH, 0, 1), cancelled);
        if (cancelled.getAsBoolean()) return Optional.empty();
        if (best.best().equals(played)) return Optional.empty();

        // 2) 둔 수의 점수: 둔 뒤 상대 차례에서 한 단계 얕게 탐색해 부호를 뒤집는다
        pos.make(played);
        if (rules.legalMoves(pos).isEmpty()) return Optional.empty();   // 대국이 끝나는 수에는 경고하지 않음
        SearchResult reply = search.search(pos, new SearchLimits(CHECK_DEPTH - 1, 0, 1), cancelled);
        if (cancelled.getAsBoolean()) return Optional.empty();

        int playedScore = -reply.score();
        int loss = best.score() - playedScore;
        if (loss < thresholdCp) return Optional.empty();
        return Optional.of(new Blunder(played, loss, reply.best()));
    }
}
