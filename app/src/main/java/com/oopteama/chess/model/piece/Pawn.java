package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * 폰: 앞으로 한 칸(처음에는 두 칸까지) 가고, 잡을 때만 대각선 앞으로 갑니다.
 * 앙파상도 대각선 잡기의 한 종류입니다. 끝 줄에 도착하면 승진합니다(Rules에서 처리).
 */
public class Pawn extends Piece {

    public Pawn(Side side) {
        super(side);
    }

    @Override
    public String name() {
        return "폰";
    }

    @Override
    public char symbol() {
        return 'P';
    }

    @Override
    public int value() {
        return 100;
    }

    /** 폰은 앞쪽 대각선 두 칸을 공격합니다. */
    @Override
    public List<Position> attackedSquares(Board board, Position from) {
        List<Position> result = new ArrayList<>();
        for (int dCol : new int[]{-1, 1}) {
            Position target = from.offset(dCol, side().forward());
            if (target != null) {
                result.add(target);
            }
        }
        return result;
    }

    /** 폰은 공격하는 칸과 움직이는 칸이 달라서 이 메서드를 다시 정의합니다. */
    @Override
    public List<Position> movableSquares(Board board, Position from) {
        List<Position> result = new ArrayList<>();
        int forward = side().forward();

        // 1. 앞으로 한 칸, 처음 자리에서는 두 칸까지 (앞이 비어 있어야 함)
        Position oneStep = from.offset(0, forward);
        if (oneStep != null && board.isEmpty(oneStep)) {
            result.add(oneStep);
            int startRow = side().homeRow() + forward;
            Position twoStep = from.offset(0, 2 * forward);
            if (from.row() == startRow && twoStep != null && board.isEmpty(twoStep)) {
                result.add(twoStep);
            }
        }

        // 2. 대각선 앞의 상대 기물 잡기, 또는 앙파상
        for (Position target : attackedSquares(board, from)) {
            Piece piece = board.pieceAt(target);
            boolean enemyThere = piece != null && piece.side() != side();
            if (enemyThere || target.equals(board.enPassant())) {
                result.add(target);
            }
        }
        return result;
    }
}
