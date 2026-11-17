package com.oopteama.chess.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * UI 체스판이 한 국면을 그리는 데 필요한 읽기 전용 스냅샷.
 * 규칙 계산 없이 이 객체만으로 기물, 체크 상태, 마지막 수, 합법 수 힌트를 모두 그릴 수 있다.
 */
public record BoardSnapshot(
        List<PieceView> squares,
        SideColor turn,
        OptionalInt checkSquare,
        Optional<MoveRange> lastMove,
        List<LegalMoveView> legalMoves
) {

    public static final int SQUARE_COUNT = 64;

    /** 직전 수의 출발/도착 칸 인덱스. */
    public record MoveRange(int from, int to) {
        public MoveRange {
            checkSquare(from);
            checkSquare(to);
        }
    }

    public BoardSnapshot {
        Objects.requireNonNull(squares, "squares");
        if (squares.size() != SQUARE_COUNT) {
            throw new IllegalArgumentException("squares size must be 64: " + squares.size());
        }
        squares = Collections.unmodifiableList(new ArrayList<>(squares));
        Objects.requireNonNull(turn, "turn");
        Objects.requireNonNull(checkSquare, "checkSquare");
        checkSquare.ifPresent(BoardSnapshot::checkSquare);
        Objects.requireNonNull(lastMove, "lastMove");
        legalMoves = List.copyOf(Objects.requireNonNull(legalMoves, "legalMoves"));
    }

    public static BoardSnapshot empty() {
        return new BoardSnapshot(
                Collections.nCopies(SQUARE_COUNT, null),
                SideColor.WHITE,
                OptionalInt.empty(),
                Optional.empty(),
                List.of()
        );
    }

    public Optional<PieceView> pieceAt(int square) {
        checkSquare(square);
        return Optional.ofNullable(squares.get(square));
    }

    private static void checkSquare(int square) {
        if (square < 0 || square >= SQUARE_COUNT) {
            throw new IllegalArgumentException("Square index must be 0..63: " + square);
        }
    }
}
