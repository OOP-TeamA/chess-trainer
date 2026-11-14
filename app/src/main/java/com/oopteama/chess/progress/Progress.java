package com.oopteama.chess.progress;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 프로필 하나의 학습 기록(메모리 모델). EDT 전용.
 *
 * <p>현재 범위: 레슨 완료, 퍼즐별 시도·정답, 용어 퀴즈 기록, 점수, 변경 리스너.
 * AI 전적·기보 맞히기·오답 노트·즐겨찾기는 04단계에 추가한다.
 * 저장은 {@link ProgressStore}가 하며, 변경 리스너와 저장의 연결은 AppContext가 맡는다.
 *
 * <p>값이 실제로 바뀐 경우에만 리스너를 호출한다.
 */
public final class Progress {

    /** 완료 레슨 ID 하나의 점수. */
    static final int LESSON_POINTS = 10;
    /** 해결한 퍼즐 ID 하나의 점수. */
    static final int PUZZLE_POINTS = 5;
    /** 정답 용어 문항 ID 하나의 점수. */
    static final int TERM_QUESTION_POINTS = 1;

    private final Set<String> lessonsDone = new LinkedHashSet<>();
    private final Set<String> termQuizCorrectIds = new LinkedHashSet<>();
    /** 퍼즐 ID별 시도·정답(처음 기록한 순서). 없는 ID는 UNSEEN. */
    private final Map<String, AttemptStats> puzzles = new LinkedHashMap<>();
    private int termQuizAttempted;
    private int termQuizSolved;
    /** 이 단계가 아직 해석하지 않는 키(오답 노트·즐겨찾기 등). 다시 저장할 때 그대로 보존한다. */
    private final Map<String, String> preserved = new LinkedHashMap<>();
    private static final AttemptStats NO_ATTEMPTS = new AttemptStats(0, 0);
    private final List<Runnable> listeners = new ArrayList<>();
    /** 읽지 못한 기록 파일에서 시작했으면 그 경로, 아니면 null. ProgressStore만 다룬다. */
    private Path unreadableSource;

    /** 빈 기록. {@link ProgressStore}만 만든다. */
    Progress() {
    }

