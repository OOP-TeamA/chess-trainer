package com.oopteama.chess.model;

import com.oopteama.chess.model.piece.Piece;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 한 판의 대국입니다. 시작 국면부터 둔 수를 기록하고, 무르기·기권·대국 결과를 처리합니다.
 */
public final class Game {

    /** 대국 상태. */
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

    /** 기보 한 줄: 둔 수, 움직인 기물, 잡은 기물(없으면 null), 기보 표기(SAN). */
    public record PlayedMove(Move move, Piece piece, Piece captured, String san) {
    }

    private Board startBoard;
    private final List<PlayedMove> moves = new ArrayList<>();
    private final List<Board> boards = new ArrayList<>(); // boards.get(i) = i번째 수를 둔 뒤의 판
    private Side resignedSide;

    public Game() {
        this(Board.initial());
    }

    public Game(Board startBoard) {
        this.startBoard = Objects.requireNonNull(startBoard, "startBoard");
    }

    /** 지금 국면. */
    public Board board() {
        return boards.isEmpty() ? startBoard : boards.getLast();
    }

    public Board startBoard() {
        return startBoard;
    }

    public Side turn() {
        return board().turn();
    }

    /** 지금까지 둔 수 (기보). */
    public List<PlayedMove> history() {
        return List.copyOf(moves);
    }

    /** 마지막에 둔 수 (아직 없으면 null). */
    public PlayedMove lastMove() {
        return moves.isEmpty() ? null : moves.getLast();
    }

    // ---- 진행 ----

    /** 수를 둡니다. 둘 수 없는 수이거나 대국이 끝났으면 예외가 납니다. */
    public PlayedMove play(Move move) {
        if (isOver()) {
            throw new IllegalStateException("이미 끝난 대국입니다: " + status());
        }
        Board before = board();
        if (!Rules.legalMoves(before).contains(move)) {
            throw new IllegalArgumentException("둘 수 없는 수입니다: " + move);
        }
        PlayedMove played = new PlayedMove(move, before.pieceAt(move.from()),
                Rules.capturedPiece(before, move), Notation.toSan(before, move));
        moves.add(played);
        boards.add(before.apply(move));
        return played;
    }

    /** 마지막 수 하나를 무릅니다. 무를 수가 없으면 false. */
    public boolean undo() {
        if (moves.isEmpty()) {
            return false;
        }
        moves.removeLast();
        boards.removeLast();
        resignedSide = null;
        return true;
    }

    /** 최대 count수를 무르고, 실제로 무른 수를 돌려줍니다. */
    public int undo(int count) {
        int undone = 0;
        while (undone < count && undo()) {
            undone++;
        }
        return undone;
    }

    /** side가 기권합니다. */
    public void resign(Side side) {
        if (!isOver()) {
            resignedSide = Objects.requireNonNull(side, "side");
        }
    }

    /** 같은 시작 국면에서 새로 시작합니다. */
    public void reset() {
        moves.clear();
        boards.clear();
        resignedSide = null;
    }

    /** 다른 시작 국면에서 새로 시작합니다. */
    public void reset(Board newStart) {
        startBoard = Objects.requireNonNull(newStart, "newStart");
        reset();
    }

    // ---- 결과 판정 ----

    public Status status() {
        if (resignedSide != null) {
            return resignedSide == Side.WHITE ? Status.RESIGNED_WHITE : Status.RESIGNED_BLACK;
        }
        Board current = board();
        boolean inCheck = Rules.isInCheck(current);
        if (!Rules.hasAnyLegalMove(current)) {
            return inCheck ? Status.CHECKMATE : Status.STALEMATE;
        }
        if (!current.isSandbox()) {
            if (Rules.isInsufficientMaterial(current)) {
                return Status.DRAW_INSUFFICIENT;
            }
            if (current.halfmoveClock() >= 100) {
                return Status.DRAW_FIFTY_MOVE;
            }
            if (repetitionCount() >= 3) {
                return Status.DRAW_REPETITION;
            }
        }
        return inCheck ? Status.CHECK : Status.PLAYING;
    }

    public boolean isOver() {
        return status().isOver();
    }

    /** 이긴 쪽 (무승부이거나 진행 중이면 null). */
    public Side winner() {
        return switch (status()) {
            case CHECKMATE -> turn().opposite();
            case RESIGNED_WHITE -> Side.BLACK;
            case RESIGNED_BLACK -> Side.WHITE;
            default -> null;
        };
    }

    /** 화면에 보여 줄 상태 문구. */
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

    /** 지금 국면이 대국 중에 몇 번 나왔는지 (3번이면 무승부). */
    public int repetitionCount() {
        String key = repetitionKey(board());
        int count = repetitionKey(startBoard).equals(key) ? 1 : 0;
        for (Board past : boards) {
            if (repetitionKey(past).equals(key)) {
                count++;
            }
        }
        return count;
    }

    /** 같은 국면인지 비교할 때 쓰는 값: 기물 배치, 차례, 캐슬링 권리, 실제로 가능한 앙파상. */
    private static String repetitionKey(Board board) {
        String[] fields = board.toFen().split(" ");
        boolean canCaptureEnPassant = board.enPassant() != null
                && Rules.legalMoves(board).stream().anyMatch(m -> Rules.isEnPassant(board, m));
        return fields[0] + " " + fields[1] + " " + fields[2] + " " + (canCaptureEnPassant ? fields[3] : "-");
    }

    /** capturer가 잡은 상대 기물 목록 (가치가 큰 순서). */
    public List<Piece> capturedBy(Side capturer) {
        List<Piece> result = new ArrayList<>();
        for (PlayedMove played : moves) {
            if (played.piece().side() == capturer && played.captured() != null) {
                result.add(played.captured());
            }
        }
        result.sort(Comparator.comparingInt(Piece::value).reversed());
        return result;
    }
}
