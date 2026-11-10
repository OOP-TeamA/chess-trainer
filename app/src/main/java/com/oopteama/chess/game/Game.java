package com.oopteama.chess.game;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 대국 상태의 단일 소유자. Position과 GameHistory를 함께 갱신해 둘이 어긋나지 않게 한다. EDT 전용. */
public final class Game {

    private final RuleSet rules;
    private final Position start;   // history() 사본을 다시 만들 때 쓰는 시작 국면
    private final Position pos;
    private final GameHistory history;
    private final List<GameListener> listeners = new ArrayList<>();
    private GameResult result;

    public Game(Position start, RuleSet rules) {
        Objects.requireNonNull(start);
        this.rules = Objects.requireNonNull(rules);
        this.start = start.copy();
        this.pos = start.copy();   // copy()는 되돌리기 스택을 비우므로 시작 국면 너머로는 undo 불가(G12)
        this.history = new GameHistory(Fen.write(start), rules.repetitionKey(start));
        this.result = rules.checkEnd(pos, history).orElse(null);   // 시작부터 끝난 국면 대비
    }

    public Position snapshot() { return pos.copy(); }

    public Side sideToMove() { return pos.sideToMove(); }

    /** 종료 후에는 빈 목록. */
    public List<Move> legalMoves() {
        return result != null ? List.of() : List.copyOf(rules.legalMoves(pos));
    }

    /** legalMoves()에 없으면 IllegalArgumentException, 종료 상태면 IllegalStateException. */
    public void make(Move move) {
        Objects.requireNonNull(move);
        if (result != null) throw new IllegalStateException("이미 끝난 대국");
        if (!rules.legalMoves(pos).contains(move)) throw new IllegalArgumentException("합법 수가 아님: " + move);

        pos.make(move);
        history.push(move, rules.repetitionKey(pos));
        result = rules.checkEnd(pos, history).orElse(null);

        for (GameListener l : snapshotListeners()) l.moveMade(move, pos.copy());   // 리스너마다 사본
        if (result != null) {
            GameResult r = result;
            for (GameListener l : snapshotListeners()) l.gameEnded(r);
        }
    }

    /**
     * plies수만큼 되돌린다. 종료 상태(기권 포함)도 해제. 되돌릴 수보다 많으면 IllegalArgumentException.
     * 0이면 아무것도 바꾸지 않는다(종료 상태도 그대로).
     */
    public void undo(int plies) {
        if (plies < 0 || plies > history.moves().size()) {
            throw new IllegalArgumentException("되돌릴 수 없는 수: " + plies);
        }
        if (plies == 0) return;
        result = null;
        for (int i = 0; i < plies; i++) {
            List<Move> moves = history.moves();
            Move last = moves.get(moves.size() - 1);
            pos.unmake();
            history.pop();
            for (GameListener l : snapshotListeners()) l.moveUndone(last, pos.copy());
        }
    }

    public void resign(Side loser) {
        Objects.requireNonNull(loser);
        if (result != null) throw new IllegalStateException("이미 끝난 대국");
        result = GameResult.resignation(loser);
        GameResult r = result;
        for (GameListener l : snapshotListeners()) l.gameEnded(r);
    }

    public Optional<GameResult> result() { return Optional.ofNullable(result); }

    /**
     * 수순과 반복 기록의 사본. 바꿔도 대국에는 영향이 없다(국면과 기록이 어긋나지 않도록).
     * 호출마다 시작 국면부터 다시 만들므로 수 개수에 비례하는 비용이 든다.
     * 다른 스레드(AI 탐색)에 넘겨도 안전하다.
     */
    public GameHistory history() {
        Position replay = start.copy();
        GameHistory copy = new GameHistory(history.startFen(), rules.repetitionKey(replay));
        for (Move m : history.moves()) {
            replay.make(m);
            copy.push(m, rules.repetitionKey(replay));
        }
        return copy;
    }

    // TODO(AI-08, GAME-06): toPgn(Map<String, String> tags) : notation.PgnGame
    //  notation 패키지(CORE-08 San/Pgn, 백1)가 들어오는 11/20 PGN 단계에서 추가한다.
    //  - import는 com.oopteama.chess.notation.* (core.notation 아님, ArchitectureTest L1)
    //  - 시작 국면은 Fen.parse(history.startFen())으로 재생하고 San.write(before, rules, m)로 수마다 SAN 생성
    //  - 시작 국면이 Fen.INITIAL이 아니면 SetUp=1, FEN 태그 추가(G6). Result 태그 = result.pgnToken() 또는 "*"

    public void addListener(GameListener l) { listeners.add(Objects.requireNonNull(l)); }

    public void removeListener(GameListener l) { listeners.remove(l); }

    /** 콜백 안에서 리스너를 추가/제거해도 안전하도록 사본을 순회. */
    private List<GameListener> snapshotListeners() { return new ArrayList<>(listeners); }
}
