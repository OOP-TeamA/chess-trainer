package com.oopteama.chess.ui.controller;

import com.oopteama.chess.data.Profile;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.training.Puzzle;
import com.oopteama.chess.ui.ScreenId;
import com.oopteama.chess.ui.screen.HomeScreen;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 홈 화면 컨트롤러입니다. 프로필 기록으로 현황과 다음 추천 학습을 계산해 보여 주고,
 * 추천 버튼이나 바로가기 카드를 누르면 해당 화면으로 이동합니다.
 */
public final class HomeController implements ScreenController {

    private final ChessBackend backend;
    private final HomeScreen screen = new HomeScreen();
    private ScreenId recommendTarget = ScreenId.LESSONS;

    /** @param navigator 다른 화면으로 이동하는 동작 (MainFrame이 넘겨 줌) */
    public HomeController(ChessBackend backend, Consumer<ScreenId> navigator) {
        this.backend = backend;
        screen.recommendButton().addActionListener(e -> navigator.accept(recommendTarget));
        screen.addLaunchListener(navigator);
        refresh();
    }

    private void refresh() {
        Profile profile = backend.activeProfile();
        screen.showProfile(profile.name(), profile.totalScore());

        long doneLessons = backend.lessons().stream().filter(l -> profile.isLessonCompleted(l.id())).count();
        screen.showStats(
                doneLessons + " / " + backend.lessons().size(),
                profile.solvedPuzzleCount() + " / " + backend.puzzles().size() + " (" + profile.puzzleAccuracyPercent() + "%)",
                profile.aiWins() + "승 " + profile.aiDraws() + "무 " + profile.aiLosses() + "패");

        // 추천 순서: 안 끝낸 레슨 → 안 푼 퍼즐 → AI 대전
        Optional<Lesson> nextLesson = backend.lessons().stream()
                .filter(lesson -> !profile.isLessonCompleted(lesson.id()))
                .findFirst();
        if (nextLesson.isPresent()) {
            Lesson lesson = nextLesson.get();
            screen.showRecommendation("[" + lesson.category().label() + "] " + lesson.title(), lesson.summary(), "레슨 이어하기");
            recommendTarget = ScreenId.LESSONS;
            return;
        }
        Optional<Puzzle> nextPuzzle = backend.puzzles().stream()
                .filter(puzzle -> !profile.isPuzzleSolved(puzzle.id()))
                .findFirst();
        if (nextPuzzle.isPresent()) {
            screen.showRecommendation("[퍼즐] " + PuzzleController.neutralTitle(backend.puzzles(), nextPuzzle.get()),
                    "아직 해결하지 않은 퍼즐에 도전해 보세요.", "퍼즐 풀러 가기");
            recommendTarget = ScreenId.PUZZLES;
            return;
        }
        screen.showRecommendation("모든 학습 콘텐츠를 완료했습니다!", "이제 AI 대전에서 실전 감각을 끌어올려 보세요.", "AI 대전 시작");
        recommendTarget = ScreenId.AI_GAME;
    }

    @Override
    public JComponent view() {
        return screen;
    }

    @Override
    public void onShow() {
        refresh();
    }
}
