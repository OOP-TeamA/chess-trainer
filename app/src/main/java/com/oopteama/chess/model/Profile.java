package com.oopteama.chess.model;

import com.oopteama.chess.engine.ChessAI.Difficulty;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 사용자 프로필과 학습·대국 기록(레슨 완료, 퍼즐 통계, AI 전적)을 보관합니다.
 */
public final class Profile {

    private final String id;
    private String name;
    private final Set<String> completedLessons = new LinkedHashSet<>();
    private final Map<String, int[]> puzzleStats = new LinkedHashMap<>(); // [attempts, solves]
    private final Map<Difficulty, int[]> aiRecords = new EnumMap<>(Difficulty.class); // [wins, draws, losses]

    public Profile(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name").trim();
        for (Difficulty d : Difficulty.values()) {
            aiRecords.put(d, new int[3]);
        }
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        String trimmed = Objects.requireNonNull(name, "name").trim();
        if (!trimmed.isEmpty()) {
            this.name = trimmed;
        }
    }

    // ---- 레슨 ----

    public boolean isLessonCompleted(String lessonId) {
        return completedLessons.contains(lessonId);
    }

    public void markLessonCompleted(String lessonId) {
        completedLessons.add(lessonId);
    }

    public Set<String> completedLessons() {
        return Collections.unmodifiableSet(completedLessons);
    }

    // ---- 퍼즐 ----

    public void recordPuzzle(String puzzleId, boolean solved) {
        int[] stat = puzzleStats.computeIfAbsent(puzzleId, k -> new int[2]);
        stat[0]++;
        if (solved) {
            stat[1]++;
        }
    }

    public void putPuzzleStat(String puzzleId, int attempts, int solves) {
        puzzleStats.put(puzzleId, new int[]{Math.max(0, attempts), Math.max(0, solves)});
    }

    public boolean isPuzzleSolved(String puzzleId) {
        int[] stat = puzzleStats.get(puzzleId);
        return stat != null && stat[1] > 0;
    }

    public Map<String, int[]> puzzleStats() {
        return Collections.unmodifiableMap(puzzleStats);
    }

    public int solvedPuzzleCount() {
        int count = 0;
        for (int[] stat : puzzleStats.values()) {
            if (stat[1] > 0) {
                count++;
            }
        }
        return count;
    }

    public int puzzleAttempts() {
        int total = 0;
        for (int[] stat : puzzleStats.values()) {
            total += stat[0];
        }
        return total;
    }

    public int puzzleSolves() {
        int total = 0;
        for (int[] stat : puzzleStats.values()) {
            total += stat[1];
        }
        return total;
    }

    public int puzzleAccuracyPercent() {
        int attempts = puzzleAttempts();
        return attempts == 0 ? 0 : (int) Math.round(100.0 * puzzleSolves() / attempts);
    }

    // ---- AI 대전 ----

    /**
     * @param outcome +1(승), 0(무), -1(패)
     */
    public void recordAiResult(Difficulty difficulty, int outcome) {
        int[] rec = aiRecords.get(difficulty);
        if (outcome > 0) {
            rec[0]++;
        } else if (outcome == 0) {
            rec[1]++;
        } else {
            rec[2]++;
        }
    }

    public void putAiRecord(Difficulty difficulty, int wins, int draws, int losses) {
        aiRecords.put(difficulty, new int[]{Math.max(0, wins), Math.max(0, draws), Math.max(0, losses)});
    }

    public int[] aiRecord(Difficulty difficulty) {
        return aiRecords.get(difficulty).clone();
    }

    public int aiWins() {
        return aiRecords.values().stream().mapToInt(r -> r[0]).sum();
    }

    public int aiDraws() {
        return aiRecords.values().stream().mapToInt(r -> r[1]).sum();
    }

    public int aiLosses() {
        return aiRecords.values().stream().mapToInt(r -> r[2]).sum();
    }

    // ---- 점수 및 초기화 ----

    public int totalScore() {
        return completedLessons.size() * 10 + solvedPuzzleCount() * 5 + aiWins() * 3;
    }

    public void clearProgress() {
        completedLessons.clear();
        puzzleStats.clear();
        for (Difficulty d : Difficulty.values()) {
            aiRecords.put(d, new int[3]);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
