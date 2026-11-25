package com.oopteama.chess.backend.engine;

import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.engine.Piece.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 불변 64칸 체스 국면 스냅샷.
 * 인덱스: 0 = a1, 7 = h1, 56 = a8, 63 = h8 (rank * 8 + file).
 * 표준 규칙과 킹이 없는 튜토리얼(sandbox) 국면을 모두 지원합니다.
 */
public final class Board {

    public static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static final int CASTLE_WK = 1;
    public static final int CASTLE_WQ = 2;
    public static final int CASTLE_BK = 4;
    public static final int CASTLE_BQ = 8;

    private static final int[][] KNIGHT_DIRS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] DIAGONALS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    private static final int[][] STRAIGHTS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] ALL_DIRS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };
    private static final Type[] PROMOTIONS = {Type.QUEEN, Type.ROOK, Type.BISHOP, Type.KNIGHT};

    private final Piece[] squares;
    private final Side turn;
    private final int castling;
    private final int enPassant;
    private final int halfmoveClock;
    private final int fullmoveNumber;
    private final boolean sandbox;

    private Board(
            Piece[] squares,
            Side turn,
            int castling,
            int enPassant,
            int halfmoveClock,
            int fullmoveNumber,
            boolean sandbox
    ) {
        this.squares = squares;
        this.turn = turn;
        this.castling = castling;
        this.enPassant = enPassant;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
        this.sandbox = sandbox;
    }

    public static Board initial() {
        return fromFen(INITIAL_FEN);
    }

    public static Board fromFen(String fen) {
        return parseFen(fen, false);
    }

    public static Board fromSandboxFen(String fen) {
        return parseFen(fen, true);
    }

    // ---- 좌표 유틸리티 ----

    public static int file(int sq) {
        return sq & 7;
    }

    public static int rank(int sq) {
        return sq >> 3;
    }

    public static int square(int file, int rank) {
        return (rank << 3) | file;
    }

    public static boolean inside(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    public static void checkSquare(int sq) {
        if (sq < 0 || sq >= 64) {
            throw new IllegalArgumentException("칸 인덱스가 0..63 범위를 벗어났습니다: " + sq);
        }
    }

    public static int parseSquare(String name) {
        if (name == null || name.length() != 2) {
            throw new IllegalArgumentException("잘못된 칸 표기: " + name);
        }
        int f = name.charAt(0) - 'a';
        int r = name.charAt(1) - '1';
        if (!inside(f, r)) {
            throw new IllegalArgumentException("잘못된 칸 표기: " + name);
        }
        return square(f, r);
    }

    public static String formatSquare(int sq) {
        checkSquare(sq);
        return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq));
    }

    private static int cornerRight(int sq) {
        return switch (sq) {
            case 0 -> CASTLE_WQ;
            case 7 -> CASTLE_WK;
            case 56 -> CASTLE_BQ;
            case 63 -> CASTLE_BK;
            default -> 0;
        };
    }

    // ---- 상태 조회 ----

    public Piece pieceAt(int sq) {
        checkSquare(sq);
        return squares[sq];
    }

    public Piece pieceAt(String sq) {
        return pieceAt(parseSquare(sq));
    }

    public Side turn() {
        return turn;
    }

    public int castling() {
        return castling;
    }

    public int enPassant() {
        return enPassant;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public int findKing(Side side) {
        Piece king = Piece.of(side, Type.KING);
        for (int sq = 0; sq < 64; sq++) {
            if (squares[sq] == king) {
                return sq;
            }
        }
        return -1;
    }

    public boolean isInCheck() {
        return isInCheck(turn);
    }

    public boolean isInCheck(Side side) {
        int kingSq = findKing(side);
        return kingSq >= 0 && isSquareAttacked(kingSq, side.opposite());
    }

    public boolean isCheckmate() {
        return isInCheck() && legalMoves().isEmpty();
    }

    public boolean isStalemate() {
        return !isInCheck() && legalMoves().isEmpty();
    }

    public boolean isInsufficientMaterial() {
        if (sandbox) {
            return false;
        }
        int whiteBishops = 0;
        int blackBishops = 0;
        int whiteBishopColor = -1;
        int blackBishopColor = -1;
        int minorTotal = 0;

        for (int sq = 0; sq < 64; sq++) {
            Piece p = squares[sq];
            if (p.isEmpty() || p.type() == Type.KING) {
                continue;
            }
            if (p.type() == Type.PAWN || p.type() == Type.ROOK || p.type() == Type.QUEEN) {
                return false;
            }
            minorTotal++;
            if (minorTotal > 2) {
                return false;
            }
            if (p == Piece.WB) {
                whiteBishops++;
                whiteBishopColor = (file(sq) + rank(sq)) & 1;
            } else if (p == Piece.BB) {
                blackBishops++;
                blackBishopColor = (file(sq) + rank(sq)) & 1;
            }
        }
        if (minorTotal <= 1) {
            return true;
        }
        return whiteBishops == 1 && blackBishops == 1 && whiteBishopColor == blackBishopColor;
    }

    // ---- 합법 수 생성 및 착수 ----

    public List<Move> legalMoves() {
        List<Move> pseudo = pseudoLegalMoves();
        int ownKing = findKing(turn);
        if (sandbox && ownKing < 0) {
            return List.copyOf(pseudo);
        }
        List<Move> legal = new ArrayList<>(pseudo.size());
        Side enemy = turn.opposite();
        for (Move move : pseudo) {
            Board next = applyMove(move);
            int kingSq = next.findKing(turn);
            if (kingSq >= 0 && !next.isSquareAttacked(kingSq, enemy)) {
                legal.add(move);
            }
        }
        return List.copyOf(legal);
    }

    public List<Move> legalMovesFrom(int from) {
        checkSquare(from);
        List<Move> out = new ArrayList<>();
        for (Move m : legalMoves()) {
            if (m.from() == from) {
                out.add(m);
            }
        }
        return List.copyOf(out);
    }

    public Move.Kind classifyMove(Move move) {
        Piece moving = squares[move.from()];
        Piece target = squares[move.to()];
        if (moving.type() == Type.KING) {
            int diff = move.to() - move.from();
            if (diff == 2) {
                return Move.Kind.CASTLE_SHORT;
            }
            if (diff == -2) {
                return Move.Kind.CASTLE_LONG;
            }
        }
        if (moving.type() == Type.PAWN && move.to() == enPassant && file(move.from()) != file(move.to())) {
            return Move.Kind.EN_PASSANT;
        }
        return target.isEmpty() ? Move.Kind.NORMAL : Move.Kind.CAPTURE;
    }

    public Piece capturedPiece(Move move) {
        Move.Kind kind = classifyMove(move);
        if (kind == Move.Kind.EN_PASSANT) {
            int dir = turn == Side.WHITE ? 8 : -8;
            return squares[move.to() - dir];
        }
        return squares[move.to()];
    }

    /**
     * 합법성 검사를 마친 수를 적용해 새 불변 Board를 반환합니다.
     */
    public Board applyMove(Move move) {
        Objects.requireNonNull(move, "move");
        Piece[] next = squares.clone();
        int from = move.from();
        int to = move.to();
        Piece moving = next[from];
        if (!moving.is(turn)) {
            throw new IllegalArgumentException("현재 차례의 기물이 아닙니다: " + formatSquare(from));
        }

        Piece captured = next[to];
        int dir = turn == Side.WHITE ? 8 : -8;

        // 앙파상 처리
        if (moving.type() == Type.PAWN && to == enPassant && file(from) != file(to)) {
            int capSq = to - dir;
            captured = next[capSq];
            next[capSq] = Piece.EMPTY;
        }

        // 기물 이동 및 승진
        next[from] = Piece.EMPTY;
        next[to] = move.promotion() != null ? Piece.of(turn, move.promotion()) : moving;

        // 캐슬링 룩 이동 및 권리 갱신
        int nextCastling = castling;
        if (moving.type() == Type.KING) {
            nextCastling &= (turn == Side.WHITE) ? ~(CASTLE_WK | CASTLE_WQ) : ~(CASTLE_BK | CASTLE_BQ);
            if (to - from == 2) {
                next[from + 1] = next[from + 3];
                next[from + 3] = Piece.EMPTY;
            } else if (to - from == -2) {
                next[from - 1] = next[from - 4];
                next[from - 4] = Piece.EMPTY;
            }
        }
        nextCastling &= ~(cornerRight(from) | cornerRight(to));

        // 앙파상 타깃 칸 설정
        int nextEp = (moving.type() == Type.PAWN && Math.abs(to - from) == 16) ? (from + dir) : -1;

        int nextHalfmove = (moving.type() == Type.PAWN || !captured.isEmpty()) ? 0 : halfmoveClock + 1;
        int nextFullmove = turn == Side.BLACK ? fullmoveNumber + 1 : fullmoveNumber;

        return new Board(next, turn.opposite(), nextCastling, nextEp, nextHalfmove, nextFullmove, sandbox);
    }

    /**
     * 특정 칸에 놓인 기물이 현재 국면에서 직접 공격(위협)하는 상대 기물 칸 목록을 반환합니다.
     */
    public List<Integer> attackedEnemySquaresFrom(int sq) {
        checkSquare(sq);
        Piece piece = squares[sq];
        if (piece.isEmpty()) {
            return List.of();
        }
        Side enemy = piece.side().opposite();
        int f = file(sq);
        int r = rank(sq);
        List<Integer> targets = new ArrayList<>();

        switch (piece.type()) {
            case PAWN -> {
                int nr = r + (piece.side() == Side.WHITE ? 1 : -1);
                for (int df : new int[]{-1, 1}) {
                    if (inside(f + df, nr)) {
                        int t = square(f + df, nr);
                        if (squares[t].is(enemy)) {
                            targets.add(t);
                        }
                    }
                }
            }
            case KNIGHT -> collectAttackedEnemies(f, r, KNIGHT_DIRS, false, enemy, targets);
            case BISHOP -> collectAttackedEnemies(f, r, DIAGONALS, true, enemy, targets);
            case ROOK -> collectAttackedEnemies(f, r, STRAIGHTS, true, enemy, targets);
            case QUEEN -> collectAttackedEnemies(f, r, ALL_DIRS, true, enemy, targets);
            case KING -> collectAttackedEnemies(f, r, ALL_DIRS, false, enemy, targets);
        }
        return List.copyOf(targets);
    }

    private void collectAttackedEnemies(
            int f,
            int r,
            int[][] dirs,
            boolean sliding,
            Side enemy,
            List<Integer> out
    ) {
        for (int[] d : dirs) {
            int nf = f + d[0];
            int nr = r + d[1];
            while (inside(nf, nr)) {
                int to = square(nf, nr);
                Piece p = squares[to];
                if (!p.isEmpty()) {
                    if (p.is(enemy)) {
                        out.add(to);
                    }
                    break;
                }
                if (!sliding) {
                    break;
                }
                nf += d[0];
                nr += d[1];
            }
        }
    }

    /**
     * 특정 칸을 공격하고 있는 지정 진영 기물들의 칸 인덱스 목록을 반환합니다.
     */
    public List<Integer> attackersOf(int sq, Side by) {
        checkSquare(sq);
        int f = file(sq);
        int r = rank(sq);
        List<Integer> out = new ArrayList<>();

        int pawnRank = r + (by == Side.WHITE ? -1 : 1);
        for (int df : new int[]{-1, 1}) {
            if (inside(f + df, pawnRank)) {
                int s = square(f + df, pawnRank);
                if (squares[s].is(by, Type.PAWN)) {
                    out.add(s);
                }
            }
        }
        for (int[] d : KNIGHT_DIRS) {
            int nf = f + d[0];
            int nr = r + d[1];
            if (inside(nf, nr)) {
                int s = square(nf, nr);
                if (squares[s].is(by, Type.KNIGHT)) {
                    out.add(s);
                }
            }
        }
        for (int[] d : ALL_DIRS) {
            boolean diag = d[0] != 0 && d[1] != 0;
            for (int step = 1; step < 8; step++) {
                int nf = f + d[0] * step;
                int nr = r + d[1] * step;
                if (!inside(nf, nr)) {
                    break;
                }
                int s = square(nf, nr);
                Piece p = squares[s];
                if (p.isEmpty()) {
                    continue;
                }
                if (p.is(by)) {
                    Type t = p.type();
                    if (t == Type.QUEEN || (diag && t == Type.BISHOP) || (!diag && t == Type.ROOK)
                            || (step == 1 && t == Type.KING)) {
                        out.add(s);
                    }
                }
                break;
            }
        }
        return List.copyOf(out);
    }

    /**
     * 특정 칸이 지정 진영에 의해 공격받는지 역방향 탐색으로 판별합니다.
     */
    public boolean isSquareAttacked(int sq, Side by) {
        int f = file(sq);
        int r = rank(sq);

        // 1. 폰 공격
        int pawnRank = r + (by == Side.WHITE ? -1 : 1);
        for (int df : new int[]{-1, 1}) {
            if (inside(f + df, pawnRank) && squares[square(f + df, pawnRank)].is(by, Type.PAWN)) {
                return true;
            }
        }

        // 2. 나이트 공격
        for (int[] d : KNIGHT_DIRS) {
            int nf = f + d[0];
            int nr = r + d[1];
            if (inside(nf, nr) && squares[square(nf, nr)].is(by, Type.KNIGHT)) {
                return true;
            }
        }

        // 3. 8방향 슬라이딩(퀸/비숍/룩) 및 킹 공격
        for (int[] d : ALL_DIRS) {
            boolean diag = d[0] != 0 && d[1] != 0;
            for (int step = 1; step < 8; step++) {
                int nf = f + d[0] * step;
                int nr = r + d[1] * step;
                if (!inside(nf, nr)) {
                    break;
                }
                Piece p = squares[square(nf, nr)];
                if (p.isEmpty()) {
                    continue;
                }
                if (p.is(by)) {
                    Type t = p.type();
                    if (t == Type.QUEEN || (diag && t == Type.BISHOP) || (!diag && t == Type.ROOK)
                            || (step == 1 && t == Type.KING)) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    private List<Move> pseudoLegalMoves() {
        List<Move> moves = new ArrayList<>(36);
        for (int sq = 0; sq < 64; sq++) {
            Piece p = squares[sq];
            if (!p.is(turn)) {
                continue;
            }
            switch (p.type()) {
                case PAWN -> addPawnMoves(sq, moves);
                case KNIGHT -> addStepMoves(sq, KNIGHT_DIRS, false, moves);
                case BISHOP -> addStepMoves(sq, DIAGONALS, true, moves);
                case ROOK -> addStepMoves(sq, STRAIGHTS, true, moves);
                case QUEEN -> addStepMoves(sq, ALL_DIRS, true, moves);
                case KING -> {
                    addStepMoves(sq, ALL_DIRS, false, moves);
                    addCastlingMoves(sq, moves);
                }
            }
        }
        return moves;
    }

    private void addPawnMoves(int from, List<Move> moves) {
        int f = file(from);
        int r = rank(from);
        int step = turn == Side.WHITE ? 1 : -1;
        int startRank = turn == Side.WHITE ? 1 : 6;
        int promoRank = turn == Side.WHITE ? 7 : 0;

        // 1칸 및 2칸 전진
        int r1 = r + step;
        if (inside(f, r1)) {
            int one = square(f, r1);
            if (squares[one].isEmpty()) {
                addPawnTarget(from, one, r1 == promoRank, moves);
                int r2 = r + 2 * step;
                if (r == startRank && inside(f, r2) && squares[square(f, r2)].isEmpty()) {
                    moves.add(new Move(from, square(f, r2)));
                }
            }
        }

        // 대각선 잡기 및 앙파상
        Side enemy = turn.opposite();
        for (int df : new int[]{-1, 1}) {
            if (!inside(f + df, r1)) {
                continue;
            }
            int to = square(f + df, r1);
            Piece target = squares[to];
            if ((target.is(enemy) && target.type() != Type.KING) || to == enPassant) {
                addPawnTarget(from, to, r1 == promoRank, moves);
            }
        }
    }

    private static void addPawnTarget(int from, int to, boolean promote, List<Move> moves) {
        if (!promote) {
            moves.add(new Move(from, to));
            return;
        }
        for (Type p : PROMOTIONS) {
            moves.add(new Move(from, to, p));
        }
    }

    private void addStepMoves(int from, int[][] dirs, boolean sliding, List<Move> moves) {
        int f = file(from);
        int r = rank(from);
        Side enemy = turn.opposite();
        for (int[] d : dirs) {
            int nf = f + d[0];
            int nr = r + d[1];
            while (inside(nf, nr)) {
                int to = square(nf, nr);
                Piece target = squares[to];
                if (target.isEmpty()) {
                    moves.add(new Move(from, to));
                } else {
                    if (target.is(enemy) && target.type() != Type.KING) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
                if (!sliding) {
                    break;
                }
                nf += d[0];
                nr += d[1];
            }
        }
    }

    private void addCastlingMoves(int kingSq, List<Move> moves) {
        int homeKing = turn == Side.WHITE ? 4 : 60;
        if (kingSq != homeKing || isSquareAttacked(kingSq, turn.opposite())) {
            return;
        }
        Side enemy = turn.opposite();
        int ksRight = turn == Side.WHITE ? CASTLE_WK : CASTLE_BK;
        int qsRight = turn == Side.WHITE ? CASTLE_WQ : CASTLE_BQ;

        if ((castling & ksRight) != 0
                && squares[kingSq + 3].is(turn, Type.ROOK)
                && squares[kingSq + 1].isEmpty()
                && squares[kingSq + 2].isEmpty()
                && !isSquareAttacked(kingSq + 1, enemy)) {
            moves.add(new Move(kingSq, kingSq + 2));
        }
        if ((castling & qsRight) != 0
                && squares[kingSq - 4].is(turn, Type.ROOK)
                && squares[kingSq - 1].isEmpty()
                && squares[kingSq - 2].isEmpty()
                && squares[kingSq - 3].isEmpty()
                && !isSquareAttacked(kingSq - 1, enemy)) {
            moves.add(new Move(kingSq, kingSq - 2));
        }
    }

    public boolean hasLegalEnPassant() {
        if (enPassant < 0) {
            return false;
        }
        for (Move m : legalMoves()) {
            if (squares[m.from()].type() == Type.PAWN && m.to() == enPassant) {
                return true;
            }
        }
        return false;
    }

    // ---- FEN 직렬화 및 파싱 ----

    public String positionKey(boolean effectiveEpOnly) {
        StringBuilder sb = new StringBuilder(72);
        for (int r = 7; r >= 0; r--) {
            int empty = 0;
            for (int f = 0; f < 8; f++) {
                Piece p = squares[square(f, r)];
                if (p.isEmpty()) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(p.fenChar());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (r > 0) {
                sb.append('/');
            }
        }
        sb.append(' ').append(turn.fenChar()).append(' ');
        if (castling == 0) {
            sb.append('-');
        } else {
            if ((castling & CASTLE_WK) != 0) sb.append('K');
            if ((castling & CASTLE_WQ) != 0) sb.append('Q');
            if ((castling & CASTLE_BK) != 0) sb.append('k');
            if ((castling & CASTLE_BQ) != 0) sb.append('q');
        }
        sb.append(' ');
        boolean includeEp = enPassant >= 0 && (!effectiveEpOnly || hasLegalEnPassant());
        sb.append(includeEp ? formatSquare(enPassant) : "-");
        return sb.toString();
    }

    public String toFen() {
        return positionKey(false) + " " + halfmoveClock + " " + fullmoveNumber;
    }

    private static Board parseFen(String fen, boolean sandbox) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN 문자열이 비어 있습니다.");
        }
        String[] parts = fen.trim().split("\\s+");
        if (parts.length != 6) {
            throw new IllegalArgumentException("FEN은 6개 필드가 필요합니다: " + fen);
        }

        // 1. 기물 배치
        Piece[] board = new Piece[64];
        Arrays.fill(board, Piece.EMPTY);
        String[] ranks = parts[0].split("/", -1);
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN 배치에는 8개 행이 필요합니다: " + parts[0]);
        }
        for (int i = 0; i < 8; i++) {
            int r = 7 - i;
            int f = 0;
            for (char c : ranks[i].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    f += c - '0';
                } else {
                    if (f >= 8) {
                        throw new IllegalArgumentException("FEN 행 길이가 8을 초과했습니다: " + ranks[i]);
                    }
                    board[square(f++, r)] = Piece.fromFen(c);
                }
            }
            if (f != 8) {
                throw new IllegalArgumentException("FEN 행 길이가 8이 아닙니다: " + ranks[i]);
            }
        }

        // 2. 차례
        Side turn = switch (parts[1]) {
            case "w" -> Side.WHITE;
            case "b" -> Side.BLACK;
            default -> throw new IllegalArgumentException("잘못된 FEN 차례 필드: " + parts[1]);
        };

        // 3. 캐슬링 권리
        int castling = 0;
        if (!"-".equals(parts[2])) {
            for (char c : parts[2].toCharArray()) {
                int bit = switch (c) {
                    case 'K' -> CASTLE_WK;
                    case 'Q' -> CASTLE_WQ;
                    case 'k' -> CASTLE_BK;
                    case 'q' -> CASTLE_BQ;
                    default -> throw new IllegalArgumentException("잘못된 캐슬링 필드: " + parts[2]);
                };
                if ((castling & bit) != 0) {
                    throw new IllegalArgumentException("중복된 캐슬링 문자: " + parts[2]);
                }
                castling |= bit;
            }
        }

        // 4. 앙파상
        int ep = "-".equals(parts[3]) ? -1 : parseSquare(parts[3]);

        // 5~6. 반수 시계 / 수 번호
        int halfmove;
        int fullmove;
        try {
            halfmove = Integer.parseInt(parts[4]);
            fullmove = Integer.parseInt(parts[5]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("FEN 숫자 필드가 올바르지 않습니다: " + fen, e);
        }
        if (halfmove < 0 || fullmove < 1) {
            throw new IllegalArgumentException("FEN 시계/수 번호 범위 오류: " + fen);
        }

        Board result = new Board(board, turn, castling, ep, halfmove, fullmove, sandbox);
        if (!sandbox) {
            validateStandard(result);
        }
        return result;
    }

    private static void validateStandard(Board pos) {
        int wk = 0;
        int bk = 0;
        for (int sq = 0; sq < 64; sq++) {
            Piece p = pos.squares[sq];
            if (p == Piece.WK) wk++;
            if (p == Piece.BK) bk++;
            if (p.type() == Type.PAWN && (rank(sq) == 0 || rank(sq) == 7)) {
                throw new IllegalArgumentException("1행 또는 8행에 폰을 둘 수 없습니다.");
            }
        }
        if (wk != 1 || bk != 1) {
            throw new IllegalArgumentException("표준 국면에는 백·흑 킹이 각각 1개씩 있어야 합니다.");
        }
        if (pos.isInCheck(pos.turn.opposite())) {
            throw new IllegalArgumentException("상대 킹이 이미 체크 상태인 불법 국면입니다.");
        }
        int[] corners = {0, 7, 56, 63};
        for (int corner : corners) {
            int right = cornerRight(corner);
            if ((pos.castling & right) != 0) {
                Side s = corner < 8 ? Side.WHITE : Side.BLACK;
                int kingSq = s == Side.WHITE ? 4 : 60;
                if (!pos.squares[kingSq].is(s, Type.KING) || !pos.squares[corner].is(s, Type.ROOK)) {
                    throw new IllegalArgumentException("캐슬링 권리와 킹/룩 위치가 일치하지 않습니다.");
                }
            }
        }
        if (pos.enPassant >= 0) {
            int expectedRank = pos.turn == Side.WHITE ? 5 : 2;
            int dir = pos.turn == Side.WHITE ? 8 : -8;
            if (rank(pos.enPassant) != expectedRank
                    || !pos.squares[pos.enPassant].isEmpty()
                    || !pos.squares[pos.enPassant - dir].is(pos.turn.opposite(), Type.PAWN)
                    || !pos.squares[pos.enPassant + dir].isEmpty()) {
                throw new IllegalArgumentException("잘못된 앙파상 칸입니다: " + formatSquare(pos.enPassant));
            }
        }
    }

    @Override
    public String toString() {
        return toFen();
    }
}
