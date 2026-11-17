package com.oopteama.chess.backend.core;

import com.oopteama.chess.backend.core.BoardPosition.Piece;
import com.oopteama.chess.backend.core.BoardPosition.Side;
import com.oopteama.chess.backend.core.Move.MoveKind;
import com.oopteama.chess.backend.core.Move.MoveRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FEN 및 표준 대수 표기(SAN)의 파싱, 검증, 직렬화를 담당한다.
 */
public final class ChessNotation {

    private static final Pattern CASTLE_FEN = Pattern.compile("K?Q?k?q?");
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");
    private static final Pattern SAN_PATTERN = Pattern.compile(
            "^([KQRBN])?([a-h])?([1-8])?(x)?([a-h][1-8])(?:=([QRBN]))?([+#])?$");
    private static final Pattern CASTLE_SAN = Pattern.compile("^(O-O(?:-O)?)([+#]?)$");

    private ChessNotation() {
    }

    /** FEN 문자열을 파싱해 검증된 {@link BoardPosition}을 반환한다. */
    public static BoardPosition parseFen(String fen) {
        Objects.requireNonNull(fen, "fen");
        String[] fields = fen.trim().split("\\s+");
        if (fields.length != 6) {
            throw new IllegalArgumentException("FEN은 6개 필드여야 한다");
        }
        String[] rows = fields[0].split("/", -1);
        if (rows.length != 8) {
            throw new IllegalArgumentException("FEN은 8개 행이어야 한다");
        }

        Piece[] board = new Piece[64];
        Arrays.fill(board, Piece.EMPTY);
        for (int row = 0; row < 8; row++) {
            int file = 0;
            for (char c : rows[row].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += c - '0';
                } else if (file < 8) {
                    board[BoardPosition.squareOf(file++, 7 - row)] = Piece.fromFen(c);
                } else {
                    throw new IllegalArgumentException("한 행은 8칸이어야 한다");
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("한 행은 8칸이어야 한다");
            }
        }

        Side turn = switch (fields[1]) {
            case "w" -> Side.WHITE;
            case "b" -> Side.BLACK;
            default -> throw new IllegalArgumentException("차례는 w/b여야 한다");
        };

        int rights = 0;
        if (!fields[2].equals("-")) {
            if (!CASTLE_FEN.matcher(fields[2]).matches()) {
                throw new IllegalArgumentException("캐슬링 권리 형식 오류");
            }
            for (char c : fields[2].toCharArray()) {
                rights |= switch (c) {
                    case 'K' -> BoardPosition.WK;
                    case 'Q' -> BoardPosition.WQ;
                    case 'k' -> BoardPosition.BK;
                    default -> BoardPosition.BQ;
                };
            }
        }
        int enPassantSquare = fields[3].equals("-") ? -1 : BoardPosition.parseSquare(fields[3]);
        if (!DIGITS.matcher(fields[4]).matches() || !DIGITS.matcher(fields[5]).matches()) {
            throw new IllegalArgumentException("FEN 카운터는 정수여야 한다");
        }
        int halfmoveClock = Integer.parseInt(fields[4]);
        int fullmove = Integer.parseInt(fields[5]);
        if (fullmove < 1) {
            throw new IllegalArgumentException("수 번호는 1 이상이어야 한다");
        }
        BoardPosition pos = new BoardPosition(board, turn, rights, enPassantSquare, halfmoveClock, fullmove);
        validatePosition(pos);
        return pos;
    }

    /** 6개 필드를 모두 포함한 표준 FEN 문자열을 생성한다. */
    public static String formatFen(BoardPosition pos) {
        return positionKey(pos, false) + " " + pos.halfmoveClock() + " " + pos.fullmoveNumber();
    }

