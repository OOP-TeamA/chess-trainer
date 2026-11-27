package com.oopteama.chess.model;

import com.oopteama.chess.model.piece.Bishop;
import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.model.piece.Queen;
import com.oopteama.chess.model.piece.Rook;

import java.util.ArrayList;
import java.util.List;

/**
 * 체스 규칙 판단을 모아 둔 클래스입니다.
 * 기물 하나의 움직임은 각 Piece 클래스가 알고 있고,
 * 여기서는 판 전체를 봐야 하는 규칙(체크, 캐슬링, 승진, 체크메이트, 무승부)을 처리합니다.
 */
public final class Rules {

    /** 폰이 승진할 수 있는 기물 (퀸, 룩, 비숍, 나이트 순). */
    private static final char[] PROMOTIONS = {'Q', 'R', 'B', 'N'};

    private Rules() {
    }

    // ---- 합법 수 ----

    /** 지금 차례인 쪽이 둘 수 있는 모든 수. */
    public static List<Move> legalMoves(Board board) {
        List<Move> legal = new ArrayList<>();
        for (Move move : candidateMoves(board)) {
            if (keepsKingSafe(board, move)) {
                legal.add(move);
            }
        }
        return legal;
    }

    /** from 칸의 기물이 둘 수 있는 수. */
    public static List<Move> legalMovesFrom(Board board, Position from) {
        List<Move> result = new ArrayList<>();
        for (Move move : legalMoves(board)) {
            if (move.from().equals(from)) {
                result.add(move);
            }
        }
        return result;
    }

