package com.oopteama.chess.ui;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.exercise.Exercise;
import com.oopteama.chess.exercise.FeedbackLadder;
import com.oopteama.chess.exercise.Verdict;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 레슨 단계·용어 직접 해보기·퍼즐이 함께 쓰는 공용 컨트롤러(RULE-02, RULE-03, QUIZ-01, GLOS-07).
 * {@link Exercise}와 {@link FeedbackLadder}, 프1의 {@link BoardPanel}을 잇는다. EDT 전용이다.
 *
 * <p>화면(프2·프3)은 {@link View}를 구현하고 {@link #start()}·{@link #detach()}를 부른다.
 * 학습 기록은 화면이 {@code Progress}에 풀이 종료당 한 번 남긴다. 퍼즐은 {@link #revealUsed()}가 true면
 * 완주해도 {@code recordPuzzle(id, false)}, HINT까지만 썼으면 true로 기록한다.
 *
 * <p>사다리 단계별 보드 처리(인터페이스 명세 시퀀스 2):
 * <ul>
 *   <li>오답마다 국면을 오답 직전으로 다시 그리고 틀린 칸을 {@link BoardPanel#flashWrong}으로 깜빡인다.</li>
 *   <li>HINT 이상: {@link Exercise#hintSquares()}를 {@link BoardPanel.Mark#HINT}로 표시한다.</li>
 *   <li>REVEAL: 이동 연습은 정답 수를 {@link BoardPanel.ArrowKind#SOLUTION} 화살표로, 칸 선택 연습은
 *       정답 칸을 {@link BoardPanel.Mark#SOLUTION}으로 표시한다. 정답을 공개해도 사용자가 정답을 입력해야
 *       다음 단계가 열린다.</li>
 * </ul>
 * 여러 수짜리 퍼즐의 중간 정답({@link Verdict.Continue})이 나오면 상대 응수까지 둔 국면을 그리고, 다음 수를 위해
 * 사다리를 처음 단계로 되돌린다. 공개 사용 여부는 {@link #restart()} 전까지 유지한다.
 */
public final class ExerciseController {

    private static final Logger LOG = Logger.getLogger(ExerciseController.class.getName());

    /** 컨트롤러가 화면에 알리는 일. 프2·프3의 화면이 구현한다. */
    public interface View {

        /**
         * 문제 설명을 보여준다. {@link #start()}와 {@link #restart()}에서 한 번 부른다.
         *
         * @param prompt 설명 문장
         */
        void showPrompt(String prompt);

        /**
         * 입력 한 번의 채점 결과를 보여준다. 보드 표시는 컨트롤러가 이미 끝낸 뒤다.
         *
         * @param verdict 채점 결과
         * @param stage   이 결과 뒤의 사다리 단계(정답·중간 정답이면 그 수를 맞히기 전까지의 최고 단계)
         */
        void showVerdict(Verdict verdict, FeedbackLadder.Stage stage);

        /**
         * 다음 단계 버튼을 켜거나 끈다. 시작하면 false, 정답(Solved)이면 true.
         *
         * @param enabled 켜려면 true
         */
        void setNextEnabled(boolean enabled);
    }

    private final Exercise exercise;
    private final BoardPanel board;
    private final View view;
    private final FeedbackLadder ladder = new FeedbackLadder();

    /** 보드에 한 번만 등록하고 detach에서 같은 인스턴스를 제거한다. */
    private final Consumer<Move> moveListener = this::onMove;
    private final Consumer<Square> squareListener = this::onSquare;

    private boolean attached;
    private boolean solved;
    private boolean revealUsed;

    /**
     * @param exercise 풀 연습
     * @param board    입력을 받을 보드(화면이 소유)
     * @param view     결과를 보여줄 화면
     */
    public ExerciseController(Exercise exercise, BoardPanel board, View view) {
        this.exercise = Objects.requireNonNull(exercise, "exercise");
        this.board = Objects.requireNonNull(board, "board");
        this.view = Objects.requireNonNull(view, "view");
    }

    /**
     * 보드에 국면·합법 수·입력 모드를 설정하고 입력 리스너를 등록한다. 여러 번 불러도 리스너는 하나다.
     * detach 뒤에 다시 부르면 진행 상태를 유지한 채 다시 연결한다.
     */
    public void start() {
        if (!attached) {
            if (exercise.inputMode() == InputMode.MOVE) {
                board.addMoveListener(moveListener);
            } else {
                board.addSquareListener(squareListener);
            }
            attached = true;
        }
        render(Optional.empty());
        view.showPrompt(exercise.prompt());
        view.setNextEnabled(solved);
    }

    /** 연습과 사다리를 처음 상태로 되돌리고 다시 그린다. 리스너 등록은 그대로 둔다. */
    public void restart() {
        exercise.reset();
        ladder.reset();
        solved = false;
        revealUsed = false;
        start();
    }

    /** 보드 리스너를 제거하고 입력을 잠근다. 단계를 바꾸거나 화면을 떠날 때 부른다. 여러 번 불러도 안전하다. */
    public void detach() {
        board.removeMoveListener(moveListener);
        board.removeSquareListener(squareListener);
        board.setInputMode(Optional.empty());
        attached = false;
    }

    /**
     * 이 연습을 끝냈는지.
     *
     * @return 정답(Solved)을 받았으면 true
     */
    public boolean solved() {
        return solved;
    }

    /**
     * 시작(또는 restart) 뒤 정답 공개(REVEAL) 단계에 한 번이라도 도달했는지. 퍼즐 기록에 쓴다.
     *
     * @return 공개를 봤으면 true
     */
    public boolean revealUsed() {
        return revealUsed;
    }

    /**
     * 지금 풀고 있는 수의 오답 횟수. 중간 정답 뒤에는 0부터 다시 센다.
     *
     * @return 0 이상
     */
    public int wrongCount() {
        return ladder.wrongCount();
    }

    /**
     * 지금 풀고 있는 수의 사다리 단계.
     *
     * @return 사다리 단계
     */
    public FeedbackLadder.Stage stage() {
        return ladder.stage();
    }

    /** 보드가 내보낸 수를 채점한다. 리스너와 같은 패키지의 테스트가 부른다. */
    void onMove(Move move) {
        if (!acceptsInput(InputMode.MOVE)) {
            return;
        }
        Verdict verdict;
        try {
            verdict = exercise.submit(move);
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOG.warning("채점할 수 없는 입력을 무시합니다: " + e.getMessage());
            render(Optional.empty());
            return;
        }
        switch (verdict) {
            case Verdict.Solved s -> finish(s, Optional.of(move), Set.of());
            case Verdict.Continue c -> proceed(c);
            case Verdict.Wrong w -> wrong(w, move.to());
        }
    }

    /** 보드가 내보낸 칸을 채점한다. 리스너와 같은 패키지의 테스트가 부른다. */
    void onSquare(Square square) {
        if (!acceptsInput(InputMode.SQUARE)) {
            return;
        }
        Verdict verdict;
        try {
            verdict = exercise.submitSquare(square);
        } catch (IllegalStateException e) {
            LOG.warning("채점할 수 없는 입력을 무시합니다: " + e.getMessage());
            return;
        }
        switch (verdict) {
            case Verdict.Solved s -> finish(s, Optional.empty(), Set.of(square));
            case Verdict.Wrong w -> wrong(w, square);
            case Verdict.Continue c -> throw new IllegalStateException("칸 선택 연습은 중간 정답이 없습니다");
        }
    }

    private boolean acceptsInput(InputMode mode) {
        return attached && !solved && exercise.inputMode() == mode;
    }

    /** 정답. 둔 수(이동 연습)나 고른 칸(칸 선택 연습)을 표시하고 입력을 잠근다. */
    private void finish(Verdict.Solved verdict, Optional<Move> last, Set<Square> chosen) {
        FeedbackLadder.Stage stage = ladder.stage();
        solved = true;
        render(last);
        if (!chosen.isEmpty()) {
            board.mark(chosen, BoardPanel.Mark.SOLUTION);
        }
        view.showVerdict(verdict, stage);
        view.setNextEnabled(true);
    }

    private void proceed(Verdict.Continue verdict) {
        FeedbackLadder.Stage stage = ladder.stage();
        ladder.reset();
        render(Optional.of(verdict.reply()));
        view.showVerdict(verdict, stage);
    }

    private void wrong(Verdict.Wrong verdict, Square at) {
        FeedbackLadder.Stage stage = ladder.recordWrong();
        if (stage == FeedbackLadder.Stage.REVEAL) {
            revealUsed = true;
        }
        render(Optional.empty());
        board.flashWrong(at);
        view.showVerdict(verdict, stage);
    }

    /**
     * 연습의 현재 국면을 보드에 다시 그린다. {@link BoardPanel#setPosition}이 표시를 모두 지우므로
     * 그 뒤에 합법 수·체크·마지막 수와 현재 사다리 단계의 표시를 다시 얹는다.
     */
    private void render(Optional<Move> last) {
        Position position = exercise.position();
        board.setPosition(position);
        board.setLegalMoves(exercise.legalMoves());
        board.setInputMode(solved || !attached ? Optional.empty() : Optional.of(exercise.inputMode()));
        board.setCheckSquare(exercise.rules().inCheck(position)
                ? position.kingSquare(position.sideToMove())
                : Optional.empty());
        last.ifPresent(m -> board.mark(List.of(m.from(), m.to()), BoardPanel.Mark.LAST_MOVE));
        if (solved) {
            return;
        }
        FeedbackLadder.Stage stage = ladder.stage();
        if (stage.compareTo(FeedbackLadder.Stage.HINT) >= 0) {
            board.mark(exercise.hintSquares(), BoardPanel.Mark.HINT);
        }
        if (stage == FeedbackLadder.Stage.REVEAL) {
            if (exercise.inputMode() == InputMode.MOVE) {
                exercise.solution().ifPresent(m -> board.setArrows(
                        List.of(new BoardPanel.Arrow(m.from(), m.to(), BoardPanel.ArrowKind.SOLUTION))));
            } else {
                board.mark(exercise.solutionSquares(), BoardPanel.Mark.SOLUTION);
            }
        }
    }
}
