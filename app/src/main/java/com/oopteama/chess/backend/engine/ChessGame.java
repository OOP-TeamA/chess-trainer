package com.oopteama.chess.backend.engine;

import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.engine.Piece.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한 판의 체스 대국 흐름(시작 국면, 수순 이력, 무르기, 종료 판정, SAN 표기)을 관리합니다.
 */
public final class ChessGame {

    public enum Status {
        PLAYING,
        CHECK,
        CHECKMATE,
        STALEMATE,
        DRAW_INSUFFICIENT,
        DRAW_FIFTY_MOVE,
        DRAW_REPETITION,
        RESIGNED_WHITE,
        RESIGNED_BLACK;

        public boolean isOver() {
            return this != PLAYING && this != CHECK;
        }
    }

    private record Entry(Move.Record record, Board board) {
    }

    private static final Pattern CASTLE_SAN = Pattern.compile("^(O-O(?:-O)?)([+#]?)$");
    private static final Pattern MOVE_SAN = Pattern.compile(
            "^([KQRBN])?([a-h])?([1-8])?(x)?([a-h][1-8])(?:=([QRBN]))?([+#])?$"
    );

    private Board startBoard;
    private final List<Entry> history = new ArrayList<>();
    private Side resignedSide;

    public ChessGame() {
        this(Board.initial());
    }

    public ChessGame(Board startBoard) {
        this.startBoard = Objects.requireNonNull(startBoard, "startBoard");
    }

    public static ChessGame fromFen(String fen) {
        return new ChessGame(Board.fromFen(fen));
    }

    public static ChessGame fromSandboxFen(String fen) {
        return new ChessGame(Board.fromSandboxFen(fen));
    }

    public Board board() {
        return history.isEmpty() ? startBoard : history.getLast().board();
    }

    public Board startBoard() {
        return startBoard;
    }

    public Side turn() {
        return board().turn();
    }

    public List<Move.Record> history() {
        return history.stream().map(Entry::record).toList();
    }

