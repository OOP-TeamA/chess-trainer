package com.oopteama.chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.exercise.Exercise;
import com.oopteama.chess.exercise.FeedbackLadder.Stage;
import com.oopteama.chess.exercise.LessonStepExercise;
import com.oopteama.chess.exercise.Verdict;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.LessonRepository;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** RULE-02·03, QUIZ-01: 공용 컨트롤러가 채점 결과를 보드 표시와 화면 콜백으로 옮긴다. EDT에서 실행한다. */
class ExerciseControllerTest {

    private static Lesson knight;

    @BeforeAll
    static void loadKnight() throws Exception {
        try (Reader in = new DataPaths(Path.of("unused")).openResource("data/lessons/07_knight.txt")) {
            knight = new LessonRepository().loadAll(in, "07_knight.txt").get(0);
        }
    }

    private static Move knightMove(String from, String to) {
        return new Move(Square.of(from), Square.of(to), MoveKind.QUIET);
    }

    private static Set<Square> squares(String... names) {
        Set<Square> out = new LinkedHashSet<>();
        for (String n : names) {
            out.add(Square.of(n));
        }
        return out;
    }

    private static void onEdt(Runnable body) throws Exception {
        SwingUtilities.invokeAndWait(body);
    }

    @Test
    void startShowsThePositionUnlocksInputAndRegistersOnce() throws Exception {
        onEdt(() -> {
            RecordingBoard board = new RecordingBoard();
            RecordingView view = new RecordingView();
            Exercise exercise = new LessonStepExercise(knight.steps().get(1));
            ExerciseController controller = new ExerciseController(exercise, board, view);

            controller.start();
            controller.start();

            assertEquals(1, board.moveListenerAdds);
            assertEquals(0, board.squareListenerAdds);
            assertEquals(Optional.of(InputMode.MOVE), board.inputMode);
            assertEquals(exercise.legalMoves(), board.legalMoves);
            assertEquals(Fen.write(exercise.position()), Fen.write(board.shown));
            assertEquals(List.of(knight.steps().get(1).text(), knight.steps().get(1).text()), view.prompts);
            assertEquals(List.of(false, false), view.nextEnabled);
        });
    }

    @Test
    void moveStepClimbsTheLadderAndStillNeedsTheAnswerAfterReveal() throws Exception {
        onEdt(() -> {
            RecordingBoard board = new RecordingBoard();
            RecordingView view = new RecordingView();
            ExerciseController controller =
                    new ExerciseController(new LessonStepExercise(knight.steps().get(1)), board, view);
            controller.start();

            controller.onMove(knightMove("d4", "b3"));
            assertEquals(Stage.EXPLAIN, view.lastStage());
            assertInstanceOf(Verdict.Wrong.class, view.lastVerdict());
            assertEquals(List.of(Square.of("b3")), board.flashes);
            assertTrue(board.marks.isEmpty(), "EXPLAIN은 힌트를 표시하지 않는다");

            controller.onMove(knightMove("d4", "b5"));
            assertEquals(Stage.HINT, view.lastStage());
            assertEquals(squares("d4", "f5"), board.marks.get(BoardPanel.Mark.HINT));
            assertTrue(board.arrows.isEmpty());
            assertFalse(controller.revealUsed());

            controller.onMove(knightMove("d4", "c2"));
            assertEquals(Stage.REVEAL, view.lastStage());
            assertEquals(List.of(new BoardPanel.Arrow(Square.of("d4"), Square.of("f5"), BoardPanel.ArrowKind.SOLUTION)),
                    board.arrows);
            assertEquals(squares("d4", "f5"), board.marks.get(BoardPanel.Mark.HINT), "REVEAL에도 힌트는 남는다");
            assertTrue(controller.revealUsed());
            assertEquals(3, controller.wrongCount());
            assertEquals(Optional.of(InputMode.MOVE), board.inputMode, "공개 후에도 직접 정답을 둬야 한다");
            assertFalse(view.nextEnabled.contains(true));

            controller.onMove(knightMove("d4", "f5"));
            assertInstanceOf(Verdict.Solved.class, view.lastVerdict());
            assertEquals(Stage.REVEAL, view.lastStage(), "정답 직전까지의 단계를 함께 알린다");
            assertTrue(controller.solved());
            assertEquals(Optional.empty(), board.inputMode, "푼 뒤에는 입력을 잠근다");
            assertEquals(List.of(), board.legalMoves);
            assertEquals(squares("d4", "f5"), board.marks.get(BoardPanel.Mark.LAST_MOVE));
            assertTrue(board.arrows.isEmpty(), "정답 뒤에는 공개 화살표를 지운다");
            assertEquals(true, view.nextEnabled.getLast());

            int verdicts = view.verdicts.size();
            controller.onMove(knightMove("d4", "b3"));
            assertEquals(verdicts, view.verdicts.size(), "푼 뒤의 입력은 무시한다");
        });
    }

