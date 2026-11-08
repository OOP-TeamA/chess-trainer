package com.oopteama.chess.core;

/** FEN 문자열의 문법 오류. 어느 필드가 문제인지 field()로 알려 준다. */
public final class FenFormatException extends DataException {

    private final int field;

    /** field는 잘못된 FEN 필드 번호(1~6). 나머지 인자는 DataException과 같다. */
    public FenFormatException(String message, String source, int line, int field) {
        super(message, source, line);
        this.field = field;
    }

    /** 잘못된 FEN 필드 번호 1~6. */
    public int field() {
        return field;
    }
}

