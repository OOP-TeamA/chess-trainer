package com.oopteama.chess.model;

/**
 * 체스의 한 수입니다. 출발 칸(from)에서 도착 칸(to)으로 움직입니다.
 * 폰이 끝 줄에 도착하면 promotion에 바꿀 기물 기호('Q', 'R', 'B', 'N')를 담습니다.
 */
public record Move(Position from, Position to, char promotion) {

    /** 승진이 없는 수에 쓰는 값. */
    public static final char NO_PROMOTION = ' ';

    public Move {
        if (from.equals(to)) {
            throw new IllegalArgumentException("출발 칸과 도착 칸이 같습니다: " + from);
        }
        if (promotion != NO_PROMOTION && "QRBN".indexOf(promotion) < 0) {
            throw new IllegalArgumentException("승진할 수 없는 기물입니다: " + promotion);
        }
    }

    /** 승진이 없는 보통 수. */
    public Move(Position from, Position to) {
        this(from, to, NO_PROMOTION);
    }

    /** "e2e4", "b7b8q" 같은 좌표 표기를 Move로 바꿉니다. */
    public static Move of(String text) {
        if (text == null) {
            throw new IllegalArgumentException("수 표기가 비어 있습니다.");
        }
        String s = text.trim();
        if (s.length() != 4 && s.length() != 5) {
            throw new IllegalArgumentException("잘못된 수 표기: " + text);
        }
        Position from = Position.of(s.substring(0, 2));
        Position to = Position.of(s.substring(2, 4));
        char promotion = s.length() == 5 ? Character.toUpperCase(s.charAt(4)) : NO_PROMOTION;
        return new Move(from, to, promotion);
    }

    /** 폰 승진 수인지 여부. */
    public boolean isPromotion() {
        return promotion != NO_PROMOTION;
    }

    /** "e2e4" 형식 (승진은 "b7b8q"). */
    @Override
    public String toString() {
        String text = from.toString() + to;
        return isPromotion() ? text + Character.toLowerCase(promotion) : text;
    }
}
