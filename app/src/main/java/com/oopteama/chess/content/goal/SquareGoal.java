package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@code square:e4|e5}: 수를 두지 않고 칸을 골라 답한다. 적힌 칸 중 하나면 정답.
 *
 * @param squares 정답 칸. 하나 이상
 */
public record SquareGoal(Set<Square> squares) implements StepGoal {

    /**
     * @throws IllegalArgumentException 정답 칸이 없을 때
     */
    public SquareGoal {
        Objects.requireNonNull(squares, "squares");
        squares = Set.copyOf(squares);
        if (squares.isEmpty()) {
            throw new IllegalArgumentException("정답 칸이 없습니다");
        }
    }

    @Override
    public InputMode inputMode() {
        return InputMode.SQUARE;
    }

    /** 칸 선택 목표이므로 수는 정답이 될 수 없다. */
    @Override
    public boolean accepts(Position before, Move move, Position after, RuleSet rules) {
        return false;
    }

    @Override
    public boolean acceptsSquare(Square square) {
        return squares.contains(square);
    }

    /** 칸 선택 목표는 정답 수가 없다. 정답은 {@link #solutionSquares()}로 보여준다. */
    @Override
    public Optional<Move> solution(Position start, RuleSet rules) {
        return Optional.empty();
    }

    @Override
    public Set<Square> solutionSquares() {
        return squares;
    }

    /** 정답 칸으로 갈 수 있는 기물의 칸. 그런 기물이 없으면 빈 집합. 정답 칸 자체는 공개하지 않는다. */
    @Override
    public Set<Square> defaultHint(Position start, RuleSet rules) {
        Set<Square> from = new LinkedHashSet<>();
        for (Move m : rules.legalMoves(start)) {
            if (squares.contains(m.to())) {
                from.add(m.from());
            }
        }
        return Set.copyOf(from);
    }
}