    @Test
    void squareStepRevealsTheAnswerSquareAndAcceptsIt() throws Exception {
        onEdt(() -> {
            RecordingBoard board = new RecordingBoard();
            RecordingView view = new RecordingView();
            ExerciseController controller =
                    new ExerciseController(new LessonStepExercise(knight.steps().get(2)), board, view);
            controller.start();
            assertEquals(1, board.squareListenerAdds);
            assertEquals(0, board.moveListenerAdds);
            assertEquals(Optional.of(InputMode.SQUARE), board.inputMode);

            for (String wrong : new String[] {"a1", "a2", "a3"}) {
                controller.onSquare(Square.of(wrong));
            }
            assertEquals(List.of(Square.of("a1"), Square.of("a2"), Square.of("a3")), board.flashes);
            assertEquals(Stage.REVEAL, view.lastStage());
            assertEquals(squares("c7"), board.marks.get(BoardPanel.Mark.SOLUTION));
            assertEquals(squares("d5"), board.marks.get(BoardPanel.Mark.HINT));

            controller.onSquare(Square.of("c7"));
            assertInstanceOf(Verdict.Solved.class, view.lastVerdict());
            assertEquals(squares("c7"), board.marks.get(BoardPanel.Mark.SOLUTION));
            assertEquals(Optional.empty(), board.inputMode);
            assertEquals(true, view.nextEnabled.getLast());
        });
    }

    @Test
    void detachRemovesTheListenerLocksInputAndIgnoresLateEvents() throws Exception {
        onEdt(() -> {
            RecordingBoard board = new RecordingBoard();
            RecordingView view = new RecordingView();
            ExerciseController controller =
                    new ExerciseController(new LessonStepExercise(knight.steps().get(1)), board, view);
            controller.start();
            Consumer<Move> registered = board.moveListener;

            controller.detach();
            controller.detach();
            assertFalse(board.moveListenerRemoves.isEmpty());
            assertTrue(board.moveListenerRemoves.stream().allMatch(l -> l == registered),
                    "start에서 등록한 바로 그 인스턴스를 제거한다");
            assertEquals(Optional.empty(), board.inputMode);

            controller.onMove(knightMove("d4", "b3"));
            assertTrue(view.verdicts.isEmpty(), "떼어 낸 뒤의 입력은 채점하지 않는다");

            controller.start();
            assertEquals(2, board.moveListenerAdds, "다시 시작하면 같은 리스너를 다시 등록한다");
            assertEquals(registered, board.moveListener);
        });
    }

    @Test
    void restartClearsLadderRevealAndSolvedState() throws Exception {
        onEdt(() -> {
            RecordingBoard board = new RecordingBoard();
            RecordingView view = new RecordingView();
            ExerciseController controller =
                    new ExerciseController(new LessonStepExercise(knight.steps().get(1)), board, view);
            controller.start();
            controller.onMove(knightMove("d4", "b3"));
            controller.onMove(knightMove("d4", "b5"));
            controller.onMove(knightMove("d4", "c2"));
            controller.onMove(knightMove("d4", "f5"));

            controller.restart();
            assertFalse(controller.solved());
            assertFalse(controller.revealUsed());
            assertEquals(0, controller.wrongCount());
            assertEquals(Optional.of(InputMode.MOVE), board.inputMode);
            assertTrue(board.marks.isEmpty());
            assertTrue(board.arrows.isEmpty());
            assertEquals(1, board.moveListenerAdds, "restart는 등록을 유지한다");
            assertEquals(false, view.nextEnabled.getLast());
        });
    }

    @Test
    void continueShowsTheReplyResetsTheLadderButRemembersReveal() throws Exception {
        onEdt(() -> {
            RecordingBoard board = new RecordingBoard();
            RecordingView view = new RecordingView();
            TwoStepExercise exercise = new TwoStepExercise();
            ExerciseController controller = new ExerciseController(exercise, board, view);
            controller.start();

            controller.onMove(TwoStepExercise.WRONG);
            controller.onMove(TwoStepExercise.WRONG);
            controller.onMove(TwoStepExercise.WRONG);
            assertTrue(controller.revealUsed());

            controller.onMove(TwoStepExercise.FIRST);
            assertInstanceOf(Verdict.Continue.class, view.lastVerdict());
            assertEquals(Stage.REVEAL, view.lastStage());
            assertEquals(Stage.NONE, controller.stage(), "다음 수는 사다리를 처음부터 쓴다");
            assertTrue(board.arrows.isEmpty());
            assertEquals(squares("e7", "e5"), board.marks.get(BoardPanel.Mark.LAST_MOVE), "상대 응수를 강조한다");
            assertFalse(view.nextEnabled.contains(true));

            controller.onMove(TwoStepExercise.WRONG_AFTER_REPLY);
            assertEquals(Stage.EXPLAIN, view.lastStage());

            controller.onMove(TwoStepExercise.SECOND);
            assertInstanceOf(Verdict.Solved.class, view.lastVerdict());
            assertTrue(controller.revealUsed(), "앞 수에서 공개를 봤으면 완주해도 공개 사용으로 남는다");
            assertEquals(true, view.nextEnabled.getLast());
        });
    }

