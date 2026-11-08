package com.oopteama.chess.core.piece;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

import java.util.List;

/**
 * 위치를 갖지 않는 공유 인스턴스 12개 (PieceFactory가 보관). 상태가 없으므로 스레드 안전.
 */
public abstract class Piece {

    private final Side side;

    protected Piece(Side side) {
        this.side = side;
    }

    /** 이 기물의 진영. */
    public final Side side() {
        return side;
    }

    /** 기물 종류. */
    public abstract PieceType type();

    /** 백은 대문자, 흑은 소문자. */
    public final char fenChar() {
        char c = switch (type()) {
            case KING -> 'k';
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            case PAWN -> 'p';
        };
        return side == Side.WHITE ? Character.toUpperCase(c) : c;
    }

    /**
     * 이동 규칙만 지킨 수(자기 킹이 체크에 남는지는 검사하지 않음).
     * 캐슬링은 공격 판정이 필요하므로 여기서 만들지 않고 RuleSet이 만든다.
     */
    public abstract List<Move> pseudoLegalMoves(Position pos, Square from);
}

