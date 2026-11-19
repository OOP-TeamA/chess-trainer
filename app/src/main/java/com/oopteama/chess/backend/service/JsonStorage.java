package com.oopteama.chess.backend.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.ChessAI.Difficulty;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.training.Lesson;
import com.oopteama.chess.backend.training.Puzzle;

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
 * Gson 기반 JSON 직렬화 저장소 구현체입니다.
 * 책임(SRP): 번들 JSON(lessons.json, puzzles.json) 역직렬화와 사용자 데이터(userdata.json)의 원자적 JSON 저장/로드만 담당합니다.
 */
public final class JsonStorage implements Storage {

    private static final String LESSONS_JSON = "/data/lessons.json";
    private static final String PUZZLES_JSON = "/data/puzzles.json";
    private static final String USERDATA_FILE = "userdata.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LESSON_LIST_TYPE = new TypeToken<List<LessonDto>>() {}.getType();
    private static final Type PUZZLE_LIST_TYPE = new TypeToken<List<PuzzleDto>>() {}.getType();

    private final Path dataDir;

    public JsonStorage(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
    }

    public static Path defaultDataDir() {
        String prop = System.getProperty("chessmate.home", "").trim();
        return prop.isEmpty()
                ? Path.of(System.getProperty("user.home"), ".chessmate")
                : Path.of(prop);
    }

    // ---- 번들 JSON 콘텐츠 로드 ----

    @Override
    public List<Lesson> loadLessons() {
        List<LessonDto> dtos = readResourceJson(LESSONS_JSON, LESSON_LIST_TYPE);
        List<Lesson> out = new ArrayList<>(dtos.size());
        for (LessonDto dto : dtos) {
            if (dto.id == null || dto.steps == null || dto.steps.isEmpty()) continue;

            Lesson.Category cat = Lesson.Category.valueOf(
                    dto.category != null ? dto.category.toUpperCase() : "TUTORIAL"
            );
            Side side = "BLACK".equalsIgnoreCase(dto.side) ? Side.BLACK : Side.WHITE;

            Board running = Board.initial();
            if (dto.setup != null) {
                for (String uci : dto.setup) {
                    running = running.applyMove(Move.fromUci(uci));
                }
            }

            List<Lesson.Step> steps = new ArrayList<>(dto.steps.size());
            for (StepDto s : dto.steps) {
                Board stepBoard;
                if ("start".equalsIgnoreCase(s.fen) || "continue".equalsIgnoreCase(s.fen)) {
                    stepBoard = running;
                } else {
                    stepBoard = s.sandbox ? Board.fromSandboxFen(s.fen) : Board.fromFen(s.fen);
                }

                List<Integer> hints = s.hints == null ? List.of()
                        : s.hints.stream().map(Board::parseSquare).toList();
                Move reply = (s.reply != null && !s.reply.isBlank()) ? Move.fromUci(s.reply) : null;

                Lesson.Step step = new Lesson.Step(
                        stepBoard, s.goal, hints,
                        s.prompt != null ? s.prompt : "",
                        s.success != null ? s.success : "",
                        reply
                );
                steps.add(step);

                Board after = stepBoard;
                Move primary = step.primarySolution(stepBoard).orElse(null);
                if (primary != null) {
                    after = after.applyMove(primary);
                    if (reply != null && after.legalMoves().contains(reply)) {
                        after = after.applyMove(reply);
                    }
                }
                running = after;
            }
            out.add(new Lesson(dto.id, cat, dto.title, dto.summary != null ? dto.summary : "", side, List.copyOf(steps)));
        }
        return List.copyOf(out);
    }

    @Override
    public List<Puzzle> loadPuzzles() {
        List<PuzzleDto> dtos = readResourceJson(PUZZLES_JSON, PUZZLE_LIST_TYPE);
        List<Puzzle> out = new ArrayList<>(dtos.size());
        for (PuzzleDto dto : dtos) {
            if (dto.id == null || dto.fen == null) continue;
            Board board = Board.fromFen(dto.fen);
            List<Move> sol = dto.solution == null ? List.of()
                    : dto.solution.stream().map(Move::fromUci).toList();
            String theme = dto.theme != null ? dto.theme : "전술";
            int mateIn = dto.mate;
            if (mateIn <= 0 && theme.startsWith("메이트인")) {
                try {
                    mateIn = Integer.parseInt(theme.substring("메이트인".length()));
                } catch (NumberFormatException ignored) {
                }
            }
            if (mateIn <= 0 && sol.isEmpty()) continue;
            out.add(new Puzzle(dto.id, theme, dto.title != null ? dto.title : dto.id, board, sol, mateIn,
                    dto.explain != null ? dto.explain : ""));
        }
        return List.copyOf(out);
    }

    // ---- 사용자 JSON 로드 / 원자적 저장 ----

