package com.oopteama.chess.core;

import com.oopteama.chess.core.piece.Piece;
import com.oopteama.chess.core.piece.PieceFactory;

/** FEN 문자열과 Position 사이의 변환. 읽을 때는 문법만 검사한다. */
public final class Fen {

    public static final String INITIAL =
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Fen() {}

    /** 문법만 검사한다. 킹 개수 등 규칙 검증은 StandardRules.validate. */
    public static Position parse(String fen) throws FenFormatException {
        if (fen == null) {
            throw new FenFormatException("FEN이 null입니다.", "<input>", 0, 1);
        }
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 4) {
            throw new FenFormatException("필드 수가 부족합니다.", "<input>", 0, 1);
        }

        Position p = Position.raw();

        // 1. 배치
        String[] rows = fields[0].split("/");
        if (rows.length != 8) {
            throw new FenFormatException("랭크 수가 8이 아닙니다.", "<input>", 0, 1);
        }
        for (int r = 0; r < 8; r++) {
            String row = rows[7 - r]; // FEN은 8랭크부터 시작
            int file = 0;
            for (char c : row.toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += (c - '0');
                } else {
                    if (file > 7) {
                        throw new FenFormatException("랭크 길이가 8을 넘습니다.", "<input>", 0, 1);
                    }
                    Piece piece;
                    try {
                        piece = PieceFactory.fromFenChar(c);
                    } catch (IllegalArgumentException e) {
                        throw new FenFormatException("모르는 기물 문자: " + c, "<input>", 0, 1);
                    }
                    p.setAt(new Square(file, r), piece);
                    file++;
                }
            }
            if (file != 8) {
                throw new FenFormatException("랭크 길이가 8이 아닙니다.", "<input>", 0, 1);
            }
        }

        // 2. 차례
        if (fields[1].equals("w")) {
            p.setSideToMove(Side.WHITE);
        } else if (fields[1].equals("b")) {
            p.setSideToMove(Side.BLACK);
        } else {
            throw new FenFormatException("차례 필드가 w/b가 아닙니다: " + fields[1], "<input>", 0, 2);
        }

        // 3. 캐슬링
        p.setCastleRight(Side.WHITE, true, false);
        p.setCastleRight(Side.WHITE, false, false);
        p.setCastleRight(Side.BLACK, true, false);
        p.setCastleRight(Side.BLACK, false, false);
        if (!fields[2].equals("-")) {
            for (char c : fields[2].toCharArray()) {
                switch (c) {
                    case 'K' -> p.setCastleRight(Side.WHITE, true, true);
                    case 'Q' -> p.setCastleRight(Side.WHITE, false, true);
                    case 'k' -> p.setCastleRight(Side.BLACK, true, true);
                    case 'q' -> p.setCastleRight(Side.BLACK, false, true);
                    default -> throw new FenFormatException(
                        "모르는 캐슬링 문자: " + c, "<input>", 0, 3);
                }
            }
        }

        // 4. 앙파상
        if (fields[3].equals("-")) {
            p.setEpTarget(null);
        } else {
            try {
                p.setEpTarget(Square.of(fields[3]));
            } catch (IllegalArgumentException e) {
                throw new FenFormatException("잘못된 앙파상 칸: " + fields[3], "<input>", 0, 4);
            }
        }

        // 5. 하프무브 클럭, 6. 풀무브 번호 (없으면 기본값)
        try {
            p.setHalfmoveClock(fields.length > 4 ? Integer.parseInt(fields[4]) : 0);
        } catch (NumberFormatException e) {
            throw new FenFormatException("하프무브 클럭이 숫자가 아닙니다.", "<input>", 0, 5);
        }
        try {
            p.setFullmoveNumber(fields.length > 5 ? Integer.parseInt(fields[5]) : 1);
        } catch (NumberFormatException e) {
            throw new FenFormatException("풀무브 번호가 숫자가 아닙니다.", "<input>", 0, 6);
        }

        return p;
    }

    /** 국면을 6필드 FEN 문자열로 쓴다. */
    public static String write(Position p) {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            int empty = 0;
            for (int f = 0; f < 8; f++) {
                Piece piece = p.at(new Square(f, r));
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(piece.fenChar());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (r > 0) {
                sb.append('/');
            }
        }
        sb.append(' ').append(p.sideToMove() == Side.WHITE ? 'w' : 'b');

        StringBuilder castle = new StringBuilder();
        if (p.canCastle(Side.WHITE, true)) castle.append('K');
        if (p.canCastle(Side.WHITE, false)) castle.append('Q');
        if (p.canCastle(Side.BLACK, true)) castle.append('k');
        if (p.canCastle(Side.BLACK, false)) castle.append('q');
        sb.append(' ').append(castle.length() == 0 ? "-" : castle.toString());

        sb.append(' ').append(p.enPassantTarget().map(Square::name).orElse("-"));
        sb.append(' ').append(p.halfmoveClock());
        sb.append(' ').append(p.fullmoveNumber());
        return sb.toString();
    }
}

