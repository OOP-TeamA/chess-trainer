package com.oopteama.chess.content.goal;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * {@code move:d4f5|d4f3}: 적힌 수 중 하나를 두면 정답. 승진 수는 승진 기물까지 같아야 한다.
 *
 * @param uci 정답 수의 UCI 표기(소문자, 파일 순서). 하나 이상
 */
public record MoveGoal(List<String> uci) implements StepGoal {

    /** 소문자 UCI. 승진 기물은 q, r, b, n만 허용. */
    private static final Pattern UCI = Pattern.compile("[a-h][1-8][a-h][1-8][qrbn]?");

    /**
     * @throws IllegalArgumentException 목록이 비었거나 UCI 표기가 아닌 항목이 있을 때
     */
    public MoveGoal {
        Objects.requireNonNull(uci, "uci");
        uci = List.copyOf(uci);
        if (uci.isEmpty()) {
            throw new IllegalArgumentException("정답 수가 없습니다");
        }
        for (String u : uci) {
            if (!UCI.matcher(u).matches()) {
                throw new IllegalArgumentException("UCI 표기가 아닙니다: " + u);
            }
        }
    }

    @Override
    public InputMode inputMode() {
        return InputMode.MOVE;
    }

    @Override
    public boolean accepts(Position before, Move move, Position after, RuleSet rules) {
        return uci.contains(move.uci());
    }

    /** 적힌 순서대로 start에서 합법인 첫 수. 모두 불법이면 empty. */
    @Override
    public Optional<Move> solution(Position start, RuleSet rules) {
        for (String u : uci) {
            Optional<Move> m = rules.fromUci(start, u);
            if (m.isPresent()) {
                return m;
            }
        }
        return Optional.empty();
    }

    /** 정답 수들의 출발 칸과 도착 칸. */
    @Override
    public Set<Square> defaultHint(Position start, RuleSet rules) {
        Set<Square> squares = new LinkedHashSet<>();
        for (String u : uci) {
            squares.add(Square.of(u.substring(0, 2)));
            squares.add(Square.of(u.substring(2, 4)));
        }
        return Set.copyOf(squares);
    }
}
