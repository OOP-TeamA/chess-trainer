package com.oopteama.chess.core.rules;

import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 규칙 전략. 상태가 없으므로 인스턴스를 공유해도 된다.
 * 넘겨받은 Position은 수정하지 않는다(내부에서 복사본으로 계산).
 */
public interface RuleSet {

    /** 차례인 쪽의 합법 수. 불변 목록. */
    List<Move> legalMoves(Position pos);

    /** 차례인 쪽 킹이 공격받는가. 그 킹이 없으면 false. */
    boolean inCheck(Position pos);

    /** {@code by} 쪽 기물이 {@code square}를 공격하는가. 칸이 비어 있어도 된다. */
    boolean isAttacked(Position pos, Square square, Side by);

    /** 종료가 아니면 empty. 기권은 여기서 나오지 않는다(Game.resign). */
    Optional<GameResult> checkEnd(Position pos, GameHistory history);

    /** 3회 반복 비교 키: FEN 앞 4필드, 앙파상 칸은 실제로 잡을 수 있을 때만 포함. */
    String repetitionKey(Position pos);

    /** from, to, promotion이 모두 같은 합법 수를 찾는다. */
    default Optional<Move> findLegal(Position pos, Square from, Square to, PieceType promotion) {
        for (Move m : legalMoves(pos)) {
            if (m.from().equals(from) && m.to().equals(to) && Objects.equals(m.promotion(), promotion)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /** "e2e4", "e7e8q" 형식의 UCI 문자열을 합법 수로 바꾼다. 문법 오류나 불법 수는 empty. */
    default Optional<Move> fromUci(Position pos, String uci) {
        if (uci == null || (uci.length() != 4 && uci.length() != 5)) {
            return Optional.empty();
        }
        Square from;
        Square to;
        try {
            from = Square.of(uci.substring(0, 2));
            to = Square.of(uci.substring(2, 4));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        PieceType promotion = null;
        if (uci.length() == 5) {
            promotion = switch (uci.charAt(4)) {
                case 'q' -> PieceType.QUEEN;
                case 'r' -> PieceType.ROOK;
                case 'b' -> PieceType.BISHOP;
                case 'n' -> PieceType.KNIGHT;
                default -> null;
            };
            if (promotion == null) {
                return Optional.empty();
            }
        }
        return findLegal(pos, from, to, promotion);
    }
}
