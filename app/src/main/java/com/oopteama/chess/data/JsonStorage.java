package com.oopteama.chess.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.training.LessonStep;
import com.oopteama.chess.training.Puzzle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON 파일로 데이터를 읽고 쓰는 Storage 구현입니다 (Gson 라이브러리 사용).
 * <ul>
 *   <li>레슨·퍼즐: 프로그램에 함께 들어 있는 data/lessons.json, data/puzzles.json을 읽습니다.</li>
 *   <li>사용자 데이터: dataDir/userdata.json에 저장합니다.</li>
 * </ul>
 */
public final class JsonStorage implements Storage {

    private static final String LESSONS_JSON = "/data/lessons.json";
    private static final String PUZZLES_JSON = "/data/puzzles.json";
    private static final String USERDATA_FILE = "userdata.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataDir;

    public JsonStorage(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
    }

    /** 기본 저장 폴더: ~/.chessmate (실행 옵션 -Dchessmate.home=경로 로 바꿀 수 있음). */
    public static Path defaultDataDir() {
        String custom = System.getProperty("chessmate.home", "").trim();
        return custom.isEmpty() ? Path.of(System.getProperty("user.home"), ".chessmate") : Path.of(custom);
    }

    // ---- 레슨 ----

    @Override
    public List<Lesson> loadLessons() {
        List<LessonJson> lessonJsons = readResource(LESSONS_JSON, new TypeToken<List<LessonJson>>() { }.getType());
        List<Lesson> lessons = new ArrayList<>();
        for (LessonJson json : lessonJsons) {
            if (json.id == null || json.steps == null || json.steps.isEmpty()) {
                continue;
            }
            Lesson.Category category = Lesson.Category.valueOf(json.category != null ? json.category.toUpperCase() : "TUTORIAL");
            Side side = "BLACK".equalsIgnoreCase(json.side) ? Side.BLACK : Side.WHITE;
            lessons.add(new Lesson(json.id, category, json.title, json.summary != null ? json.summary : "",
                    side, buildSteps(json)));
        }
        return List.copyOf(lessons);
    }

    /**
     * 레슨 단계들을 만듭니다. 오프닝 레슨은 fen이 "start"/"continue"라서
     * 앞 단계의 정답 수와 상대 응수를 차례로 둔 국면에서 이어집니다.
     */
    private static List<LessonStep> buildSteps(LessonJson json) {
        Board running = Board.initial();
        if (json.setup != null) {
            for (String move : json.setup) {
                running = running.apply(Move.of(move));
            }
        }
        List<LessonStep> steps = new ArrayList<>();
        for (StepJson s : json.steps) {
            Board stepBoard;
            if ("start".equalsIgnoreCase(s.fen) || "continue".equalsIgnoreCase(s.fen)) {
                stepBoard = running;
            } else {
                stepBoard = s.sandbox ? Board.fromSandboxFen(s.fen) : Board.fromFen(s.fen);
            }
            List<Position> hints = new ArrayList<>();
            if (s.hints != null) {
                for (String square : s.hints) {
                    hints.add(Position.of(square));
                }
            }
            Move reply = (s.reply != null && !s.reply.isBlank()) ? Move.of(s.reply) : null;
            LessonStep step = new LessonStep(stepBoard, s.goal, List.copyOf(hints),
                    s.prompt != null ? s.prompt : "", s.success != null ? s.success : "", reply);
            steps.add(step);

            // 다음 단계가 "continue"이면 이 단계의 정답과 상대 응수를 둔 국면에서 시작합니다.
            Board after = stepBoard;
            Move solution = step.primarySolution(stepBoard).orElse(null);
            if (solution != null) {
                after = after.apply(solution);
                if (reply != null && Rules.legalMoves(after).contains(reply)) {
                    after = after.apply(reply);
                }
            }
            running = after;
        }
        return List.copyOf(steps);
    }

    // ---- 퍼즐 ----

    @Override
    public List<Puzzle> loadPuzzles() {
        List<PuzzleJson> puzzleJsons = readResource(PUZZLES_JSON, new TypeToken<List<PuzzleJson>>() { }.getType());
        List<Puzzle> puzzles = new ArrayList<>();
        for (PuzzleJson json : puzzleJsons) {
            if (json.id == null || json.fen == null) {
                continue;
            }
            List<Move> solution = new ArrayList<>();
            if (json.solution != null) {
                for (String move : json.solution) {
                    solution.add(Move.of(move));
                }
            }
            String theme = json.theme != null ? json.theme : "전술";
            int mateIn = json.mate;
            if (mateIn <= 0 && theme.startsWith("메이트인")) { // "메이트인2" → 2
                try {
                    mateIn = Integer.parseInt(theme.substring("메이트인".length()));
                } catch (NumberFormatException ignored) {
                    // 숫자가 없으면 전술 퍼즐로 봅니다.
                }
            }
            if (mateIn <= 0 && solution.isEmpty()) {
                continue;
            }
            puzzles.add(new Puzzle(json.id, theme, json.title != null ? json.title : json.id,
                    Board.fromFen(json.fen), List.copyOf(solution), mateIn,
                    json.explain != null ? json.explain : ""));
        }
        return List.copyOf(puzzles);
    }

    // ---- 사용자 데이터 ----

