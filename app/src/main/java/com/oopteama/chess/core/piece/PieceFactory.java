package com.oopteama.chess.core.piece;

import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Side;

/** 상태가 없는 기물 12개를 보관하고 돌려주는 공장(Flyweight). */
public final class PieceFactory {

    private PieceFactory() {}

    private static final Piece WHITE_KING = new King(Side.WHITE);
    private static final Piece WHITE_QUEEN = new Queen(Side.WHITE);
    private static final Piece WHITE_ROOK = new Rook(Side.WHITE);
    private static final Piece WHITE_BISHOP = new Bishop(Side.WHITE);
    private static final Piece WHITE_KNIGHT = new Knight(Side.WHITE);
    private static final Piece WHITE_PAWN = new Pawn(Side.WHITE);

    private static final Piece BLACK_KING = new King(Side.BLACK);
    private static final Piece BLACK_QUEEN = new Queen(Side.BLACK);
    private static final Piece BLACK_ROOK = new Rook(Side.BLACK);
    private static final Piece BLACK_BISHOP = new Bishop(Side.BLACK);
    private static final Piece BLACK_KNIGHT = new Knight(Side.BLACK);
    private static final Piece BLACK_PAWN = new Pawn(Side.BLACK);

    /** 진영과 종류에 해당하는 공유 기물. */
    public static Piece of(Side side, PieceType type) {
        if (side == Side.WHITE) {
            return switch (type) {
                case KING -> WHITE_KING;
                case QUEEN -> WHITE_QUEEN;
                case ROOK -> WHITE_ROOK;
                case BISHOP -> WHITE_BISHOP;
                case KNIGHT -> WHITE_KNIGHT;
                case PAWN -> WHITE_PAWN;
            };
        }
        return switch (type) {
            case KING -> BLACK_KING;
            case QUEEN -> BLACK_QUEEN;
            case ROOK -> BLACK_ROOK;
            case BISHOP -> BLACK_BISHOP;
            case KNIGHT -> BLACK_KNIGHT;
            case PAWN -> BLACK_PAWN;
        };
    }

    /** 모르는 문자면 IllegalArgumentException. */
    public static Piece fromFenChar(char c) {
        Side side = Character.isUpperCase(c) ? Side.WHITE : Side.BLACK;
        PieceType type = switch (Character.toLowerCase(c)) {
            case 'k' -> PieceType.KING;
            case 'q' -> PieceType.QUEEN;
            case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP;
            case 'n' -> PieceType.KNIGHT;
            case 'p' -> PieceType.PAWN;
            default -> throw new IllegalArgumentException("모르는 기물 문자: " + c);
        };
        return of(side, type);
    }
}

