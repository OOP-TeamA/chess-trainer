package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.Optional;
import java.util.Set;

/**
 * {@code mate}: 상대를 체크메이트하면 정답. 상대 킹이 없는 교육용 국면에서는 이룰 수 없으므로
 * 레슨 로더가 로드 시점에 거른다(G7).
 */
public record MateGoal() implements StepGoal {

    @Override
    public InputMode inputMode() {
        return InputMode.MOVE;
    }

    /** 상대 킹이 체크이고 상대에게 합법 수가 없는가. */
    @Override
    public boolean accepts(Position before, Move move, Position after, RuleSet rules) {
        return rules.inCheck(after) && rules.legalMoves(after).isEmpty();
    }

    /** 1수 전수 탐색으로 찾은 첫 메이트 수. */
    @Override
    public Optional<Move> solution(Position start, RuleSet rules) {
        return OneMoveSearch.first(this, start, rules);
    }

    /** 메이트를 거는 기물의 칸. */
    @Override
    public Set<Square> defaultHint(Position start, RuleSet rules) {
        return OneMoveSearch.fromOfFirst(this, start, rules);
    }
}
