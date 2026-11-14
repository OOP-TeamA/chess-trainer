package com.oopteama.chess.notation;

import com.oopteama.chess.core.DataException;

/** PGN 기보 파일의 형식 오류. 파일 이름과 행 번호를 담는다. */
public final class PgnFormatException extends DataException {

    /**
     * 예외를 만든다.
     *
     * @param message 사람이 읽을 설명
     * @param source 파일 이름(문자열 입력이면 {@code <input>})
     * @param line 오류가 난 행(1부터, 모르면 0)
     */
    public PgnFormatException(String message, String source, int line) {
        super(message, source, line);
    }
}
