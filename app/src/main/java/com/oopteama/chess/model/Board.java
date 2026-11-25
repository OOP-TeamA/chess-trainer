package com.oopteama.chess.model;

import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 8×8 체스판과 현재 국면 정보(둘 차례, 캐슬링 권리, 앙파상 칸 등)입니다.
 * 한 번 만든 Board는 바뀌지 않습니다. 수를 두면 apply()가 새 Board를 만들어 돌려줍니다(불변 객체).
 */
public final class Board {

    private final Piece[][] grid;           // grid[row][col], 빈 칸은 null
    private final Side turn;                // 둘 차례
    private final CastlingRights castling;  // 캐슬링 권리
    private final Position enPassant;       // 앙파상으로 잡을 수 있는 칸 (없으면 null)
    private final int halfmoveClock;        // 50수 규칙용: 폰 이동·잡기 없이 지나간 반수
    private final int fullmoveNumber;       // 몇 번째 수인지 (흑이 두면 1 증가)
    private final boolean sandbox;          // 킹이 없어도 되는 연습용 판인지

    Board(Piece[][] grid, Side turn, CastlingRights castling, Position enPassant,
          int halfmoveClock, int fullmoveNumber, boolean sandbox) {
        this.grid = grid;
        this.turn = Objects.requireNonNull(turn, "turn");
        this.castling = Objects.requireNonNull(castling, "castling");
        this.enPassant = enPassant;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
        this.sandbox = sandbox;
    }

    /** 표준 시작 국면. */
    public static Board initial() {
        return Fen.parse(Fen.START);
    }

    /** FEN 문자열로 표준 국면을 만듭니다. */
    public static Board fromFen(String fen) {
        return Fen.parse(fen);
    }

    /** FEN 문자열로 킹이 없어도 되는 연습용 국면을 만듭니다. */
    public static Board fromSandboxFen(String fen) {
        return Fen.parseSandbox(fen);
    }

    // ---- 칸 조회 ----

    /** 칸에 있는 기물 (빈 칸이면 null). */
    public Piece pieceAt(Position position) {
        return grid[position.row()][position.col()];
    }

    /** "e4" 같은 칸 이름으로 기물을 찾습니다. */
    public Piece pieceAt(String square) {
        return pieceAt(Position.of(square));
    }

    public boolean isEmpty(Position position) {
        return pieceAt(position) == null;
    }

    /** side 진영의 기물이 놓인 칸 목록. */
    public List<Position> positionsOf(Side side) {
        List<Position> result = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.side() == side) {
                    result.add(new Position(col, row));
                }
            }
        }
        return result;
    }

    /** side 진영 킹의 위치 (킹이 없는 연습판이면 null). */
    public Position findKing(Side side) {
        for (Position position : positionsOf(side)) {
            if (pieceAt(position) instanceof King) {
                return position;
            }
        }
        return null;
    }

    // ---- 국면 정보 ----

    public Side turn() {
        return turn;
    }

    public CastlingRights castlingRights() {
        return castling;
    }

    public Position enPassant() {
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

    // ---- 수 두기 ----

    /**
     * 수를 둔 뒤의 새 체스판을 돌려줍니다. 이 Board 자체는 바뀌지 않습니다.
     * 둘 수 있는 수인지는 호출하기 전에 Rules.legalMoves()로 확인합니다.
     */
    public Board apply(Move move) {
        Position from = move.from();
        Position to = move.to();
        Piece moving = pieceAt(from);
        if (moving == null || moving.side() != turn) {
            throw new IllegalArgumentException("지금 차례의 기물이 아닙니다: " + from);
        }
        Piece[][] next = copyGrid();
        boolean capture = pieceAt(to) != null;

        // 앙파상: 폰이 비어 있는 앙파상 칸으로 대각선 이동하면 바로 옆의 상대 폰을 잡습니다.
        if (moving instanceof Pawn && to.equals(enPassant) && from.col() != to.col()) {
            next[from.row()][to.col()] = null;
            capture = true;
        }

        // 기물 이동 (승진이면 새 기물로 바꿈)
        next[from.row()][from.col()] = null;
        next[to.row()][to.col()] = move.isPromotion() ? Piece.create(move.promotion(), turn) : moving;

        // 캐슬링: 킹이 두 칸 움직이면 룩도 킹 옆으로 옮깁니다.
        if (moving instanceof King && Math.abs(to.col() - from.col()) == 2) {
            boolean kingSide = to.col() > from.col();
            int rookFromCol = kingSide ? 7 : 0;
            int rookToCol = kingSide ? 5 : 3;
            next[from.row()][rookToCol] = next[from.row()][rookFromCol];
            next[from.row()][rookFromCol] = null;
        }

        CastlingRights nextCastling = castling.without(from).without(to);
        Position nextEnPassant = null;
        if (moving instanceof Pawn && Math.abs(to.row() - from.row()) == 2) {
            nextEnPassant = new Position(from.col(), (from.row() + to.row()) / 2);
        }
        int nextHalfmove = (moving instanceof Pawn || capture) ? 0 : halfmoveClock + 1;
        int nextFullmove = turn == Side.BLACK ? fullmoveNumber + 1 : fullmoveNumber;
        return new Board(next, turn.opposite(), nextCastling, nextEnPassant, nextHalfmove, nextFullmove, sandbox);
    }

    private Piece[][] copyGrid() {
        Piece[][] copy = new Piece[8][];
        for (int row = 0; row < 8; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }

    /** FEN 문자열로 바꿉니다. */
    public String toFen() {
        return Fen.format(this);
    }

    @Override
    public String toString() {
        return toFen();
    }
}