    /**
     * 완료한 레슨 ID.
     *
     * @return 완료 순서를 유지한 불변 집합
     */
    public Set<String> lessonsDone() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(lessonsDone));
    }

    /**
     * 레슨을 완료로 기록한다(RULE-08). 이미 완료한 레슨이면 아무것도 바꾸지 않는다.
     *
     * @param lessonId 레슨 ID
     * @throws IllegalArgumentException ID가 비었거나 쉼표·등호(=)·공백·제어 문자를 포함할 때
     */
    public void markLessonDone(String lessonId) {
        if (lessonsDone.add(checkId(lessonId))) {
            fireChanged();
        }
    }

    /**
     * 퍼즐 풀이 하나가 끝났음을 기록한다. 풀이 종료당 한 번만 호출한다(중간 정답에는 호출하지 않음).
     * 시도가 1 늘고, {@code solved}면 정답도 1 는다. 이미 해결한 퍼즐은 실패를 기록해도 SOLVED를 유지한다.
     *
     * @param puzzleId 퍼즐 ID
     * @param solved   정답 공개(REVEAL) 전에 끝까지 풀었으면 true(HINT까지는 허용), 공개에 한 번이라도 도달했으면 false
     * @throws IllegalArgumentException ID가 비었거나 쉼표·등호(=)·공백·제어 문자를 포함할 때
     */
    public void recordPuzzle(String puzzleId, boolean solved) {
        AttemptStats before = puzzleAttempts(checkId(puzzleId));
        puzzles.put(puzzleId, new AttemptStats(before.attempted() + 1, before.solved() + (solved ? 1 : 0)));
        fireChanged();
    }

    /**
     * 퍼즐의 풀이 상태. 시도 0이면 UNSEEN, 정답이 1 이상이면 SOLVED, 그 밖에는 FAILED.
     *
     * @param puzzleId 퍼즐 ID
     * @return 풀이 상태
     */
    public PuzzleState puzzleState(String puzzleId) {
        AttemptStats stats = puzzleAttempts(puzzleId);
        if (stats.attempted() == 0) {
            return PuzzleState.UNSEEN;
        }
        return stats.solved() > 0 ? PuzzleState.SOLVED : PuzzleState.FAILED;
    }

    /**
     * 퍼즐의 누적 시도·정답 수. 주제별 정답률은 이 값을 주제마다 합산해 계산한다(QUIZ-03).
     *
     * @param puzzleId 퍼즐 ID
     * @return 시도와 REVEAL 전 완주 횟수. 기록이 없으면 0,0
     */
    public AttemptStats puzzleAttempts(String puzzleId) {
        Objects.requireNonNull(puzzleId, "puzzleId");
        return puzzles.getOrDefault(puzzleId, NO_ATTEMPTS);
    }

    /**
     * 용어 4지선다 한 문항의 결과를 기록한다(QUIZ-05). 호출마다 시도가 1 늘고,
     * 정답이면 정답 수가 1 늘며 문항 ID가 정답 집합에 들어간다(점수는 문항별 한 번).
     *
     * @param questionId 문항 ID
     * @param correct    정답 여부
     * @throws IllegalArgumentException ID가 비었거나 쉼표·등호(=)·공백·제어 문자를 포함할 때
     */
    public void recordTermQuiz(String questionId, boolean correct) {
        String id = checkId(questionId);
        termQuizAttempted++;
        if (correct) {
            termQuizSolved++;
            termQuizCorrectIds.add(id);
        }
        fireChanged();
    }

    /**
     * 용어 퀴즈의 누적 시도·정답 수(반복 응시 포함).
     *
     * @return 시도와 정답 답안 수
     */
    public AttemptStats termQuizAttempts() {
        return new AttemptStats(termQuizAttempted, termQuizSolved);
    }

    /**
     * 한 번이라도 맞힌 용어 문항 ID.
     *
     * @return 처음 맞힌 순서를 유지한 불변 집합
     */
    public Set<String> termQuizCorrectIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(termQuizCorrectIds));
    }

    /**
     * 순위 점수. 서로 다른 완료 레슨 ×10 + 해결 퍼즐 ID ×5 + 정답 용어 문항 ID ×1.
     *
     * @return 점수
     */
    public int score() {
        long solvedPuzzles = puzzles.values().stream().filter(s -> s.solved() > 0).count();
        return lessonsDone.size() * LESSON_POINTS
                + (int) solvedPuzzles * PUZZLE_POINTS
                + termQuizCorrectIds.size() * TERM_QUESTION_POINTS;
    }

    /**
     * 기록이 바뀔 때마다 호출할 리스너를 등록한다.
     *
     * @param r 리스너
     */
    public void addChangeListener(Runnable r) {
        listeners.add(Objects.requireNonNull(r, "r"));
    }

    /**
     * 등록한 리스너를 해제한다. 등록되지 않은 리스너면 아무것도 하지 않는다.
     *
     * @param r 리스너
     */
    public void removeChangeListener(Runnable r) {
        listeners.remove(Objects.requireNonNull(r, "r"));
    }

    // ---- ProgressStore 전용 ----

    /** 저장 파일에서 읽은 값을 리스너 호출 없이 채운다. */
    void restore(Set<String> lessons, Map<String, AttemptStats> puzzleStats, Set<String> correctIds,
                 AttemptStats quiz, Map<String, String> others) {
        lessonsDone.addAll(lessons);
        puzzles.putAll(puzzleStats);
        termQuizCorrectIds.addAll(correctIds);
        termQuizAttempted = quiz.attempted();
        termQuizSolved = quiz.solved();
        preserved.putAll(others);
    }

    /** 퍼즐 ID별 시도·정답(기록 순서). */
    Map<String, AttemptStats> puzzles() {
        return Collections.unmodifiableMap(puzzles);
    }

    /** 해석하지 않고 보존 중인 키와 값(파일 순서). */
    Map<String, String> preserved() {
        return Collections.unmodifiableMap(preserved);
    }

    /** 읽지 못한 기록 파일에서 왔음을 표시한다. 저장 전에 원본을 보존해야 한다. */
    void markUnreadableSource(Path file) {
        unreadableSource = Objects.requireNonNull(file, "file");
    }

    /** 읽지 못한 원본 파일. 정상적으로 읽었거나 이미 보존했으면 비어 있다. */
    Optional<Path> unreadableSource() {
        return Optional.ofNullable(unreadableSource);
    }

    /** 원본 보존을 마쳤음을 표시한다. */
    void clearUnreadableSource() {
        unreadableSource = null;
    }

    /**
     * 기록 파일의 목록 값이나 키(예: {@code puzzle.attempt.<id>})에 들어갈 수 있는 ID인지 검사한다.
     *
     * @throws IllegalArgumentException 비었거나 쉼표·등호(=)·공백·제어 문자를 포함할 때
     */
    static String checkId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isEmpty() || id.chars().anyMatch(c -> c == ',' || c == '=' || Character.isWhitespace(c) || Character.isISOControl(c))) {
            throw new IllegalArgumentException("기록할 수 없는 ID: '" + id + "'");
        }
        return id;
    }

    private void fireChanged() {
        for (Runnable r : List.copyOf(listeners)) {
            r.run();
        }
    }
}
