package com.oopteama.chess.notation;

import com.oopteama.chess.core.Move;
import java.util.Objects;
import java.util.Set;

/**
 * 기보의 수 하나.
 *
 * @param san 정규 SAN(평가 기호 없음, 체크와 체크메이트 표시 포함). 예: {@code Qb8+}
 * @param move 실제 수
 * @param suffix 평가 기호({@code !!}, {@code !}, {@code ?}, {@code ??}, {@code !?}, {@code ?!}). 없으면 빈 문자열
 * @param comment 해설. 없으면 빈 문자열
 */
public record PgnMove(String san, Move move, String suffix, String comment) {

    private static final Set<String> SUFFIXES = Set.of("", "!!", "!", "?", "??", "!?", "?!");

    /** null을 거부하고 평가 기호가 허용된 값인지 확인한다. */
    public PgnMove {
        Objects.requireNonNull(san, "san");
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(suffix, "suffix");
        Objects.requireNonNull(comment, "comment");
        if (!SUFFIXES.contains(suffix)) {
            throw new IllegalArgumentException("지원하지 않는 평가 기호: " + suffix);
        }
    }
}
