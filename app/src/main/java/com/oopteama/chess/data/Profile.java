package com.oopteama.chess.data;

import com.oopteama.chess.ai.Difficulty;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 사용자 프로필 하나의 학습 기록입니다: 완료한 레슨, 퍼즐 기록, AI 대전 전적.
 */
public final class Profile {

    private final String id;
    private String name;
    private final Set<String> completedLessons = new LinkedHashSet<>();
    private final Map<String, int[]> puzzleStats = new LinkedHashMap<>();          // 퍼즐 id → {시도, 성공}
    private final Map<Difficulty, int[]> aiRecords = new EnumMap<>(Difficulty.class); // 난이도 → {승, 무, 패}

    public Profile(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name").trim();
        for (Difficulty difficulty : Difficulty.values()) {
            aiRecords.put(difficulty, new int[3]);
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

    /** 퍼즐 한 번 시도를 기록합니다. */
    public void recordPuzzle(String puzzleId, boolean solved) {
        int[] stat = puzzleStats.computeIfAbsent(puzzleId, k -> new int[2]);
        stat[0]++;
        if (solved) {
            stat[1]++;
        }
    }

    /** 저장된 기록을 불러올 때 씁니다. */
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

    /** 퍼즐 정답률(%). */
    public int puzzleAccuracyPercent() {
        int attempts = 0;
        int solves = 0;
        for (int[] stat : puzzleStats.values()) {
            attempts += stat[0];
            solves += stat[1];
        }
        return attempts == 0 ? 0 : Math.round(100f * solves / attempts);
    }

    // ---- AI 대전 ----

    /** AI 대전 결과를 기록합니다. outcome: 1 = 승, 0 = 무, -1 = 패 */
    public void recordAiResult(Difficulty difficulty, int outcome) {
        int[] record = aiRecords.get(difficulty);
        if (outcome > 0) {
            record[0]++;
        } else if (outcome == 0) {
            record[1]++;
        } else {
            record[2]++;
        }
    }

    /** 저장된 기록을 불러올 때 씁니다. */
    public void putAiRecord(Difficulty difficulty, int wins, int draws, int losses) {
        aiRecords.put(difficulty, new int[]{Math.max(0, wins), Math.max(0, draws), Math.max(0, losses)});
    }

    /** {승, 무, 패} */
    public int[] aiRecord(Difficulty difficulty) {
        return aiRecords.get(difficulty).clone();
    }

    public int aiWins() {
        return sumAi(0);
    }

    public int aiDraws() {
        return sumAi(1);
    }

    public int aiLosses() {
        return sumAi(2);
    }

    private int sumAi(int index) {
        int sum = 0;
        for (int[] record : aiRecords.values()) {
            sum += record[index];
        }
        return sum;
    }

    // ---- 점수 ----

    /** 총점 = 레슨 완료 × 10 + 퍼즐 해결 × 5 + AI 승리 × 3 */
    public int totalScore() {
        return completedLessons.size() * 10 + solvedPuzzleCount() * 5 + aiWins() * 3;
    }

    /** 이 프로필의 모든 기록을 지웁니다. */
    public void clearProgress() {
        completedLessons.clear();
        puzzleStats.clear();
        for (Difficulty difficulty : Difficulty.values()) {
            aiRecords.put(difficulty, new int[3]);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