    /** 둘 수 있는 수가 하나라도 있는지 (모든 수를 만들지 않아서 legalMoves()보다 빠릅니다). */
    public static boolean hasAnyLegalMove(Board board) {
        for (Move move : candidateMoves(board)) {
            if (keepsKingSafe(board, move)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 상대 기물을 잡는 합법 수와 폰 승진(퀸으로)만 구합니다.
     * 잡는 수만 필요할 때(AI의 잡기 정리) 모든 수를 검사하는 legalMoves()보다 훨씬 빠릅니다.
     */
    public static List<Move> legalCaptures(Board board) {
        Side me = board.turn();
        int lastRow = me.opposite().homeRow();
        List<Move> captures = new ArrayList<>();
        for (Position from : board.positionsOf(me)) {
            Piece piece = board.pieceAt(from);
            for (Position to : piece.movableSquares(board, from)) {
                boolean promotion = piece instanceof Pawn && to.row() == lastRow;
                Move move = new Move(from, to, promotion ? 'Q' : Move.NO_PROMOTION);
                Piece captured = capturedPiece(board, move);
                boolean worthChecking = promotion || (captured != null && !(captured instanceof King));
                if (worthChecking && keepsKingSafe(board, move)) {
                    captures.add(move);
                }
            }
        }
        return captures;
    }

    /** 기물 규칙만 따진 후보 수 (자기 킹이 위험해지는지는 아직 검사하지 않음). */
    private static List<Move> candidateMoves(Board board) {
        Side me = board.turn();
        int lastRow = me.opposite().homeRow();
        List<Move> moves = new ArrayList<>();
        for (Position from : board.positionsOf(me)) {
            Piece piece = board.pieceAt(from);
            for (Position to : piece.movableSquares(board, from)) {
                if (board.pieceAt(to) instanceof King) {
                    continue; // 킹은 잡을 수 없습니다.
                }
                if (piece instanceof Pawn && to.row() == lastRow) {
                    for (char promotion : PROMOTIONS) {
                        moves.add(new Move(from, to, promotion)); // 끝 줄에 도착한 폰은 승진
                    }
                } else {
                    moves.add(new Move(from, to));
                }
            }
            if (piece instanceof King) {
                addCastlingMoves(board, from, moves);
            }
        }
        return moves;
    }

    /** 수를 둔 뒤 자기 킹이 공격받지 않는지 확인합니다. 킹이 없는 연습판은 항상 안전합니다. */
    private static boolean keepsKingSafe(Board board, Move move) {
        Side me = board.turn();
        if (board.findKing(me) == null) {
            return true;
        }
        return !isInCheck(board.apply(move), me);
    }

    /**
     * 캐슬링: 킹과 룩이 처음 자리에 있고, 사이 칸이 비어 있고,
     * 킹이 체크 중이 아니며 지나가는 칸이 공격받지 않을 때 킹이 두 칸 움직입니다.
     */
    private static void addCastlingMoves(Board board, Position kingAt, List<Move> moves) {
        Side me = board.turn();
        Side enemy = me.opposite();
        int row = me.homeRow();
        if (!kingAt.equals(new Position(4, row)) || isAttacked(board, kingAt, enemy)) {
            return;
        }
        CastlingRights rights = board.castlingRights();
        if (rights.kingSide(me)
                && board.pieceAt(new Position(7, row)) instanceof Rook rook && rook.side() == me
                && board.isEmpty(new Position(5, row)) && board.isEmpty(new Position(6, row))
                && !isAttacked(board, new Position(5, row), enemy)) {
            moves.add(new Move(kingAt, new Position(6, row)));
        }
        if (rights.queenSide(me)
                && board.pieceAt(new Position(0, row)) instanceof Rook rook && rook.side() == me
                && board.isEmpty(new Position(1, row)) && board.isEmpty(new Position(2, row))
                && board.isEmpty(new Position(3, row))
                && !isAttacked(board, new Position(3, row), enemy)) {
            moves.add(new Move(kingAt, new Position(2, row)));
        }
        // 도착 칸이 공격받는지는 다른 수와 똑같이 keepsKingSafe()에서 검사합니다.
    }

    // ---- 체크와 공격 ----

    /** 지금 차례인 쪽의 킹이 체크인지. */
    public static boolean isInCheck(Board board) {
        return isInCheck(board, board.turn());
    }

    /** side 진영의 킹이 공격받고 있는지. */
    public static boolean isInCheck(Board board, Side side) {
        Position king = board.findKing(side);
        return king != null && isAttacked(board, king, side.opposite());
    }

    /** by 진영의 기물 중 하나라도 square 칸을 공격하는지. */
    public static boolean isAttacked(Board board, Position square, Side by) {
        for (Position from : board.positionsOf(by)) {
            if (board.pieceAt(from).attacks(board, from, square)) {
                return true;
            }
        }
        return false;
    }

    // ---- 대국 종료 ----

    /** 체크메이트: 체크인데 둘 수 있는 수가 없음. */
    public static boolean isCheckmate(Board board) {
        return isInCheck(board) && !hasAnyLegalMove(board);
    }

    /** 스테일메이트: 체크가 아닌데 둘 수 있는 수가 없음 (무승부). */
    public static boolean isStalemate(Board board) {
        return !isInCheck(board) && !hasAnyLegalMove(board);
    }

    /** 양쪽 모두 체크메이트를 만들 기물이 부족하면 무승부입니다. */
    public static boolean isInsufficientMaterial(Board board) {
        if (board.isSandbox()) {
            return false;
        }
        int minorPieces = 0;
        Position whiteBishop = null;
        Position blackBishop = null;
        for (Side side : Side.values()) {
            for (Position position : board.positionsOf(side)) {
                Piece piece = board.pieceAt(position);
                if (piece instanceof King) {
                    continue;
                }
                if (piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen) {
                    return false;
                }
                minorPieces++;
                if (piece instanceof Bishop) {
                    if (side == Side.WHITE) {
                        whiteBishop = position;
                    } else {
                        blackBishop = position;
                    }
                }
            }
        }
        if (minorPieces <= 1) {
            return true; // 킹끼리, 또는 킹 + 비숍이나 나이트 하나
        }
        // 양쪽에 같은 색 칸 비숍이 하나씩만 있는 경우
        return minorPieces == 2 && whiteBishop != null && blackBishop != null
                && whiteBishop.isLight() == blackBishop.isLight();
    }

    // ---- 수의 종류 ----

    /** 킹이 두 칸 움직이는 캐슬링 수인지. */
    public static boolean isCastling(Board board, Move move) {
        return board.pieceAt(move.from()) instanceof King
                && Math.abs(move.to().col() - move.from().col()) == 2;
    }

    /** 앙파상으로 폰을 잡는 수인지. */
    public static boolean isEnPassant(Board board, Move move) {
        return board.pieceAt(move.from()) instanceof Pawn
                && move.to().equals(board.enPassant())
                && move.from().col() != move.to().col();
    }

    /** 이 수로 잡히는 기물 (없으면 null). 앙파상이면 옆 칸의 폰입니다. */
    public static Piece capturedPiece(Board board, Move move) {
        return board.pieceAt(capturedSquare(board, move));
    }

    /** 이 수로 잡히는 기물이 있는 칸: 보통은 도착 칸이고, 앙파상이면 도착 칸 옆(출발 칸과 같은 줄)입니다. */
    public static Position capturedSquare(Board board, Move move) {
        return isEnPassant(board, move) ? new Position(move.to().col(), move.from().row()) : move.to();
    }
}