    /** 표시 호출을 기록하는 보드. 오답 깜빡임 타이머는 띄우지 않는다. */
    private static final class RecordingBoard extends BoardPanel {
        Position shown;
        List<Move> legalMoves = List.of();
        Optional<InputMode> inputMode = Optional.empty();
        final Map<Mark, Set<Square>> marks = new EnumMap<>(Mark.class);
        List<Arrow> arrows = List.of();
        final List<Square> flashes = new ArrayList<>();
        int moveListenerAdds;
        int squareListenerAdds;
        Consumer<Move> moveListener;
        final List<Consumer<Move>> moveListenerRemoves = new ArrayList<>();

        @Override
        public void setPosition(Position snapshot) {
            super.setPosition(snapshot);
            shown = snapshot.copy();
            legalMoves = List.of();
            marks.clear();
            arrows = List.of();
        }

        @Override
        public void setLegalMoves(List<Move> moves) {
            super.setLegalMoves(moves);
            legalMoves = List.copyOf(moves);
        }

        @Override
        public void setInputMode(Optional<InputMode> mode) {
            super.setInputMode(mode);
            inputMode = mode;
        }

        @Override
        public void mark(Collection<Square> squares, Mark mark) {
            super.mark(squares, mark);
            marks.computeIfAbsent(mark, m -> new LinkedHashSet<>()).addAll(squares);
        }

        @Override
        public void setArrows(List<Arrow> arrows) {
            super.setArrows(arrows);
            this.arrows = List.copyOf(arrows);
        }

        @Override
        public void flashWrong(Square square) {
            flashes.add(square);
        }

        @Override
        public void addMoveListener(Consumer<Move> listener) {
            super.addMoveListener(listener);
            moveListenerAdds++;
            moveListener = listener;
        }

        @Override
        public void addSquareListener(Consumer<Square> listener) {
            super.addSquareListener(listener);
            squareListenerAdds++;
        }

        @Override
        public void removeMoveListener(Consumer<Move> listener) {
            super.removeMoveListener(listener);
            moveListenerRemoves.add(listener);
        }
    }

    /** 화면 콜백을 기록한다. */
    private static final class RecordingView implements ExerciseController.View {
        final List<String> prompts = new ArrayList<>();
        final List<Verdict> verdicts = new ArrayList<>();
        final List<Stage> stages = new ArrayList<>();
        final List<Boolean> nextEnabled = new ArrayList<>();

        @Override
        public void showPrompt(String prompt) {
            prompts.add(prompt);
        }

        @Override
        public void showVerdict(Verdict verdict, Stage stage) {
            verdicts.add(verdict);
            stages.add(stage);
        }

        @Override
        public void setNextEnabled(boolean enabled) {
            nextEnabled.add(enabled);
        }

        Verdict lastVerdict() {
            return verdicts.getLast();
        }

        Stage lastStage() {
            return stages.getLast();
        }
    }

    /** 1.e4 e5 2.Nf3 순서를 정답으로 받는 두 수짜리 가짜 연습(PuzzleExercise 없이 Continue 처리를 확인한다). */
    private static final class TwoStepExercise implements Exercise {
        static final Move FIRST = new Move(Square.of("e2"), Square.of("e4"), MoveKind.DOUBLE_PUSH);
        static final Move REPLY = new Move(Square.of("e7"), Square.of("e5"), MoveKind.DOUBLE_PUSH);
        static final Move SECOND = new Move(Square.of("g1"), Square.of("f3"), MoveKind.QUIET);
        static final Move WRONG = new Move(Square.of("a2"), Square.of("a3"), MoveKind.QUIET);
        static final Move WRONG_AFTER_REPLY = new Move(Square.of("b1"), Square.of("c3"), MoveKind.QUIET);
        private static final RuleSet RULES = RulesKind.STANDARD.rules();

        private Position current = Position.initial();
        private int step;

        @Override
        public String prompt() {
            return "두 수를 두세요";
        }

        @Override
        public InputMode inputMode() {
            return InputMode.MOVE;
        }

        @Override
        public RuleSet rules() {
            return RULES;
        }

        @Override
        public Position position() {
            return current.copy();
        }

        @Override
        public List<Move> legalMoves() {
            return step == 2 ? List.of() : RULES.legalMoves(current);
        }

        @Override
        public Verdict submit(Move move) {
            Move expected = step == 0 ? FIRST : SECOND;
            if (!move.equals(expected)) {
                return new Verdict.Wrong("아닙니다");
            }
            current.make(move);
            if (step++ == 0) {
                current.make(REPLY);
                return new Verdict.Continue(REPLY, "좋습니다");
            }
            return new Verdict.Solved("완료");
        }

        @Override
        public Verdict submitSquare(Square square) {
            throw new IllegalStateException();
        }

        @Override
        public Optional<Move> solution() {
            return step == 2 ? Optional.empty() : Optional.of(step == 0 ? FIRST : SECOND);
        }

        @Override
        public Set<Square> hintSquares() {
            return Set.of(solution().orElseThrow().from());
        }

        @Override
        public void reset() {
            current = Position.initial();
            step = 0;
        }
    }
}
