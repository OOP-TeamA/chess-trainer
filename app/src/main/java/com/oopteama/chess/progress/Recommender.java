package com.oopteama.chess.progress;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.game.AiSetup;
import com.oopteama.chess.nav.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 홈 화면 추천과 주제별 정답률(PROG-02, QUIZ-03). 화면 밖에서 단위 테스트하도록 progress에 둔다(G13).
 *
 * <p>기록은 바뀔 수 있으므로 부를 때마다 현재 {@link Progress}로 계산한다. 추천 우선순위:
 * <ol>
 *   <li>레슨 목록 순서로 아직 끝내지 않은 첫 레슨</li>
 *   <li>시도가 {@value #MIN_THEME_ATTEMPTS}회 이상인 퍼즐 주제 중 정답률이 가장 낮은 주제(100%면 제외,
 *       같으면 {@link Puzzle#THEMES} 순서)</li>
 *   <li>AI 다음 난이도: 이긴 적 있는 가장 높은 발표 난이도(입문·초급·중급)의 다음 난이도. 이긴 적이 없으면 입문</li>
 * </ol>
 * 셋 다 해당하지 않으면(중급까지 이김) 추천이 없다.
 */
public final class Recommender {

    /** 약한 주제로 추천하려면 필요한 최소 시도 횟수. */
    static final int MIN_THEME_ATTEMPTS = 5;

    /** 발표 화면에 노출하는 AI 난이도(기능 명세 D-5). 상급은 발표 후 선택 기능이라 추천하지 않는다. */
    private static final List<Difficulty> PRESENTED = List.of(Difficulty.INTRO, Difficulty.BEGINNER,
            Difficulty.INTERMEDIATE);

    private final List<Lesson> lessons;
    private final List<Puzzle> puzzles;
    private final Progress progress;

    /**
     * @param lessons  레슨(목록 순서)
     * @param puzzles  퍼즐(파일 순서)
     * @param progress 활성 프로필의 기록
     */
    public Recommender(List<Lesson> lessons, List<Puzzle> puzzles, Progress progress) {
        this.lessons = List.copyOf(Objects.requireNonNull(lessons, "lessons"));
        this.puzzles = List.copyOf(Objects.requireNonNull(puzzles, "puzzles"));
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    /**
     * 다음에 할 일 하나.
     *
     * @return 우선순위가 가장 높은 추천. 추천할 것이 없으면 empty
     */
    public Optional<Recommendation> next() {
        return firstUnfinishedLesson()
                .or(this::weakestTheme)
                .or(this::nextAiDifficulty);
    }

    /**
     * 퍼즐이 하나 이상 있는 주제의 누적 시도·정답. 한 퍼즐이 주제 여러 개에 속하면 각 주제에 더한다.
     *
     * @return {@link Puzzle#THEMES} 순서(목록에 없는 주제는 뒤에 퍼즐 순서로), 불변 목록
     */
    public List<ThemeStat> themeStats() {
        Set<String> themes = new LinkedHashSet<>();
        for (String theme : Puzzle.THEMES) {
            if (puzzles.stream().anyMatch(p -> p.themes().contains(theme))) {
                themes.add(theme);
            }
        }
        puzzles.forEach(p -> themes.addAll(p.themes()));
        List<ThemeStat> out = new ArrayList<>();
        for (String theme : themes) {
            int attempted = 0;
            int solved = 0;
            for (Puzzle p : puzzles) {
                if (p.themes().contains(theme)) {
                    AttemptStats s = progress.puzzleAttempts(p.id());
                    attempted += s.attempted();
                    solved += s.solved();
                }
            }
            out.add(new ThemeStat(theme, attempted, solved));
        }
        return List.copyOf(out);
    }

    private Optional<Recommendation> firstUnfinishedLesson() {
        Set<String> done = progress.lessonsDone();
        return lessons.stream()
                .filter(l -> !done.contains(l.id()))
                .findFirst()
                .map(l -> new Recommendation(new Route.Lesson(l.id(), 0),
                        "레슨 %d. %s".formatted(l.order(), l.title()),
                        "아직 끝내지 않은 첫 레슨입니다."));
    }

    private Optional<Recommendation> weakestTheme() {
        return themeStats().stream()
                .filter(s -> s.attempted() >= MIN_THEME_ATTEMPTS && s.solved() < s.attempted())
                .min(Comparator.comparingDouble(ThemeStat::ratio))
                .map(s -> new Recommendation(new Route.Puzzle(s.theme(), ""),
                        s.theme() + " 퍼즐",
                        "정답률 %d%%(%d/%d)로 가장 약한 주제입니다.".formatted(
                                Math.round(s.ratio() * 100), s.solved(), s.attempted())));
    }

    private Optional<Recommendation> nextAiDifficulty() {
        int beaten = -1;
        for (int i = 0; i < PRESENTED.size(); i++) {
            if (progress.aiRecord(PRESENTED.get(i)).wins() > 0) {
                beaten = i;
            }
        }
        if (beaten == PRESENTED.size() - 1) {
            return Optional.empty();
        }
        Difficulty next = PRESENTED.get(beaten + 1);
        String reason = beaten < 0
                ? "입문 AI부터 한 판 도전해 보세요."
                : "%s AI를 이겼습니다. 한 단계 올려 보세요.".formatted(PRESENTED.get(beaten).label());
        return Optional.of(new Recommendation(new Route.AiGame(AiSetup.standard(Side.WHITE, next)),
                "AI 대전 · " + next.label(), reason));
    }
}
