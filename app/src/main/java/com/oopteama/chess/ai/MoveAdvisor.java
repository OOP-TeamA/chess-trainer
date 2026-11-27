package com.oopteama.chess.ai;

import com.oopteama.chess.ai.ComputerPlayer.ScoredMove;
import com.oopteama.chess.ai.MoveInsight.Reason;
import com.oopteama.chess.ai.MoveInsight.Verdict;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Notation;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.Bishop;
import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Knight;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * 수 설명을 만듭니다: 지금 국면에서 둘 수 있는 수마다 왜 좋은지, 왜 나쁜지를 정리합니다.
 * <ol>
 *   <li>판정: 수마다 상대의 최선 응수와 그다음 내 수까지(3수 앞, 중급 AI와 같은 깊이) 미니맥스로 점수를 매기고,
 *       가장 좋은 수와의 점수 차이(손해)로 최선·좋은·아쉬운·나쁜 수를 정합니다.</li>
 *   <li>이유: 잡기·체크·잡힐 위험·노리는 기물처럼 체스판에서 바로 보이는 사실을 간단한 규칙으로 찾습니다.</li>
 * </ol>
 */
public final class MoveAdvisor {

    private static final int REPLY_DEPTH = 2; // 내 수 뒤에 따져 볼 수: 상대 응수 + 내 다음 수
    private static final int MATE_THRESHOLD = ComputerPlayer.MATE_SCORE - 100; // 이보다 크면 체크메이트 점수
    private static final int MAX_REASONS = 4;
    private static final List<Position> CENTER =
            List.of(Position.of("d4"), Position.of("e4"), Position.of("d5"), Position.of("e5"));

    private final ComputerPlayer computer;

    /** 잡기 정리를 켠 ComputerPlayer로 점수를 매깁니다 (AI 대전의 컴퓨터 상대와는 따로 씀). */
    public MoveAdvisor() {
        this(new ComputerPlayer(new Random(), true));
    }

    public MoveAdvisor(ComputerPlayer computer) {
        this.computer = Objects.requireNonNull(computer, "computer");
    }

