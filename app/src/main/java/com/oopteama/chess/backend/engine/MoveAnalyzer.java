package com.oopteama.chess.backend.engine;

import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.engine.Piece.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 후보 수의 전술적 영향(공격·포크·체크·수비선·방어 이탈·상대 예상 최선 응수·유불리 판정)을 분석합니다.
 * 책임(SRP): 대국 탐색(ChessAI)과 분리되어 호버 미리보기 및 수 해설 평가만 담당합니다.
 */
public final class MoveAnalyzer {

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

    public record Evaluation(
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

    /**
     * 현재 국면에서 최선의 수를 두었을 때의 기준 점수(센티폰)를 계산합니다.
     */
    public int baselineScore(Board board) {
        if (board.isSandbox()) {
            return 0;
        }
        List<Move> legal = ChessAI.orderedMoves(board, board.legalMoves());
        if (legal.isEmpty()) {
            return board.isInCheck() ? -ChessAI.MATE_SCORE : 0;
        }
        long deadline = System.currentTimeMillis() + 140L;
        int alpha = -ChessAI.INF;
        for (Move m : legal) {
            try {
                int score = -negamaxPreview(board.applyMove(m), 1, -ChessAI.INF, -alpha, 1, deadline);
                if (score > alpha) {
                    alpha = score;
                }
            } catch (ChessAI.AbortedException e) {
                break;
            }
        }
        return alpha == -ChessAI.INF ? ChessAI.MATERIAL_PST.evaluate(board) : alpha;
    }

    /**
     * 후보 수 하나를 시뮬레이션해 상대 최선 응수, 반격/방어 이탈, 공격·체크·포크 및 수의 유불리를 평가합니다.
     */
    public Evaluation evaluate(Board before, Move move, int baselineBestScore) {
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

        if (after.isSandbox()) {
            Verdict v = checkmate ? Verdict.MATE : ((check || !attackedEnemies.isEmpty()) ? Verdict.GOOD : Verdict.NEUTRAL);
            String reason = checkmate ? "체크메이트!"
                    : (check ? "상대 킹에게 체크"
                    : (attackedEnemies.size() >= 2 ? "포크! 상대 기물 " + attackedEnemies.size() + "개 동시 위협"
                    : (!attackedEnemies.isEmpty() ? "도착 칸에서 상대 기물 공격" : "이동 가능 칸")));
            return new Evaluation(move, after, v, 0, 0, check, checkmate,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", reason);
        }

        if (checkmate) {
            return new Evaluation(move, after, Verdict.MATE, ChessAI.MATE_SCORE, 0, true, true,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", "체크메이트! 즉시 승리합니다");
        }
        if (enemyReplies.isEmpty()) {
            Verdict v = baselineBestScore > 200 ? Verdict.BLUNDER : Verdict.NEUTRAL;
            return new Evaluation(move, after, v, 0, Math.max(0, baselineBestScore), false, false,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", "스테일메이트 — 무승부로 끝납니다");
        }
        if (after.isInsufficientMaterial()) {
            Verdict v = baselineBestScore > 200 ? Verdict.RISKY : Verdict.NEUTRAL;
            return new Evaluation(move, after, v, 0, Math.max(0, baselineBestScore), false, false,
                    attackedEnemies, checkingAttackers, defendersOfDest, attackersOfDest,
                    newlyExposed, null, "", "기물 부족 — 무승부가 됩니다");
        }

        long deadline = System.currentTimeMillis() + 90L;
        List<Move> orderedReplies = ChessAI.orderedMoves(after, enemyReplies);
        Move bestReply = orderedReplies.getFirst();
        int bestOppScore = -ChessAI.INF;
        for (Move reply : orderedReplies) {
            try {
                int oppScore = -negamaxPreview(after.applyMove(reply), 0, -ChessAI.INF, -bestOppScore, 2, deadline);
                if (oppScore > bestOppScore) {
                    bestOppScore = oppScore;
                    bestReply = reply;
                }
            } catch (ChessAI.AbortedException e) {
                break;
            }
        }

        int moveScore = bestOppScore == -ChessAI.INF ? -ChessAI.MATERIAL_PST.evaluate(after) : -bestOppScore;
        int loss = Math.max(0, baselineBestScore - moveScore);
        String replySan = formatReplySan(after, bestReply, enemyReplies);

        boolean opponentMatesBack = moveScore <= -(ChessAI.MATE_SCORE - 20);
        boolean forcesMateSoon = moveScore >= ChessAI.MATE_SCORE - 20;
        boolean missedForcedMate = baselineBestScore >= ChessAI.MATE_SCORE - 20 && !forcesMateSoon;
        boolean replyCapturesMoved = bestReply.to() == move.to();
        boolean replyCapturesExposed = newlyExposed.contains(bestReply.to());
        boolean destHanging = !attackersOfDest.isEmpty() && defendersOfDest.isEmpty();

        Verdict verdict = forcesMateSoon ? Verdict.GOOD : classifyVerdict(
                loss, moveScore, ChessAI.MATERIAL_PST.evaluate(before),
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

        return new Evaluation(
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
            return check
                    ? "체크이지만 메이트 놓침 · 상대가 " + replySan + "로 탈출"
                    : "이 국면에 더 강한 메이트 수가 있습니다";
        }
        if (verdict == Verdict.BLUNDER || verdict == Verdict.RISKY) {
            if (replyCapturesMoved && destHanging) {
                Piece attacker = after.pieceAt(bestReply.from());
                return "도착 칸 무방비! 상대 " + attacker.type().label() + "에게 공짜로 잡힘";
            }
            if (replyCapturesMoved && !attackersOfDest.isEmpty()) {
                Piece attacker = after.pieceAt(bestReply.from());
                if (attacker.type().value() < movedPiece.type().value()) {
                    return "손해 교환! 더 싼 상대 " + attacker.type().label() + "에게 잡힘";
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
            return loss >= 200
                    ? "유리한 흐름 상실 · 상대 " + replySan + " 반격 주의"
                    : "조금 아쉬운 수 · 상대 " + replySan + " 대응 예상";
        }

        if (check && attackedCount >= 2) return "강력한 체크 + 포크! 상대 " + replySan + " 강제";
        if (attackedCount >= 2) return "포크! 상대 기물 " + attackedCount + "개 동시 공격";
        if (check) return "체크! 상대 " + replySan + " 대응 강제";
        if (!captured.isEmpty()) {
            return (!attackersOfDest.isEmpty() && !defendersOfDest.isEmpty())
                    ? captured.type().label() + " 포획 (아군 엄호 · 교환 가능)"
                    : "상대 " + captured.type().label() + " 안전하게 포획";
        }
        if (attackedCount == 1) return "상대 기물 압박 · 주도권 확보";
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
            if (sq == destSq) continue;
            Piece ally = after.pieceAt(sq);
            if (!ally.is(mover) || ally.type() == Type.KING) continue;

            int minAfterAtt = Integer.MAX_VALUE;
            for (Move r : enemyReplies) {
                if (r.to() == sq) {
                    minAfterAtt = Math.min(minAfterAtt, after.pieceAt(r.from()).type().value());
                }
            }
            if (minAfterAtt == Integer.MAX_VALUE) continue;
            if (!after.attackersOf(sq, mover).isEmpty() && minAfterAtt >= ally.type().value()) continue;

            List<Integer> beforeAtt = before.attackersOf(sq, enemy);
            int minBeforeAtt = beforeAtt.stream()
                    .mapToInt(s -> before.pieceAt(s).type().value())
                    .min()
                    .orElse(Integer.MAX_VALUE);
            boolean endangeredBefore = !beforeAtt.isEmpty()
                    && (before.attackersOf(sq, mover).isEmpty() || minBeforeAtt < ally.type().value());
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
            throw new ChessAI.AbortedException();
        }
        if (board.isInsufficientMaterial()) {
            return 0;
        }
        List<Move> legal = board.legalMoves();
        if (legal.isEmpty()) {
            return board.isInCheck() ? -(ChessAI.MATE_SCORE - ply) : 0;
        }
        if (depth <= 0) {
            return quiescence(board, alpha, beta, 3, ply, deadline);
        }
        int best = -ChessAI.INF;
        for (Move move : ChessAI.orderedMoves(board, legal)) {
            int score = -negamaxPreview(board.applyMove(move), depth - 1, -beta, -alpha, ply + 1, deadline);
            if (score > best) best = score;
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }
        return best;
    }

    private int quiescence(Board board, int alpha, int beta, int qDepth, int ply, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            throw new ChessAI.AbortedException();
        }
        if (board.isInCheck()) {
            List<Move> evasions = board.legalMoves();
            if (evasions.isEmpty()) {
                return -(ChessAI.MATE_SCORE - ply);
            }
            if (qDepth <= 0) {
                return ChessAI.MATERIAL_PST.evaluate(board);
            }
            for (Move m : ChessAI.orderedMoves(board, evasions)) {
                int score = -quiescence(board.applyMove(m), -beta, -alpha, qDepth - 1, ply + 1, deadline);
                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            }
            return alpha;
        }
        int standPat = ChessAI.MATERIAL_PST.evaluate(board);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;
        if (qDepth <= 0) return alpha;

        List<Move> tactical = new ArrayList<>();
        for (Move m : board.legalMoves()) {
            if (!board.capturedPiece(m).isEmpty() || m.promotion() != null) {
                tactical.add(m);
            }
        }
        tactical.sort(Comparator.comparingInt((Move m) -> ChessAI.movePriority(board, m)).reversed());
        for (Move m : tactical) {
            int score = -quiescence(board.applyMove(m), -beta, -alpha, qDepth - 1, ply + 1, deadline);
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }
}
