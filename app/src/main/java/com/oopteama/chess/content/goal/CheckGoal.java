package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.Optional;
import java.util.Set;

/**
 * {@code check}: 상대 킹을 체크하면 정답(메이트 포함). 상대 킹이 없는 교육용 국면에서는 이룰 수 없으므로
 * 레슨 로더가 로드 시점에 거른다(G7).
 */
public record CheckGoal() implements StepGoal {

    @Override
    public InputMode inputMode() {
        return InputMode.MOVE;
    }

    /** 수를 둔 뒤 차례가 된 상대 킹이 공격받는가. */
    @Override
    public boolean accepts(Position before, Move move, Position after, RuleSet rules) {
        return rules.inCheck(after);
    }

    /** 1수 전수 탐색으로 찾은 첫 체크 수. */
    @Override
    public Optional<Move> solution(Position start, RuleSet rules) {
        return OneMoveSearch.first(this, start, rules);
    }

    /** 체크를 거는 기물의 칸. */
    @Override
    public Set<Square> defaultHint(Position start, RuleSet rules) {
        return OneMoveSearch.fromOfFirst(this, start, rules);
    }
}
