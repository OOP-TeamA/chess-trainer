package com.oopteama.chess.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 수순과 3회 반복 판정용 키 기록. Game이 소유하고 갱신한다.
 * 키는 {@code RuleSet.repetitionKey}가 만든 문자열이며, 시작 국면도 1회로 센다.
 */
public final class GameHistory {

    private final String startFen;
    private final List<Move> moves = new ArrayList<>();
    private final List<String> keys = new ArrayList<>(); // keys[0] = 시작 국면, keys[i] = i번째 수 뒤
    private final Map<String, Integer> counts = new HashMap<>();

    /**
     * @param startFen 시작 국면의 FEN
     * @param startKey 시작 국면의 반복 키
     */
    public GameHistory(String startFen, String startKey) {
        this.startFen = Objects.requireNonNull(startFen, "startFen");
        Objects.requireNonNull(startKey, "startKey");
        keys.add(startKey);
        counts.put(startKey, 1);
    }

    /**
     * 수 하나와 그 수를 둔 뒤 국면의 반복 키를 기록한다.
     *
     * @param move     둔 수
     * @param keyAfter 둔 뒤 국면의 반복 키
     */
    public void push(Move move, String keyAfter) {
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(keyAfter, "keyAfter");
        moves.add(move);
        keys.add(keyAfter);
        counts.merge(keyAfter, 1, Integer::sum);
    }

    /**
     * 마지막으로 기록한 수를 지운다.
     *
     * @throws IllegalStateException 기록된 수가 없을 때
     */
    public void pop() {
        if (moves.isEmpty()) {
            throw new IllegalStateException("되돌릴 수가 없습니다.");
        }
        moves.remove(moves.size() - 1);
        String key = keys.remove(keys.size() - 1);
        counts.computeIfPresent(key, (k, n) -> n == 1 ? null : n - 1);
    }

    /** 시작 국면의 FEN. */
    public String startFen() {
        return startFen;
    }

    /** 지금까지 둔 수의 불변 사본. */
    public List<Move> moves() {
        return List.copyOf(moves);
    }

    /** 이 키의 국면이 시작 국면을 포함해 몇 번 나왔는지. 없으면 0. */
    public int occurrences(String repetitionKey) {
        return counts.getOrDefault(repetitionKey, 0);
    }
}
