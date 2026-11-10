package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.Optional;
import java.util.Set;

/** 체크·메이트 목표의 정답과 힌트를 찾는 1수 전수 탐색. */
final class OneMoveSearch {

    private OneMoveSearch() {
    }

    /**
     * start의 합법 수를 {@link RuleSet#legalMoves} 순서대로 두어 보고 처음으로 목표를 이루는 수를 돌려준다.
     * start는 바꾸지 않는다.
     */
    static Optional<Move> first(StepGoal goal, Position start, RuleSet rules) {
        Position after = start.copy();
        for (Move m : rules.legalMoves(start)) {
            after.make(m);
            boolean ok = goal.accepts(start, m, after, rules);
            after.unmake();
            if (ok) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /** 정답 수를 둘 기물의 칸. 정답이 없으면 빈 집합. */
    static Set<Square> fromOfFirst(StepGoal goal, Position start, RuleSet rules) {
        return first(goal, start, rules).map(m -> Set.of(m.from())).orElse(Set.of());
    }
}
