package com.oopteama.chess.ai;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.core.piece.Piece;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * 반복 심화 + 알파-베타(negamax) 탐색. Swing도 스레드도 모르는 순수 계산(G1).
 * 호출마다 상태를 새로 만들므로 인스턴스를 여러 작업이 공유해도 안전하다.
 * 단, 같은 Position을 두 스레드가 동시에 넘기면 안 된다.
 */
public final class AlphaBetaSearch {

    /** 메이트 점수 = MATE - ply. */
    public static final int MATE = 100_000;

    private static final int MATE_BOUND = MATE - 1_000;   // 이 이상이면 메이트 점수로 취급
    private static final int INF = MATE + 1_000;
    private static final int MAX_QUIESCENCE_PLY = 6;
    private static final int ABORT_CHECK_MASK = 1023;     // 1024 노드마다 취소/시간 확인

    private final RuleSet rules;
    private final Evaluator evaluator;
    private final Random random;

    public AlphaBetaSearch(RuleSet rules, Evaluator evaluator, Random random) {
        this.rules = Objects.requireNonNull(rules);
        this.evaluator = Objects.requireNonNull(evaluator);
        this.random = Objects.requireNonNull(random);
    }

    /** BlunderCheck가 같은 규칙을 쓰도록 패키지 안에만 공개. */
    RuleSet rules() { return rules; }

    /**
     * 사전조건: 합법 수 1개 이상. pos는 탐색 중 변하지만 반환 시 원상태.
     * cancelled가 true가 되거나 시간이 다 되면 직전에 끝낸 깊이의 결과를 반환한다.
     */
    public SearchResult search(Position pos, SearchLimits limits, BooleanSupplier cancelled) {
        Objects.requireNonNull(pos);
        Objects.requireNonNull(limits);
        Objects.requireNonNull(cancelled);
        List<Move> root = rules.legalMoves(pos);
        if (root.isEmpty()) throw new IllegalArgumentException("합법 수가 없는 국면");
        return new Run(pos, limits, cancelled).run(root);
    }

    // ------------------------------------------------------------------

