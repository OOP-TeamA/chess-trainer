package com.oopteama.chess.model;

/**
 * 캐슬링을 할 수 있는 권리 4가지입니다.
 * 킹이나 해당 룩이 한 번이라도 움직이면(또는 룩이 잡히면) 그 권리는 사라집니다.
 */
public record CastlingRights(boolean whiteKingSide, boolean whiteQueenSide,
                             boolean blackKingSide, boolean blackQueenSide) {

    public static final CastlingRights NONE = new CastlingRights(false, false, false, false);

    /** 킹 쪽(g파일) 캐슬링 권리. */
    public boolean kingSide(Side side) {
        return side == Side.WHITE ? whiteKingSide : blackKingSide;
    }

    /** 퀸 쪽(c파일) 캐슬링 권리. */
    public boolean queenSide(Side side) {
        return side == Side.WHITE ? whiteQueenSide : blackQueenSide;
    }

    /**
     * square 칸의 기물이 움직이거나 잡혔을 때 남는 권리를 돌려줍니다.
     * 킹 시작 칸(e1, e8)이면 그 진영의 권리가 모두, 룩 시작 칸(a1, h1, a8, h8)이면 그쪽 권리만 사라집니다.
     */
    public CastlingRights without(Position square) {
        return switch (square.toString()) {
            case "e1" -> new CastlingRights(false, false, blackKingSide, blackQueenSide);
            case "h1" -> new CastlingRights(false, whiteQueenSide, blackKingSide, blackQueenSide);
            case "a1" -> new CastlingRights(whiteKingSide, false, blackKingSide, blackQueenSide);
            case "e8" -> new CastlingRights(whiteKingSide, whiteQueenSide, false, false);
            case "h8" -> new CastlingRights(whiteKingSide, whiteQueenSide, false, blackQueenSide);
            case "a8" -> new CastlingRights(whiteKingSide, whiteQueenSide, blackKingSide, false);
            default -> this;
        };
    }
}
