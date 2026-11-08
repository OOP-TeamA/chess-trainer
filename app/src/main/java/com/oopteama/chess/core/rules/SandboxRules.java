package com.oopteama.chess.core.rules;

import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 교육용 규칙. 킹이 없거나 한쪽만 있는 국면도 예외 없이 동작한다.
 * 자기 킹이 있으면 그 킹을 체크에 남기는 수를 빼고, 킹과 룩이 제자리에 있으면
 * 캐슬링 권리 플래그와 관계없이 캐슬링도 허용한다. checkEnd는 항상 empty다.
 */
public final class SandboxRules implements RuleSet {

    @Override
    public List<Move> legalMoves(Position pos) {
        Objects.requireNonNull(pos, "pos");
        return MoveGenerator.legalMoves(pos, false);
    }

    @Override
    public boolean inCheck(Position pos) {
        Objects.requireNonNull(pos, "pos");
        return MoveGenerator.inCheck(pos);
    }

    @Override
    public boolean isAttacked(Position pos, Square square, Side by) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(square, "square");
        Objects.requireNonNull(by, "by");
        return MoveGenerator.isAttacked(pos, square, by);
    }

    @Override
    public Optional<GameResult> checkEnd(Position pos, GameHistory history) {
        return Optional.empty();
    }

    @Override
    public String repetitionKey(Position pos) {
        Objects.requireNonNull(pos, "pos");
        return MoveGenerator.repetitionKey(pos, false);
    }
}
