package com.oopteama.chess.exercise;

import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 퍼즐 채점(QUIZ-01, QUIZ-02). 표준 규칙으로 두며 정답 수열의 짝수 위치는 사용자 수, 홀수 위치는 상대 응수다.
 *
 * <ul>
 *   <li>사용자 수가 정답 수열의 다음 수와 같으면 정답이다. 마지막 수가 아니면 상대 응수를 자동으로 두고
 *       {@link Verdict.Continue}를 돌려준다.</li>
 *   <li>메이트 주제의 마지막 수는 체크메이트가 되는 어떤 수든 정답이다(QUIZ-02).</li>
 *   <li>오답이면 국면을 퍼즐 처음이 아니라 오답 직전 국면으로 둔다(G14). 실제로는 수를 두지 않는다.</li>
 * </ul>
 * 오답 횟수와 사다리 단계는 화면 쪽 {@link FeedbackLadder}가 관리한다. 해설({@link Puzzle#explain()})은
 * {@code [[용어]]} 링크가 있을 수 있어 화면이 용어 링크 위젯으로 따로 표시한다.
 */
public final class PuzzleExercise implements Exercise {

    static final String SOLVED = "정답입니다.";
    static final String SOLVED_MATE = "체크메이트! 정답입니다.";
    static final String CONTINUE = "좋은 수입니다. 상대가 응수했습니다. 다음 수를 찾아보세요.";
    static final String WRONG = "이 국면의 정답 수가 아닙니다. 다시 생각해 보세요.";
    static final String WRONG_MATE = "체크메이트가 되지 않습니다. 상대 킹이 피할 칸을 모두 막는 수를 찾아보세요.";

    private static final RuleSet RULES = RulesKind.STANDARD.rules();

    private final Puzzle puzzle;
    private final Position start;
    private final String prompt;

    private Position current;
    /** 다음에 둘 정답 수열의 위치. 사용자 차례에는 항상 짝수다. */
    private int index;
    private boolean solved;

    /**
     * @param puzzle 로더가 검증한 퍼즐
     * @throws IllegalArgumentException FEN이 잘못됐거나 정답 수열에 둘 수 없는 수가 있을 때
     */
    public PuzzleExercise(Puzzle puzzle) {
        this.puzzle = Objects.requireNonNull(puzzle, "puzzle");
        try {
            this.start = Fen.parse(puzzle.fen());
        } catch (FenFormatException e) {
            throw new IllegalArgumentException("퍼즐 " + puzzle.id() + " FEN 오류: " + e.getMessage(), e);
        }
        Position replay = start.copy();
        for (String uci : puzzle.solutionUci()) {
            Move m = RULES.fromUci(replay, uci).orElseThrow(() -> new IllegalArgumentException(
                    "퍼즐 " + puzzle.id() + "의 정답 수 " + uci + "는 둘 수 없습니다"));
            replay.make(m);
        }
        this.prompt = prompt(puzzle, start.sideToMove());
        this.current = start.copy();
    }

    private static String prompt(Puzzle p, Side side) {
        String who = side == Side.WHITE ? "백" : "흑";
        if (p.isMateTheme()) {
            return "%s 차례입니다. %d수 만에 체크메이트하세요.".formatted(who, p.userMoveCount());
        }
        return "%s 차례입니다. 가장 좋은 수를 찾아보세요. (주제: %s)".formatted(who, String.join(", ", p.themes()));
    }

    /**
     * 풀고 있는 퍼즐. 화면이 해설과 기록 ID에 쓴다.
     *
     * @return 생성자에 넘긴 퍼즐
     */
    public Puzzle puzzle() {
        return puzzle;
    }

    @Override
    public String prompt() {
        return prompt;
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
        return solved ? List.of() : List.copyOf(RULES.legalMoves(current));
    }

    @Override
    public Verdict submit(Move move) {
        Objects.requireNonNull(move, "move");
        if (solved) {
            throw new IllegalStateException("이미 푼 퍼즐입니다. reset() 후 다시 제출하세요");
        }
        if (!RULES.legalMoves(current).contains(move)) {
            throw new IllegalArgumentException("현재 국면에서 둘 수 없는 수: " + move.uci());
        }
        boolean last = index == puzzle.solutionUci().size() - 1;
        Position after = current.copy();
        after.make(move);
        boolean mates = isCheckmate(after);
        boolean correct = move.equals(expected()) || (last && puzzle.isMateTheme() && mates);
        if (!correct) {
            return new Verdict.Wrong(last && puzzle.isMateTheme() ? WRONG_MATE : WRONG);
        }
        current = after;
        index++;
        if (last) {
            solved = true;
            return new Verdict.Solved(mates ? SOLVED_MATE : SOLVED);
        }
        Move reply = expected();
        current.make(reply);
        index++;
        return new Verdict.Continue(reply, CONTINUE);
    }

    @Override
    public Verdict submitSquare(Square square) {
        throw new IllegalStateException("퍼즐은 수를 두는 연습입니다");
    }

    @Override
    public Optional<Move> solution() {
        return solved ? Optional.empty() : Optional.of(expected());
    }

    /** 힌트 단계에서는 움직일 기물만 강조한다. 도착 칸은 정답 공개 단계의 화살표가 보여 준다. */
    @Override
    public Set<Square> hintSquares() {
        return solved ? Set.of() : Set.of(expected().from());
    }

    @Override
    public void reset() {
        current = start.copy();
        index = 0;
        solved = false;
    }

    /**
     * 지금까지 맞힌 사용자 수의 개수.
     *
     * @return 0 이상 {@link Puzzle#userMoveCount()} 이하
     */
    public int userMovesPlayed() {
        return (index + 1) / 2;
    }

    /** 현재 국면에서 정답 수열의 다음 수. 생성자에서 수열 전체를 검증했으므로 항상 있다. */
    private Move expected() {
        String uci = puzzle.solutionUci().get(index);
        return RULES.fromUci(current, uci).orElseThrow(() -> new IllegalStateException("정답 수를 둘 수 없음: " + uci));
    }

    private static boolean isCheckmate(Position pos) {
        return RULES.inCheck(pos) && RULES.legalMoves(pos).isEmpty();
    }
}
