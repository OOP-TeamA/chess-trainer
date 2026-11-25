package com.oopteama.chess.ui.controller;

import com.oopteama.chess.data.Profile;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.ui.screen.ProfileScreen;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.util.List;

/**
 * 프로필 화면 컨트롤러입니다. 프로필 추가·선택·삭제와 기록 초기화를 처리합니다.
 */
public final class ProfileController implements ScreenController {

    private final ChessBackend backend;
    private final ProfileScreen screen = new ProfileScreen();

    public ProfileController(ChessBackend backend) {
        this.backend = backend;
        screen.addButton().addActionListener(e -> createProfile());
        screen.nameField().addActionListener(e -> createProfile()); // 이름 칸에서 Enter
        screen.clearButton().addActionListener(e -> clearActiveProgress());
        refresh();
    }

    private void createProfile() {
        String name = screen.nameField().getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(screen, "프로필 이름을 입력해 주세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        backend.createProfile(name);
        screen.nameField().setText("");
        refresh();
    }

    private void selectProfile(Profile profile) {
        backend.setActiveProfile(profile.id());
        refresh();
    }

    private void deleteProfile(Profile profile) {
        if (backend.profiles().size() <= 1) {
            JOptionPane.showMessageDialog(screen, "마지막 프로필은 삭제할 수 없습니다.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(screen, "'" + profile.name() + "' 프로필과 학습 기록을 삭제할까요?",
                "프로필 삭제", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            backend.deleteProfile(profile.id());
            refresh();
        }
    }

    private void clearActiveProgress() {
        Profile active = backend.activeProfile();
        int answer = JOptionPane.showConfirmDialog(screen, "'" + active.name() + "'의 모든 학습·퍼즐·AI 전적을 초기화할까요?",
                "기록 초기화", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            backend.clearActiveProgress();
            refresh();
        }
    }

    private void refresh() {
        Profile active = backend.activeProfile();
        screen.showProfiles(backend.profiles(), active.id(), this::selectProfile, this::deleteProfile);
        screen.showActiveProfile(active.name(), active.totalScore(),
                completedText(active, Lesson.Category.TUTORIAL),
                completedText(active, Lesson.Category.OPENING),
                active.solvedPuzzleCount() + " / " + backend.puzzles().size() + " (" + active.puzzleAccuracyPercent() + "%)");
        screen.showAiRecords(active);
    }

    /** "완료한 레슨 수 / 전체 레슨 수" */
    private String completedText(Profile profile, Lesson.Category category) {
        List<Lesson> lessons = backend.lessons(category);
        long done = lessons.stream().filter(lesson -> profile.isLessonCompleted(lesson.id())).count();
        return done + " / " + lessons.size();
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
