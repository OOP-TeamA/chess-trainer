package com.oopteama.chess.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.game.AiSetup;
import com.oopteama.chess.nav.Route;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** PROG-02·QUIZ-03: 추천 우선순위와 주제별 정답률. */
class RecommenderTest {

    private static final String FEN = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1";
    private static final GameResult WHITE_WINS =
            new GameResult(GameResult.Outcome.WHITE_WINS, GameResult.Reason.CHECKMATE);

    private List<Lesson> lessons;
    private List<Puzzle> puzzles;
    private Progress progress;

    private static Lesson lesson(String id, int order, String title) throws MalformedDataException {
        LessonStep step = new LessonStep("8/8/8/3N4/8/8/8/8 w - - 0 1", RulesKind.SANDBOX, "아무 수나",
                Set.of(), StepGoal.parse("any-move"), Set.of(), "", "");
        return new Lesson(id, order, title, "요약", List.of(step));
    }

    private static Puzzle puzzle(String id, String... themes) {
        return new Puzzle(id, new LinkedHashSet<>(List.of(themes)), FEN, List.of("d1d8"), "");
    }

    private void attempts(String puzzleId, int attempted, int solved) {
        for (int i = 0; i < attempted; i++) {
            progress.recordPuzzle(puzzleId, i < solved);
        }
    }

    @BeforeEach
    void setUp() throws MalformedDataException {
        lessons = List.of(lesson("pawn", 1, "폰"), lesson("knight", 7, "나이트"));
        puzzles = List.of(puzzle("p1", "메이트인1", "백랭크"), puzzle("p2", "포크"), puzzle("p3", "포크"),
                puzzle("p4", "핀"));
        progress = new Progress();
    }

    private Optional<Recommendation> next() {
        return new Recommender(lessons, puzzles, progress).next();
    }

    @Test
    void firstUnfinishedLessonComesFirstInListOrder() {
        Recommendation r = next().orElseThrow();
        assertEquals(new Route.Lesson("pawn", 0), r.route());
        assertEquals("레슨 1. 폰", r.title());

        progress.markLessonDone("pawn");
        assertEquals(new Route.Lesson("knight", 0), next().orElseThrow().route());
        assertEquals("레슨 7. 나이트", next().orElseThrow().title());
    }

    @Test
    void weakestThemeWithEnoughAttemptsComesAfterLessons() {
        progress.markLessonDone("pawn");
        progress.markLessonDone("knight");
        attempts("p2", 3, 1);   // 포크: p2 + p3 합산 → 6회 중 2회
        attempts("p3", 3, 1);
        attempts("p4", 5, 3);   // 핀: 5회 중 3회
        attempts("p1", 4, 0);   // 메이트인1·백랭크: 4회라 후보 아님

        Recommendation r = next().orElseThrow();
        assertEquals(new Route.Puzzle("포크", ""), r.route());
        assertEquals("포크 퍼즐", r.title());
        assertEquals("정답률 33%(2/6)로 가장 약한 주제입니다.", r.reason());
    }

    @Test
    void perfectThemesAreNotRecommended() {
        progress.markLessonDone("pawn");
        progress.markLessonDone("knight");
        attempts("p4", 6, 6);
        assertEquals(Route.AiGame.class, next().orElseThrow().route().getClass());
    }

    @Test
    void aiRecommendationClimbsThePresentedDifficulties() {
        progress.markLessonDone("pawn");
        progress.markLessonDone("knight");

        Recommendation first = next().orElseThrow();
        assertEquals(new Route.AiGame(AiSetup.standard(Side.WHITE, Difficulty.INTRO)), first.route());
        assertEquals("AI 대전 · 입문", first.title());

        progress.recordAiGame(Difficulty.INTRO, WHITE_WINS, Side.WHITE);
        assertEquals(new Route.AiGame(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER)), next().orElseThrow().route());
        assertEquals("입문 AI를 이겼습니다. 한 단계 올려 보세요.", next().orElseThrow().reason());

        progress.recordAiGame(Difficulty.BEGINNER, WHITE_WINS, Side.BLACK);   // 흑으로 져서 승리 아님
        assertEquals(new Route.AiGame(AiSetup.standard(Side.WHITE, Difficulty.BEGINNER)), next().orElseThrow().route());

        progress.recordAiGame(Difficulty.INTERMEDIATE, WHITE_WINS, Side.WHITE);
        assertEquals(Optional.empty(), next(), "중급까지 이기면 더 추천하지 않는다(상급은 발표 후 선택)");
    }

    @Test
    void themeStatsSumPuzzlesPerThemeInTheFixedThemeOrder() {
        attempts("p1", 3, 2);
        attempts("p2", 2, 1);
        List<ThemeStat> stats = new Recommender(lessons, puzzles, progress).themeStats();
        assertEquals(List.of(
                new ThemeStat("메이트인1", 3, 2),
                new ThemeStat("백랭크", 3, 2),
                new ThemeStat("포크", 2, 1),
                new ThemeStat("핀", 0, 0)), stats);
        assertEquals(2.0 / 3.0, stats.get(0).ratio(), 1e-9);
        assertEquals(0.0, stats.get(3).ratio());
    }

    @Test
    void recommenderReadsTheCurrentProgressEachTime() {
        Recommender recommender = new Recommender(lessons, puzzles, progress);
        assertEquals(new Route.Lesson("pawn", 0), recommender.next().orElseThrow().route());
        progress.markLessonDone("pawn");
        assertEquals(new Route.Lesson("knight", 0), recommender.next().orElseThrow().route());
    }

    @Test
    void themeStatRejectsImpossibleCounts() {
        assertThrows(IllegalArgumentException.class, () -> new ThemeStat("포크", 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new ThemeStat("포크", -1, 0));
    }
}
