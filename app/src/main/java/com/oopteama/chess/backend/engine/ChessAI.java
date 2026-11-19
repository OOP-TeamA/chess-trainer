package com.oopteama.chess.backend.engine;

import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.engine.Piece.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * 자체 구현 체스 AI 탐색 엔진입니다.
 * 책임(SRP): 난이도별 알파-베타·PST 수 탐색(chooseMove/hint)과 N수 강제 메이트 솔버(findMateIn/bestDefenseAgainstMate)만 담당합니다.
 */
public final class ChessAI {

    public enum Difficulty {
        BEGINNER("입문", 1, false, true, 1000L),
        EASY("초급", 2, false, false, 1500L),
        MEDIUM("중급", 3, true, false, 2000L),
        HARD("상급", 4, true, false, 2500L);

        private final String label;
        private final int depth;
        private final boolean usePst;
        private final boolean topRandom;
        private final long timeLimitMs;

        Difficulty(String label, int depth, boolean usePst, boolean topRandom, long timeLimitMs) {
            this.label = label;
            this.depth = depth;
            this.usePst = usePst;
            this.topRandom = topRandom;
            this.timeLimitMs = timeLimitMs;
        }

        public String label() {
            return label;
        }

        public int depth() {
            return depth;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** OCP: 평가 함수를 교체할 수 있는 전략 인터페이스. */
    @FunctionalInterface
    public interface Evaluator {
        int evaluate(Board board);
    }

    static final int MATE_SCORE = 100_000;
    static final int INF = 1_000_000;

    // 8행(rank 7) -> 1행(rank 0) 기준 백 시점 위치 점수표 (흑은 상하 대칭 적용)
    private static final int[] PAWN_PST = {
             0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
             5,  5, 10, 25, 25, 10,  5,  5,
             0,  0,  0, 20, 20,  0,  0,  0,
             5, -5,-10,  0,  0,-10, -5,  5,
             5, 10, 10,-20,-20, 10, 10,  5,
             0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] KNIGHT_PST = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] BISHOP_PST = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] ROOK_PST = {
              0,  0,  0,  0,  0,  0,  0,  0,
              5, 10, 10, 10, 10, 10, 10,  5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
             -5,  0,  0,  0,  0,  0,  0, -5,
              0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] QUEEN_PST = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
             -5,  0,  5,  5,  5,  5,  0, -5,
              0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] KING_PST = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
             20, 20,  0,  0,  0,  0, 20, 20,
             20, 30, 10,  0,  0, 10, 30, 20
    };

    public static final Evaluator MATERIAL = board -> evaluateBoard(board, false);
    public static final Evaluator MATERIAL_PST = board -> evaluateBoard(board, true);

    static final class AbortedException extends RuntimeException {
    }

    private record ScoredMove(Move move, int score) {
    }

    private final Random random;

    public ChessAI() {
        this(new Random());
    }

    public ChessAI(Random random) {
        this.random = random;
    }

    // ---- 대국 AI 수 선택 및 힌트 ----

    public Optional<Move> chooseMove(Board board, Difficulty difficulty) {
        return chooseMove(board, difficulty, () -> false);
    }

    public Optional<Move> chooseMove(Board board, Difficulty difficulty, BooleanSupplier cancelled) {
        List<Move> legal = orderedMoves(board, board.legalMoves());
        if (legal.isEmpty()) {
            return Optional.empty();
        }
        if (legal.size() == 1) {
            return Optional.of(legal.getFirst());
        }

        Evaluator evaluator = difficulty.usePst ? MATERIAL_PST : MATERIAL;
        long deadline = System.currentTimeMillis() + difficulty.timeLimitMs;
        Move bestMove = legal.getFirst();

        if (difficulty.topRandom) {
            List<ScoredMove> scored = new ArrayList<>();
            for (Move move : legal) {
                if (cancelled.getAsBoolean()) {
                    return Optional.empty();
                }
                int score = -negamax(board.applyMove(move), 0, -INF, INF, 1, evaluator, deadline, cancelled);
                scored.add(new ScoredMove(move, score));
            }
            scored.sort(Comparator.comparingInt(ScoredMove::score).reversed());
            int topScore = scored.getFirst().score();
            List<Move> pool = new ArrayList<>();
            for (int i = 0; i < Math.min(3, scored.size()); i++) {
                if (topScore - scored.get(i).score() <= 180) {
                    pool.add(scored.get(i).move());
                }
            }
            return Optional.of(pool.get(random.nextInt(pool.size())));
        }

        for (int depth = 1; depth <= difficulty.depth; depth++) {
            try {
                Move depthBest = searchRoot(board, legal, depth, evaluator, deadline, cancelled);
                if (depthBest != null) {
                    bestMove = depthBest;
                    legal.remove(depthBest);
                    legal.addFirst(depthBest);
                }
            } catch (AbortedException e) {
                if (cancelled.getAsBoolean()) {
                    return Optional.empty();
                }
                break;
            }
        }
        return Optional.of(bestMove);
    }

    public Optional<Move> hint(Board board) {
        return chooseMove(board, Difficulty.MEDIUM, () -> false);
    }

    // ---- 자유 메이트 퍼즐용 정확한 N수 강제 메이트 탐색 & 최선 방어 응수 ----

    public Optional<Move> findMateIn(Board board, int maxMoves) {
        if (maxMoves <= 0 || board.isSandbox()) {
            return Optional.empty();
        }
        long deadline = System.currentTimeMillis() + 800L;
        List<Move> candidates = orderChecksFirst(board, board.legalMoves());
        for (int limit = 1; limit <= maxMoves; limit++) {
            for (Move m : candidates) {
                try {
                    Board after = board.applyMove(m);
                    if (after.isCheckmate()) {
                        return Optional.of(m);
                    }
                    if (limit > 1 && isForcedMateForOpponentToMove(after, limit - 1, deadline)) {
                        return Optional.of(m);
                    }
                } catch (AbortedException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public boolean isForcedMateAfterMove(Board after, int attackerMovesLeft) {
        if (after.isCheckmate()) {
            return true;
        }
        if (attackerMovesLeft <= 0 || after.isSandbox()) {
            return false;
        }
        long deadline = System.currentTimeMillis() + 600L;
        try {
            return isForcedMateForOpponentToMove(after, attackerMovesLeft, deadline);
        } catch (AbortedException e) {
            return false;
        }
    }

    public Optional<Move> bestDefenseAgainstMate(Board after, int attackerMovesLeft) {
        List<Move> replies = after.legalMoves();
        if (replies.isEmpty()) {
            return Optional.empty();
        }
        if (replies.size() == 1) {
            return Optional.of(replies.getFirst());
        }

        long deadline = System.currentTimeMillis() + 600L;
        Move bestEscape = null;
        int bestEscapeScore = -INF;
        Move bestDelay = replies.getFirst();
        int bestDelayRank = -INF;

        for (Move reply : orderedMoves(after, replies)) {
            Board next = after.applyMove(reply);
            int staticEval = -MATERIAL_PST.evaluate(next);
            boolean escapes;
            try {
                escapes = attackerMovesLeft <= 0 || !canAttackerForceMate(next, attackerMovesLeft, deadline);
            } catch (AbortedException e) {
                escapes = true;
            }
            if (escapes) {
                if (staticEval > bestEscapeScore) {
                    bestEscapeScore = staticEval;
                    bestEscape = reply;
                }
            } else {
                int rank = (hasImmediateMate(next) ? -10_000 : 0) + staticEval;
                if (rank > bestDelayRank) {
                    bestDelayRank = rank;
                    bestDelay = reply;
                }
            }
        }
        return Optional.of(bestEscape != null ? bestEscape : bestDelay);
    }

    // ---- 내부 탐색 구현 ----

    private Move searchRoot(
            Board board,
            List<Move> moves,
            int depth,
            Evaluator evaluator,
            long deadline,
            BooleanSupplier cancelled
    ) {
        int alpha = -INF;
        Move best = null;
        for (Move move : moves) {
            checkAbort(deadline, cancelled);
            int score = -negamax(board.applyMove(move), depth - 1, -INF, -alpha, 1, evaluator, deadline, cancelled);
            if (score > alpha) {
                alpha = score;
                best = move;
            }
        }
        return best;
    }

    private int negamax(
            Board board,
            int depth,
            int alpha,
            int beta,
            int ply,
            Evaluator evaluator,
            long deadline,
            BooleanSupplier cancelled
    ) {
        checkAbort(deadline, cancelled);
        if (board.isInsufficientMaterial()) {
            return 0;
        }
        List<Move> legal = board.legalMoves();
        if (legal.isEmpty()) {
            return board.isInCheck() ? -(MATE_SCORE - ply) : 0;
        }
        if (depth <= 0) {
            return evaluator.evaluate(board);
        }

        int best = -INF;
        for (Move move : orderedMoves(board, legal)) {
            int score = -negamax(board.applyMove(move), depth - 1, -beta, -alpha, ply + 1, evaluator, deadline, cancelled);
            if (score > best) best = score;
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }
        return best;
    }

    private boolean canAttackerForceMate(Board board, int movesLeft, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
        if (movesLeft <= 1) {
            return hasImmediateMate(board);
        }
        for (Move m : orderChecksFirst(board, board.legalMoves())) {
            Board after = board.applyMove(m);
            if (after.isCheckmate() || isForcedMateForOpponentToMove(after, movesLeft - 1, deadline)) {
                return true;
            }
        }
        return false;
    }

    private boolean isForcedMateForOpponentToMove(Board defenderBoard, int attackerMovesLeft, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
        List<Move> replies = defenderBoard.legalMoves();
        if (replies.isEmpty()) {
            return defenderBoard.isInCheck();
        }
        for (Move reply : replies) {
            if (!canAttackerForceMate(defenderBoard.applyMove(reply), attackerMovesLeft, deadline)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasImmediateMate(Board board) {
        for (Move m : board.legalMoves()) {
            if (board.applyMove(m).isCheckmate()) {
                return true;
            }
        }
        return false;
    }

    private static List<Move> orderChecksFirst(Board board, List<Move> legal) {
        List<Move> checks = new ArrayList<>();
        List<Move> others = new ArrayList<>();
        for (Move m : legal) {
            if (board.applyMove(m).isInCheck()) {
                checks.add(m);
            } else {
                others.add(m);
            }
        }
        checks.addAll(orderedMoves(board, others));
        return checks;
    }

    private static void checkAbort(long deadline, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
    }

    static List<Move> orderedMoves(Board board, List<Move> legal) {
        List<Move> list = new ArrayList<>(legal);
        list.sort(Comparator.comparingInt((Move m) -> movePriority(board, m)).reversed());
        return list;
    }

    static int movePriority(Board board, Move m) {
        int score = 0;
        Piece captured = board.capturedPiece(m);
        if (!captured.isEmpty()) {
            score += 10 * captured.type().value() - board.pieceAt(m.from()).type().value();
        }
        if (m.promotion() != null) {
            score += m.promotion().value();
        }
        return score;
    }

    private static int evaluateBoard(Board board, boolean usePst) {
        int whiteScore = 0;
        for (int sq = 0; sq < 64; sq++) {
            Piece p = board.pieceAt(sq);
            if (p.isEmpty()) {
                continue;
            }
            int value = p.type().value();
            if (usePst) {
                value += pstBonus(p.type(), sq, p.side());
            }
            whiteScore += (p.side() == Side.WHITE) ? value : -value;
        }
        return board.turn() == Side.WHITE ? whiteScore : -whiteScore;
    }

    private static int pstBonus(Type type, int sq, Side side) {
        int f = Board.file(sq);
        int r = Board.rank(sq);
        int idx = (side == Side.WHITE ? (7 - r) : r) * 8 + f;
        return switch (type) {
            case PAWN -> PAWN_PST[idx];
            case KNIGHT -> KNIGHT_PST[idx];
            case BISHOP -> BISHOP_PST[idx];
            case ROOK -> ROOK_PST[idx];
            case QUEEN -> QUEEN_PST[idx];
            case KING -> KING_PST[idx];
        };
    }
}
