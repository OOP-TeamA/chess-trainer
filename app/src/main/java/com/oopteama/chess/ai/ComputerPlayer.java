package com.oopteama.chess.ai;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * AI 대전의 컴퓨터 상대입니다. 미니맥스(minimax) 탐색으로 수를 고릅니다.
 * <ul>
 *   <li>점수는 항상 백 기준입니다. 백은 점수가 가장 큰 수를, 흑은 가장 작은 수를 고릅니다.</li>
 *   <li>난이도의 depth만큼 서로 번갈아 두는 경우를 모두 따져 봅니다.</li>
 *   <li>알파-베타 가지치기: 이미 더 좋은 선택이 있어 결과에 영향이 없는 가지는 계산하지 않습니다.</li>
 * </ul>
 */
public final class ComputerPlayer {

    /** 체크메이트 점수. 빨리 메이트할수록 조금 더 큰 점수를 줍니다. */
    static final int MATE_SCORE = 100_000;
    private static final int INFINITY = 1_000_000;

    private final Random random;

    public ComputerPlayer() {
        this(new Random());
    }

    public ComputerPlayer(Random random) {
        this.random = random;
    }

    /** 난이도에 맞는 수를 고릅니다. */
    public Optional<Move> chooseMove(Board board, Difficulty difficulty) {
        return chooseMove(board, difficulty, () -> false);
    }

    /**
     * 난이도에 맞는 수를 고릅니다.
     * 계산 중에 cancelled가 true가 되면(새 대국 시작 등) 멈추고 빈 값을 돌려줍니다.
     */
    public Optional<Move> chooseMove(Board board, Difficulty difficulty, BooleanSupplier cancelled) {
        List<Move> moves = new ArrayList<>(Rules.legalMoves(board));
        if (moves.isEmpty() || cancelled.getAsBoolean()) {
            return Optional.empty();
        }
        Collections.shuffle(moves, random); // 점수가 같은 수끼리는 매번 다르게 고르도록 섞습니다.
        moves = orderMoves(board, moves);   // 잡는 수를 먼저 보면 가지치기가 잘 됩니다.

        if (difficulty == Difficulty.BEGINNER) {
            return Optional.of(pickBeginnerMove(board, moves));
        }

        boolean whiteToMove = board.turn() == Side.WHITE;
        Move bestMove = moves.getFirst();
        int alpha = -INFINITY; // 백이 지금까지 확보한 최고 점수
        int beta = INFINITY;   // 흑이 지금까지 확보한 최저 점수
        for (Move move : moves) {
            int score = minimax(board.apply(move), difficulty.depth() - 1, alpha, beta, 1, cancelled);
            if (cancelled.getAsBoolean()) {
                return Optional.empty();
            }
            if (whiteToMove && score > alpha) {
                alpha = score;
                bestMove = move;
            } else if (!whiteToMove && score < beta) {
                beta = score;
                bestMove = move;
            }
        }
        return Optional.of(bestMove);
    }

    /** 사람에게 보여 줄 추천 수 (중급 수준으로 계산). */
    public Optional<Move> hint(Board board) {
        return chooseMove(board, Difficulty.MEDIUM);
    }

    /**
     * 미니맥스 탐색: depth 수 앞까지 서로 최선을 둔다고 보고 국면 점수(백 기준)를 계산합니다.
     *
     * @param alpha 백이 이미 확보한 최고 점수 (이보다 나쁜 가지는 백이 고르지 않음)
     * @param beta  흑이 이미 확보한 최저 점수 (이보다 나쁜 가지는 흑이 고르지 않음)
     * @param ply   시작 국면에서 몇 수째인지 (빠른 메이트를 더 좋게 보기 위해 씀)
     */
    private int minimax(Board board, int depth, int alpha, int beta, int ply, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            return 0; // 취소되면 결과를 쓰지 않으므로 아무 값이나 돌려줍니다.
        }
        if (depth == 0) {
            return Rules.hasAnyLegalMove(board) ? Evaluator.evaluate(board) : endScore(board, ply);
        }
        List<Move> moves = Rules.legalMoves(board);
        if (moves.isEmpty()) {
            return endScore(board, ply);
        }
        if (Rules.isInsufficientMaterial(board)) {
            return 0;
        }

        if (board.turn() == Side.WHITE) {
            int best = -INFINITY;
            for (Move move : orderMoves(board, moves)) {
                best = Math.max(best, minimax(board.apply(move), depth - 1, alpha, beta, ply + 1, cancelled));
                alpha = Math.max(alpha, best);
                if (alpha >= beta) {
                    break; // 흑이 이 가지를 피할 것이므로 나머지 수는 볼 필요가 없습니다.
                }
            }
            return best;
        } else {
            int best = INFINITY;
            for (Move move : orderMoves(board, moves)) {
                best = Math.min(best, minimax(board.apply(move), depth - 1, alpha, beta, ply + 1, cancelled));
                beta = Math.min(beta, best);
                if (alpha >= beta) {
                    break; // 백이 이 가지를 피할 것이므로 나머지 수는 볼 필요가 없습니다.
                }
            }
            return best;
        }
    }

    /** 둘 수가 없는 국면의 점수: 체크메이트면 진 쪽에 아주 나쁜 점수, 스테일메이트면 0(무승부). */
    private static int endScore(Board board, int ply) {
        if (!Rules.isInCheck(board)) {
            return 0;
        }
        int mate = MATE_SCORE - ply;
        return board.turn() == Side.WHITE ? -mate : mate;
    }

    /** 입문: 한 수 앞만 보고, 최선과 큰 차이가 없는 상위 3개 수 중 하나를 무작위로 고릅니다. */
    private Move pickBeginnerMove(Board board, List<Move> moves) {
        int sign = board.turn() == Side.WHITE ? 1 : -1;
        List<ScoredMove> scored = new ArrayList<>();
        for (Move move : moves) {
            int score = minimax(board.apply(move), 0, -INFINITY, INFINITY, 1, () -> false);
            scored.add(new ScoredMove(move, sign * score)); // 내 기준 점수로 바꿈
        }
        scored.sort(Comparator.comparingInt(ScoredMove::score).reversed());
        int topScore = scored.getFirst().score();
        List<Move> candidates = new ArrayList<>();
        for (int i = 0; i < Math.min(3, scored.size()); i++) {
            if (topScore - scored.get(i).score() <= 150) {
                candidates.add(scored.get(i).move());
            }
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /** 잡는 수(비싼 기물을 싼 기물로 잡는 수일수록)와 승진을 먼저 보도록 정렬합니다. */
    private static List<Move> orderMoves(Board board, List<Move> moves) {
        List<Move> ordered = new ArrayList<>(moves);
        ordered.sort(Comparator.comparingInt((Move move) -> priority(board, move)).reversed());
        return ordered;
    }

    private static int priority(Board board, Move move) {
        int priority = 0;
        Piece captured = Rules.capturedPiece(board, move);
        if (captured != null) {
            priority += 10 * captured.value() - board.pieceAt(move.from()).value();
        }
        if (move.isPromotion()) {
            priority += 800;
        }
        return priority;
    }

    private record ScoredMove(Move move, int score) {
    }
}
