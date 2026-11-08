package com.oopteama.chess.core;

/** 데이터 파일의 한 항목이나 행이 형식에 맞지 않을 때. */
public class MalformedDataException extends DataException {
    /** message는 오류 설명, source는 파일명, line은 1부터 시작하는 행 번호(모르면 0). */
    public MalformedDataException(String message, String source, int line) {
        super(message, source, line);
    }
}

