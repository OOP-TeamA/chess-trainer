package com.oopteama.chess.engine;

import com.oopteama.chess.engine.Piece.Side;
import com.oopteama.chess.engine.Piece.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * 자체 구현 알파-베타 가지치기 체스 AI.
 * 난이도별 탐색 깊이와 평가 전략(Evaluator)을 조합해 수를 선택합니다.
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

    @FunctionalInterface
    public interface Evaluator {
        int evaluate(Board board);
    }

    public enum Verdict {
        MATE("메이트"),
        GOOD("좋은 수"),
        NEUTRAL("무난"),
        RISKY("주의"),
        BLUNDER("위험");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * 후보 수를 두었을 때의 이후 전개(상대 예상 최선 응수, 반격/방어 이탈, 공격·포크·체크, 판정 및 이유) 요약입니다.
     */
    public record MoveEvaluation(
            Move move,
            Board after,
            Verdict verdict,
            int scoreCp,
            int lossCp,
            boolean check,
            boolean checkmate,
            List<Integer> attackedEnemies,
            List<Integer> checkingAttackers,
            List<Integer> defendersOfDest,
            List<Integer> attackersOfDest,
            List<Integer> newlyExposedAllies,
            Move opponentReply,
            String opponentReplySan,
            String reason
    ) {
        public boolean isFork() {
            return attackedEnemies.size() >= 2;
        }
    }

    private static final int MATE_SCORE = 100_000;
    private static final int INF = 1_000_000;

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

    private static final class AbortedException extends RuntimeException {
    }

    private final Random random;

    public ChessAI() {
        this(new Random());
    }

    public ChessAI(Random random) {
        this.random = random;
    }

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

        // 입문 난이도: 1-ply 상위 후보 중 무작위 선택
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

        // 반복 심화(Iterative Deepening): 제한 시간 내 직전 깊이의 최선 수를 보장
        for (int depth = 1; depth <= difficulty.depth; depth++) {
            try {
                Move depthBest = searchRoot(board, legal, depth, evaluator, deadline, cancelled);
                if (depthBest != null) {
                    bestMove = depthBest;
                    // 직전 최선 수를 맨 앞으로 당겨 다음 깊이 가지치기 효율 향상
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

    // ---- 자유 메이트 퍼즐용 정확한 N수 메이트 탐색 & 최선 방어 응수 ----

    /**
     * 현재 차례 진영이 maxMoves 수 이내에 강제 체크메이트할 수 있는 첫 수를 찾습니다.
     */
    public Optional<Move> findMateIn(Board board, int maxMoves) {
        if (maxMoves <= 0 || board.isSandbox()) {
            return Optional.empty();
        }
        long deadline = System.currentTimeMillis() + 800L;
        List<Move> candidates = orderChecksFirst(board, board.legalMoves());
        // 더 빠른 메이트(1수 -> 2수 -> ... -> maxMoves수)를 우선 탐색
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

    /**
     * 공격자가 수를 둔 직후(after, 수비자 차례)에 공격자가 남은 attackerMovesLeft 수 안에
     * 모든 수비 응수를 상대로 강제 메이트할 수 있는지 판별합니다.
     */
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

    /**
     * 수비자 차례(after)에서 메이트를 피하거나 가장 오래 버티는 최선 방어 응수를 고릅니다.
     */
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
            int staticEval = -MATERIAL_PST.evaluate(next); // 수비자 시점 정적 점수
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
                // 어차피 메이트를 당한다면 1수 즉시 메이트를 피하고 가장 오래 버티는 응수 선택
                boolean matedInOne = hasImmediateMate(next);
                int rank = (matedInOne ? -10_000 : 0) + staticEval;
                if (rank > bestDelayRank) {
                    bestDelayRank = rank;
                    bestDelay = reply;
                }
            }
        }
        return Optional.of(bestEscape != null ? bestEscape : bestDelay);
    }

    private boolean canAttackerForceMate(Board board, int movesLeft, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
        if (movesLeft <= 1) {
            return hasImmediateMate(board);
        }
        List<Move> legal = orderChecksFirst(board, board.legalMoves());
        for (Move m : legal) {
            Board after = board.applyMove(m);
            if (after.isCheckmate()) {
                return true;
            }
            if (isForcedMateForOpponentToMove(after, movesLeft - 1, deadline)) {
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
            return defenderBoard.isInCheck(); // 스테일메이트면 수비자 탈출(false)
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

    /**
     * 현재 국면에서 최선의 수를 두었을 때의 기준 점수(센티폰)를 빠르게 계산합니다(호버 평가 비교 기준).
     */
    public int baselineScore(Board board) {
        if (board.isSandbox()) {
            return 0;
        }
        List<Move> legal = orderedMoves(board, board.legalMoves());
        if (legal.isEmpty()) {
            return board.isInCheck() ? -MATE_SCORE : 0;
        }
        long deadline = System.currentTimeMillis() + 140L;
        int alpha = -INF;
        for (Move m : legal) {
            try {
                int score = -negamaxPreview(board.applyMove(m), 1, -INF, -alpha, 1, deadline);
                if (score > alpha) {
                    alpha = score;
                }
            } catch (AbortedException e) {
                break;
            }
        }
        return alpha == -INF ? MATERIAL_PST.evaluate(board) : alpha;
    }

    /**
     * 후보 수 하나를 시뮬레이션해 상대의 예상 최선 응수, 반격 위험, 방어 이탈, 공격·체크·포크 및 수의 유불리를 평가합니다.
     */
    public MoveEvaluation evaluateMove(Board before, Move move, int baselineBestScore) {
        Board after = before.applyMove(move);
        Side mover = before.turn();
        Piece movedPiece = after.pieceAt(move.to());
        Piece captured = before.capturedPiece(move);

        List<Integer> attackedEnemies = after.attackedEnemySquaresFrom(move.to());
        boolean check = after.isInCheck();
        List<Move> enemyReplies = after.legalMoves();
        boolean checkmate = check && enemyReplies.isEmpty();

        List<Integer> checkingAttackers = List.of();
        if (check) {
            int enemyKing = after.findKing(after.turn());
            if (enemyKing >= 0) {
                checkingAttackers = after.attackersOf(enemyKing, mover);
            }
        }

        List<Integer> defendersOfDest = after.attackersOf(move.to(), mover);
        List<Integer> attackersOfDest = enemyReplies.stream()
                .filter(r -> r.to() == move.to())
                .map(Move::from)
                .distinct()
                .toList();
        List<Integer> newlyExposed = findNewlyExposedAllies(before, after, move.to(), mover, enemyReplies);

        // 샌드박스(튜토리얼) 국면은 상대 탐색 없이 공격·체크만 즉시 표시
        if (after.isSandbox()) {
            Verdict v = checkmate ? Verdict.MATE : ((check || !attackedEnemies.isEmpty()) ? Verdict.GOOD : Verdict.NEUTRAL);
            String reason = checkmate ? "체크메이트!"
                    : (check ? "상대 킹에게 체크"
                    : (attackedEnemies.size() >= 2 ? "포크! 상대 기물 " + attackedEnemies.size() + "개 동시 위협"
                    : (!attackedEnemies.isEmpty() ? "도착 칸에서 상대 기물 공격" : "이동 가능 칸")));
            return new MoveEvaluation(move, after, v, 0, 0, check, checkmate,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", reason);
        }

        // 즉시 종료 국면 (메이트 / 스테일메이트 / 기물 부족)
        if (checkmate) {
            return new MoveEvaluation(move, after, Verdict.MATE, MATE_SCORE, 0, true, true,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", "체크메이트! 즉시 승리합니다");
        }
        if (enemyReplies.isEmpty()) {
            Verdict v = baselineBestScore > 200 ? Verdict.BLUNDER : Verdict.NEUTRAL;
            return new MoveEvaluation(move, after, v, 0, Math.max(0, baselineBestScore), false, false,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", "스테일메이트 — 무승부로 끝납니다");
        }
        if (after.isInsufficientMaterial()) {
            Verdict v = baselineBestScore > 200 ? Verdict.RISKY : Verdict.NEUTRAL;
            return new MoveEvaluation(move, after, v, 0, Math.max(0, baselineBestScore), false, false,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", "기물 부족 — 무승부가 됩니다");
        }

        // 상대의 최선 응수 탐색 (1-ply + 포획 정지 탐색으로 즉각 반격·되잡기·메이트 위협 정확 판별)
        long deadline = System.currentTimeMillis() + 90L;
        List<Move> orderedReplies = orderedMoves(after, enemyReplies);
        Move bestReply = orderedReplies.getFirst();
        int bestOppScore = -INF;
        for (Move reply : orderedReplies) {
            try {
                int oppScore = -negamaxPreview(after.applyMove(reply), 0, -INF, -bestOppScore, 2, deadline);
                if (oppScore > bestOppScore) {
                    bestOppScore = oppScore;
                    bestReply = reply;
                }
            } catch (AbortedException e) {
                break;
            }
        }

        int moveScore = bestOppScore == -INF ? -MATERIAL_PST.evaluate(after) : -bestOppScore;
        int loss = Math.max(0, baselineBestScore - moveScore);
        String replySan = formatReplySan(after, bestReply, enemyReplies);

        boolean opponentMatesBack = moveScore <= -(MATE_SCORE - 20);
        boolean forcesMateSoon = !checkmate && moveScore >= MATE_SCORE - 20;
        boolean missedForcedMate = baselineBestScore >= MATE_SCORE - 20 && moveScore < MATE_SCORE - 20;
        boolean replyCapturesMoved = bestReply.to() == move.to();
        boolean replyCapturesExposed = newlyExposed.contains(bestReply.to());
        boolean destHanging = !attackersOfDest.isEmpty() && defendersOfDest.isEmpty();

        Verdict verdict = forcesMateSoon ? Verdict.GOOD : classifyVerdict(
                loss, moveScore, MATERIAL_PST.evaluate(before),
                opponentMatesBack, missedForcedMate, destHanging && replyCapturesMoved,
                check, attackedEnemies.size() >= 2, !captured.isEmpty()
        );
        String reason = forcesMateSoon
                ? "강력한 수! 상대 " + replySan + " 후 체크메이트로 이어집니다"
                : buildReason(
                        verdict, opponentMatesBack, missedForcedMate, check, attackedEnemies.size(),
                        destHanging, attackersOfDest, defendersOfDest, movedPiece,
                        replyCapturesMoved, replyCapturesExposed, newlyExposed, bestReply, replySan,
                        captured, after, loss
                );

        return new MoveEvaluation(
                move, after, verdict, moveScore, loss, check, false,
                attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                newlyExposed, bestReply, replySan, reason
        );
    }

    private static Verdict classifyVerdict(
            int loss,
            int moveScore,
            int staticBefore,
            boolean opponentMatesBack,
            boolean missedForcedMate,
            boolean hangsMovedPiece,
            boolean check,
            boolean fork,
            boolean capture
    ) {
        if (opponentMatesBack) {
            return Verdict.BLUNDER;
        }
        if (missedForcedMate) {
            return hangsMovedPiece ? Verdict.BLUNDER : Verdict.RISKY;
        }
        if (loss >= 240 || (hangsMovedPiece && loss >= 150)) {
            return Verdict.BLUNDER;
        }
        if (loss >= 95) {
            return Verdict.RISKY;
        }
        if (loss <= 45 && (moveScore - staticBefore >= 80 || fork || check || capture)) {
            return Verdict.GOOD;
        }
        return loss <= 35 ? Verdict.GOOD : Verdict.NEUTRAL;
    }

    private static String buildReason(
            Verdict verdict,
            boolean opponentMatesBack,
            boolean missedForcedMate,
            boolean check,
            int attackedCount,
            boolean destHanging,
            List<Integer> attackersOfDest,
            List<Integer> defendersOfDest,
            Piece movedPiece,
            boolean replyCapturesMoved,
            boolean replyCapturesExposed,
            List<Integer> newlyExposed,
            Move bestReply,
            String replySan,
            Piece captured,
            Board after,
            int loss
    ) {
        if (opponentMatesBack) {
            return "치명적 실수! 상대의 " + replySan + " 체크메이트 허용";
        }
        if (missedForcedMate) {
            if (check) {
                return "체크이지만 메이트 놓침 · 상대가 " + replySan + "로 탈출";
            }
            return "이 국면에 더 강한 메이트 수가 있습니다";
        }
        if (verdict == Verdict.BLUNDER || verdict == Verdict.RISKY) {
            if (replyCapturesMoved && destHanging) {
                Piece enemyAttacker = after.pieceAt(bestReply.from());
                return "도착 칸 무방비! 상대 " + enemyAttacker.type().label() + "에게 공짜로 잡힘";
            }
            if (replyCapturesMoved && !attackersOfDest.isEmpty()) {
                Piece enemyAttacker = after.pieceAt(bestReply.from());
                if (enemyAttacker.type().value() < movedPiece.type().value()) {
                    return "손해 교환! 더 싼 상대 " + enemyAttacker.type().label() + "에게 잡힘";
                }
                return "상대 반격(" + replySan + ")으로 기물 손실";
            }
            if (replyCapturesExposed) {
                Piece exposed = after.pieceAt(bestReply.to());
                return "방어 이탈! " + Board.formatSquare(bestReply.to()) + " 아군 " + exposed.type().label() + " 피격";
            }
            if (!newlyExposed.isEmpty()) {
                int sq = newlyExposed.getFirst();
                return "주의: " + Board.formatSquare(sq) + " 아군 " + after.pieceAt(sq).type().label() + " 방어 약화";
            }
            if (loss >= 200) {
                return "유리한 흐름 상실 · 상대 " + replySan + " 반격 주의";
            }
            return "조금 아쉬운 수 · 상대 " + replySan + " 대응 예상";
        }

        if (check && attackedCount >= 2) {
            return "강력한 체크 + 포크! 상대 " + replySan + " 강제";
        }
        if (attackedCount >= 2) {
            return "포크! 상대 기물 " + attackedCount + "개 동시 공격";
        }
        if (check) {
            return "체크! 상대 " + replySan + " 대응 강제";
        }
        if (!captured.isEmpty()) {
            if (!attackersOfDest.isEmpty() && !defendersOfDest.isEmpty()) {
                return captured.type().label() + " 포획 (아군 엄호 · 교환 가능)";
            }
            return "상대 " + captured.type().label() + " 안전하게 포획";
        }
        if (attackedCount == 1) {
            return "상대 기물 압박 · 주도권 확보";
        }
        if (!attackersOfDest.isEmpty() && !defendersOfDest.isEmpty()) {
            return "아군 수비(" + defendersOfDest.size() + ")로 교환 방어 가능";
        }
        return "안정적인 전개 · 균형 유지";
    }

    private static List<Integer> findNewlyExposedAllies(
            Board before,
            Board after,
            int destSq,
            Side mover,
            List<Move> enemyReplies
    ) {
        List<Integer> exposed = new ArrayList<>();
        Side enemy = mover.opposite();
        for (int sq = 0; sq < 64; sq++) {
            if (sq == destSq) {
                continue;
            }
            Piece ally = after.pieceAt(sq);
            if (!ally.is(mover) || ally.type() == Type.KING) {
                continue;
            }
            int minAfterAttacker = Integer.MAX_VALUE;
            for (Move r : enemyReplies) {
                if (r.to() == sq) {
                    minAfterAttacker = Math.min(minAfterAttacker, after.pieceAt(r.from()).type().value());
                }
            }
            if (minAfterAttacker == Integer.MAX_VALUE) {
                continue;
            }
            boolean endangeredAfter = after.attackersOf(sq, mover).isEmpty() || minAfterAttacker < ally.type().value();
            if (!endangeredAfter) {
                continue;
            }
            List<Integer> beforeAtt = before.attackersOf(sq, enemy);
            int minBeforeAttacker = beforeAtt.stream()
                    .mapToInt(s -> before.pieceAt(s).type().value())
                    .min()
                    .orElse(Integer.MAX_VALUE);
            boolean endangeredBefore = !beforeAtt.isEmpty()
                    && (before.attackersOf(sq, mover).isEmpty() || minBeforeAttacker < ally.type().value());
            if (!endangeredBefore) {
                exposed.add(sq);
            }
        }
        return List.copyOf(exposed);
    }

    private static String formatReplySan(Board after, Move reply, List<Move> enemyReplies) {
        Move.Kind kind = after.classifyMove(reply);
        Board next = after.applyMove(reply);
        boolean chk = next.isInCheck();
        boolean mate = chk && next.legalMoves().isEmpty();
        return ChessGame.formatSan(after, reply, kind, enemyReplies, chk, mate);
    }

    private int negamaxPreview(Board board, int depth, int alpha, int beta, int ply, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
        if (board.isInsufficientMaterial()) {
            return 0;
        }
        List<Move> legal = board.legalMoves();
        if (legal.isEmpty()) {
            return board.isInCheck() ? -(MATE_SCORE - ply) : 0;
        }
        if (depth <= 0) {
            return quiescence(board, alpha, beta, 3, ply, deadline);
        }
        int best = -INF;
        for (Move move : orderedMoves(board, legal)) {
            int score = -negamaxPreview(board.applyMove(move), depth - 1, -beta, -alpha, ply + 1, deadline);
            if (score > best) best = score;
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }
        return best;
    }

    private int quiescence(Board board, int alpha, int beta, int qDepth, int ply, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
        boolean inCheck = board.isInCheck();
        if (inCheck) {
            List<Move> evasions = board.legalMoves();
            if (evasions.isEmpty()) {
                return -(MATE_SCORE - ply);
            }
            if (qDepth <= 0) {
                return MATERIAL_PST.evaluate(board);
            }
            for (Move m : orderedMoves(board, evasions)) {
                int score = -quiescence(board.applyMove(m), -beta, -alpha, qDepth - 1, ply + 1, deadline);
                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            }
            return alpha;
        }
        int standPat = MATERIAL_PST.evaluate(board);
        if (standPat >= beta) {
            return beta;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }
        if (qDepth <= 0) {
            return alpha;
        }
        List<Move> tactical = new ArrayList<>();
        for (Move m : board.legalMoves()) {
            if (!board.capturedPiece(m).isEmpty() || m.promotion() != null) {
                tactical.add(m);
            }
        }
        tactical.sort(Comparator.comparingInt((Move m) -> movePriority(board, m)).reversed());
        for (Move m : tactical) {
            int score = -quiescence(board.applyMove(m), -beta, -alpha, qDepth - 1, ply + 1, deadline);
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    private record ScoredMove(Move move, int score) {
    }

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

        List<Move> ordered = orderedMoves(board, legal);
        int best = -INF;
        for (Move move : ordered) {
            int score = -negamax(board.applyMove(move), depth - 1, -beta, -alpha, ply + 1, evaluator, deadline, cancelled);
            if (score > best) {
                best = score;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                break;
            }
        }
        return best;
    }

    private static void checkAbort(long deadline, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || System.currentTimeMillis() > deadline) {
            throw new AbortedException();
        }
    }

    private static List<Move> orderedMoves(Board board, List<Move> legal) {
        List<Move> list = new ArrayList<>(legal);
        list.sort(Comparator.comparingInt((Move m) -> movePriority(board, m)).reversed());
        return list;
    }

    private static int movePriority(Board board, Move m) {
        int score = 0;
        Piece captured = board.capturedPiece(m);
        if (!captured.isEmpty()) {
            Piece attacker = board.pieceAt(m.from());
            score += 10 * captured.type().value() - attacker.type().value();
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
        int tableRow = side == Side.WHITE ? (7 - r) : r;
        int idx = tableRow * 8 + f;
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
