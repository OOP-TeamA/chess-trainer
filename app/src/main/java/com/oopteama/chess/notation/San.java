package com.oopteama.chess.notation;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;
import com.oopteama.chess.core.rules.RuleSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SAN(대수 표기)과 {@link Move} 사이의 변환. 출발 칸이 생략되므로 해석하려면 그 국면의 합법 수 목록이
 * 필요하며, 엔진의 {@link RuleSet} 위에서만 동작한다.
 */
public final class San {

    /** [기물][출발 파일][출발 랭크][x]도착칸[=승진]. 캐슬링은 따로 처리한다. */
    private static final Pattern MOVE =
        Pattern.compile("^([KQRBN])?([a-h])?([1-8])?(x)?([a-h][1-8])(?:=?([QRBN]))?$");

    /** 문자열 끝의 평가 기호. */
    private static final Pattern EVAL_TAIL = Pattern.compile("(!!|\\?\\?|!\\?|\\?!|!|\\?)$");

    private San() {}

    /**
     * 문자열 끝의 평가 기호를 돌려준다.
     *
     * @param san SAN 문자열(예: {@code Qb8+!!})
     * @return {@code !!}, {@code !}, {@code ?}, {@code ??}, {@code !?}, {@code ?!} 중 하나. 없으면 빈 문자열
     */
    public static String suffixOf(String san) {
        Objects.requireNonNull(san, "san");
        Matcher m = EVAL_TAIL.matcher(san);
        return m.find() ? m.group(1) : "";
    }

    /**
     * SAN 문자열을 그 국면의 합법 수로 해석한다. {@code +}, {@code #}, 평가 기호는 떼어내고 검사하지 않는다.
     * 잡기 표시 {@code x}는 정확히 지켜야 한다: 잡는 수에는 있어야 하고 잡지 않는 수에는 없어야 한다
     * (어긋나면 {@code ILLEGAL}). 폰이 잡을 때는 출발 파일이 필요하다. {@code O-O}와 {@code 0-0}, 승진의
     * {@code =} 생략({@code e8Q})은 허용한다.
     *
     * @param pos 수를 두기 전의 국면(바뀌지 않는다)
     * @param rules 합법 수를 만들 규칙
     * @param san 예: {@code Nbd7}, {@code exd5}, {@code O-O}, {@code e8=Q+}, {@code Qb8+!!}
     * @return 해당하는 합법 수
     * @throws SanException 문법 오류({@code SYNTAX}), 합법 수가 아님({@code ILLEGAL}), 둘 이상 해당({@code AMBIGUOUS})
     */
    public static Move parse(Position pos, RuleSet rules, String san) throws SanException {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(san, "san");

        String body = stripDecorations(trimBlanks(san));
        if (body.isEmpty()) {
            throw new SanException(SanException.Reason.SYNTAX, san, "빈 SAN입니다.");
        }
        List<Move> legal = rules.legalMoves(pos);

        if (body.equals("O-O") || body.equals("0-0")) {
            return castle(legal, MoveKind.CASTLE_SHORT, san);
        }
        if (body.equals("O-O-O") || body.equals("0-0-0")) {
            return castle(legal, MoveKind.CASTLE_LONG, san);
        }

        Matcher m = MOVE.matcher(body);
        if (!m.matches()) {
            throw new SanException(SanException.Reason.SYNTAX, san, "SAN 문법이 아닙니다: " + san);
        }
        PieceType piece = m.group(1) == null ? PieceType.PAWN : pieceOf(m.group(1).charAt(0));
        Integer fromFile = m.group(2) == null ? null : m.group(2).charAt(0) - 'a';
        Integer fromRank = m.group(3) == null ? null : m.group(3).charAt(0) - '1';
        boolean capture = m.group(4) != null;
        Square to = Square.of(m.group(5));
        PieceType promotion = m.group(6) == null ? null : pieceOf(m.group(6).charAt(0));

        if (piece != PieceType.PAWN && promotion != null) {
            throw new SanException(SanException.Reason.SYNTAX, san, "폰이 아닌 기물은 승진할 수 없습니다: " + san);
        }
        if (piece == PieceType.PAWN && fromFile == null && (capture || fromRank != null)) {
            throw new SanException(SanException.Reason.SYNTAX, san, "폰이 잡을 때는 출발 파일이 필요합니다: " + san);
        }
        if (piece == PieceType.PAWN && fromFile != null && !capture && fromRank == null) {
            throw new SanException(SanException.Reason.SYNTAX, san, "잡지 않는 폰 수에는 출발 파일을 쓰지 않습니다: " + san);
        }

        List<Move> candidates = new ArrayList<>();
        for (Move mv : legal) {
            Piece moving = pos.at(mv.from());
            if (moving == null || moving.type() != piece || !mv.to().equals(to)) {
                continue;
            }
            if (mv.kind() == MoveKind.CASTLE_SHORT || mv.kind() == MoveKind.CASTLE_LONG) {
                continue; // 캐슬링은 O-O, O-O-O로만 쓴다.
            }
            if (fromFile != null && mv.from().file() != fromFile) {
                continue;
            }
            if (fromRank != null && mv.from().rank() != fromRank) {
                continue;
            }
            if (mv.isCapture() != capture || !Objects.equals(mv.promotion(), promotion)) {
                continue;
            }
            candidates.add(mv);
        }
        if (candidates.isEmpty()) {
            throw new SanException(SanException.Reason.ILLEGAL, san, "이 국면에서 둘 수 없는 수입니다: " + san);
        }
        if (candidates.size() > 1) {
            List<String> options = candidates.stream().map(Move::uci).toList();
            throw new SanException(SanException.Reason.AMBIGUOUS, san,
                "여러 수가 해당합니다(출발 칸을 적어야 합니다): " + san + " → " + options);
        }
        return candidates.get(0);
    }

