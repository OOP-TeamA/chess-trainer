package com.oopteama.chess.progress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 프로필 하나의 학습 기록(메모리 모델). EDT 전용.
 *
 * <p>02단계 범위: 레슨 완료와 용어 퀴즈 기록, 점수, 변경 리스너.
 * 퍼즐·AI 전적·기보 맞히기·오답 노트·즐겨찾기는 04단계에 추가한다.
 * 저장은 {@link ProgressStore}가 하며, 변경 리스너와 저장의 연결은 AppContext가 맡는다.
 *
 * <p>값이 실제로 바뀐 경우에만 리스너를 호출한다.
 */
public final class Progress {

    /** 완료 레슨 ID 하나의 점수. */
    static final int LESSON_POINTS = 10;
    /** 정답 용어 문항 ID 하나의 점수. */
    static final int TERM_QUESTION_POINTS = 1;

    private final Set<String> lessonsDone = new LinkedHashSet<>();
    private final Set<String> termQuizCorrectIds = new LinkedHashSet<>();
    private int termQuizAttempted;
    private int termQuizSolved;
    /** 이 단계가 아직 해석하지 않는 키(퍼즐·오답 노트 등). 다시 저장할 때 그대로 보존한다. */
    private final Map<String, String> preserved = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

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
     * @throws IllegalArgumentException ID가 비었거나 쉼표·공백·제어 문자를 포함할 때
     */
    public void markLessonDone(String lessonId) {
        if (lessonsDone.add(checkId(lessonId))) {
            fireChanged();
        }
    }

    /**
     * 용어 4지선다 한 문항의 결과를 기록한다(QUIZ-05). 호출마다 시도가 1 늘고,
     * 정답이면 정답 수가 1 늘며 문항 ID가 정답 집합에 들어간다(점수는 문항별 한 번).
     *
     * @param questionId 문항 ID
     * @param correct    정답 여부
     * @throws IllegalArgumentException ID가 비었거나 쉼표·공백·제어 문자를 포함할 때
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
     * 퍼즐 기록은 04단계에 연결한다.
     *
     * @return 점수
     */
    public int score() {
        return lessonsDone.size() * LESSON_POINTS + termQuizCorrectIds.size() * TERM_QUESTION_POINTS;
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
    void restore(Set<String> lessons, Set<String> correctIds, AttemptStats quiz, Map<String, String> others) {
        lessonsDone.addAll(lessons);
        termQuizCorrectIds.addAll(correctIds);
        termQuizAttempted = quiz.attempted();
        termQuizSolved = quiz.solved();
        preserved.putAll(others);
    }

    /** 해석하지 않고 보존 중인 키와 값(파일 순서). */
    Map<String, String> preserved() {
        return Collections.unmodifiableMap(preserved);
    }

    /**
     * 기록 파일의 목록 값에 들어갈 수 있는 ID인지 검사한다.
     *
     * @throws IllegalArgumentException 비었거나 쉼표·공백·제어 문자를 포함할 때
     */
    static String checkId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isEmpty() || id.chars().anyMatch(c -> c == ',' || Character.isWhitespace(c) || Character.isISOControl(c))) {
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