    @Override
    public UserData loadUserData() {
        Path file = dataDir.resolve(USERDATA_FILE);
        if (!Files.exists(file)) {
            return new UserData(null, new Settings(), List.of());
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            UserDataDto dto = GSON.fromJson(reader, UserDataDto.class);
            if (dto == null) {
                return new UserData(null, new Settings(), List.of());
            }
            Settings settings = fromSettingsDto(dto.settings);
            List<Profile> profiles = new ArrayList<>();
            if (dto.profiles != null) {
                for (ProfileDto pd : dto.profiles) {
                    if (pd.id != null && !pd.id.isBlank()) {
                        profiles.add(fromProfileDto(pd));
                    }
                }
            }
            return new UserData(dto.activeProfileId, settings, profiles);
        } catch (Exception e) {
            System.err.println("사용자 JSON 로드 실패(기본값 사용): " + e.getMessage());
            return new UserData(null, new Settings(), List.of());
        }
    }

    @Override
    public void saveUserData(UserData data) {
        UserDataDto dto = new UserDataDto();
        dto.activeProfileId = data.activeProfileId();
        dto.settings = toSettingsDto(data.settings());
        dto.profiles = data.profiles().stream().map(JsonStorage::toProfileDto).toList();
        try {
            Files.createDirectories(dataDir);
            Path target = dataDir.resolve(USERDATA_FILE);
            Path tmp = target.resolveSibling(USERDATA_FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(dto) + "\n", StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("사용자 JSON 저장 실패: " + e.getMessage());
        }
    }

    private static <T> T readResourceJson(String path, Type type) {
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

    // ---- JSON 직렬화 전용 DTO (외부 노출 없음) ----

    private static final class LessonDto {
        String id;
        String category;
        String title;
        String summary;
        String side;
        List<String> setup;
        List<StepDto> steps;
    }

    private static final class StepDto {
        String fen;
        boolean sandbox;
        String goal;
        List<String> hints;
        String prompt;
        String success;
        String reply;
    }

    private static final class PuzzleDto {
        String id;
        String theme;
        int mate;
        String title;
        String fen;
        List<String> solution;
        String explain;
    }

    private static final class UserDataDto {
        String activeProfileId;
        SettingsDto settings;
        List<ProfileDto> profiles;
    }

    private static final class SettingsDto {
        String boardTheme = "WALNUT";
        boolean showCoords = true;
        boolean showLegalMoves = true;
        boolean highlightLastMove = true;
        boolean showMovePreview = true;
        boolean autoFlipLocal = false;
    }

    private static final class ProfileDto {
        String id;
        String name;
        List<String> completedLessons = List.of();
        Map<String, int[]> puzzleStats = new LinkedHashMap<>();
        Map<String, int[]> aiRecords = new LinkedHashMap<>();
    }

    private static Settings fromSettingsDto(SettingsDto d) {
        Settings s = new Settings();
        if (d != null) {
            s.setBoardTheme(Settings.BoardTheme.parse(d.boardTheme));
            s.setShowCoords(d.showCoords);
            s.setShowLegalMoves(d.showLegalMoves);
            s.setHighlightLastMove(d.highlightLastMove);
            s.setShowMovePreview(d.showMovePreview);
            s.setAutoFlipLocal(d.autoFlipLocal);
        }
        return s;
    }

    private static SettingsDto toSettingsDto(Settings s) {
        SettingsDto d = new SettingsDto();
        d.boardTheme = s.boardTheme().name();
        d.showCoords = s.showCoords();
        d.showLegalMoves = s.showLegalMoves();
        d.highlightLastMove = s.highlightLastMove();
        d.showMovePreview = s.showMovePreview();
        d.autoFlipLocal = s.autoFlipLocal();
        return d;
    }

    private static Profile fromProfileDto(ProfileDto d) {
        Profile p = new Profile(d.id, d.name != null ? d.name : "플레이어");
        if (d.completedLessons != null) {
            d.completedLessons.forEach(p::markLessonCompleted);
        }
        if (d.puzzleStats != null) {
            d.puzzleStats.forEach((pid, stat) -> {
                if (stat != null && stat.length == 2) p.putPuzzleStat(pid, stat[0], stat[1]);
            });
        }
        if (d.aiRecords != null) {
            d.aiRecords.forEach((diffName, rec) -> {
                if (rec != null && rec.length == 3) {
                    try {
                        p.putAiRecord(Difficulty.valueOf(diffName), rec[0], rec[1], rec[2]);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });
        }
        return p;
    }

    private static ProfileDto toProfileDto(Profile p) {
        ProfileDto d = new ProfileDto();
        d.id = p.id();
        d.name = p.name();
        d.completedLessons = new ArrayList<>(p.completedLessons());
        d.puzzleStats = new LinkedHashMap<>(p.puzzleStats());
        d.aiRecords = new LinkedHashMap<>();
        for (Difficulty diff : Difficulty.values()) {
            int[] rec = p.aiRecord(diff);
            if (rec[0] > 0 || rec[1] > 0 || rec[2] > 0) {
                d.aiRecords.put(diff.name(), rec);
            }
        }
        return d;
    }
}
