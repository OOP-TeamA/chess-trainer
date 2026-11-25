package com.oopteama.chess.backend.service;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.ChessAI;
import com.oopteama.chess.backend.engine.ChessAI.Difficulty;
import com.oopteama.chess.backend.engine.ChessGame;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.MoveAnalyzer;
import com.oopteama.chess.backend.training.Lesson;
import com.oopteama.chess.backend.training.LessonSession;
import com.oopteama.chess.backend.training.Puzzle;
import com.oopteama.chess.backend.training.PuzzleSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 프론트엔드가 백엔드(엔진·학습·저장소)와 통신하는 유일한 서비스 퍼사드입니다.
 * DIP: 구체 파일 구현이 아닌 Storage 인터페이스에 의존하며, 모든 상태 변경·저장·알림을 한 곳에서 보장합니다.
 */
public final class ChessBackend {

    private final Storage storage;
    private final ChessAI ai;
    private final MoveAnalyzer analyzer;

    private final List<Lesson> lessons;
    private final List<Puzzle> puzzles;
    private final List<Profile> profiles = new ArrayList<>();
    private final Settings settings;
    private final List<Runnable> listeners = new ArrayList<>();
    private String activeProfileId;

    // 국면별 호버 수 평가 기준 점수 캐시 (프론트엔드가 AI/분석기를 직접 알지 않아도 되도록 백엔드 내부 관리)
    private final Map<Board, Integer> baselineCache = new WeakHashMap<>();

    public ChessBackend(Storage storage) {
        this(storage, new ChessAI(), new MoveAnalyzer());
    }

    public ChessBackend(Storage storage, ChessAI ai, MoveAnalyzer analyzer) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.ai = Objects.requireNonNull(ai, "ai");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");

        this.lessons = List.copyOf(storage.loadLessons());
        this.puzzles = List.copyOf(storage.loadPuzzles());

        Storage.UserData userData = storage.loadUserData();
        this.settings = userData.settings() != null ? userData.settings() : new Settings();
        if (userData.profiles() != null) {
            this.profiles.addAll(userData.profiles());
        }
        if (this.profiles.isEmpty()) {
            Profile def = new Profile("default", "플레이어");
            this.profiles.add(def);
            this.activeProfileId = def.id();
        } else {
            String candidate = userData.activeProfileId();
            boolean valid = candidate != null && this.profiles.stream().anyMatch(p -> p.id().equals(candidate));
            this.activeProfileId = valid ? candidate : this.profiles.getFirst().id();
        }
    }

    // ---- 콘텐츠 조회 ----

    public List<Lesson> lessons() {
        return lessons;
    }

    public List<Lesson> lessons(Lesson.Category category) {
        return lessons.stream().filter(l -> l.category() == category).toList();
    }

    public List<Puzzle> puzzles() {
        return puzzles;
    }

    public List<String> puzzleThemes() {
        return puzzles.stream().map(Puzzle::theme).distinct().toList();
    }

    // ---- 세션 및 대국 생성 ----

    public LessonSession startLesson(Lesson lesson) {
        return new LessonSession(lesson);
    }

    public PuzzleSession startPuzzle(Puzzle puzzle) {
        return new PuzzleSession(puzzle, ai);
    }

    public ChessGame newGame() {
        return new ChessGame();
    }

    // ---- AI 탐색 및 호버 수 평가 ----

    public Optional<Move> chooseAiMove(Board board, Difficulty difficulty, BooleanSupplier cancelled) {
        return ai.chooseMove(board, difficulty, cancelled);
    }

    public Optional<Move> computeHint(Board board) {
        return ai.hint(board);
    }

    public synchronized MoveAnalyzer.Evaluation evaluateMove(Board board, Move move) {
        int base = baselineCache.computeIfAbsent(board, analyzer::baselineScore);
        return analyzer.evaluate(board, move, base);
    }

    // ---- 프로필 & 학습 기록 관리 (변경 즉시 저장 + UI 알림) ----

    public List<Profile> profiles() {
        return List.copyOf(profiles);
    }

    public Profile activeProfile() {
        return profiles.stream()
                .filter(p -> p.id().equals(activeProfileId))
                .findFirst()
                .orElseGet(profiles::getFirst);
    }

    public void setActiveProfile(String profileId) {
        if (profiles.stream().anyMatch(p -> p.id().equals(profileId))) {
            this.activeProfileId = profileId;
            persistAndNotify();
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
        persistAndNotify();
        return profile;
    }

    public boolean deleteProfile(String profileId) {
        if (profiles.size() <= 1) {
            return false;
        }
        boolean removed = profiles.removeIf(p -> p.id().equals(profileId));
        if (removed) {
            if (profileId.equals(activeProfileId)) {
                activeProfileId = profiles.getFirst().id();
            }
            persistAndNotify();
        }
        return removed;
    }

    public void completeLesson(String lessonId) {
        if (!activeProfile().isLessonCompleted(lessonId)) {
            activeProfile().markLessonCompleted(lessonId);
            persistAndNotify();
        }
    }

    public void recordPuzzle(String puzzleId, boolean solved) {
        activeProfile().recordPuzzle(puzzleId, solved);
        persistAndNotify();
    }

    public void recordAiResult(Difficulty difficulty, int outcome) {
        activeProfile().recordAiResult(difficulty, outcome);
        persistAndNotify();
    }

    public void clearActiveProgress() {
        activeProfile().clearProgress();
        persistAndNotify();
    }

    // ---- 설정 관리 ----

    public Settings settings() {
        return settings;
    }

    public void updateSettings(Consumer<Settings> updater) {
        Objects.requireNonNull(updater, "updater").accept(settings);
        persistAndNotify();
    }

    // ---- 관찰자 & 영속 저장 ----

    public void addChangeListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void save() {
        storage.saveUserData(new Storage.UserData(activeProfileId, settings, List.copyOf(profiles)));
    }

    private void persistAndNotify() {
        save();
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
