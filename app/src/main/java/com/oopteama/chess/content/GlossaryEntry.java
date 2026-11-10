package com.oopteama.chess.content;

import com.oopteama.chess.core.Square;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 용어 하나(GLOS-03).
 *
 * @param id         용어 ID. 링크 대상, 즐겨찾기, 퀴즈 문항이 이 값으로 가리킨다
 * @param ko         한글명
 * @param en         영문명
 * @param category   분류
 * @param definition 정의
 * @param fen        예시 국면. 기보 표기 분류는 없을 수 있다
 * @param highlight  예시 국면에서 강조할 칸. 없으면 빈 집합
 * @param related    관련 용어 ID. 없으면 빈 목록
 * @param aliases    링크에서 이 용어로 해석할 다른 이름. 없으면 빈 목록
 * @param tryStep    직접 해보기. 없으면 empty
 */
public record GlossaryEntry(String id, String ko, String en, TermCategory category, String definition,
                            Optional<String> fen, Set<Square> highlight, List<String> related,
                            List<String> aliases, Optional<TermTry> tryStep) {

    /**
     * 모든 필드는 null 불가. 칸 집합과 목록은 불변 사본으로 바꾼다.
     *
     * @throws IllegalArgumentException id, ko, en 중 하나가 비었을 때
     */
    public GlossaryEntry {
        requireText(id, "id");
        requireText(ko, "ko");
        requireText(en, "en");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(fen, "fen");
        highlight = Set.copyOf(Objects.requireNonNull(highlight, "highlight"));
        related = List.copyOf(Objects.requireNonNull(related, "related"));
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        Objects.requireNonNull(tryStep, "tryStep");
    }

    private static void requireText(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + "이(가) 비어 있습니다");
        }
    }
}
