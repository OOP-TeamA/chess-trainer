package com.oopteama.chess.training;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;

import java.util.List;

/**
 * 퍼즐 한 문제입니다.
 * <ul>
 *   <li>mateIn > 0: "N수 안에 체크메이트" 퍼즐. 어떤 수순이든 N수 안에 메이트하면 정답입니다.</li>
 *   <li>mateIn == 0: 전술 퍼즐. solution(내 수, 상대 응수, 내 수, ...) 순서대로 두어야 합니다.</li>
 * </ul>
 */
public record Puzzle(String id, String theme, String title, Board startBoard,
                     List<Move> solution, int mateIn, String explanation) {

    public boolean isMatePuzzle() {
        return mateIn > 0;
    }
}
