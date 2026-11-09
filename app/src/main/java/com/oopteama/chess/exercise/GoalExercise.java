package com.oopteama.chess.exercise;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 목표 하나짜리 연습의 공통 구현(레슨 단계, 용어 직접 해보기).
 *
 * <p>정답이면 수를 둔 국면을 보여주고 입력을 잠근다. 오답이면 국면은 시작 그대로다(오답 직전 국면).
 * 오답 횟수와 사다리 단계는 화면 쪽 {@link FeedbackLadder}가 관리한다.
 */
public abstract class GoalExercise implements Exercise {

    /** 파일에 success가 없을 때의 정답 메시지. */
    static final String DEFAULT_SUCCESS = "정답입니다.";
    /** 파일에 fail이 없을 때의 오답 메시지. */
    static final String DEFAULT_FAIL = "목표를 이루지 못했습니다. 다시 시도해 보세요.";

    private final Position start;
    private final RuleSet rules;
    private final StepGoal goal;
    private final Set<Square> hint;
    private final String prompt;
    private final String fail;
    private final String success;

    private Position current;
    private boolean solved;

    /**
     * @param fen     시작 국면. 로드 시점에 검증된 값이어야 한다
     * @param rules   적용 규칙
     * @param goal    목표
     * @param hint    파일에 적힌 힌트 칸. 비었으면 목표의 기본 힌트를 쓴다
     * @param prompt  문제 설명
     * @param fail    오답 메시지. 빈 문자열이면 기본 문구
     * @param success 정답 메시지. 빈 문자열이면 기본 문구
     * @throws IllegalArgumentException fen이 FEN 형식이 아닐 때
     */
    protected GoalExercise(String fen, RuleSet rules, StepGoal goal, Set<Square> hint,
                           String prompt, String fail, String success) {
        try {
            this.start = Fen.parse(Objects.requireNonNull(fen, "fen"));
        } catch (FenFormatException e) {
            throw new IllegalArgumentException("시작 국면 FEN 오류: " + e.getMessage(), e);
        }
        this.rules = Objects.requireNonNull(rules, "rules");
        this.goal = Objects.requireNonNull(goal, "goal");
        this.hint = Set.copyOf(Objects.requireNonNull(hint, "hint"));
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.fail = orDefault(fail, DEFAULT_FAIL);
        this.success = orDefault(success, DEFAULT_SUCCESS);
        this.current = start.copy();
    }

    @Override
    public final String prompt() {
        return prompt;
    }

    @Override
    public final InputMode inputMode() {
        return goal.inputMode();
    }

    @Override
    public final RuleSet rules() {
        return rules;
    }

    @Override
    public final Position position() {
        return current.copy();
    }

    @Override
    public final List<Move> legalMoves() {
        if (solved || inputMode() != InputMode.MOVE) {
            return List.of();
        }
        return List.copyOf(rules.legalMoves(current));
    }

    @Override
    public final Verdict submit(Move move) {
        Objects.requireNonNull(move, "move");
        requireOpen(InputMode.MOVE);
        if (!rules.legalMoves(current).contains(move)) {
            throw new IllegalArgumentException("현재 국면에서 둘 수 없는 수: " + move.uci());
        }
        Position after = current.copy();
        after.make(move);
        if (!goal.accepts(current, move, after, rules)) {
            return new Verdict.Wrong(fail);      // current는 그대로: 오답 직전 국면
        }
        current = after;
        solved = true;
        return new Verdict.Solved(success);
    }

    @Override
    public final Verdict submitSquare(Square square) {
        Objects.requireNonNull(square, "square");
        requireOpen(InputMode.SQUARE);
        if (!goal.acceptsSquare(square)) {
            return new Verdict.Wrong(fail);
        }
        solved = true;
        return new Verdict.Solved(success);
    }

    @Override
    public final Optional<Move> solution() {
        return solved ? Optional.empty() : goal.solution(current, rules);
    }

    @Override
    public final Set<Square> solutionSquares() {
        return goal.solutionSquares();
    }

    @Override
    public final Set<Square> hintSquares() {
        return hint.isEmpty() ? goal.defaultHint(start, rules) : hint;
    }

    @Override
    public final void reset() {
        current = start.copy();
        solved = false;
    }

    private void requireOpen(InputMode expected) {
        if (inputMode() != expected) {
            throw new IllegalStateException("이 연습의 입력 종류는 " + inputMode() + "입니다");
        }
        if (solved) {
            throw new IllegalStateException("이미 푼 연습입니다. reset() 후 다시 제출하세요");
        }
    }

    private static String orDefault(String message, String fallback) {
        Objects.requireNonNull(message, "message");
        return message.isBlank() ? fallback : message;
    }
}
