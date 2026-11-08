package com.oopteama.chess.core;

import com.oopteama.chess.core.piece.Piece;
import com.oopteama.chess.core.piece.PieceFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * 유일한 가변 핵심 객체. 스레드 안전하지 않으므로 다른 스레드에는 copy()를 넘긴다.
 * 캐슬링 권리는 castleRights[0..3] = {백 킹사이드, 백 퀸사이드, 흑 킹사이드, 흑 퀸사이드}.
 */
public final class Position {

    private final Piece[] board = new Piece[64];
    private Side sideToMove;
    private final boolean[] castleRights = new boolean[4];
    private Square epTarget; // nullable
    private int halfmoveClock;
    private int fullmoveNumber;

    private final Deque<Undo> undoStack = new ArrayDeque<>();

    private record Undo(
        Move move,
        Piece movedBefore,     // 승진 되돌릴 때 쓸 원래 폰
        Piece captured,
        Square capturedSquare, // 앙파상이면 from/to와 다름
        boolean[] prevCastleRights,
        Square prevEpTarget,
        int prevHalfmove,
        int prevFullmove,
        Side prevSideToMove
    ) {}

    private Position() {}

    /** 표준 시작 국면. */
    public static Position initial() {
        Position p = new Position();
        PieceType[] backRank = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int file = 0; file < 8; file++) {
            p.board[new Square(file, 0).index()] = PieceFactory.of(Side.WHITE, backRank[file]);
            p.board[new Square(file, 1).index()] = PieceFactory.of(Side.WHITE, PieceType.PAWN);
            p.board[new Square(file, 6).index()] = PieceFactory.of(Side.BLACK, PieceType.PAWN);
            p.board[new Square(file, 7).index()] = PieceFactory.of(Side.BLACK, backRank[file]);
        }
        p.sideToMove = Side.WHITE;
        p.castleRights[0] = p.castleRights[1] = p.castleRights[2] = p.castleRights[3] = true;
        p.epTarget = null;
        p.halfmoveClock = 0;
        p.fullmoveNumber = 1;
        return p;
    }

    /** Fen.parse 전용 생성 통로. 같은 패키지(core)에서만 쓴다. */
    static Position raw() {
        return new Position();
    }

    // ---- 패키지 전용 세터 (Fen이 사용) ----
    void setAt(Square s, Piece piece) { board[s.index()] = piece; }
    void setSideToMove(Side side) { this.sideToMove = side; }
    void setCastleRight(Side side, boolean shortSide, boolean value) {
        castleRights[castleIndex(side, shortSide)] = value;
    }
    void setEpTarget(Square target) { this.epTarget = target; }
    void setHalfmoveClock(int v) { this.halfmoveClock = v; }
    void setFullmoveNumber(int v) { this.fullmoveNumber = v; }

    private static int castleIndex(Side side, boolean shortSide) {
        int base = side == Side.WHITE ? 0 : 2;
        return base + (shortSide ? 0 : 1);
    }

    /** 빈 칸이면 null. API 전체에서 유일한 null 반환. */
    public Piece at(Square s) {
        return board[s.index()];
    }

    /** 지금 둘 차례인 진영. */
    public Side sideToMove() {
        return sideToMove;
    }

    /** side의 캐슬링 권리가 남아 있는지(킹사이드면 shortSide=true). 기물 위치까지는 확인하지 않는다. */
    public boolean canCastle(Side side, boolean shortSide) {
        return castleRights[castleIndex(side, shortSide)];
    }

    /** 앙파상으로 잡을 수 있는 칸. 직전 수가 폰 2칸 전진이 아니면 empty. */
    public Optional<Square> enPassantTarget() {
        return Optional.ofNullable(epTarget);
    }

    /** 교육용 국면에서는 비어 있을 수 있음. */
    public Optional<Square> kingSquare(Side side) {
        for (int i = 0; i < 64; i++) {
            Piece p = board[i];
            if (p != null && p.type() == PieceType.KING && p.side() == side) {
                return Optional.of(Square.ofIndex(i));
            }
        }
        return Optional.empty();
    }

    /** side의 기물이 놓인 칸들(인덱스 순서). */
    public List<Square> occupied(Side side) {
        List<Square> squares = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            Piece p = board[i];
            if (p != null && p.side() == side) {
                squares.add(Square.ofIndex(i));
            }
        }
        return squares;
    }

    /** 50수 규칙용 반수 카운터. */
    public int halfmoveClock() {
        return halfmoveClock;
    }

    /** 풀무브 번호. 흑이 둘 때마다 1 늘어난다. */
    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    /**
     * 사전조건: m은 이 국면의 RuleSet이 만든 수. 검증하지 않으므로
     * 다른 수를 넣으면 상태가 깨진다.
     */
    public void make(Move m) {
        Piece moving = at(m.from());
        Piece capturedPiece;
        Square capturedSquare;

        if (m.kind() == MoveKind.EN_PASSANT) {
            capturedSquare = new Square(m.to().file(), m.from().rank());
            capturedPiece = at(capturedSquare);
            board[capturedSquare.index()] = null;
        } else {
            capturedSquare = m.to();
            capturedPiece = at(m.to());
        }

        boolean[] prevRights = castleRights.clone();
        Square prevEp = epTarget;
        int prevHalf = halfmoveClock;
        int prevFull = fullmoveNumber;
        Side prevSide = sideToMove;

        // 캐슬링이면 룩도 옮긴다
        if (m.kind() == MoveKind.CASTLE_SHORT || m.kind() == MoveKind.CASTLE_LONG) {
            int rank = m.from().rank();
            int rookFromFile = m.kind() == MoveKind.CASTLE_SHORT ? 7 : 0;
            int rookToFile = m.kind() == MoveKind.CASTLE_SHORT ? 5 : 3;
            Square rookFrom = new Square(rookFromFile, rank);
            Square rookTo = new Square(rookToFile, rank);
            board[rookTo.index()] = board[rookFrom.index()];
            board[rookFrom.index()] = null;
        }

        Piece placed = m.promotion() != null ? PieceFactory.of(sideToMove, m.promotion()) : moving;
        board[m.to().index()] = placed;
        board[m.from().index()] = null;

        // 캐슬링 권리 갱신
        if (moving.type() == PieceType.KING) {
            castleRights[castleIndex(sideToMove, true)] = false;
            castleRights[castleIndex(sideToMove, false)] = false;
        }
        clearRookRight(m.from());
        clearRookRight(m.to()); // 상대 룩이 원래 자리에서 잡혔으면 그쪽 권리도 사라짐

        // 앙파상 타깃 갱신
        if (m.kind() == MoveKind.DOUBLE_PUSH) {
            int dir = sideToMove == Side.WHITE ? 1 : -1;
            epTarget = new Square(m.from().file(), m.from().rank() + dir);
        } else {
            epTarget = null;
        }

        // 50수 규칙 카운터
        if (m.isCapture() || moving.type() == PieceType.PAWN) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        if (sideToMove == Side.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = sideToMove.opposite();

        undoStack.push(new Undo(m, moving, capturedPiece, capturedSquare,
            prevRights, prevEp, prevHalf, prevFull, prevSide));
    }

    private void clearRookRight(Square sq) {
        if (sq.rank() == 0 && sq.file() == 0) castleRights[castleIndex(Side.WHITE, false)] = false;
        if (sq.rank() == 0 && sq.file() == 7) castleRights[castleIndex(Side.WHITE, true)] = false;
        if (sq.rank() == 7 && sq.file() == 0) castleRights[castleIndex(Side.BLACK, false)] = false;
        if (sq.rank() == 7 && sq.file() == 7) castleRights[castleIndex(Side.BLACK, true)] = false;
    }

    /** 직전 make 취소. 되돌릴 것이 없으면 IllegalStateException. */
    public void unmake() {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("되돌릴 수가 없습니다.");
        }
        Undo u = undoStack.pop();
        Move m = u.move();

        // 캐슬링이면 룩부터 원위치
        if (m.kind() == MoveKind.CASTLE_SHORT || m.kind() == MoveKind.CASTLE_LONG) {
            int rank = m.from().rank();
            int rookFromFile = m.kind() == MoveKind.CASTLE_SHORT ? 7 : 0;
            int rookToFile = m.kind() == MoveKind.CASTLE_SHORT ? 5 : 3;
            Square rookFrom = new Square(rookFromFile, rank);
            Square rookTo = new Square(rookToFile, rank);
            board[rookFrom.index()] = board[rookTo.index()];
            board[rookTo.index()] = null;
        }

        board[m.from().index()] = u.movedBefore();
        board[m.to().index()] = null;
        if (u.captured() != null) {
            board[u.capturedSquare().index()] = u.captured();
        }

        System.arraycopy(u.prevCastleRights(), 0, castleRights, 0, 4);
        epTarget = u.prevEpTarget();
        halfmoveClock = u.prevHalfmove();
        fullmoveNumber = u.prevFullmove();
        sideToMove = u.prevSideToMove();
    }

    /** unmake로 되돌릴 수 있는 수의 개수. */
    public int undoDepth() {
        return undoStack.size();
    }

    /** 배치와 상태 필드만 복사하고 되돌리기 스택은 비운다(G12). */
    public Position copy() {
        Position p = new Position();
        System.arraycopy(board, 0, p.board, 0, 64);
        p.sideToMove = this.sideToMove;
        System.arraycopy(castleRights, 0, p.castleRights, 0, 4);
        p.epTarget = this.epTarget;
        p.halfmoveClock = this.halfmoveClock;
        p.fullmoveNumber = this.fullmoveNumber;
        return p;
    }
}