    /**
     * 수를 정규 SAN으로 쓴다. 모호성 표기는 최소한으로(출발 파일 → 출발 랭크 → 둘 다) 붙이고,
     * 체크는 {@code +}, 체크메이트는 {@code #}를 붙인다. 평가 기호는 붙이지 않는다.
     *
     * @param before 수를 두기 전의 국면(바뀌지 않는다)
     * @param rules 합법 수를 만들 규칙
     * @param move before의 합법 수
     * @return 정규 SAN
     * @throws IllegalArgumentException move가 before의 합법 수가 아닐 때
     */
    public static String write(Position before, RuleSet rules, Move move) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(move, "move");

        List<Move> legal = rules.legalMoves(before);
        if (!legal.contains(move)) {
            throw new IllegalArgumentException("합법 수가 아닙니다: " + move.uci());
        }

        StringBuilder sb = new StringBuilder();
        if (move.kind() == MoveKind.CASTLE_SHORT) {
            sb.append("O-O");
        } else if (move.kind() == MoveKind.CASTLE_LONG) {
            sb.append("O-O-O");
        } else {
            PieceType type = before.at(move.from()).type();
            if (type == PieceType.PAWN) {
                if (move.isCapture()) {
                    sb.append(fileChar(move.from()));
                }
            } else {
                sb.append(letter(type)).append(disambiguation(before, legal, move, type));
            }
            if (move.isCapture()) {
                sb.append('x');
            }
            sb.append(move.to().name());
            if (move.promotion() != null) {
                sb.append('=').append(letter(move.promotion()));
            }
        }

        Position after = before.copy();
        after.make(move);
        if (rules.inCheck(after)) {
            sb.append(rules.legalMoves(after).isEmpty() ? '#' : '+');
        }
        return sb.toString();
    }

    private static Move castle(List<Move> legal, MoveKind kind, String san) throws SanException {
        for (Move mv : legal) {
            if (mv.kind() == kind) {
                return mv;
            }
        }
        throw new SanException(SanException.Reason.ILLEGAL, san, "이 국면에서는 캐슬링할 수 없습니다: " + san);
    }

    /** 앞뒤 공백을 자른다. 줄바꿈 없는 공백(NBSP)과 전각 공백도 공백으로 본다. */
    private static String trimBlanks(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isBlank(s.charAt(start))) {
            start++;
        }
        while (end > start && isBlank(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(start, end);
    }

    private static boolean isBlank(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    /** 끝의 평가 기호 하나와 +/# 하나를 차례로 떼어낸다. */
    private static String stripDecorations(String s) {
        String t = EVAL_TAIL.matcher(s).replaceFirst("");
        if (t.endsWith("+") || t.endsWith("#")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String disambiguation(Position before, List<Move> legal, Move move, PieceType type) {
        boolean any = false;
        boolean sameFile = false;
        boolean sameRank = false;
        for (Move other : legal) {
            if (other.equals(move) || other.from().equals(move.from()) || !other.to().equals(move.to())) {
                continue;
            }
            Piece p = before.at(other.from());
            if (p == null || p.type() != type) {
                continue;
            }
            any = true;
            sameFile |= other.from().file() == move.from().file();
            sameRank |= other.from().rank() == move.from().rank();
        }
        if (!any) {
            return "";
        }
        if (!sameFile) {
            return String.valueOf(fileChar(move.from()));
        }
        if (!sameRank) {
            return String.valueOf(rankChar(move.from()));
        }
        return "" + fileChar(move.from()) + rankChar(move.from());
    }

    private static char fileChar(Square s) {
        return (char) ('a' + s.file());
    }

    private static char rankChar(Square s) {
        return (char) ('1' + s.rank());
    }

    private static char letter(PieceType type) {
        return switch (type) {
            case KING -> 'K';
            case QUEEN -> 'Q';
            case ROOK -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
            case PAWN -> throw new IllegalArgumentException("폰은 기물 글자가 없습니다.");
        };
    }

    private static PieceType pieceOf(char c) {
        return switch (c) {
            case 'K' -> PieceType.KING;
            case 'Q' -> PieceType.QUEEN;
            case 'R' -> PieceType.ROOK;
            case 'B' -> PieceType.BISHOP;
            case 'N' -> PieceType.KNIGHT;
            default -> throw new IllegalArgumentException("모르는 기물 글자: " + c);
        };
    }
}
