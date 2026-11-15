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
    /** 경고 후보 확인용. 둔 수 뒤 상대 차례에서 4반수(희생 → 응수 → 조용한 메이트까지). */
    private static final int CONFIRM_DEPTH = 4;
    /** 확인 탐색 상한. 넘으면 마친 깊이까지의 결과로 판정한다. */
    private static final long CONFIRM_TIME_MILLIS = 1_500;

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
     *
     * <p>대국이 끝나는 수(메이트·스테일메이트)는 판정하지 않는다. 메이트는 실수일 수 없고,
     * 이긴 국면을 스테일메이트로 비기는 수는 응수(refutation)가 없어 Blunder로 표현할 수 없으며,
     * GameController도 종료 수 뒤에는 실수 판정 없이 바로 ENDED로 간다.
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
        if (rules.legalMoves(pos).isEmpty()) return Optional.empty();   // 대국이 끝나는 수는 판정 대상 아님
        SearchResult reply = search.search(pos, new SearchLimits(CHECK_DEPTH - 1, 0, 1), cancelled);
        if (cancelled.getAsBoolean()) return Optional.empty();
        if (best.score() - (-reply.score()) < thresholdCp) return Optional.empty();

        // 3) 경고 후보만 더 깊게 다시 본다. 정지 탐색은 잡기만 이어 보므로 기물을 내준 뒤
        //    조용한 수로 끝나는 메이트(오페라 게임 15.Bxd7+ Nxd7 16.Qb8+!! Nxb8 17.Rd8#)를
        //    손실로 잘못 센다. 평소(손실 없음)에는 추가 비용이 없다.
        SearchResult confirm = search.search(
            pos, new SearchLimits(CONFIRM_DEPTH, CONFIRM_TIME_MILLIS, 1), cancelled);
        if (cancelled.getAsBoolean()) return Optional.empty();

        int loss = best.score() - (-confirm.score());
        if (loss < thresholdCp) return Optional.empty();
        return Optional.of(new Blunder(played, loss, confirm.best()));
    }
}
