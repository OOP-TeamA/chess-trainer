package com.oopteama.chess.core;

/** 데이터 파일과 표기 문자열의 형식 오류. 모든 패키지의 검사 예외가 이것을 상속. */
public abstract class DataException extends Exception {

    private final String source;
    private final int line;

    protected DataException(String message, String source, int line) {
        super(message);
        this.source = source;
        this.line = line;
    }

    /** 파일명. 문자열 입력이면 "<input>". */
    public String source() {
        return source;
    }

    /** 1부터. 모르면 0. */
    public int line() {
        return line;
    }
}

