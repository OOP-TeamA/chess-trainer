package com.oopteama.chess.ai;

import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;

/**
 * 기물 가치 + 위치표(Piece-Square Table) + 끝내기 마무리 항(MopUp). 표는 8랭크가 윗줄인 백 기준.
 * 기물이 적은 끝내기에서는 킹 표를 중앙 선호로 바꾼다.
 */
public final class PstEvaluator implements Evaluator {

    private static final int[] PAWN = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0 };
    private static final int[] KNIGHT = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50 };
    private static final int[] BISHOP = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20 };
    private static final int[] ROOK = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0 };
    private static final int[] QUEEN = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20 };
    private static final int[] KING = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20 };
    /** 끝내기 킹: 중앙으로 나와 싸운다. */
    private static final int[] KING_END = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50 };

    /** 양쪽의 폰·킹 외 기물 합이 이 값 이하면 끝내기 킹 표를 쓴다(퀸+룩 정도). */
    private static final int ENDGAME_PIECE_MATERIAL = 1_400;

    private static int[] table(PieceType t, boolean endgame) {
        return switch (t) {
            case PAWN -> PAWN;
            case KNIGHT -> KNIGHT;
            case BISHOP -> BISHOP;
            case ROOK -> ROOK;
            case QUEEN -> QUEEN;
            case KING -> endgame ? KING_END : KING;
        };
    }

    private static int pieceScore(Piece p, Square sq, boolean endgame) {
        int row = p.side() == Side.WHITE ? 7 - sq.rank() : sq.rank();   // 흑은 상하 반전
        return MaterialEvaluator.value(p.type()) + table(p.type(), endgame)[row * 8 + sq.file()];
    }

    private static boolean isEndgame(Position pos) {
        int pieces = 0;
        for (Side side : Side.values()) {
            for (Square sq : pos.occupied(side)) {
                PieceType t = pos.at(sq).type();
                if (t != PieceType.PAWN && t != PieceType.KING) pieces += MaterialEvaluator.value(t);
            }
        }
        return pieces <= ENDGAME_PIECE_MATERIAL;
    }

    @Override
    public int evaluate(Position pos) {
        boolean endgame = isEndgame(pos);
        int score = 0;
        for (Square sq : pos.occupied(Side.WHITE)) score += pieceScore(pos.at(sq), sq, endgame);
        for (Square sq : pos.occupied(Side.BLACK)) score -= pieceScore(pos.at(sq), sq, endgame);
        score += MopUp.whiteBonus(pos);
        return pos.sideToMove() == Side.WHITE ? score : -score;
    }
}
