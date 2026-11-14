package com.oopteama.chess.exercise;

import com.oopteama.chess.content.TermQuestion;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 용어 4지선다 한 세트의 진행과 채점(QUIZ-05). 보드 입력이 아니라서 {@link Exercise}와 따로 둔다.
 *
 * <p>문항마다 {@link #submit(int)}으로 채점한 뒤 {@link #next()}로 넘어간다. 마지막 문항에서 next()를 부르면
 * {@link #finished()}가 된다. 문항 수(10개)와 부족할 때의 안내는 화면이 정한다. 상태를 가지며 EDT 전용이다.
 */
public final class TermQuizSession {

    private final List<TermQuestion> questions;
    private int index;
    private boolean answered;
    private int correctCount;

    /**
     * @param questions 풀 문항(순서대로 출제). 화면은 파일 순서의 처음 10문항을 넘긴다
     * @throws IllegalArgumentException 문항이 하나도 없을 때
     */
    public TermQuizSession(List<TermQuestion> questions) {
        this.questions = List.copyOf(Objects.requireNonNull(questions, "questions"));
        if (this.questions.isEmpty()) {
            throw new IllegalArgumentException("문항이 하나 이상 있어야 합니다");
        }
    }

    /**
     * 지금 풀 문항. 채점한 뒤에도 next()를 부르기 전까지는 같은 문항이다.
     *
     * @return 세트가 끝났으면 empty
     */
    public Optional<TermQuestion> current() {
        return finished() ? Optional.empty() : Optional.of(questions.get(index));
    }

    /**
     * 현재 문항을 채점한다. 한 문항은 한 번만 제출할 수 있다.
     *
     * @param choiceIndex 고른 선택지의 위치(0~3)
     * @return 정답 여부와 정답 위치, 해설
     * @throws IllegalArgumentException choiceIndex가 선택지 범위 밖일 때
     * @throws IllegalStateException    세트가 끝났거나 현재 문항을 이미 제출했을 때
     */
    public TermAnswer submit(int choiceIndex) {
        if (finished()) {
            throw new IllegalStateException("세트가 끝났습니다");
        }
        if (answered) {
            throw new IllegalStateException("이미 제출한 문항입니다. next()로 넘어가세요");
        }
        TermQuestion q = questions.get(index);
        if (choiceIndex < 0 || choiceIndex >= q.choices().size()) {
            throw new IllegalArgumentException("선택지 범위 밖입니다: " + choiceIndex);
        }
        boolean correct = choiceIndex == q.answerIndex();
        answered = true;
        if (correct) {
            correctCount++;
        }
        return new TermAnswer(q.id(), correct, q.answerIndex(), q.explanation());
    }

    /**
     * 다음 문항으로 넘어간다. 마지막 문항이었으면 세트가 끝난다.
     *
     * @throws IllegalStateException 현재 문항을 아직 제출하지 않았거나 세트가 이미 끝났을 때
     */
    public void next() {
        if (finished() || !answered) {
            throw new IllegalStateException("현재 문항을 제출한 뒤에 넘어갈 수 있습니다");
        }
        index++;
        answered = false;
    }

    /**
     * 모든 문항을 제출하고 넘겼는지.
     *
     * @return 끝났으면 true
     */
    public boolean finished() {
        return index >= questions.size();
    }

    /**
     * 지금까지 맞힌 문항 수.
     *
     * @return 0 이상 {@link #totalCount()} 이하
     */
    public int correctCount() {
        return correctCount;
    }

    /**
     * 세트의 전체 문항 수.
     *
     * @return 1 이상
     */
    public int totalCount() {
        return questions.size();
    }
}
