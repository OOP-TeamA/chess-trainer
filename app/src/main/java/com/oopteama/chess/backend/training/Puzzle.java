package com.oopteama.chess.backend.training;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.Move;

import java.util.List;

/**
 * 체스 전술 퍼즐 데이터 모델.
 * mateIn > 0이면 지정 수 이내에 어떤 경로로든 체크메이트하면 정답이 되는 자유 메이트 퍼즐로 동작합니다.
 * mateIn == 0이면 solution([사용자 수, 상대 응수, ...]) 순서의 전술 퍼즐로 동작합니다.
 */
public record Puzzle(
        String id,
        String theme,
        String title,
        Board startBoard,
        List<Move> solution,
        int mateIn,
        String explanation
) {
    public boolean isMatePuzzle() {
        return mateIn > 0;
    }
}
