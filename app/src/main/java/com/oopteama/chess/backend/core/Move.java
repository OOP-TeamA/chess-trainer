package com.oopteama.chess.backend.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 이동 좌표({@link Move})와 보드에 적용 완료된 이동 기록({@link MoveRecord})을 담는다.
 *
 * @param from      출발 칸 인덱스 (0=a1 ~ 63=h8)
 * @param to        도착 칸 인덱스 (0=a1 ~ 63=h8)
 * @param promotion 승진 기물 대문자('Q','R','B','N'), 승진 아니면 '-'
 */
public record Move(int from, int to, char promotion) {

    private static final Pattern UCI_PATTERN = Pattern.compile("[a-h][1-8][a-h][1-8][qrbnQRBN]?");

    public Move {
        BoardPosition.checkSquare(from);
        BoardPosition.checkSquare(to);
        promotion = Character.toUpperCase(promotion);
        if (from == to || "-QRBN".indexOf(promotion) < 0) {
            throw new IllegalArgumentException("Invalid move/pawn promotion: " + promotion);
        }
    }

    public Move(int from, int to) {
        this(from, to, '-');
    }

    /** UCI 문자열("e2e4", "a7a8q")을 파싱해 Move 객체를 만든다. */
    public static Move fromUci(String uci) {
        Objects.requireNonNull(uci, "uci");
        if (!UCI_PATTERN.matcher(uci).matches()) {
            throw new IllegalArgumentException("Invalid UCI format: " + uci);
        }
        return new Move(
                BoardPosition.parseSquare(uci.substring(0, 2)),
                BoardPosition.parseSquare(uci.substring(2, 4)),
                uci.length() == 5 ? uci.charAt(4) : '-'
        );
    }

    /** 이 이동을 소문자 UCI 문자열("e2e4", "a7a8q")로 변환한다. */
    public String uci() {
        return BoardPosition.formatSquare(from) + BoardPosition.formatSquare(to)
                + (promotion == '-' ? "" : String.valueOf(Character.toLowerCase(promotion)));
    }

    /** 한 수의 이동 유형. */
    public enum MoveKind {
        NORMAL,
        CAPTURE,
        EN_PASSANT,
        CASTLE_SHORT,
        CASTLE_LONG;

        /** 일반 잡기 또는 앙파상 잡기인지 반환한다. */
        public boolean isCapture() {
            return this == CAPTURE || this == EN_PASSANT;
        }
    }

    /** 실제로 보드에 적용 완료된 한 수의 불변 기록 객체. */
    public record MoveRecord(
            Move move,
            MoveKind kind,
            boolean check,
            boolean checkmate,
            String san
    ) {
        /** 일반 잡기 또는 앙파상 잡기인지 반환한다. */
        public boolean isCapture() {
            return kind.isCapture();
        }
    }
}
