package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** {@code any-move}: 합법 수라면 무엇이든 정답. 기물의 움직임을 직접 체험하는 단계에 쓴다. */
public record AnyMoveGoal() implements StepGoal {

    @Override
    public InputMode inputMode() {
        return InputMode.MOVE;
    }

    /** 합법성은 호출한 쪽이 확인하므로 항상 true. */
    @Override
    public boolean accepts(Position before, Move move, Position after, RuleSet rules) {
        return true;
    }

    /** 합법 수 목록의 첫 수. 둘 수가 없으면 empty. */
    @Override
    public Optional<Move> solution(Position start, RuleSet rules) {
        return rules.legalMoves(start).stream().findFirst();
    }

    /** 움직일 수 있는 기물이 놓인 칸. */
    @Override
    public Set<Square> defaultHint(Position start, RuleSet rules) {
        Set<Square> from = new LinkedHashSet<>();
        for (Move m : rules.legalMoves(start)) {
            from.add(m.from());
        }
        return Set.copyOf(from);
    }
}
