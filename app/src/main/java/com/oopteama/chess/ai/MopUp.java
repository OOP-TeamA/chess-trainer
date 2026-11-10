package com.oopteama.chess.ai;

import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

import java.util.Optional;

/**
 * 끝내기 마무리 항(mop-up). 한쪽이 킹만 남고 다른 쪽에 룩 이상의 기물이 있으면
 * 외톨이 킹을 가장자리로 밀고 두 킹을 가깝게 하는 쪽에 점수를 준다.
 * 이 항이 없으면 기물 점수가 평평해서 이긴 끝내기에서 같은 수를 왕복하다 반복 무승부가 된다.
 * 그 밖의 국면에서는 0이라 중반 평가에는 영향이 없다.
 */
final class MopUp {

    private static final int EDGE_WEIGHT = 10;
    private static final int CLOSE_WEIGHT = 4;
    private static final int MIN_STRONG_MATERIAL = 500;   // 룩 1개 이상

    private MopUp() { }

    /** 백 기준 점수. */
    static int whiteBonus(Position pos) {
        if (loneKing(pos, Side.BLACK) && pieceMaterial(pos, Side.WHITE) >= MIN_STRONG_MATERIAL) {
            return bonus(pos, Side.WHITE);
        }
        if (loneKing(pos, Side.WHITE) && pieceMaterial(pos, Side.BLACK) >= MIN_STRONG_MATERIAL) {
            return -bonus(pos, Side.BLACK);
        }
        return 0;
    }

    private static int bonus(Position pos, Side strong) {
        Optional<Square> mine = pos.kingSquare(strong);
        Optional<Square> lone = pos.kingSquare(strong.opposite());
        if (mine.isEmpty() || lone.isEmpty()) return 0;
        Square w = mine.get();
        Square l = lone.get();
        int centerDist = Math.max(3 - l.file(), l.file() - 4) + Math.max(3 - l.rank(), l.rank() - 4);   // 0..6
        int kingDist = Math.abs(w.file() - l.file()) + Math.abs(w.rank() - l.rank());                    // 1..14
        return EDGE_WEIGHT * centerDist + CLOSE_WEIGHT * (14 - kingDist);
    }

    private static boolean loneKing(Position pos, Side side) {
        for (Square sq : pos.occupied(side)) {
            if (pos.at(sq).type() != PieceType.KING) return false;
        }
        return true;
    }

    /** 폰과 킹을 뺀 기물 가치 합. 폰만 많은 끝내기는 승진이 계획이라 대상에서 뺀다. */
    private static int pieceMaterial(Position pos, Side side) {
        int sum = 0;
        for (Square sq : pos.occupied(side)) {
            PieceType t = pos.at(sq).type();
            if (t != PieceType.KING && t != PieceType.PAWN) sum += MaterialEvaluator.value(t);
        }
        return sum;
    }
}
