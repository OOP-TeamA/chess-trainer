package com.oopteama.chess.exercise;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 보드 위 행동으로 푸는 연습 하나(P1). 레슨 단계, 용어 직접 해보기, 퍼즐이 이 추상을 공유하고
 * 화면은 {@code ExerciseController}를 통해 같은 방식으로 다룬다. 상태를 가지며 EDT 전용이다.
 */
public interface Exercise {

    /**
     * 사용자에게 보여줄 문제 설명.
     *
     * @return 설명 문장
     */
    String prompt();

    /**
     * 입력 종류.
     *
     * @return 수를 두면 MOVE, 칸을 고르면 SQUARE
     */
    InputMode inputMode();

    /**
     * 적용 규칙.
     *
     * @return 이 연습의 RuleSet
     */
    RuleSet rules();

    /**
     * 현재 국면의 사본. 고쳐도 연습에는 영향이 없다.
     *
     * @return 새 Position
     */
    Position position();

    /**
     * 지금 제출할 수 있는 수. 칸 선택 연습이거나 이미 풀었으면 빈 목록.
     *
     * @return 불변 목록
     */
    List<Move> legalMoves();

    /**
     * 수를 채점한다. Wrong이면 오답 직전 국면으로 되돌린다(퍼즐은 처음이 아니라 직전, G14).
     *
     * @param move {@link #legalMoves()}에 있는 수
     * @return 채점 결과
     * @throws IllegalStateException    칸 선택 연습이거나 이미 풀었을 때
     * @throws IllegalArgumentException 현재 국면에서 둘 수 없는 수일 때
     */
    Verdict submit(Move move);

    /**
     * 고른 칸을 채점한다. inputMode가 SQUARE일 때만 쓴다.
     *
     * @param square 고른 칸
     * @return 채점 결과
     * @throws IllegalStateException 수를 두는 연습이거나 이미 풀었을 때
     */
    Verdict submitSquare(Square square);

    /**
     * 현재 국면 기준 정답 수. 정답 공개(REVEAL) 화살표에 쓴다. 칸 선택 연습이거나 이미 풀었으면 empty.
     *
     * @return 정답 수
     */
    Optional<Move> solution();

    /**
     * 칸 선택 연습의 정답 칸. 정답 공개(REVEAL)에 쓴다.
     *
     * @return 불변 집합. 수를 두는 연습이면 빈 집합
     */
    default Set<Square> solutionSquares() {
        return Set.of();
    }

    /**
     * 힌트(HINT) 단계에서 강조할 칸.
     *
     * @return 불변 집합
     */
    Set<Square> hintSquares();

    /** 처음 국면, 풀지 않은 상태로 되돌린다. */
    void reset();
}
