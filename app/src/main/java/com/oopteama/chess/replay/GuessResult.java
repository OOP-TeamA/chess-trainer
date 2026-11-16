package com.oopteama.chess.replay;

import com.oopteama.chess.notation.PgnMove;

import java.util.Objects;

/**
 * 다음 수 맞히기 한 번의 결과(GAME-05).
 *
 * @param correct 사용자 수가 실제 수와 같으면 true. 포기는 false
 * @param actual  실제로 둔 수(SAN, 평가 기호, 해설 포함). 해설은 이 시점에 공개한다
 */
public record GuessResult(boolean correct, PgnMove actual) {

    /** actual은 null 불가. */
    public GuessResult {
        Objects.requireNonNull(actual, "actual");
    }
}
