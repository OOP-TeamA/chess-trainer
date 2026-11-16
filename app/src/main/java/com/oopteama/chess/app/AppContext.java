package com.oopteama.chess.app;

import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.TermQuestion;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.GameLibrary;
import com.oopteama.chess.io.GlossaryRepository;
import com.oopteama.chess.io.LessonRepository;
import com.oopteama.chess.io.PuzzleRepository;
import com.oopteama.chess.io.TermQuestionRepository;
import com.oopteama.chess.progress.ProfileManager;
import com.oopteama.chess.progress.Progress;
import com.oopteama.chess.progress.ProgressStore;
import com.oopteama.chess.progress.Recommender;
import com.oopteama.chess.ui.Services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 객체 조립의 유일한 장소(Composition Root). 공유 객체를 여기서 만들어 {@link Services}로 화면에 넘긴다.
 * Singleton은 쓰지 않는다. ui는 app을 모르고 이 클래스가 ui의 Services를 구현한다(G16).
 *
 * <p>{@link #load(DataPaths)}는 작업 스레드에서 부르고, 만든 뒤에는 EDT에서만 쓴다.
 * 활성 프로필의 {@link Progress}가 바뀔 때마다 {@link ProgressStore}로 자동 저장한다.
 */
public final class AppContext implements Services {

    private static final Logger LOG = Logger.getLogger(AppContext.class.getName());

    static final String LESSONS_DIR = "data/lessons";
    static final String GLOSSARY_FILE = "data/glossary.txt";
    static final String PUZZLES_FILE = "data/puzzles.txt";
    static final String PUZZLES_MISSING = PUZZLES_FILE + "이 없어 퍼즐 없이 시작합니다";
    static final String TERM_QUIZ_FILE = "data/term-quiz.txt";

    private final DataPaths paths;
    private final List<Lesson> lessons;
    private final List<Puzzle> puzzles;
    private final Glossary glossary;
    private final List<TermQuestion> termQuestions;
    private final GameLibrary games;
    private final ProfileManager profiles;
    private final ProgressStore progressStore;
    private final Progress progress;
    private final Recommender recommender;

    /** 기록이 바뀔 때마다 저장한다. 실패는 로그에 남기고, 창을 닫을 때 {@link #saveProgress()}가 다시 시도한다. */
    private final Runnable autoSave = this::saveQuietly;

    private AppContext(DataPaths paths, List<Lesson> lessons, List<Puzzle> puzzles, Glossary glossary,
                       List<TermQuestion> termQuestions, GameLibrary games,
                       ProfileManager profiles, ProgressStore progressStore, Progress progress) {
        this.paths = paths;
        this.lessons = List.copyOf(lessons);
        this.puzzles = List.copyOf(puzzles);
        this.glossary = glossary;
        this.termQuestions = List.copyOf(termQuestions);
        this.games = games;
        this.profiles = profiles;
        this.progressStore = progressStore;
        this.progress = progress;
        this.recommender = new Recommender(this.lessons, this.puzzles, progress);
        progress.addChangeListener(autoSave);
    }

    /**
     * 모든 콘텐츠와 활성 프로필의 기록을 읽는다. 콘텐츠 형식 오류는 각 저장소가 로그에 남기고 그 항목만 건너뛴다.
     * 레슨 목록에 적힌 파일이 없으면 경고하고 그 레슨만 건너뛴다. 퍼즐 파일이 아직 없으면 경고하고 퍼즐 없이 시작한다.
     *
     * @param paths 경로 계산기(앱은 {@link DataPaths#standard()}, 테스트는 임시 폴더)
     * @return 조립된 컨텍스트
     * @throws IOException 레슨 목록·용어집·용어 퀴즈 파일이 없거나 읽지 못했을 때, 프로필 목록을 읽거나 만들지 못했을 때
     */
    public static AppContext load(DataPaths paths) throws IOException {
        Objects.requireNonNull(paths, "paths");
        List<Lesson> lessons = loadLessons(paths);
        List<Puzzle> puzzles = loadPuzzles(paths);
        Glossary glossary;
        try (Reader in = paths.openResource(GLOSSARY_FILE)) {
            glossary = new Glossary(new GlossaryRepository().loadAll(in, fileName(GLOSSARY_FILE)));
        }
        List<TermQuestion> questions;
        try (Reader in = paths.openResource(TERM_QUIZ_FILE)) {
            questions = new TermQuestionRepository(glossary).loadAll(in, fileName(TERM_QUIZ_FILE));
        }
        ProfileManager profiles = new ProfileManager(paths);
        GameLibrary games = new GameLibrary(paths, profiles.active().id());
        int bundledGames = games.bundled().size(); // 시작할 때 읽어 두고 형식 오류를 로그로 알린다
        ProgressStore store = new ProgressStore(paths);
        Progress progress = store.load(profiles.active().id());
        LOG.info("콘텐츠 로드: 레슨 %d개, 퍼즐 %d개, 용어 %d개, 용어 문항 %d개, 기보 %d개, 프로필 %s".formatted(
                lessons.size(), puzzles.size(), glossary.all().size(), questions.size(), bundledGames,
                profiles.active().id()));
        return new AppContext(paths, lessons, puzzles, glossary, questions, games, profiles, store, progress);
    }

    /** {@code lessons/index.txt} 순서대로 레슨을 읽는다. 목록에 있지만 없는 파일은 건너뛴다. */
    private static List<Lesson> loadLessons(DataPaths paths) throws IOException {
        List<Lesson> out = new ArrayList<>();
        for (String name : paths.index(LESSONS_DIR)) {
            try (Reader in = paths.openResource(LESSONS_DIR + "/" + name)) {
                out.addAll(new LessonRepository().loadAll(in, name));
            } catch (FileNotFoundException e) {
                LOG.warning(LESSONS_DIR + "/index.txt에 적힌 레슨 파일이 없어 건너뜁니다: " + name);
            }
        }
        return out;
    }

    /** 퍼즐 파일을 읽는다. 아직 작성되지 않았으면 빈 목록. */
    private static List<Puzzle> loadPuzzles(DataPaths paths) throws IOException {
        try (Reader in = paths.openResource(PUZZLES_FILE)) {
            return new PuzzleRepository().loadAll(in, fileName(PUZZLES_FILE));
        } catch (FileNotFoundException e) {
            LOG.warning(PUZZLES_MISSING);
            return List.of();
        }
    }

    private static String fileName(String resource) {
        return resource.substring(resource.lastIndexOf('/') + 1);
    }

    /**
     * 경로 계산기.
     *
     * @return load에 넘긴 DataPaths
     */
    public DataPaths paths() {
        return paths;
    }

    /**
     * 활성 프로필의 기록을 지금 저장한다. 창을 닫기 직전에 MainFrame의 CloseHandler로 부른다.
     *
     * @throws IOException 쓰기에 실패했을 때(기존 파일은 그대로)
     */
    public void saveProgress() throws IOException {
        progressStore.save(profiles.active().id(), progress);
    }

    private void saveQuietly() {
        try {
            saveProgress();
        } catch (IOException e) {
            LOG.warning("학습 기록 자동 저장 실패(종료할 때 다시 시도합니다): " + e);
        }
    }

    // ---- Services ----

    @Override
    public RuleSet standardRules() {
        return RulesKind.STANDARD.rules();
    }

    @Override
    public RuleSet sandboxRules() {
        return RulesKind.SANDBOX.rules();
    }

    @Override
    public List<Lesson> lessons() {
        return lessons;
    }

    @Override
    public List<Puzzle> puzzles() {
        return puzzles;
    }

    @Override
    public List<TermQuestion> termQuestions() {
        return termQuestions;
    }

    @Override
    public Glossary glossary() {
        return glossary;
    }

    @Override
    public GameLibrary games() {
        return games;
    }

    @Override
    public Progress progress() {
        return progress;
    }

    @Override
    public ProfileManager profiles() {
        return profiles;
    }

    @Override
    public Recommender recommender() {
        return recommender;
    }
}
