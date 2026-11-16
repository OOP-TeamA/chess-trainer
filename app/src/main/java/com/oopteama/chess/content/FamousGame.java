package com.oopteama.chess.content;

import com.oopteama.chess.notation.PgnGame;

import java.util.Objects;

/**
 * 번들 명국 기보 하나(GAME-01). 백2의 {@code GameLibrary.bundled()}가 {@code data/games/index.txt}
 * 순서로 만들고, 기보 목록·재생·다음 수 맞히기 화면이 쓴다.
 *
 * @param id    번들 ID. 파일명에서 {@code .txt}(UCI 대안은 {@code .uci.txt})를 뺀 값.
 *              학습 기록의 {@code replay.best.<id>}에 저장된다
 * @param alias 화면 표시용 별칭(PGN {@code [Alias]} 태그, 예: "오페라 게임")
 * @param level 난이도(PGN {@code [Level]} 태그)
 * @param pgn   대국자·연도·오프닝 태그와 수순
 */
public record FamousGame(String id, String alias, Level level, PgnGame pgn) {

    /**
     * 모든 필드는 null 불가.
     *
     * @throws IllegalArgumentException id나 alias가 비었을 때
     */
    public FamousGame {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(alias, "alias");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pgn, "pgn");
        if (id.isBlank()) {
            throw new IllegalArgumentException("기보 ID가 비었습니다");
        }
        if (alias.isBlank()) {
            throw new IllegalArgumentException("기보 별칭이 비었습니다: " + id);
        }
    }

    /**
     * 총 수(반수, ply). 목록 화면의 "총 수"는 이 값을 2로 나눠 올림한 수 번호로 표시할 수 있다.
     *
     * @return 수순의 길이
     */
    public int plyCount() {
        return pgn.moves().size();
    }
}
