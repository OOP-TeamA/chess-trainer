package com.oopteama.chess.ai;

import com.oopteama.chess.core.GameHistory;
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
        return search(pos, null, limits, cancelled);
    }

    /**
     * 대국 이력을 함께 받는 탐색. 이력에 이미 나온 국면으로 돌아가는 수는 무승부(0)로 보므로,
     * 이긴 쪽은 같은 수를 왕복하지 않고 진 쪽은 반복을 노린다(실제 대국의 3회 반복 무승부 대비).
     * history는 탐색 중 읽기만 하며, 다른 스레드가 바꾸지 않는 사본을 넘겨야 한다. null이면 이력 없음.
     */
    public SearchResult search(Position pos, GameHistory history, SearchLimits limits, BooleanSupplier cancelled) {
        Objects.requireNonNull(pos);
        Objects.requireNonNull(limits);
        Objects.requireNonNull(cancelled);
        List<Move> root = rules.legalMoves(pos);
        if (root.isEmpty()) throw new IllegalArgumentException("합법 수가 없는 국면");
        return new Run(pos, history, limits, cancelled).run(root);
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
        private final GameHistory history;   // null 가능
        private final SearchLimits limits;
        private final BooleanSupplier cancelled;
        private final long deadlineNanos;
        private final boolean timed;
        private long nodes;

        /** 탐색 경로(루트 포함)의 반복 키. 경로 안에서 같은 국면이 다시 나오면 무승부(0)로 본다. */
        private final List<String> pathKeys = new ArrayList<>();

        Run(Position pos, GameHistory history, SearchLimits limits, BooleanSupplier cancelled) {
            this.pos = pos;
            this.history = history;
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
            pathKeys.add(rules.repetitionKey(pos));
            try {
                for (int d = 1; d <= limits.depth(); d++) {
                    checkAbort();                                     // 이미 취소된 요청은 탐색하지 않는다
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

        /** move·score = 돌려줄 수와 그 수의 점수, bestByScore = 점수가 가장 높은 수(다음 깊이 정렬용). */
        private record RootOutcome(Move move, int score, Move bestByScore) { }

        private RootOutcome searchRoot(List<Move> moves, int depth, int topN) {
            int n = moves.size();
            int[] scores = new int[n];
            int alpha = -INF;
            int bestIdx = 0;
            int bestScore = -INF;

            for (int i = 0; i < n; i++) {
                Move m = moves.get(i);
                int s;
                makeOnPath(m);
                try {
                    // topN > 1이면 모든 수의 정확한 점수가 필요하므로 창을 좁히지 않는다.
                    s = topN > 1
                        ? -negamax(depth - 1, -INF, INF, 1)
                        : -negamax(depth - 1, -INF, -alpha, 1);
                } finally {
                    unmakeOnPath();
                }
                scores[i] = s;
                if (s > bestScore) { bestScore = s; bestIdx = i; }
                if (topN <= 1 && s > alpha) alpha = s;
            }

            Move best = moves.get(bestIdx);
            if (topN <= 1 || bestScore >= MATE_BOUND) {
                return new RootOutcome(best, bestScore, best);
            }
            int picked = pickRandomTop(scores, topN);
            return new RootOutcome(moves.get(picked), scores[picked], best);   // 점수는 고른 수의 것
        }

        /** 점수 상위 topN 수 중 무작위로 고른 인덱스. 지는 메이트로 가는 수는 후보에서 뺀다. */
        private int pickRandomTop(int[] scores, int topN) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < scores.length; i++) idx.add(i);
            idx.sort(Comparator.comparingInt((Integer i) -> scores[i]).reversed());   // 안정 정렬: 동점은 원래 순서
            List<Integer> candidates = new ArrayList<>();
            for (int k = 0; k < Math.min(topN, idx.size()); k++) {
                int i = idx.get(k);
                if (k == 0 || scores[i] > -MATE_BOUND) candidates.add(i);
            }
            return candidates.get(random.nextInt(candidates.size()));
        }

        /**
         * 종료 판정은 StandardRules.checkEnd처럼 메이트·스테일메이트를 50수 규칙보다 먼저 한다.
         * 경로 반복은 맨 앞에서 봐도 된다: 앞에서 이미 수를 둔 국면이라 메이트일 수 없다.
         * 깊이가 다 되면 정지 탐색으로 넘기며, 종료 판정은 정지 탐색이 먼저 한다.
         */
        private int negamax(int depth, int alpha, int beta, int ply) {
            tick();
            if (repeatsOnPath()) return 0;                           // 같은 국면으로 돌아오면 무승부
            if (depth <= 0) return quiesce(alpha, beta, ply, 0);

            List<Move> moves = rules.legalMoves(pos);
            if (moves.isEmpty()) return terminalScore(ply);
            if (pos.halfmoveClock() >= 100) return 0;                // 50수 규칙(메이트 다음)

            int best = -INF;
            for (Move m : order(moves)) {
                int s;
                makeOnPath(m);
                try {
                    s = -negamax(depth - 1, -beta, -alpha, ply + 1);
                } finally {
                    unmakeOnPath();
                }
                if (s > best) best = s;
                if (s > alpha) alpha = s;
                if (alpha >= beta) break;
            }
            return best;
        }

        /**
         * 잡기와 승진만 이어 보는 정지 탐색(수평선 효과 완화).
         * 체크 중이면 메이트를 먼저 판정하고, stand-pat 없이 체크를 피하는 합법 수 전체를 본다
         * (깊이 1에서도 한 수 메이트를 찾도록). 체크가 아니면 메이트일 수 없으므로 stand-pat 컷오프를
         * 먼저 하고(속도), 컷오프되지 않은 노드에서만 스테일메이트를 판정한다.
         */
        private int quiesce(int alpha, int beta, int ply, int qply) {
            tick();
            boolean inCheck = rules.inCheck(pos);
            List<Move> moves = null;
            if (inCheck) {
                moves = rules.legalMoves(pos);
                if (moves.isEmpty()) return terminalScore(ply);
            }
            if (pos.halfmoveClock() >= 100) return 0;                // 50수 규칙(메이트 다음)

            int standPat = evaluator.evaluate(pos);
            if (qply >= MAX_QUIESCENCE_PLY) return standPat;

            int best;
            if (inCheck) {
                best = -INF;                                         // 체크 중에는 "아무것도 안 하기"가 없다
            } else {
                if (standPat >= beta) return standPat;
                moves = rules.legalMoves(pos);
                if (moves.isEmpty()) return 0;                       // 스테일메이트
                if (standPat > alpha) alpha = standPat;
                best = standPat;
            }

            for (Move m : order(moves)) {
                if (!inCheck && !m.isCapture() && !m.isPromotion()) continue;
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
            checkAbort();
        }

        private void checkAbort() {
            if (cancelled.getAsBoolean() || (timed && System.nanoTime() >= deadlineNanos)) throw new Abort();
        }

        private void makeOnPath(Move m) {
            pos.make(m);
            pathKeys.add(rules.repetitionKey(pos));
        }

        private void unmakeOnPath() {
            pathKeys.remove(pathKeys.size() - 1);
            pos.unmake();
        }

        /**
         * 현재 국면(경로의 마지막)이 탐색 경로 앞쪽이나 대국 이력에 이미 있는가.
         * 경로는 잡기·폰 수 이후로는 반복이 불가능하므로 반수 시계만큼만 거슬러 본다.
         */
        private boolean repeatsOnPath() {
            int last = pathKeys.size() - 1;
            String key = pathKeys.get(last);
            if (history != null && history.occurrences(key) > 0) return true;   // 루트 국면 자신도 이력에 있다
            int from = Math.max(0, last - pos.halfmoveClock());
            for (int i = last - 2; i >= from; i -= 2) {              // 같은 차례끼리만 같은 국면이 된다
                if (pathKeys.get(i).equals(key)) return true;
            }
            return false;
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
