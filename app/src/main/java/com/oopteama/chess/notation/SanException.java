package com.oopteama.chess.notation;

import com.oopteama.chess.core.DataException;
import java.util.Objects;

/** SAN 문자열을 수로 해석하지 못했을 때 던지는 검사 예외. */
public final class SanException extends DataException {

    /** 해석에 실패한 이유. */
    public enum Reason {
        /** SAN 문법에 맞지 않는다. */
        SYNTAX,
        /** 문법은 맞지만 그 국면의 합법 수가 아니다. */
        ILLEGAL,
        /** 합법 수가 둘 이상 해당한다(출발 파일이나 랭크 표기가 필요하다). */
        AMBIGUOUS
    }

    private final Reason reason;
    private final String san;

    /**
     * 예외를 만든다.
     *
     * @param reason 실패 이유
     * @param san 해석하려던 원문
     * @param message 사람이 읽을 설명
     */
    public SanException(Reason reason, String san, String message) {
        super(message, "<input>", 0);
        this.reason = Objects.requireNonNull(reason, "reason");
        this.san = Objects.requireNonNull(san, "san");
    }

    /** 실패 이유를 돌려준다. */
    public Reason reason() {
        return reason;
    }

    /** 해석하려던 SAN 원문을 돌려준다. */
    public String san() {
        return san;
    }
}