    /**
     * FEN의 앞 4개 필드(배치·차례·캐슬링·앙파상)를 조립한다.
     * {@code effectiveEpOnly=true}이면 실제 앙파상 잡기 가능할 때만 EP 칸을 유지해 3회 반복 키로 쓴다.
     */
    public static String positionKey(BoardPosition pos, boolean effectiveEpOnly) {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                Piece p = pos.pieceAt(BoardPosition.squareOf(file, rank));
                if (p == Piece.EMPTY) {
                    empty++;
                } else {
                    if (empty != 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(p.fen());
                }
            }
            if (empty != 0) sb.append(empty);
            if (rank != 0) sb.append('/');
        }
        sb.append(pos.turn() == Side.WHITE ? " w " : " b ");
        int cr = pos.castlingRights();
        String rights = ((cr & BoardPosition.WK) != 0 ? "K" : "")
                + ((cr & BoardPosition.WQ) != 0 ? "Q" : "")
                + ((cr & BoardPosition.BK) != 0 ? "k" : "")
                + ((cr & BoardPosition.BQ) != 0 ? "q" : "");
        sb.append(rights.isEmpty() ? "-" : rights).append(' ');
        int ep = pos.enPassantSquare();
        if (ep >= 0 && effectiveEpOnly && !pos.hasLegalEnPassant()) {
            ep = -1;
        }
        return sb.append(ep < 0 ? "-" : BoardPosition.formatSquare(ep)).toString();
    }

    /** 보드를 변경하지 않고 정규 SAN을 합법 수로 변환한다. */
    public static Move parseSan(BoardPosition position, String san) {
        if (san == null || san.isBlank()) {
            throw new IllegalArgumentException("Empty SAN");
        }
        String input = san.trim();
        List<Move> legal = position.legalMoves();
        Move move = findMoveForSan(position, legal, input);
        MoveRecord record = recordSan(position, position.simulateLegalMove(move), move, legal);
        if (!input.equals(record.san())) {
            throw new IllegalArgumentException("정규 SAN은 " + record.san() + "이다");
        }
        return move;
    }

    /** 이동 전후 국면을 변경하지 않고 정규 SAN을 포함한 기록을 생성한다. */
    public static MoveRecord recordSan(BoardPosition pos, BoardPosition next, Move move, List<Move> legal) {
        int from = move.from();
        int to = move.to();
        Piece piece = pos.pieceAt(from);
        char type = piece.type();
        boolean castle = type == 'K' && Math.abs(to - from) == 2;
        boolean ep = type == 'P'
                && to == pos.enPassantSquare()
                && pos.pieceAt(to) == Piece.EMPTY
                && BoardPosition.file(from) != BoardPosition.file(to);
        boolean capture = pos.pieceAt(to) != Piece.EMPTY || ep;

        MoveKind kind = castle ? (to > from ? MoveKind.CASTLE_SHORT : MoveKind.CASTLE_LONG)
                : ep ? MoveKind.EN_PASSANT
                : capture ? MoveKind.CAPTURE
                : MoveKind.NORMAL;

        String prefix = buildSanPrefix(pos, move, piece, kind, legal);
        boolean check = next.isCheck();
        boolean checkmate = check && next.legalMoves().isEmpty();
        String san = prefix + (checkmate ? "#" : (check ? "+" : ""));
        return new MoveRecord(move, kind, check, checkmate, san);
    }

    private static void validatePosition(BoardPosition pos) {
        int whiteKings = 0;
        int blackKings = 0;
        for (int s = 0; s < 64; s++) {
            Piece p = pos.pieceAt(s);
            if (p == Piece.WK) {
                whiteKings++;
            } else if (p == Piece.BK) {
                blackKings++;
            } else if (p.type() == 'P' && (BoardPosition.rank(s) == 0 || BoardPosition.rank(s) == 7)) {
                throw new IllegalArgumentException("폰은 1/8행에 있을 수 없다");
            }
        }
        if (whiteKings != 1 || blackKings != 1) {
            throw new IllegalArgumentException("양쪽 킹이 하나씩 필요하다");
        }
        Side turn = pos.turn();
        if (pos.isSquareAttacked(pos.kingSquare(turn.opposite()), turn)) {
            throw new IllegalArgumentException("직전에 둔 쪽의 킹이 체크인 국면");
        }
        for (int corner : new int[]{0, 7, 56, 63}) {
            if ((pos.castlingRights() & BoardPosition.cornerRight(corner)) == 0) continue;
            Side side = corner < 8 ? Side.WHITE : Side.BLACK;
            if (!pos.pieceAt(corner).is(side, 'R')
                    || !pos.pieceAt(side == Side.WHITE ? 4 : 60).is(side, 'K')) {
                throw new IllegalArgumentException("캐슬링 권리와 킹/룩 배치가 다르다");
            }
        }
        int ep = pos.enPassantSquare();
        if (ep >= 0) {
            int dir = turn == Side.WHITE ? 8 : -8;
            if (BoardPosition.rank(ep) != (turn == Side.WHITE ? 5 : 2)
                    || pos.pieceAt(ep) != Piece.EMPTY
                    || !pos.pieceAt(ep - dir).is(turn.opposite(), 'P')
                    || pos.pieceAt(ep + dir) != Piece.EMPTY
                    || pos.halfmoveClock() != 0) {
                throw new IllegalArgumentException("앙파상 대상과 직전 폰 이동 상태가 다르다");
            }
        }
    }

    private static Move findMoveForSan(BoardPosition pos, List<Move> legal, String input) {
        boolean castle;
        char type;
        int fromFile = -1;
        int fromRank = -1;
        int to;
        boolean capture = false;
        char promotion = '-';

        Matcher cm = CASTLE_SAN.matcher(input);
        if (cm.matches()) {
            castle = true;
            type = 'K';
            int home = pos.turn() == Side.WHITE ? 4 : 60;
            to = home + (cm.group(1).equals("O-O") ? 2 : -2);
        } else {
            Matcher m = SAN_PATTERN.matcher(input);
            if (!m.matches()) {
                throw new IllegalArgumentException("Wrong SAN syntax: " + input);
            }
            castle = false;
            type = m.group(1) == null ? 'P' : m.group(1).charAt(0);
            fromFile = m.group(2) == null ? -1 : m.group(2).charAt(0) - 'a';
            fromRank = m.group(3) == null ? -1 : m.group(3).charAt(0) - '1';
            capture = m.group(4) != null;
            to = BoardPosition.parseSquare(m.group(5));
            promotion = m.group(6) == null ? '-' : m.group(6).charAt(0);
        }

        List<Move> matches = new ArrayList<>();
        for (Move m : legal) {
            Piece p = pos.pieceAt(m.from());
            boolean isCastle = p.type() == 'K' && Math.abs(m.to() - m.from()) == 2;
            if (isCastle != castle || p.type() != type || m.to() != to) continue;
            if (fromFile >= 0 && BoardPosition.file(m.from()) != fromFile) continue;
            if (fromRank >= 0 && BoardPosition.rank(m.from()) != fromRank) continue;
            boolean isCapture = pos.pieceAt(to) != Piece.EMPTY
                    || (p.type() == 'P' && BoardPosition.file(m.from()) != BoardPosition.file(to));
            if (isCapture != capture || m.promotion() != promotion) continue;
            matches.add(m);
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("이 국면에서 둘 수 없는 SAN: " + input);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("출발 기물을 구분해야 한다: " + input);
        }
        return matches.get(0);
    }

    private static String buildSanPrefix(
            BoardPosition pos,
            Move move,
            Piece piece,
            MoveKind kind,
            List<Move> legal
    ) {
        if (kind == MoveKind.CASTLE_SHORT) return "O-O";
        if (kind == MoveKind.CASTLE_LONG) return "O-O-O";

        int from = move.from();
        int to = move.to();
        char type = piece.type();
        boolean capture = kind == MoveKind.CAPTURE || kind == MoveKind.EN_PASSANT;
        StringBuilder sb = new StringBuilder();
        if (type == 'P') {
            if (capture) sb.append((char) ('a' + BoardPosition.file(from)));
        } else {
            sb.append(type);
            boolean ambiguous = false;
            boolean sameFile = false;
            boolean sameRank = false;
            for (Move other : legal) {
                if (other.from() == from || other.to() != to || pos.pieceAt(other.from()) != piece) continue;
                ambiguous = true;
                sameFile |= BoardPosition.file(other.from()) == BoardPosition.file(from);
                sameRank |= BoardPosition.rank(other.from()) == BoardPosition.rank(from);
            }
            if (ambiguous) {
                if (!sameFile) sb.append((char) ('a' + BoardPosition.file(from)));
                else if (!sameRank) sb.append((char) ('1' + BoardPosition.rank(from)));
                else sb.append(BoardPosition.formatSquare(from));
            }
        }
        if (capture) sb.append('x');
        sb.append(BoardPosition.formatSquare(to));
        if (move.promotion() != '-') sb.append('=').append(move.promotion());
        return sb.toString();
    }
}