    public Optional<Move.Record> lastMove() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.getLast().record());
    }

    public boolean isOver() {
        return status().isOver();
    }

    public Status status() {
        if (resignedSide == Side.WHITE) {
            return Status.RESIGNED_WHITE;
        }
        if (resignedSide == Side.BLACK) {
            return Status.RESIGNED_BLACK;
        }
        Board current = board();
        if (current.isCheckmate()) {
            return Status.CHECKMATE;
        }
        if (current.isStalemate()) {
            return Status.STALEMATE;
        }
        if (!current.isSandbox()) {
            if (current.isInsufficientMaterial()) {
                return Status.DRAW_INSUFFICIENT;
            }
            if (current.halfmoveClock() >= 100) {
                return Status.DRAW_FIFTY_MOVE;
            }
            if (repetitionCount() >= 3) {
                return Status.DRAW_REPETITION;
            }
        }
        return current.isInCheck() ? Status.CHECK : Status.PLAYING;
    }

    public Side winner() {
        return switch (status()) {
            case CHECKMATE -> turn().opposite();
            case RESIGNED_WHITE -> Side.BLACK;
            case RESIGNED_BLACK -> Side.WHITE;
            default -> null;
        };
    }

    public String statusText() {
        return switch (status()) {
            case PLAYING -> turn().label() + " 차례";
            case CHECK -> turn().label() + " 차례 · 체크!";
            case CHECKMATE -> "체크메이트! " + turn().opposite().label() + " 승리";
            case STALEMATE -> "스테일메이트 · 무승부";
            case DRAW_INSUFFICIENT -> "기물 부족 · 무승부";
            case DRAW_FIFTY_MOVE -> "50수 규칙 · 무승부";
            case DRAW_REPETITION -> "3회 동형 반복 · 무승부";
            case RESIGNED_WHITE -> "백 기권 · 흑 승리";
            case RESIGNED_BLACK -> "흑 기권 · 백 승리";
        };
    }

    public Move.Record play(Move move) {
        Objects.requireNonNull(move, "move");
        if (isOver()) {
            throw new IllegalStateException("이미 종료된 대국입니다: " + status());
        }
        Board before = board();
        List<Move> legal = before.legalMoves();
        if (!legal.contains(move)) {
            throw new IllegalArgumentException("둘 수 없는 수입니다: " + move.uci());
        }
        Move.Kind kind = before.classifyMove(move);
        Piece moved = before.pieceAt(move.from());
        Piece captured = before.capturedPiece(move);
        Board after = before.applyMove(move);
        boolean check = after.isInCheck();
        boolean mate = check && after.legalMoves().isEmpty();
        String san = formatSan(before, move, kind, legal, check, mate);
        Move.Record record = new Move.Record(move, kind, moved, captured, check, mate, san);
        history.add(new Entry(record, after));
        return record;
    }

    public Move.Record playUci(String uci) {
        return play(Move.fromUci(uci));
    }

    public Move.Record playSan(String san) {
        return play(parseSan(board(), san));
    }

    public boolean undo() {
        if (history.isEmpty()) {
            return false;
        }
        history.removeLast();
        resignedSide = null;
        return true;
    }

    public int undo(int count) {
        int undone = 0;
        while (undone < count && undo()) {
            undone++;
        }
        return undone;
    }

    public void resign(Side side) {
        if (!isOver()) {
            this.resignedSide = Objects.requireNonNull(side, "side");
        }
    }

    public void reset() {
        history.clear();
        resignedSide = null;
    }

    public void reset(Board newStart) {
        this.startBoard = Objects.requireNonNull(newStart, "newStart");
        history.clear();
        resignedSide = null;
    }

    public int repetitionCount() {
        String key = board().positionKey(true);
        int count = startBoard.positionKey(true).equals(key) ? 1 : 0;
        for (Entry entry : history) {
            if (entry.board().positionKey(true).equals(key)) {
                count++;
            }
        }
        return count;
    }

    public List<Piece> capturedBy(Side capturer) {
        return history.stream()
                .map(Entry::record)
                .filter(r -> r.moved().side() == capturer && !r.captured().isEmpty())
                .map(Move.Record::captured)
                .sorted(Comparator.comparingInt((Piece p) -> p.type().value()).reversed())
                .toList();
    }

    // ---- SAN 포맷 & 파싱 ----

    public static String formatSan(
            Board before,
            Move move,
            Move.Kind kind,
            List<Move> legal,
            boolean check,
            boolean checkmate
    ) {
        StringBuilder sb = new StringBuilder();
        if (kind == Move.Kind.CASTLE_SHORT) {
            sb.append("O-O");
        } else if (kind == Move.Kind.CASTLE_LONG) {
            sb.append("O-O-O");
        } else {
            Piece moved = before.pieceAt(move.from());
            if (moved.type() == Type.PAWN) {
                if (kind.isCapture()) {
                    sb.append((char) ('a' + Board.file(move.from()))).append('x');
                }
            } else {
                sb.append(moved.type().symbol());
                appendDisambiguation(sb, before, move, moved, legal);
                if (kind.isCapture()) {
                    sb.append('x');
                }
            }
            sb.append(Board.formatSquare(move.to()));
            if (move.promotion() != null) {
                sb.append('=').append(move.promotion().symbol());
            }
        }
        if (checkmate) {
            sb.append('#');
        } else if (check) {
            sb.append('+');
        }
        return sb.toString();
    }

    private static void appendDisambiguation(
            StringBuilder sb,
            Board before,
            Move move,
            Piece moved,
            List<Move> legal
    ) {
        boolean samePiece = false;
        boolean sameFile = false;
        boolean sameRank = false;
        int fromFile = Board.file(move.from());
        int fromRank = Board.rank(move.from());

        for (Move other : legal) {
            if (other.from() != move.from()
                    && other.to() == move.to()
                    && before.pieceAt(other.from()) == moved) {
                samePiece = true;
                if (Board.file(other.from()) == fromFile) sameFile = true;
                if (Board.rank(other.from()) == fromRank) sameRank = true;
            }
        }
        if (!samePiece) {
            return;
        }
        if (!sameFile) {
            sb.append((char) ('a' + fromFile));
        } else if (!sameRank) {
            sb.append((char) ('1' + fromRank));
        } else {
            sb.append(Board.formatSquare(move.from()));
        }
    }

    public static Move parseSan(Board board, String san) {
        if (san == null || san.isBlank()) {
            throw new IllegalArgumentException("SAN 문자열이 비어 있습니다.");
        }
        String clean = san.trim().replaceAll("[!?]+$", "");
        List<Move> legal = board.legalMoves();

        Matcher castle = CASTLE_SAN.matcher(clean);
        if (castle.matches()) {
            Move.Kind expected = "O-O".equals(castle.group(1))
                    ? Move.Kind.CASTLE_SHORT
                    : Move.Kind.CASTLE_LONG;
            return legal.stream()
                    .filter(m -> board.classifyMove(m) == expected)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("현재 캐슬링할 수 없습니다: " + san));
        }

        Matcher m = MOVE_SAN.matcher(clean);
        if (!m.matches()) {
            throw new IllegalArgumentException("올바르지 않은 SAN 형식입니다: " + san);
        }
        Type pieceType = m.group(1) == null ? Type.PAWN : Type.fromSymbol(m.group(1).charAt(0));
        int fromFile = m.group(2) == null ? -1 : m.group(2).charAt(0) - 'a';
        int fromRank = m.group(3) == null ? -1 : m.group(3).charAt(0) - '1';
        int target = Board.parseSquare(m.group(5));
        Type promo = m.group(6) == null ? null : Type.fromSymbol(m.group(6).charAt(0));

        List<Move> matches = new ArrayList<>();
        for (Move candidate : legal) {
            if (candidate.to() != target || candidate.promotion() != promo) {
                continue;
            }
            if (board.pieceAt(candidate.from()).type() != pieceType) {
                continue;
            }
            if (fromFile >= 0 && Board.file(candidate.from()) != fromFile) {
                continue;
            }
            if (fromRank >= 0 && Board.rank(candidate.from()) != fromRank) {
                continue;
            }
            matches.add(candidate);
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException("SAN과 일치하는 유일한 합법 수가 없습니다: " + san);
        }
        return matches.getFirst();
    }
}
