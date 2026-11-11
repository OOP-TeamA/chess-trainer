package com.oopteama.chess.notation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 기보 파일 하나에 든 대국 하나.
 *
 * @param tags 태그 이름과 값(읽은 순서 유지). 불변 사본으로 보관한다
 * @param startFen 시작 국면. 표준 시작이면 {@code Fen.INITIAL}
 * @param moves 수순. 불변 사본으로 보관한다
 * @param result {@code 1-0}, {@code 0-1}, {@code 1/2-1/2}, {@code *} 중 하나
 */
public record PgnGame(Map<String, String> tags, String startFen, List<PgnMove> moves, String result) {

    private static final Set<String> RESULTS = Set.of("1-0", "0-1", "1/2-1/2", "*");

    /** null을 거부하고 컬렉션을 불변 사본으로 바꾼다. */
    public PgnGame {
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(startFen, "startFen");
        Objects.requireNonNull(moves, "moves");
        Objects.requireNonNull(result, "result");
        if (!RESULTS.contains(result)) {
            throw new IllegalArgumentException("알 수 없는 결과 토큰: " + result);
        }
        tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        moves = List.copyOf(moves);
    }

    /** 이름으로 태그 값을 찾는다. 없으면 빈 Optional. */
    public Optional<String> tag(String name) {
        return Optional.ofNullable(tags.get(name));
    }

    /** {@code [SetUp "1"]}과 {@code [FEN "…"]} 태그가 모두 있으면 true(중간 국면에서 시작한 대국). */
    public boolean customStart() {
        return "1".equals(tags.get("SetUp")) && tags.containsKey("FEN");
    }
}
