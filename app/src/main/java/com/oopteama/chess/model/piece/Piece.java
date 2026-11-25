package com.oopteama.chess.model.piece;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 모든 체스 기물의 공통 부모 클래스입니다.
 * 기물마다 움직이는 방법이 다르므로 attackedSquares()를 각 하위 클래스가 다르게 구현합니다(다형성).
 *
 * <pre>
 * Piece
 * ├── SlidingPiece (막힐 때까지 미끄러짐): Queen, Rook, Bishop
 * ├── StepPiece    (정해진 칸으로 뜀):     King, Knight
 * └── Pawn         (앞으로 가고 대각선으로 잡음)
 * </pre>
 */
public abstract class Piece {

    private final Side side;

    protected Piece(Side side) {
        this.side = Objects.requireNonNull(side, "side");
    }

    /** 이 기물의 진영. */
    public final Side side() {
        return side;
    }

    /** 기물 이름 (예: "룩"). */
    public abstract String name();

    /** 기보와 FEN에 쓰는 대문자 기호 (예: 'R'). */
    public abstract char symbol();

    /** AI가 쓰는 기물 가치 (폰 = 100). */
    public abstract int value();

    /**
     * from 칸에 있는 이 기물이 공격하는 칸 목록입니다.
     * 체크 판정과 이동 가능 칸 계산에 함께 쓰입니다.
     */
    public abstract List<Position> attackedSquares(Board board, Position from);

    /**
     * 기물 규칙만 따졌을 때 갈 수 있는 칸입니다(자기 킹이 위험해지는지는 Rules가 따로 검사).
     * 대부분의 기물은 공격하는 칸 중 같은 편 기물이 없는 칸으로 갈 수 있습니다. 폰은 이 메서드를 다시 정의합니다.
     */
    public List<Position> movableSquares(Board board, Position from) {
        List<Position> result = new ArrayList<>();
        for (Position to : attackedSquares(board, from)) {
            Piece target = board.pieceAt(to);
            if (target == null || target.side() != side) {
                result.add(to);
            }
        }
        return result;
    }

    /** from 칸의 이 기물이 target 칸을 공격하는지 여부. */
    public boolean attacks(Board board, Position from, Position target) {
        return attackedSquares(board, from).contains(target);
    }

    /** FEN 문자: 백은 대문자, 흑은 소문자 (예: 'R', 'r'). */
    public char fenChar() {
        return side == Side.WHITE ? symbol() : Character.toLowerCase(symbol());
    }

    /** 기호('K', 'Q', 'R', 'B', 'N', 'P')와 진영으로 기물을 만듭니다. */
    public static Piece create(char symbol, Side side) {
        return switch (Character.toUpperCase(symbol)) {
            case 'K' -> new King(side);
            case 'Q' -> new Queen(side);
            case 'R' -> new Rook(side);
            case 'B' -> new Bishop(side);
            case 'N' -> new Knight(side);
            case 'P' -> new Pawn(side);
            default -> throw new IllegalArgumentException("알 수 없는 기물 기호: " + symbol);
        };
    }

    /** FEN 문자 하나로 기물을 만듭니다 (대문자 = 백, 소문자 = 흑). */
    public static Piece fromFenChar(char c) {
        return create(c, Character.isUpperCase(c) ? Side.WHITE : Side.BLACK);
    }

    /** 같은 종류, 같은 진영의 기물이면 같다고 봅니다. */
    @Override
    public boolean equals(Object o) {
        return o instanceof Piece other && getClass() == other.getClass() && side == other.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), side);
    }

    @Override
    public String toString() {
        return side.label() + " " + name();
    }
}