    @Override
    public UserData loadUserData() {
        Path file = dataDir.resolve(USERDATA_FILE);
        if (!Files.exists(file)) {
            return new UserData(null, new Settings(), List.of());
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            UserDataJson json = GSON.fromJson(reader, UserDataJson.class);
            if (json == null) {
                return new UserData(null, new Settings(), List.of());
            }
            List<Profile> profiles = new ArrayList<>();
            if (json.profiles != null) {
                for (ProfileJson profileJson : json.profiles) {
                    if (profileJson.id != null && !profileJson.id.isBlank()) {
                        profiles.add(toProfile(profileJson));
                    }
                }
            }
            return new UserData(json.activeProfileId, toSettings(json.settings), profiles);
        } catch (Exception e) {
            System.err.println("사용자 데이터를 읽지 못해 기본값을 씁니다: " + e.getMessage());
            return new UserData(null, new Settings(), List.of());
        }
    }

    @Override
    public void saveUserData(UserData data) {
        UserDataJson json = new UserDataJson();
        json.activeProfileId = data.activeProfileId();
        json.settings = toJson(data.settings());
        json.profiles = data.profiles().stream().map(JsonStorage::toJson).toList();
        try {
            Files.createDirectories(dataDir);
            Path target = dataDir.resolve(USERDATA_FILE);
            Path temp = target.resolveSibling(USERDATA_FILE + ".tmp");
            Files.writeString(temp, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
            // 임시 파일에 다 쓴 뒤 바꿔치기해서, 저장 도중 꺼져도 기존 파일이 깨지지 않게 합니다.
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("사용자 데이터 저장 실패: " + e.getMessage());
        }
    }

    private static <T> T readResource(String path, Type type) {
        try (InputStream in = JsonStorage.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("JSON 리소스를 찾을 수 없습니다: " + path);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, type);
            }
        } catch (IOException e) {
            throw new IllegalStateException("JSON 리소스 읽기 실패: " + path, e);
        }
    }

    // ---- JSON 모양 그대로의 클래스 (Gson이 필드 이름으로 읽고 씀) ----

    private static final class LessonJson {
        String id;
        String category;
        String title;
        String summary;
        String side;
        List<String> setup;
        List<StepJson> steps;
    }

    private static final class StepJson {
        String fen;
        boolean sandbox;
        String goal;
        List<String> hints;
        String prompt;
        String success;
        String reply;
    }

    private static final class PuzzleJson {
        String id;
        String theme;
        int mate;
        String title;
        String fen;
        List<String> solution;
        String explain;
    }

    private static final class UserDataJson {
        String activeProfileId;
        SettingsJson settings;
        List<ProfileJson> profiles;
    }

    private static final class SettingsJson {
        String boardTheme = "WALNUT";
        boolean showCoords = true;
        boolean showLegalMoves = true;
        boolean highlightLastMove = true;
        boolean autoFlipLocal = false;
    }

    private static final class ProfileJson {
        String id;
        String name;
        List<String> completedLessons = List.of();
        Map<String, int[]> puzzleStats = new LinkedHashMap<>();
        Map<String, int[]> aiRecords = new LinkedHashMap<>();
    }

    // ---- JSON 클래스 ↔ 앱 클래스 변환 ----

    private static Settings toSettings(SettingsJson json) {
        Settings settings = new Settings();
        if (json != null) {
            settings.setBoardTheme(Settings.BoardTheme.parse(json.boardTheme));
            settings.setShowCoords(json.showCoords);
            settings.setShowLegalMoves(json.showLegalMoves);
            settings.setHighlightLastMove(json.highlightLastMove);
            settings.setAutoFlipLocal(json.autoFlipLocal);
        }
        return settings;
    }

    private static SettingsJson toJson(Settings settings) {
        SettingsJson json = new SettingsJson();
        json.boardTheme = settings.boardTheme().name();
        json.showCoords = settings.showCoords();
        json.showLegalMoves = settings.showLegalMoves();
        json.highlightLastMove = settings.highlightLastMove();
        json.autoFlipLocal = settings.autoFlipLocal();
        return json;
    }

    private static Profile toProfile(ProfileJson json) {
        Profile profile = new Profile(json.id, json.name != null ? json.name : "플레이어");
        if (json.completedLessons != null) {
            json.completedLessons.forEach(profile::markLessonCompleted);
        }
        if (json.puzzleStats != null) {
            json.puzzleStats.forEach((puzzleId, stat) -> {
                if (stat != null && stat.length == 2) {
                    profile.putPuzzleStat(puzzleId, stat[0], stat[1]);
                }
            });
        }
        if (json.aiRecords != null) {
            json.aiRecords.forEach((difficultyName, record) -> {
                if (record != null && record.length == 3) {
                    try {
                        profile.putAiRecord(Difficulty.valueOf(difficultyName), record[0], record[1], record[2]);
                    } catch (IllegalArgumentException ignored) {
                        // 모르는 난이도 이름은 건너뜁니다.
                    }
                }
            });
        }
        return profile;
    }

    private static ProfileJson toJson(Profile profile) {
        ProfileJson json = new ProfileJson();
        json.id = profile.id();
        json.name = profile.name();
        json.completedLessons = new ArrayList<>(profile.completedLessons());
        json.puzzleStats = new LinkedHashMap<>(profile.puzzleStats());
        json.aiRecords = new LinkedHashMap<>();
        for (Difficulty difficulty : Difficulty.values()) {
            int[] record = profile.aiRecord(difficulty);
            if (record[0] > 0 || record[1] > 0 || record[2] > 0) {
                json.aiRecords.put(difficulty.name(), record);
            }
        }
        return json;
    }
}