    /** 스택 트레이스 없는 취소 신호. */
    private static final class Abort extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Abort() { super(null, null, false, false); }
    }

    /** 한 번의 search 호출이 가지는 가변 상태. */
    private final class Run {
        private final Position pos;
        private final SearchLimits limits;
        private final BooleanSupplier cancelled;
        private final long deadlineNanos;
        private final boolean timed;
        private long nodes;

        Run(Position pos, SearchLimits limits, BooleanSupplier cancelled) {
            this.pos = pos;
            this.limits = limits;
            this.cancelled = cancelled;
            this.timed = limits.timeLimitMillis() > 0;
            this.deadlineNanos = timed ? System.nanoTime() + limits.timeLimitMillis() * 1_000_000L : 0L;
        }

        SearchResult run(List<Move> rootMoves) {
            List<Move> ordered = order(rootMoves);
            Move bestMove = ordered.get(0);          // 1단계도 못 끝냈을 때의 대비책
            int bestScore = 0;
            int reached = 0;
            try {
                for (int d = 1; d <= limits.depth(); d++) {
                    boolean last = d == limits.depth();
                    RootOutcome out = searchRoot(ordered, d, last ? limits.randomTopN() : 1);
                    bestMove = out.move;
                    bestScore = out.score;
                    reached = d;
                    ordered = moveToFront(ordered, out.bestByScore);   // 다음 깊이의 정렬 힌트
                    if (Math.abs(bestScore) >= MATE_BOUND) break;      // 메이트 확정이면 더 깊이 볼 필요 없음
                }
            } catch (Abort a) {
                // 직전 완료 깊이의 결과를 그대로 반환
            }
            return new SearchResult(bestMove, bestScore, reached, nodes);
        }

        private record RootOutcome(Move move, int score, Move bestByScore) { }

        private RootOutcome searchRoot(List<Move> moves, int depth, int topN) {
            int n = moves.size();
            int[] scores = new int[n];
            int alpha = -INF;
            int bestIdx = 0;
            int bestScore = -INF;

            for (int i = 0; i < n; i++) {
                Move m = moves.get(i);
                pos.make(m);
                int s;
                try {
                    // topN > 1이면 모든 수의 정확한 점수가 필요하므로 창을 좁히지 않는다.
                    s = topN > 1
                        ? -negamax(depth - 1, -INF, INF, 1)
                        : -negamax(depth - 1, -INF, -alpha, 1);
                } finally {
                    pos.unmake();
                }
                scores[i] = s;
                if (s > bestScore) { bestScore = s; bestIdx = i; }
                if (topN <= 1 && s > alpha) alpha = s;
            }

            Move best = moves.get(bestIdx);
            if (topN <= 1 || bestScore >= MATE_BOUND) {
                return new RootOutcome(best, bestScore, best);
            }
            return new RootOutcome(pickRandomTop(moves, scores, topN, bestScore), bestScore, best);
        }

        /** 점수 상위 topN 수 중 무작위. 지는 메이트로 가는 수는 후보에서 뺀다. */
        private Move pickRandomTop(List<Move> moves, int[] scores, int topN, int bestScore) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < moves.size(); i++) idx.add(i);
            idx.sort(Comparator.comparingInt((Integer i) -> scores[i]).reversed());   // 안정 정렬: 동점은 원래 순서
            List<Move> candidates = new ArrayList<>();
            for (int k = 0; k < Math.min(topN, idx.size()); k++) {
                int i = idx.get(k);
                if (k == 0 || scores[i] > -MATE_BOUND) candidates.add(moves.get(i));
            }
            return candidates.get(random.nextInt(candidates.size()));
        }

        private int negamax(int depth, int alpha, int beta, int ply) {
            tick();
            if (pos.halfmoveClock() >= 100) return 0;                // 50수 규칙
            if (depth <= 0) return quiesce(alpha, beta, ply, 0);

            List<Move> moves = rules.legalMoves(pos);
            if (moves.isEmpty()) return terminalScore(ply);

            int best = -INF;
            for (Move m : order(moves)) {
                pos.make(m);
                int s;
                try {
                    s = -negamax(depth - 1, -beta, -alpha, ply + 1);
                } finally {
                    pos.unmake();
                }
                if (s > best) best = s;
                if (s > alpha) alpha = s;
                if (alpha >= beta) break;
            }
            return best;
        }

        /** 잡기와 승진만 이어 보는 정지 탐색(수평선 효과 완화). */
        private int quiesce(int alpha, int beta, int ply, int qply) {
            tick();
            int standPat = evaluator.evaluate(pos);
            if (qply >= MAX_QUIESCENCE_PLY) return standPat;
            if (standPat >= beta) return standPat;
            if (standPat > alpha) alpha = standPat;

            List<Move> moves = rules.legalMoves(pos);
            if (moves.isEmpty()) return terminalScore(ply);

            int best = standPat;
            for (Move m : order(moves)) {
                if (!m.isCapture() && !m.isPromotion()) continue;
                pos.make(m);
                int s;
                try {
                    s = -quiesce(-beta, -alpha, ply + 1, qply + 1);
                } finally {
                    pos.unmake();
                }
                if (s > best) best = s;
                if (s > alpha) alpha = s;
                if (alpha >= beta) break;
            }
            return best;
        }

        /** 합법 수가 없을 때: 체크면 진 것(가까운 메이트일수록 더 나쁨), 아니면 스테일메이트. */
        private int terminalScore(int ply) {
            return rules.inCheck(pos) ? -(MATE - ply) : 0;
        }

        private void tick() {
            if ((++nodes & ABORT_CHECK_MASK) != 0) return;
            if (cancelled.getAsBoolean() || (timed && System.nanoTime() >= deadlineNanos)) throw new Abort();
        }

        /** MVV-LVA: 비싼 기물을 싼 기물로 잡는 수를 먼저. 승진도 앞으로. */
        private List<Move> order(List<Move> moves) {
            List<Move> sorted = new ArrayList<>(moves);
            sorted.sort(Comparator.comparingInt(this::orderKey).reversed());
            return sorted;
        }

        private int orderKey(Move m) {
            int key = 0;
            if (m.isCapture()) {
                Piece victim = pos.at(m.to());
                int victimValue = victim == null ? 100 : MaterialEvaluator.value(victim.type());   // null = 앙파상
                Piece attacker = pos.at(m.from());
                key += 10_000 + victimValue * 10 - MaterialEvaluator.value(attacker.type()) / 10;
            }
            if (m.isPromotion()) key += 9_000 + MaterialEvaluator.value(m.promotion());
            return key;
        }

        private List<Move> moveToFront(List<Move> moves, Move first) {
            List<Move> out = new ArrayList<>(moves.size());
            out.add(first);
            for (Move m : moves) if (!m.equals(first)) out.add(m);
            return out;
        }
    }
}
