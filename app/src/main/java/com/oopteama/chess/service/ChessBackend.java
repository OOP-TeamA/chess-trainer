package com.oopteama.chess.service;

import com.oopteama.chess.ai.ComputerPlayer;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.ai.MateSolver;
import com.oopteama.chess.data.Profile;
import com.oopteama.chess.data.Settings;
import com.oopteama.chess.data.Storage;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Game;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.training.LessonSession;
import com.oopteama.chess.training.Puzzle;
import com.oopteama.chess.training.PuzzleSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 화면(ui)이 프로그램 기능을 쓸 때 거치는 단일 창구입니다 (퍼사드 패턴).
 * 레슨·퍼즐·대국 시작, AI 계산, 프로필·설정 변경과 저장을 한곳에서 처리합니다.
 * 데이터가 바뀌면 저장하고, 등록된 리스너(화면)에게 알립니다 (옵저버 패턴).
 */
public final class ChessBackend {

    private final Storage storage;
    private final ComputerPlayer computer;
    private final MateSolver mateSolver;

    private final List<Lesson> lessons;
    private final List<Puzzle> puzzles;
    private final List<Profile> profiles = new ArrayList<>();
    private final Settings settings;
    private final List<Runnable> listeners = new ArrayList<>();
    private String activeProfileId;

    public ChessBackend(Storage storage) {
        this(storage, new ComputerPlayer(), new MateSolver());
    }

    public ChessBackend(Storage storage, ComputerPlayer computer, MateSolver mateSolver) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.computer = Objects.requireNonNull(computer, "computer");
        this.mateSolver = Objects.requireNonNull(mateSolver, "mateSolver");
        this.lessons = List.copyOf(storage.loadLessons());
        this.puzzles = List.copyOf(storage.loadPuzzles());

        Storage.UserData userData = storage.loadUserData();
        this.settings = userData.settings() != null ? userData.settings() : new Settings();
        if (userData.profiles() != null) {
            profiles.addAll(userData.profiles());
        }
        if (profiles.isEmpty()) {
            Profile first = new Profile("default", "플레이어");
            profiles.add(first);
            activeProfileId = first.id();
        } else {
            String saved = userData.activeProfileId();
            boolean exists = saved != null && profiles.stream().anyMatch(p -> p.id().equals(saved));
            activeProfileId = exists ? saved : profiles.getFirst().id();
        }
    }

    // ---- 레슨·퍼즐·대국 ----

    public List<Lesson> lessons() {
        return lessons;
    }

    public List<Lesson> lessons(Lesson.Category category) {
        return lessons.stream().filter(lesson -> lesson.category() == category).toList();
    }

    public List<Puzzle> puzzles() {
        return puzzles;
    }

    public LessonSession startLesson(Lesson lesson) {
        return new LessonSession(lesson);
    }

    public PuzzleSession startPuzzle(Puzzle puzzle) {
        return new PuzzleSession(puzzle, mateSolver);
    }

    public Game newGame() {
        return new Game();
    }

    // ---- AI ----

    /** AI가 둘 수를 계산합니다 (오래 걸릴 수 있어 화면에서는 백그라운드 스레드로 부릅니다). */
    public Optional<Move> chooseAiMove(Board board, Difficulty difficulty, BooleanSupplier cancelled) {
        return computer.chooseMove(board, difficulty, cancelled);
    }

    /** 사람에게 보여 줄 추천 수. */
    public Optional<Move> computeHint(Board board) {
        return computer.hint(board);
    }

    // ---- 프로필 (바뀌면 바로 저장하고 화면에 알림) ----

    public List<Profile> profiles() {
        return List.copyOf(profiles);
    }

    public Profile activeProfile() {
        return profiles.stream()
                .filter(p -> p.id().equals(activeProfileId))
                .findFirst()
                .orElse(profiles.getFirst());
    }

    public void setActiveProfile(String profileId) {
        if (profiles.stream().anyMatch(p -> p.id().equals(profileId))) {
            activeProfileId = profileId;
            saveAndNotify();
        }
    }

    public Profile createProfile(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("프로필 이름을 입력해 주세요.");
        }
        Profile profile = new Profile("p-" + UUID.randomUUID().toString().substring(0, 8), clean);
        profiles.add(profile);
        activeProfileId = profile.id();
        saveAndNotify();
        return profile;
    }

    /** 프로필을 지웁니다. 마지막 하나는 지울 수 없습니다. */
    public boolean deleteProfile(String profileId) {
        if (profiles.size() <= 1) {
            return false;
        }
        boolean removed = profiles.removeIf(p -> p.id().equals(profileId));
        if (removed) {
            if (profileId.equals(activeProfileId)) {
                activeProfileId = profiles.getFirst().id();
            }
            saveAndNotify();
        }
        return removed;
    }

    public void completeLesson(String lessonId) {
        if (!activeProfile().isLessonCompleted(lessonId)) {
            activeProfile().markLessonCompleted(lessonId);
            saveAndNotify();
        }
    }

    public void recordPuzzle(String puzzleId, boolean solved) {
        activeProfile().recordPuzzle(puzzleId, solved);
        saveAndNotify();
    }

    public void recordAiResult(Difficulty difficulty, int outcome) {
        activeProfile().recordAiResult(difficulty, outcome);
        saveAndNotify();
    }

    public void clearActiveProgress() {
        activeProfile().clearProgress();
        saveAndNotify();
    }

    // ---- 설정 ----

    public Settings settings() {
        return settings;
    }

    /** 설정을 바꾸고 저장합니다. 예) updateSettings(s -> s.setShowCoords(false)) */
    public void updateSettings(Consumer<Settings> change) {
        Objects.requireNonNull(change, "change").accept(settings);
        saveAndNotify();
    }

    // ---- 저장과 변경 알림 ----

    /** 데이터가 바뀔 때 실행할 동작을 등록합니다 (예: 화면 새로 고침). */
    public void addChangeListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void save() {
        storage.saveUserData(new Storage.UserData(activeProfileId, settings, List.copyOf(profiles)));
    }

    private void saveAndNotify() {
        save();
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
