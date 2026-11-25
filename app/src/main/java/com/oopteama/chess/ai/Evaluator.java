package com.oopteama.chess.ai;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.Bishop;
import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Knight;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;

/**
 * 국면이 누구에게 얼마나 유리한지 점수로 계산합니다.
 * 점수 = (백 기물 가치 + 위치 보너스) - (흑 기물 가치 + 위치 보너스). 양수면 백이, 음수면 흑이 유리합니다.
 */
public final class Evaluator {

    private Evaluator() {
    }

    /** 백 기준 국면 점수. */
    public static int evaluate(Board board) {
        int score = 0;
        for (Side side : Side.values()) {
            int sign = side == Side.WHITE ? 1 : -1;
            for (Position position : board.positionsOf(side)) {
                Piece piece = board.pieceAt(position);
                score += sign * (piece.value() + positionBonus(piece, position));
            }
        }
        return score;
    }

    /**
     * 위치 보너스: 나이트와 비숍은 가운데에 있을수록, 폰은 앞으로 나갈수록 조금 더 좋게 봅니다.
     * 킹은 가운데로 나오면 위험하므로 조금 깎습니다.
     */
    private static int positionBonus(Piece piece, Position position) {
        int distanceFromCenter = Math.abs(2 * position.col() - 7) + Math.abs(2 * position.row() - 7);
        int centrality = 14 - distanceFromCenter; // 가운데 4칸 = 12, 구석 = 0
        if (piece instanceof Knight || piece instanceof Bishop) {
            return centrality * 3;
        }
        if (piece instanceof Pawn) {
            int advanced = piece.side() == Side.WHITE ? position.row() - 1 : 6 - position.row();
            return advanced * 5 + centrality;
        }
        if (piece instanceof King) {
            return -centrality * 2;
        }
        return 0;
    }
}
