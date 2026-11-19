package com.oopteama.chess.model;

import com.oopteama.chess.engine.Board;
import com.oopteama.chess.engine.ChessAI.Difficulty;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece.Side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 번들 콘텐츠(레슨·퍼즐) 로드와 사용자 데이터(프로필·설정)의 영속 저장을 담당하는 단일 저장소입니다.
 */
public final class AppStore {

    private static final String LESSONS_RESOURCE = "/data/lessons.txt";
    private static final String PUZZLES_RESOURCE = "/data/puzzles.txt";

    private final Path dataDir;
    private final List<Lesson> lessons;
    private final List<Puzzle> puzzles;
    private final List<Profile> profiles = new ArrayList<>();
    private final Settings settings = new Settings();
    private final List<Runnable> listeners = new ArrayList<>();
    private String activeProfileId;

    public AppStore(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
        this.lessons = List.copyOf(loadLessons());
        this.puzzles = List.copyOf(loadPuzzles());
        loadSettings();
        loadProfiles();
    }

    public static Path defaultDataDir() {
        String prop = System.getProperty("chessmate.home", "").trim();
        if (!prop.isEmpty()) {
            return Path.of(prop);
        }
        return Path.of(System.getProperty("user.home"), ".chessmate");
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

    public Settings settings() {
        return settings;
    }

    // ---- 프로필 관리 ----

    public List<Profile> profiles() {
        return List.copyOf(profiles);
    }

    public Profile activeProfile() {
        return profiles.stream()
                .filter(p -> p.id().equals(activeProfileId))
                .findFirst()
                .orElseGet(() -> profiles.getFirst());
    }

    public void setActiveProfile(String profileId) {
        if (profiles.stream().anyMatch(p -> p.id().equals(profileId))) {
            this.activeProfileId = profileId;
            saveAndNotify();
        }
    }

    public Profile createProfile(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("프로필 이름을 입력해 주세요.");
        }
        String id = "p-" + UUID.randomUUID().toString().substring(0, 8);
        Profile profile = new Profile(id, clean);
        profiles.add(profile);
        activeProfileId = id;
        saveAndNotify();
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
            saveAndNotify();
        }
        return removed;
    }

    // ---- 변경 알림 및 저장 ----

    public void addChangeListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void saveAndNotify() {
        save();
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    public void save() {
        try {
            Files.createDirectories(dataDir);
            writeAtomic(dataDir.resolve("settings.txt"), serializeSettings());
            writeAtomic(dataDir.resolve("profiles.txt"), serializeProfiles());
        } catch (IOException e) {
            System.err.println("데이터 저장 실패: " + e.getMessage());
        }
    }

    private static void writeAtomic(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // ---- 번들 콘텐츠 파싱 (--- 구분, key=value) ----

    private List<Lesson> loadLessons() {
        List<Lesson> out = new ArrayList<>();
        for (List<String> block : readBlocks(readResourceLines(LESSONS_RESOURCE))) {
            Map<String, String> meta = new LinkedHashMap<>();
            List<String> stepLines = new ArrayList<>();
            for (String line : block) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if ("step".equals(key)) {
                    stepLines.add(val);
                } else {
                    meta.put(key, val);
                }
            }
            if (!meta.containsKey("id") || stepLines.isEmpty()) {
                continue;
            }
            String id = meta.get("id");
            Lesson.Category category = Lesson.Category.valueOf(meta.getOrDefault("category", "TUTORIAL").toUpperCase());
            String title = meta.getOrDefault("title", id);
            String summary = meta.getOrDefault("summary", "");
            Side side = "BLACK".equalsIgnoreCase(meta.get("side")) ? Side.BLACK : Side.WHITE;

            Board running = Board.initial();
            String setup = meta.getOrDefault("setup", "").trim();
            if (!setup.isEmpty()) {
                for (String uci : setup.split("\\s+")) {
                    running = running.applyMove(Move.fromUci(uci));
                }
            }

            List<Lesson.Step> steps = new ArrayList<>();
            for (String rawStep : stepLines) {
                // 형식: fen(또는 start/continue) | mode | goal | hints | prompt | success [| replyUci]
                String[] parts = rawStep.split("\\|", -1);
                String fenSpec = parts[0].trim();
                boolean sandbox = "sandbox".equalsIgnoreCase(parts[1].trim());
                String goal = parts[2].trim();
                List<Integer> hints = parseSquares(parts[3].trim());
                String prompt = parts[4].trim();
                String success = parts[5].trim();
                Move reply = (parts.length >= 7 && !parts[6].trim().isEmpty())
                        ? Move.fromUci(parts[6].trim())
                        : null;

                Board stepBoard;
                if ("start".equalsIgnoreCase(fenSpec)) {
                    stepBoard = running;
                } else if ("continue".equalsIgnoreCase(fenSpec)) {
                    stepBoard = running;
                } else {
                    stepBoard = sandbox ? Board.fromSandboxFen(fenSpec) : Board.fromFen(fenSpec);
                }

                Lesson.Step step = new Lesson.Step(stepBoard, goal, hints, prompt, success, reply);
                steps.add(step);

                // 다음 단계에서 'continue'를 쓰기 위해 정답 수 + 상대 응수 적용
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
            out.add(new Lesson(id, category, title, summary, side, List.copyOf(steps)));
        }
        return out;
    }

    private List<Puzzle> loadPuzzles() {
        List<Puzzle> out = new ArrayList<>();
        for (List<String> block : readBlocks(readResourceLines(PUZZLES_RESOURCE))) {
            Map<String, String> map = toMap(block);
            if (!map.containsKey("id") || !map.containsKey("fen")) {
                continue;
            }
            Board board = Board.fromFen(map.get("fen"));
            List<Move> solution = new ArrayList<>();
            String solRaw = map.getOrDefault("solution", "").trim();
            if (!solRaw.isEmpty()) {
                for (String token : solRaw.split("\\s+")) {
                    if (!token.isBlank()) {
                        solution.add(Move.fromUci(token));
                    }
                }
            }
            String theme = map.getOrDefault("theme", "전술");
            int mateIn = parseInt(map.getOrDefault("mate", "0"));
            if (mateIn <= 0 && theme.startsWith("메이트인")) {
                mateIn = parseInt(theme.substring("메이트인".length()));
            }
            if (mateIn <= 0 && solution.isEmpty()) {
                continue;
            }
            out.add(new Puzzle(
                    map.get("id"),
                    theme,
                    map.getOrDefault("title", map.get("id")),
                    board,
                    List.copyOf(solution),
                    mateIn,
                    map.getOrDefault("explain", "")
            ));
        }
        return out;
    }

    // ---- 설정 및 프로필 로드/직렬화 ----

    private void loadSettings() {
        Path file = dataDir.resolve("settings.txt");
        if (!Files.exists(file)) {
            return;
        }
        try {
            Map<String, String> map = toMap(Files.readAllLines(file, StandardCharsets.UTF_8));
            settings.setBoardTheme(Settings.BoardTheme.parse(map.getOrDefault("boardTheme", "WALNUT")));
            settings.setShowCoords(Boolean.parseBoolean(map.getOrDefault("showCoords", "true")));
            settings.setShowLegalMoves(Boolean.parseBoolean(map.getOrDefault("showLegalMoves", "true")));
            settings.setHighlightLastMove(Boolean.parseBoolean(map.getOrDefault("highlightLastMove", "true")));
            settings.setShowMovePreview(Boolean.parseBoolean(map.getOrDefault("showMovePreview", "true")));
            settings.setAutoFlipLocal(Boolean.parseBoolean(map.getOrDefault("autoFlipLocal", "false")));
        } catch (IOException ignored) {
        }
    }

    private String serializeSettings() {
        return "boardTheme=" + settings.boardTheme().name() + "\n"
                + "showCoords=" + settings.showCoords() + "\n"
                + "showLegalMoves=" + settings.showLegalMoves() + "\n"
                + "highlightLastMove=" + settings.highlightLastMove() + "\n"
                + "showMovePreview=" + settings.showMovePreview() + "\n"
                + "autoFlipLocal=" + settings.autoFlipLocal() + "\n";
    }

    private void loadProfiles() {
        Path file = dataDir.resolve("profiles.txt");
        if (Files.exists(file)) {
            try {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (List<String> block : readBlocks(lines)) {
                    Map<String, String> map = toMap(block);
                    if (map.containsKey("active")) {
                        activeProfileId = map.get("active");
                    }
                    if (!map.containsKey("id")) {
                        continue;
                    }
                    Profile p = new Profile(map.get("id"), map.getOrDefault("name", "플레이어"));
                    String lessonsDone = map.getOrDefault("lessons", "");
                    if (!lessonsDone.isEmpty()) {
                        for (String lid : lessonsDone.split(",")) {
                            if (!lid.isBlank()) p.markLessonCompleted(lid.trim());
                        }
                    }
                    for (Map.Entry<String, String> e : map.entrySet()) {
                        if (e.getKey().startsWith("puzzle.")) {
                            String pid = e.getKey().substring("puzzle.".length());
                            String[] nums = e.getValue().split(",");
                            if (nums.length == 2) {
                                p.putPuzzleStat(pid, parseInt(nums[0]), parseInt(nums[1]));
                            }
                        } else if (e.getKey().startsWith("ai.")) {
                            String dName = e.getKey().substring("ai.".length());
                            String[] nums = e.getValue().split(",");
                            if (nums.length == 3) {
                                try {
                                    Difficulty d = Difficulty.valueOf(dName);
                                    p.putAiRecord(d, parseInt(nums[0]), parseInt(nums[1]), parseInt(nums[2]));
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }
                    }
                    profiles.add(p);
                }
            } catch (IOException ignored) {
            }
        }
        if (profiles.isEmpty()) {
            Profile def = new Profile("default", "플레이어");
            profiles.add(def);
            activeProfileId = def.id();
        } else if (activeProfileId == null || profiles.stream().noneMatch(p -> p.id().equals(activeProfileId))) {
            activeProfileId = profiles.getFirst().id();
        }
    }

    private String serializeProfiles() {
        StringBuilder sb = new StringBuilder();
        sb.append("active=").append(activeProfileId).append('\n');
        for (Profile p : profiles) {
            sb.append("---\n");
            sb.append("id=").append(p.id()).append('\n');
            sb.append("name=").append(p.name()).append('\n');
            sb.append("lessons=").append(String.join(",", p.completedLessons())).append('\n');
            for (Map.Entry<String, int[]> e : p.puzzleStats().entrySet()) {
                sb.append("puzzle.").append(e.getKey()).append('=')
                        .append(e.getValue()[0]).append(',').append(e.getValue()[1]).append('\n');
            }
            for (Difficulty d : Difficulty.values()) {
                int[] r = p.aiRecord(d);
                if (r[0] > 0 || r[1] > 0 || r[2] > 0) {
                    sb.append("ai.").append(d.name()).append('=')
                            .append(r[0]).append(',').append(r[1]).append(',').append(r[2]).append('\n');
                }
            }
        }
        return sb.toString();
    }

    // ---- 공통 텍스트 유틸 ----

    private static List<String> readResourceLines(String resourcePath) {
        try (InputStream in = AppStore.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("리소스 파일을 찾을 수 없습니다: " + resourcePath);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return br.lines().toList();
            }
        } catch (IOException e) {
            throw new IllegalStateException("리소스 읽기 실패: " + resourcePath, e);
        }
    }

    private static List<List<String>> readBlocks(List<String> lines) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if ("---".equals(line)) {
                if (!current.isEmpty()) {
                    blocks.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(line);
            }
        }
        if (!current.isEmpty()) {
            blocks.add(current);
        }
        return blocks;
    }

    private static Map<String, String> toMap(List<String> lines) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return map;
    }

    private static List<Integer> parseSquares(String csv) {
        if (csv.isEmpty() || "-".equals(csv)) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>();
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (!t.isEmpty()) {
                out.add(Board.parseSquare(t));
            }
        }
        return List.copyOf(out);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
