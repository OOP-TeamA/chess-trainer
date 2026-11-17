package com.oopteama.chess.backend.core;

import com.oopteama.chess.backend.core.Move.MoveRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 대국의 수순과 각 수 이후 국면을 보관한다. 보드 조회와 규칙 판정은 position()에서 한다. */
public final class ChessCore {

    public static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private record HistoryEntry(MoveRecord move, BoardPosition position) { }

    private BoardPosition start;
    private final List<HistoryEntry> history = new ArrayList<>();

    public ChessCore() {
        this(INITIAL_FEN);
    }

    public ChessCore(String fen) {
        loadFen(fen);
    }

    public ChessCore(String fen, List<String> sanSequence) {
        loadGame(fen, sanSequence);
    }

    /** 이후 수 입력이나 무르기에도 변하지 않는 현재 국면을 반환한다. */
    public BoardPosition position() {
        return history.isEmpty() ? start : history.getLast().position();
    }

    public String initialFen() {
        return start.toFen();
    }

    public List<MoveRecord> history() {
        return history.stream().map(HistoryEntry::move).toList();
    }

    public List<String> sanHistory() {
        return history.stream().map(entry -> entry.move().san()).toList();
    }

    public void loadFen(String fen) {
        start = ChessNotation.parseFen(fen);
        history.clear();
    }

    /** 전체 수순을 검증한 뒤 교체한다. 실패하면 기존 대국을 유지한다. */
    public void loadGame(String fen, List<String> sanSequence) {
        Objects.requireNonNull(sanSequence, "sanSequence");
        ChessCore replay = new ChessCore(fen);
        for (int i = 0; i < sanSequence.size(); i++) {
            try {
                replay.play(sanSequence.get(i));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("SAN " + (i + 1) + "번째 반수 ["
                        + sanSequence.get(i) + "]: " + e.getMessage(), e);
            }
        }
        start = replay.start;
        history.clear();
        history.addAll(replay.history);
    }

    public MoveRecord play(String san) {
        return play(ChessNotation.parseSan(position(), san));
    }

    public MoveRecord playUci(String uci) {
        return play(Move.fromUci(uci));
    }

    public MoveRecord play(String from, String to) {
        return play(new Move(BoardPosition.parseSquare(from), BoardPosition.parseSquare(to)));
    }

    public MoveRecord play(String from, String to, char promotion) {
        return play(new Move(BoardPosition.parseSquare(from), BoardPosition.parseSquare(to), promotion));
    }

    public MoveRecord play(Move move) {
        Objects.requireNonNull(move, "move");
        List<Move> legal = position().legalMoves();
        if (!legal.contains(move)) {
            throw new IllegalArgumentException("불법 수: " + move.uci());
        }
        BoardPosition next = position().simulateLegalMove(move);
        MoveRecord record = ChessNotation.recordSan(position(), next, move, legal);
        history.add(new HistoryEntry(record, next));
        return record;
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        history.removeLast();
        return true;
    }

    /** 합법 앙파상 가능 여부를 포함해 현재 국면의 등장 횟수를 센다. 스테일메이트 판정 여부*/
    public int repetitionCount() {
        String key = ChessNotation.positionKey(position(), true);
        int count = key.equals(ChessNotation.positionKey(start, true)) ? 1 : 0;
        for (HistoryEntry entry : history) {
            if (key.equals(ChessNotation.positionKey(entry.position(), true))) count++;
        }
        return count;
    }
}
