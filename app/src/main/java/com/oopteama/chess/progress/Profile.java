package com.oopteama.chess.progress;

import java.util.Objects;

/**
 * 학습자 프로필. 내부 조회와 파일 경로에는 불변 {@code id}를, 화면에는 {@code name}을 쓴다.
 *
 * @param id   프로필 ID({@code [a-z0-9][a-z0-9-]{0,63}}, {@code index} 제외)
 * @param name 표시 이름(같은 이름 허용)
 */
public record Profile(String id, String name) {

    /** null을 거부한다. */
    public Profile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