    /**
     * board에서 둘 수 있는 모든 수의 설명을 만듭니다.
     * 계산이 오래 걸릴 수 있어 화면에서는 백그라운드 스레드로 부릅니다. cancelled가 true가 되면 빈 Map을 돌려줍니다.
     */
    public Map<Move, MoveInsight> analyzeAll(Board board, BooleanSupplier cancelled) {
        int sign = board.turn() == Side.WHITE ? 1 : -1; // 백 기준 점수를 "둘 차례인 쪽" 기준으로 바꾸는 부호
        List<Candidate> candidates = new ArrayList<>();
        int best = Integer.MIN_VALUE;
        for (Move move : Rules.legalMoves(board)) {
            Board after = board.apply(move);
            Optional<ScoredMove> reply = computer.bestMove(after, REPLY_DEPTH, cancelled);
            if (cancelled.getAsBoolean()) {
                return Map.of();
            }
            // 상대가 둘 수 없으면 체크메이트(내 승리)거나 스테일메이트(무승부)입니다.
            int score = reply.map(r -> sign * r.score())
                    .orElse(Rules.isInCheck(after) ? ComputerPlayer.MATE_SCORE : 0);
            candidates.add(new Candidate(move, score, reply.map(ScoredMove::move).orElse(null)));
            best = Math.max(best, score);
        }
        Map<Move, MoveInsight> insights = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            insights.put(candidate.move(), explain(board, candidate, best));
        }
        return insights;
    }

    /** 수, 그 수의 점수(둘 차례인 쪽 기준), 상대의 예상 최선 응수. */
    private record Candidate(Move move, int score, Move reply) {
    }

    /** 점수 차이로 판정을 정하고, 규칙으로 찾은 이유를 중요한 순서대로 모읍니다. */
    private static MoveInsight explain(Board board, Candidate candidate, int best) {
        Move move = candidate.move();
        Board after = board.apply(move);
        int loss = best - candidate.score();
        Verdict verdict = Verdict.fromLoss(loss);

        List<Reason> reasons = new ArrayList<>();
        addResult(reasons, after, candidate.score(), best);
        addCapture(reasons, board, move);
        addDanger(reasons, board, after, move);
        if (!verdict.isGood()) {
            addReply(reasons, after, candidate.reply());
        }
        addRescue(reasons, board, after, move);
        addThreats(reasons, board, after, move);
        addPlan(reasons, board, move);
        if (reasons.isEmpty()) {
            reasons.add(verdict.isGood()
                    ? new Reason(true, "특별한 위험 없이 둘 수 있는 수입니다", null, List.of())
                    : new Reason(false, "더 좋은 수가 있습니다", null, List.of()));
        }
        List<Reason> shown = reasons.subList(0, Math.min(MAX_REASONS, reasons.size()));
        return new MoveInsight(move, Notation.toSan(board, move), verdict, loss, candidate.reply(), shown);
    }

    // ---- 이유를 찾는 규칙 ----

    /** 대국이 끝나는 수, 체크메이트로 이어지는 수, 체크메이트 기회를 놓치는 수. */
    private static void addResult(List<Reason> reasons, Board after, int score, int best) {
        Position enemyKing = after.findKing(after.turn());
        List<Position> king = enemyKing == null ? List.of() : List.of(enemyKing);
        if (Rules.isCheckmate(after)) {
            reasons.add(new Reason(true, "체크메이트! 상대 킹이 피할 곳이 없습니다", null, king));
        } else if (Rules.isStalemate(after)) {
            reasons.add(new Reason(score >= best, "스테일메이트: 상대가 둘 수가 없어 무승부가 됩니다", null, king));
        } else if (score > MATE_THRESHOLD) {
            reasons.add(new Reason(true, "상대가 어떻게 막아도 다음 수에 체크메이트할 수 있습니다", null, king));
        } else if (best > MATE_THRESHOLD) {
            reasons.add(new Reason(false, "체크메이트할 수 있는 기회를 놓칩니다", null, List.of()));
        }
    }

    /** 상대 기물을 잡는 수, 폰 승진. */
    private static void addCapture(List<Reason> reasons, Board board, Move move) {
        Piece captured = Rules.capturedPiece(board, move);
        if (captured != null) {
            Position at = Rules.isEnPassant(board, move) ? new Position(move.to().col(), move.from().row()) : move.to();
            reasons.add(new Reason(true, "상대 " + withJosa(captured.name(), "을", "를") + " 잡습니다 (+"
                    + points(captured.value()) + ")", null, List.of(at)));
        }
        if (move.isPromotion()) {
            Piece promoted = Piece.create(move.promotion(), board.turn());
            reasons.add(new Reason(true, "폰이 " + withJosa(promoted.name(), "으로", "로") + " 승진합니다",
                    null, List.of(move.to())));
        }
    }

    /** 옮긴 기물이 잡혀서 손해를 볼 위험, 그리고 이 수 때문에 지켜 주는 기물이 없어진 다른 아군 기물. */
    private static void addDanger(List<Reason> reasons, Board board, Board after, Move move) {
        Side me = board.turn();
        Piece moved = after.pieceAt(move.to());
        Position attacker = cheapestAttacker(after, move.to(), me.opposite());
        if (attacker != null && !(moved instanceof King)) {
            Piece enemy = after.pieceAt(attacker);
            Piece captured = Rules.capturedPiece(board, move);
            boolean defended = Rules.isAttacked(after, move.to(), me);
            // 이 수로 잡은 것 - 상대가 잡아 갈 내 기물 + (지켜 주는 기물이 있으면) 내가 되잡을 상대 기물
            int net = (captured == null ? 0 : captured.value()) - moved.value() + (defended ? enemy.value() : 0);
            if (net < 0 && defended) {
                reasons.add(new Reason(false, "상대 " + withJosa(enemy.name(), "이", "가") + " 더 비싼 "
                        + withJosa(moved.name(), "을", "를") + " 노립니다 (교환하면 −" + points(-net) + ")",
                        attacker, List.of(move.to())));
            } else if (net < 0) {
                reasons.add(new Reason(false, "상대 " + withJosa(enemy.name(), "이", "가") + " 이 "
                        + withJosa(moved.name(), "을", "를") + " 공짜로 잡을 수 있습니다 (−" + points(-net) + ")",
                        attacker, List.of(move.to())));
            }
        }

        Position exposed = null; // 새로 무방비가 된 아군 기물 중 가장 비싼 것
        for (Position square : after.positionsOf(me)) {
            Piece piece = after.pieceAt(square);
            boolean newlyHanging = !square.equals(move.to()) && !(piece instanceof King)
                    && isHanging(after, square, me) && !isHanging(board, square, me);
            if (newlyHanging && (exposed == null || piece.value() > after.pieceAt(exposed).value())) {
                exposed = square;
            }
        }
        if (exposed != null) {
            reasons.add(new Reason(false, exposed + "의 " + withJosa(after.pieceAt(exposed).name(), "이", "가")
                    + " 지켜 주는 기물 없이 공격받게 됩니다", cheapestAttacker(after, exposed, me.opposite()),
                    List.of(exposed)));
        }
    }

    /** 좋지 않은 수일 때 상대의 예상 최선 응수와 그 결과. 체스판에는 화면이 응수 애니메이션으로 따로 그립니다. */
    private static void addReply(List<Reason> reasons, Board after, Move reply) {
        if (reply == null) {
            return;
        }
        Piece lost = Rules.capturedPiece(after, reply);
        boolean alreadyExplained = reasons.stream()
                .anyMatch(reason -> !reason.good() && reason.targets().contains(reply.to()));
        if (lost != null && alreadyExplained) {
            return; // 이미 "잡힐 수 있습니다"로 적은 칸을 잡는 응수면 같은 말을 되풀이하지 않습니다.
        }
        Board afterReply = after.apply(reply);
        Side me = after.turn().opposite();
        String result;
        if (Rules.isCheckmate(afterReply)) {
            result = "체크메이트를 당합니다";
        } else if (lost != null && Rules.isAttacked(afterReply, reply.to(), me)) {
            result = "내 " + withJosa(lost.name(), "을", "를") + " 잡지만 되잡을 수 있습니다";
        } else if (lost != null) {
            result = "내 " + withJosa(lost.name(), "을", "를") + " 잃습니다 (−" + points(lost.value()) + ")";
        } else if (Rules.isInCheck(afterReply)) {
            result = "체크를 당합니다";
        } else if (reasons.isEmpty()) {
            result = "상대가 더 좋은 자리를 차지합니다"; // 다른 이유가 없을 때만 적습니다.
        } else {
            return;
        }
        reasons.add(new Reason(false, "예상 응수 " + Notation.toSan(after, reply) + ": " + result, null, List.of()));
    }

    /** 공격받던 기물을 피신시키는 수, 지켜 주는 기물이 없던 아군 기물을 지켜 주는 수. */
    private static void addRescue(List<Reason> reasons, Board board, Board after, Move move) {
        Side me = board.turn();
        Piece moved = board.pieceAt(move.from());
        if (!(moved instanceof King) && isThreatened(board, move.from(), me) && !isThreatened(after, move.to(), me)) {
            reasons.add(new Reason(true, "공격받던 " + withJosa(moved.name(), "을", "를") + " 안전한 곳으로 옮깁니다",
                    null, List.of(move.to())));
        }
        Position saved = null; // 이 수로 지켜지게 된 아군 기물 중 가장 비싼 것
        for (Position square : after.positionsOf(me)) {
            Piece piece = after.pieceAt(square);
            boolean nowSafe = !square.equals(move.to()) && !(piece instanceof King)
                    && isHanging(board, square, me) && !isHanging(after, square, me);
            if (nowSafe && (saved == null || piece.value() > after.pieceAt(saved).value())) {
                saved = square;
            }
        }
        if (saved != null) {
            reasons.add(new Reason(true, saved + "의 " + withJosa(after.pieceAt(saved).name(), "을", "를") + " 지킵니다",
                    move.to(), List.of(saved)));
        }
    }

    /** 체크, 그리고 옮긴 기물이 새로 노리게 된 상대 기물 (둘 이상이면 포크). */
    private static void addThreats(List<Reason> reasons, Board board, Board after, Move move) {
        Side me = board.turn();
        Position enemyKing = after.findKing(me.opposite());
        if (enemyKing != null && Rules.isInCheck(after) && !Rules.isCheckmate(after)) {
            reasons.add(new Reason(true, "체크: 상대는 킹부터 지켜야 합니다",
                    cheapestAttacker(after, enemyKing, me), List.of(enemyKing)));
        }

        Piece before = board.pieceAt(move.from());
        Piece moved = after.pieceAt(move.to());
        List<Position> targets = new ArrayList<>();
        for (Position square : moved.attackedSquares(after, move.to())) {
            Piece target = after.pieceAt(square);
            if (target == null || target.side() == me || target instanceof King) {
                continue;
            }
            boolean worthAttacking = target.value() > moved.value() || !Rules.isAttacked(after, square, me.opposite());
            boolean newThreat = !before.attacks(board, move.from(), square);
            if (worthAttacking && newThreat) {
                targets.add(square);
            }
        }
        if (targets.size() >= 2) {
            String first = after.pieceAt(targets.get(0)).name();
            String second = after.pieceAt(targets.get(1)).name();
            reasons.add(new Reason(true, "포크: 상대 " + withJosa(first, "과", "와") + " " + withJosa(second, "을", "를")
                    + " 동시에 노립니다", move.to(), targets.subList(0, 2)));
        } else if (targets.size() == 1) {
            reasons.add(new Reason(true, "상대 " + withJosa(after.pieceAt(targets.getFirst()).name(), "을", "를")
                    + " 노립니다", move.to(), targets));
        }
    }

    /** 캐슬링, 기물 전개, 중앙 차지처럼 좋은 계획. */
    private static void addPlan(List<Reason> reasons, Board board, Move move) {
        Piece piece = board.pieceAt(move.from());
        if (Rules.isCastling(board, move)) {
            reasons.add(new Reason(true, "캐슬링: 킹을 구석으로 피신시키고 룩을 싸움에 내보냅니다",
                    null, List.of(move.to())));
        } else if ((piece instanceof Knight || piece instanceof Bishop) && move.from().row() == board.turn().homeRow()) {
            reasons.add(new Reason(true, withJosa(piece.name(), "을", "를") + " 전개해 싸움에 참여시킵니다",
                    null, List.of()));
        }
        if ((piece instanceof Pawn || piece instanceof Knight) && CENTER.contains(move.to())) {
            reasons.add(new Reason(true, "중앙(d4·e4·d5·e5)을 차지합니다", null, CENTER));
        }
    }

    // ---- 작은 도우미 ----

    /** by 진영 기물 중 square를 공격하는 가장 싼 기물의 칸 (없으면 null). */
    private static Position cheapestAttacker(Board board, Position square, Side by) {
        Position cheapest = null;
        for (Position from : board.positionsOf(by)) {
            Piece piece = board.pieceAt(from);
            if (piece.attacks(board, from, square)
                    && (cheapest == null || piece.value() < board.pieceAt(cheapest).value())) {
                cheapest = from;
            }
        }
        return cheapest;
    }

    /** 상대에게 공격받는데 지켜 주는 아군이 없는 칸인지. */
    private static boolean isHanging(Board board, Position square, Side owner) {
        return Rules.isAttacked(board, square, owner.opposite()) && !Rules.isAttacked(board, square, owner);
    }

    /** square의 기물이 잡혀서 손해를 볼 수 있는지: 지켜 주는 아군이 없거나, 더 싼 상대 기물이 노림. */
    private static boolean isThreatened(Board board, Position square, Side owner) {
        Position attacker = cheapestAttacker(board, square, owner.opposite());
        return attacker != null && (!Rules.isAttacked(board, square, owner)
                || board.pieceAt(attacker).value() < board.pieceAt(square).value());
    }

    /** 기물 가치를 폰 개수로 바꿉니다 (퀸 9, 룩 5, 비숍·나이트 3, 폰 1). */
    private static long points(int value) {
        return Math.round(value / 100.0);
    }

    /** 단어 끝에 받침이 있으면 withBatchim, 없으면 without 조사를 붙입니다. 예) 퀸을, 나이트를 */
    private static String withJosa(String word, String withBatchim, String without) {
        char last = word.charAt(word.length() - 1);
        boolean batchim = last >= '가' && last <= '힣' && (last - '가') % 28 != 0;
        return word + (batchim ? withBatchim : without);
    }
}
