package com.oopteama.chess.backend.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 체스 한 국면과 규칙 판정(합법 수 생성, 공격·체크·기물 부족 판정, 칸 좌표 계산).
 * 코어 외부에서는 조회만 가능하고, 이동은 복사한 국면에 적용한다.
 */
public final class BoardPosition {

    public static final int WK = 1;
    public static final int WQ = 2;
    public static final int BK = 4;
    public static final int BQ = 8;

    private static final int[][] KNIGHT_STEPS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] STRAIGHTS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] KING_STEPS = {
            {1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    /** 체스 플레이어 진영(백/흑). */
    public enum Side {
        WHITE,
        BLACK;

        /** 상대 진영을 반환한다. */
        public Side opposite() {
            return this == WHITE ? BLACK : WHITE;
        }
    }

    /** 현재 국면의 규칙상 진행 상태. */
    public enum Status {
        PLAYING,
        CHECK,
        CHECKMATE,
        STALEMATE
    }

    /**
     * 보드 한 칸의 기물 상태.
     * 각 상수는 FEN 표기 문자와 1:1로 대응한다.
     */
    public enum Piece {
        EMPTY('.'),
        WP('P'), WN('N'), WB('B'), WR('R'), WQ('Q'), WK('K'),
        BP('p'), BN('n'), BB('b'), BR('r'), BQ('q'), BK('k');

        private final char fen;

        Piece(char fen) {
            this.fen = fen;
        }

        public char fen() {
            return fen;
        }

        /** 진영과 무관한 기물 종류('P','N','B','R','Q','K','.')를 대문자로 반환한다. */
        public char type() {
            return Character.toUpperCase(fen);
        }

        /** 이 기물이 지정한 진영 소속인지 판정한다(빈칸은 항상 false). */
        public boolean belongsTo(Side side) {
            return this != EMPTY && (Character.isUpperCase(fen) == (side == Side.WHITE));
        }

        /** 이 기물이 지정한 진영과 기물 종류에 모두 일치하는지 확인한다. */
        public boolean is(Side side, char type) {
            return belongsTo(side) && type() == type;
        }

        /** 진영과 대문자 기물 종류('P','N','B','R','Q','K')에 해당하는 Piece를 반환한다. */
        public static Piece of(Side side, char type) {
            return fromFen(side == Side.WHITE ? type : Character.toLowerCase(type));
        }

        /** FEN 기물 문자(빈칸 '.' 제외)를 Piece 상수로 변환한다. */
        public static Piece fromFen(char fen) {
            for (Piece piece : values()) {
                if (piece != EMPTY && piece.fen == fen) {
                    return piece;
                }
            }
            throw new IllegalArgumentException("Unknown piece: " + fen);
        }
    }

    private final Piece[] board = new Piece[64];
    private Side turn;
    private int castlingRights;
    private int enPassantSquare;
    private int halfmoveClock;
    private int fullmoveNumber;

    /** 해석한 국면 상태를 복사한다. 체스 규칙상 정합성 검증은 {@link ChessNotation#parseFen}에서 수행한다. */
    public BoardPosition(Piece[] board, Side turn, int castlingRights, int enPassantSquare,
                         int halfmoveClock, int fullmoveNumber) {
        if (board.length != 64) {
            throw new IllegalArgumentException("보드는 64칸이어야 한다");
        }
        for (Piece piece : board) Objects.requireNonNull(piece, "piece");
        if ((castlingRights & ~15) != 0 || enPassantSquare < -1 || enPassantSquare >= 64
                || halfmoveClock < 0 || fullmoveNumber < 1) {
            throw new IllegalArgumentException("국면 상태 범위 오류");
        }
        System.arraycopy(board, 0, this.board, 0, 64);
        this.turn = Objects.requireNonNull(turn, "turn");
        this.castlingRights = castlingRights;
        this.enPassantSquare = enPassantSquare;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
    }

    private BoardPosition(BoardPosition source) {
        System.arraycopy(source.board, 0, board, 0, 64);
        this.turn = source.turn;
        this.castlingRights = source.castlingRights;
        this.enPassantSquare = source.enPassantSquare;
        this.halfmoveClock = source.halfmoveClock;
        this.fullmoveNumber = source.fullmoveNumber;
    }

    /** 대수 표기 칸 이름("e4")을 0~63 인덱스로 변환한다. */
    public static int parseSquare(String name) {
        if (name == null || name.length() != 2
                || name.charAt(0) < 'a' || name.charAt(0) > 'h'
                || name.charAt(1) < '1' || name.charAt(1) > '8') {
            throw new IllegalArgumentException("칸 이름 예: e4");
        }
        return (name.charAt(1) - '1') * 8 + name.charAt(0) - 'a';
    }

    /** 0~63 칸 인덱스를 대수 표기 칸 이름("e4")으로 변환한다. */
    public static String formatSquare(int square) {
        checkSquare(square);
        return "" + (char) ('a' + file(square)) + (char) ('1' + rank(square));
    }

    public static int file(int square) {
        return square % 8;
    }

    public static int rank(int square) {
        return square / 8;
    }

    public static int squareOf(int file, int rank) {
        return rank * 8 + file;
    }

    public static boolean insideSquare(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    public static void checkSquare(int square) {
        if (square < 0 || square >= 64) {
            throw new IllegalArgumentException("칸 인덱스는 0~63: " + square);
        }
    }

    public int kingSquare(Side side) {
        return findKingSquare(side).orElseThrow(() -> new IllegalStateException("킹이 없다: " + side));
    }

    public static int cornerRight(int square) {
        return switch (square) {
            case 0 -> WQ;
            case 7 -> WK;
            case 56 -> BQ;
            case 63 -> BK;
            default -> 0;
        };
    }

    public Side turn() {
        return turn;
    }

    public int castlingRights() {
        return castlingRights;
    }

    public int enPassantSquare() {
        return enPassantSquare;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    public Piece pieceAt(int square) {
        checkSquare(square);
        return board[square];
    }

    public Piece pieceAt(String square) {
        return pieceAt(parseSquare(square));
    }

    public Optional<Integer> findKingSquare(Side side) {
        for (int square = 0; square < 64; square++) {
            if (board[square].is(side, 'K')) return Optional.of(square);
        }
        return Optional.empty();
    }

    public Piece[][] currentBoard() {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(board, r * 8, copy[r], 0, 8);
        }
        return copy;
    }

    /** 현재 차례 진영의 모든 합법 수 목록을 계산한다. */
    public List<Move> legalMoves() {
        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoLegalMoves()) {
            BoardPosition trial = simulateLegalMove(move);
            if (!trial.isSquareAttacked(trial.kingSquare(turn), turn.opposite())) {
                legal.add(move);
            }
        }
        return List.copyOf(legal);
    }

    public boolean isCheck() {
        return isSquareAttacked(kingSquare(turn), turn.opposite());
    }

    public boolean isCheckmate() {
        return isCheck() && legalMoves().isEmpty();
    }

    public Status status() {
        boolean check = isCheck();
        if (legalMoves().isEmpty()) {
            return check ? Status.CHECKMATE : Status.STALEMATE;
        }
        return check ? Status.CHECK : Status.PLAYING;
    }

    /** 기물 부족(Dead Position)으로 인한 무승부 상태인지 판정한다. */
    public boolean isInsufficientMaterial() {
        int whiteKnights = 0, blackKnights = 0;
        int whiteBishops = 0, blackBishops = 0;
        int whiteBishopColor = -1, blackBishopColor = -1;
        for (int s = 0; s < 64; s++) {
            Piece p = board[s];
            if (p == Piece.EMPTY || p.type() == 'K') continue;
            switch (p) {
                case WN -> whiteKnights++;
                case BN -> blackKnights++;
                case WB -> {
                    whiteBishops++;
                    whiteBishopColor = (file(s) + rank(s)) & 1;
                }
                case BB -> {
                    blackBishops++;
                    blackBishopColor = (file(s) + rank(s)) & 1;
                }
                default -> {
                    return false;
                }
            }
        }
        int totalMinors = whiteKnights + blackKnights + whiteBishops + blackBishops;
        if (totalMinors <= 1) return true;
        return totalMinors == 2 && whiteBishops == 1 && blackBishops == 1
                && whiteBishopColor == blackBishopColor;
    }

    public String toFen() {
        return ChessNotation.formatFen(this);
    }

    /** legalMoves()에서 얻은 수를 적용한 새 국면. 원본은 바꾸지 않는다. */
    public BoardPosition simulateLegalMove(Move move) {
        BoardPosition next = new BoardPosition(this);
        next.applyUnchecked(move);
        return next;
    }

    boolean isSquareAttacked(int square, Side by) {
        int f = file(square);
        int r = rank(square);
        int pawnRank = r + (by == Side.WHITE ? -1 : 1);
        for (int df : new int[]{-1, 1}) {
            if (insideSquare(f + df, pawnRank)
                    && board[squareOf(f + df, pawnRank)].is(by, 'P')) {
                return true;
            }
        }
        for (int[] s : KNIGHT_STEPS) {
            if (insideSquare(f + s[0], r + s[1])
                    && board[squareOf(f + s[0], r + s[1])].is(by, 'N')) {
                return true;
            }
        }
        for (int[] d : KING_STEPS) {
            boolean diag = d[0] != 0 && d[1] != 0;
            for (int x = f + d[0], y = r + d[1], dist = 1;
                 insideSquare(x, y);
                 x += d[0], y += d[1], dist++) {
                Piece p = board[squareOf(x, y)];
                if (p == Piece.EMPTY) continue;
                if (p.belongsTo(by)) {
                    char t = p.type();
                    if (t == 'Q' || t == (diag ? 'B' : 'R') || (t == 'K' && dist == 1)) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    boolean hasLegalEnPassant() {
        if (enPassantSquare < 0) return false;
        return legalMoves().stream().anyMatch(m ->
                m.to() == enPassantSquare && board[m.from()].type() == 'P'
                        && file(m.from()) != file(m.to()));
    }

    private void applyUnchecked(Move move) {
        int from = move.from();
        int to = move.to();
        Piece piece = board[from];
        Piece captured = board[to];
        boolean pawn = piece.type() == 'P';
        int dir = turn == Side.WHITE ? 8 : -8;
        boolean ep = pawn && to == enPassantSquare
                && captured == Piece.EMPTY
                && file(from) != file(to);

        board[from] = Piece.EMPTY;
        if (ep) {
            captured = board[to - dir];
            board[to - dir] = Piece.EMPTY;
        }
        board[to] = move.promotion() == '-' ? piece : Piece.of(turn, move.promotion());

        if (piece.type() == 'K') {
            castlingRights &= turn == Side.WHITE ? ~(WK | WQ) : ~(BK | BQ);
            if (Math.abs(to - from) == 2) {
                boolean shortSide = to > from;
                int rookFrom = from + (shortSide ? 3 : -4);
                int rookTo = from + (shortSide ? 1 : -1);
                board[rookTo] = board[rookFrom];
                board[rookFrom] = Piece.EMPTY;
            }
        }
        castlingRights &= ~(cornerRight(from) | cornerRight(to));
        enPassantSquare = pawn && Math.abs(to - from) == 16 ? from + dir : -1;
        halfmoveClock = pawn || captured != Piece.EMPTY ? 0 : Math.addExact(halfmoveClock, 1);
        if (turn == Side.BLACK) {
            fullmoveNumber = Math.addExact(fullmoveNumber, 1);
        }
        turn = turn.opposite();
    }

    private List<Move> pseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();
        for (int from = 0; from < 64; from++) {
            Piece p = board[from];
            if (!p.belongsTo(turn)) continue;
            switch (p.type()) {
                case 'P' -> addPawnMoves(from, moves);
                case 'N' -> addDirectionalMoves(from, KNIGHT_STEPS, false, moves);
                case 'B' -> addDirectionalMoves(from, DIAGONALS, true, moves);
                case 'R' -> addDirectionalMoves(from, STRAIGHTS, true, moves);
                case 'Q' -> addDirectionalMoves(from, KING_STEPS, true, moves);
                case 'K' -> {
                    addDirectionalMoves(from, KING_STEPS, false, moves);
                    addCastlingMoves(from, moves);
                }
                default -> throw new IllegalStateException("알 수 없는 기물: " + p);
            }
        }
        return moves;
    }

    private void addPawnMoves(int from, List<Move> moves) {
        int f = file(from);
        int r = rank(from);
        int rankDir = turn == Side.WHITE ? 1 : -1;
        int nextRank = r + rankDir;
        if (!insideSquare(f, nextRank)) return;

        int oneStep = squareOf(f, nextRank);
        if (board[oneStep] == Piece.EMPTY) {
            addPawnMove(from, oneStep, moves);
            int twoStep = from + rankDir * 16;
            if (r == (turn == Side.WHITE ? 1 : 6) && board[twoStep] == Piece.EMPTY) {
                moves.add(new Move(from, twoStep));
            }
        }
        for (int df : new int[]{-1, 1}) {
            if (!insideSquare(f + df, nextRank)) continue;
            int to = squareOf(f + df, nextRank);
            boolean ep = to == enPassantSquare
                    && board[to] == Piece.EMPTY
                    && board[to - rankDir * 8].is(turn.opposite(), 'P');
            if (capturable(board[to], turn) || ep) {
                addPawnMove(from, to, moves);
            }
        }
    }

    private static void addPawnMove(int from, int to, List<Move> moves) {
        if (rank(to) == 0 || rank(to) == 7) {
            for (char promo : new char[]{'Q', 'R', 'B', 'N'}) {
                moves.add(new Move(from, to, promo));
            }
        } else {
            moves.add(new Move(from, to));
        }
    }

    private void addDirectionalMoves(int from, int[][] dirs, boolean sliding, List<Move> moves) {
        for (int[] d : dirs) {
            for (int f = file(from) + d[0], r = rank(from) + d[1];
                 insideSquare(f, r);
                 f += d[0], r += d[1]) {
                int to = squareOf(f, r);
                Piece target = board[to];
                if (target == Piece.EMPTY || capturable(target, turn)) {
                    moves.add(new Move(from, to));
                }
                if (!sliding || target != Piece.EMPTY) break;
            }
        }
    }

    private void addCastlingMoves(int from, List<Move> moves) {
        int home = turn == Side.WHITE ? 4 : 60;
        if (from != home || isCheck()) return;
        for (boolean shortSide : new boolean[]{true, false}) {
            int right = turn == Side.WHITE
                    ? (shortSide ? WK : WQ)
                    : (shortSide ? BK : BQ);
            int rookFrom = home + (shortSide ? 3 : -4);
            if ((castlingRights & right) == 0 || !board[rookFrom].is(turn, 'R')) continue;
            int dir = shortSide ? 1 : -1;
            boolean clear = true;
            for (int s = from + dir; s != rookFrom; s += dir) {
                clear &= board[s] == Piece.EMPTY;
            }
            if (clear && !isSquareAttacked(from + dir, turn.opposite())) {
                moves.add(new Move(from, from + dir * 2));
            }
        }
    }

    private static boolean capturable(Piece piece, Side turn) {
        return piece.belongsTo(turn.opposite()) && piece.type() != 'K';
    }
}
