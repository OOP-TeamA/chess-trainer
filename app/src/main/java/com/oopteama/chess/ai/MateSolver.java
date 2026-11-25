package com.oopteama.chess.ai;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "N수 안에 체크메이트" 퍼즐을 위한 계산입니다.
 * 공격하는 쪽(퍼즐을 푸는 사람)이 남은 수 안에, 상대가 어떻게 막아도 메이트할 수 있는지 찾습니다.
 * 계산이 너무 오래 걸리면 중간에 멈추고 "메이트를 찾지 못함"으로 봅니다.
 */
public final class MateSolver {

    private static final long TIME_LIMIT_MS = 1500;

    private long deadline;

    /** maxMoves 수 안에 강제 메이트하는 첫 수. 없으면 빈 값입니다. */
    public Optional<Move> findMateIn(Board board, int maxMoves) {
        if (maxMoves <= 0 || board.isSandbox()) {
            return Optional.empty();
        }
        startClock();
        List<Move> candidates = checksFirst(board, Rules.legalMoves(board));
        for (int limit = 1; limit <= maxMoves; limit++) { // 짧은 메이트부터 찾습니다.
            for (Move move : candidates) {
                if (timeUp()) {
                    return Optional.empty();
                }
                if (mateFollows(board.apply(move), limit)) {
                    return Optional.of(move);
                }
            }
        }
        return Optional.empty();
    }

    /** 공격 측이 수를 둔 뒤(after, 방어 측 차례), 남은 attackerMovesLeft 수 안에 강제 메이트가 되는지. */
    public boolean isForcedMate(Board after, int attackerMovesLeft) {
        if (Rules.isCheckmate(after)) {
            return true;
        }
        if (attackerMovesLeft <= 0 || after.isSandbox()) {
            return false;
        }
        startClock();
        return defenderIsLost(after, attackerMovesLeft);
    }

    /**
     * 방어 측(after의 차례)이 둘 최선의 응수입니다.
     * 메이트를 피할 수 있는 수가 있으면 그중 기물 점수가 가장 좋은 수를, 없으면 가장 오래 버티는 수를 고릅니다.
     */
    public Optional<Move> bestDefense(Board after, int attackerMovesLeft) {
        List<Move> replies = Rules.legalMoves(after);
        if (replies.isEmpty()) {
            return Optional.empty();
        }
        if (replies.size() == 1) {
            return Optional.of(replies.getFirst());
        }
        startClock();
        Side defender = after.turn();
        Move bestEscape = null;
        int bestEscapeScore = Integer.MIN_VALUE;
        Move bestDelay = replies.getFirst();
        int bestDelayScore = Integer.MIN_VALUE;
        for (Move reply : replies) {
            Board next = after.apply(reply);
            int score = Evaluator.evaluate(next) * (defender == Side.WHITE ? 1 : -1);
            boolean escapes = attackerMovesLeft <= 0 || !attackerCanMate(next, attackerMovesLeft);
            if (escapes) {
                if (score > bestEscapeScore) {
                    bestEscapeScore = score;
                    bestEscape = reply;
                }
            } else {
                int delayScore = score - (attackerCanMate(next, 1) ? 10_000 : 0); // 바로 메이트당하는 수는 피함
                if (delayScore > bestDelayScore) {
                    bestDelayScore = delayScore;
                    bestDelay = reply;
                }
            }
        }
        return Optional.of(bestEscape != null ? bestEscape : bestDelay);
    }

    /** 공격 측이 둔 결과(afterAttack)가 limit 수 안의 메이트로 이어지는지. limit이 1이면 그 수 자체가 메이트여야 합니다. */
    private boolean mateFollows(Board afterAttack, int limit) {
        if (Rules.isCheckmate(afterAttack)) {
            return true;
        }
        return limit > 1 && defenderIsLost(afterAttack, limit - 1);
    }

    /** 방어 측 차례(board)에서, 어떤 응수를 해도 공격 측이 movesLeft 수 안에 메이트할 수 있는지. */
    private boolean defenderIsLost(Board board, int movesLeft) {
        List<Move> replies = Rules.legalMoves(board);
        if (replies.isEmpty()) {
            return Rules.isInCheck(board); // 체크메이트면 진 것, 스테일메이트면 아님
        }
        for (Move reply : replies) {
            if (timeUp() || !attackerCanMate(board.apply(reply), movesLeft)) {
                return false;
            }
        }
        return true;
    }

    /** 공격 측 차례(board)에서 movesLeft 수 안에 강제 메이트할 수 있는지. */
    private boolean attackerCanMate(Board board, int movesLeft) {
        for (Move move : checksFirst(board, Rules.legalMoves(board))) {
            if (timeUp()) {
                return false;
            }
            if (mateFollows(board.apply(move), movesLeft)) {
                return true;
            }
        }
        return false;
    }

    /** 체크를 거는 수를 먼저 보도록 정렬합니다 (메이트는 대부분 체크로 시작). */
    private static List<Move> checksFirst(Board board, List<Move> moves) {
        List<Move> checks = new ArrayList<>();
        List<Move> others = new ArrayList<>();
        for (Move move : moves) {
            if (Rules.isInCheck(board.apply(move))) {
                checks.add(move);
            } else {
                others.add(move);
            }
        }
        checks.addAll(others);
        return checks;
    }

    private void startClock() {
        deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
    }

    private boolean timeUp() {
        return System.currentTimeMillis() > deadline;
    }
}
