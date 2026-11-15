package com.oopteama.chess.progress;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.TextFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 프로필별 학습 기록 파일 {@code profiles/<id>.txt}의 읽기·쓰기(기능 명세 6장 프로필 계약).
 *
 * <p>파일은 한 줄에 {@code 키=값} 하나다. 읽을 때:
 * <ul>
 *   <li>파일이 없거나 키가 빠지면 빈 집합·0으로 시작한다.</li>
 *   <li>파일이 있지만 읽지 못하면(잘못된 UTF-8 등) 빈 기록으로 시작하되, 그 기록을 처음 저장할 때
 *       원본을 {@code <id>.txt.unreadable-<시각>}으로 옮겨 덮어쓰지 않는다.</li>
 *   <li>잘못된 줄은 파일명·행 번호를 로그에 남기고 건너뛰며, 나머지 유효한 키는 복구한다.</li>
 *   <li>같은 키가 다시 나오면 처음 유효한 값을 쓴다.</li>
 *   <li>{@code puzzle.attempt.<id>=시도,정답}은 두 수가 0 이상이고 정답이 시도 이하여야 한다.</li>
 *   <li>이 단계가 해석하지 않는 키(오답 노트·즐겨찾기·AI 전적 등)는 그대로 보존해 다시 저장한다.</li>
 * </ul>
 * 쓰기는 {@link TextFiles#writeAtomic}으로 파일 전체를 원자적으로 바꾼다.
 */
public final class ProgressStore {

    private static final Logger LOG = Logger.getLogger(ProgressStore.class.getName());

    static final String LESSON_DONE = "lesson.done";
    static final String QUIZ_CORRECT_UNIQUE = "quiz.correct.unique";
    static final String QUIZ_ATTEMPTS = "quiz.attempts";
    static final String QUIZ_CORRECT_COUNT = "quiz.correct.count";
    /** 퍼즐 기록 키 접두사. 뒤에 퍼즐 ID가 붙는다. */
    static final String PUZZLE_ATTEMPT_PREFIX = "puzzle.attempt.";

    private static final char KEY_VALUE_SEPARATOR = '=';
    private static final String LIST_SEPARATOR = ",";
    private static final String COMMENT_PREFIX = "#";
    /** 읽지 못한 원본을 보존할 때 파일명 뒤에 붙이는 접미사(뒤에 시각이 붙는다). */
    static final String UNREADABLE_SUFFIX = ".unreadable-";
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final DataPaths paths;

    /**
     * 사용자 폴더의 기록 파일을 다루는 저장소를 만든다.
     *
     * @param paths 경로 계산기(테스트는 임시 폴더)
     */
    public ProgressStore(DataPaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    /**
     * 프로필의 기록을 읽는다. 파일이 없거나 읽지 못하면 빈 기록을, 깨진 줄은 건너뛰고 나머지를 돌려준다.
     *
     * @param profileId 프로필 ID
     * @return 리스너가 없는 새 기록
     * @throws IllegalArgumentException 프로필 ID가 규칙에 맞지 않을 때
     */
    public Progress load(String profileId) {
        Path file = paths.profileFile(profileId);
        List<String> lines;
        try {
            lines = TextFiles.readLines(file);
        } catch (IOException e) {
            // 파일이 있지만 읽지 못함(잘못된 UTF-8 바이트, 권한 등). 빈 기록으로 시작하되
            // 원본 위치를 기억해 두어, 첫 저장 때 원본을 옆으로 옮긴 뒤 쓴다(원본 보호).
            LOG.warning("%s: 기록 파일을 읽지 못해 빈 기록으로 시작합니다. 저장 시 원본은 '%s…'로 보존합니다: %s"
                    .formatted(file, UNREADABLE_SUFFIX, e));
            Progress empty = new Progress();
            empty.markUnreadableSource(file);
            return empty;
        }
        return new Parser(file.toString()).parse(lines);
    }

    /**
     * 기록을 프로필의 파일에 원자적으로 저장한다. 폴더가 없으면 만든다.
     *
     * <p>{@link #load}가 읽지 못한 파일에서 온 기록을 같은 파일에 처음 저장할 때는, 원본을
     * {@code <id>.txt.unreadable-<시각>}으로 먼저 옮겨 보존한다. 옮기지 못하면 저장하지 않고 예외를 던진다.
     *
     * @param profileId 프로필 ID
     * @param p         저장할 기록
     * @throws IOException              원본 보존이나 쓰기에 실패했을 때(기존 파일은 그대로)
     * @throws IllegalArgumentException 프로필 ID가 규칙에 맞지 않을 때
     */
    public void save(String profileId, Progress p) throws IOException {
        Objects.requireNonNull(p, "p");
        Path file = paths.profileFile(profileId);
        if (p.unreadableSource().filter(file::equals).isPresent()) {
            preserveUnreadable(file);
            p.clearUnreadableSource();
        }
        TextFiles.writeAtomic(file, format(p));
    }

    /** 읽지 못한 원본을 같은 폴더의 보존 파일로 옮긴다. 원본이 이미 없으면 아무것도 하지 않는다. */
    private static void preserveUnreadable(Path file) throws IOException {
        if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        String stamp = LocalDateTime.now().format(BACKUP_STAMP);
        Path backup = file.resolveSibling(file.getFileName() + UNREADABLE_SUFFIX + stamp);
        try {
            Files.move(file, backup);
        } catch (IOException e) {
            throw new IOException("%s: 읽지 못한 기록 원본을 보존하지 못해 저장하지 않습니다".formatted(file), e);
        }
        LOG.warning("%s: 읽지 못한 기록 원본을 %s로 옮겼습니다".formatted(file, backup.getFileName()));
    }

    /** 기록을 파일 내용으로 만든다. 빈 집합은 줄을 생략하고 횟수는 항상 쓴다. */
    static String format(Progress p) {
        StringBuilder sb = new StringBuilder();
        appendList(sb, LESSON_DONE, p.lessonsDone());
        p.puzzles().forEach((id, stats) ->
                appendLine(sb, PUZZLE_ATTEMPT_PREFIX + id, stats.attempted() + LIST_SEPARATOR + stats.solved()));
        appendList(sb, QUIZ_CORRECT_UNIQUE, p.termQuizCorrectIds());
        AttemptStats quiz = p.termQuizAttempts();
        appendLine(sb, QUIZ_ATTEMPTS, Integer.toString(quiz.attempted()));
        appendLine(sb, QUIZ_CORRECT_COUNT, Integer.toString(quiz.solved()));
        p.preserved().forEach((key, value) -> appendLine(sb, key, value));
        return sb.toString();
    }

    private static void appendList(StringBuilder sb, String key, Set<String> ids) {
        if (!ids.isEmpty()) {
            appendLine(sb, key, String.join(LIST_SEPARATOR, ids));
        }
    }

    private static void appendLine(StringBuilder sb, String key, String value) {
        sb.append(key).append(KEY_VALUE_SEPARATOR).append(value).append('\n');
    }

    /** 한 파일을 읽는 동안의 상태. */
    private static final class Parser {
        /** 잘못된 횟수 표시. */
        private static final int INVALID = -1;

        private final String source;
        /** 이미 유효한 값을 받은 키. */
        private final Set<String> seen = new HashSet<>();
        private final Set<String> lessons = new LinkedHashSet<>();
        private final Set<String> correctIds = new LinkedHashSet<>();
        private final Map<String, AttemptStats> puzzles = new LinkedHashMap<>();
        private final Map<String, String> others = new LinkedHashMap<>();
        private int attempts;
        private int correctCount;
        private int correctCountLine;

        Parser(String source) {
            this.source = source;
        }

        Progress parse(List<String> lines) {
            for (int i = 0; i < lines.size(); i++) {
                accept(lines.get(i).strip(), i + 1);
            }
            if (correctCount > attempts) {
                warn(correctCountLine, "정답 수 %d가 시도 수 %d보다 많아 0으로 둡니다".formatted(correctCount, attempts));
                correctCount = 0;
            }
            Progress p = new Progress();
            p.restore(lessons, puzzles, correctIds, new AttemptStats(attempts, correctCount), others);
            return p;
        }

        private void accept(String line, int lineNo) {
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                return;
            }
            int sep = line.indexOf(KEY_VALUE_SEPARATOR);
            if (sep <= 0) {
                warn(lineNo, "'키=값' 형식이 아닙니다: " + line);
                return;
            }
            String key = line.substring(0, sep).strip();
            String value = line.substring(sep + 1).strip();
            if (seen.contains(key)) {
                warn(lineNo, "중복 키 '%s'는 처음 값을 씁니다".formatted(key));
                return;
            }
            boolean valid = switch (key) {
                case LESSON_DONE -> readIds(value, lessons, lineNo);
                case QUIZ_CORRECT_UNIQUE -> readIds(value, correctIds, lineNo);
                case QUIZ_ATTEMPTS -> {
                    int n = readCount(value, lineNo);
                    if (n != INVALID) {
                        attempts = n;
                    }
                    yield n != INVALID;
                }
                case QUIZ_CORRECT_COUNT -> {
                    int n = readCount(value, lineNo);
                    if (n != INVALID) {
                        correctCount = n;
                        correctCountLine = lineNo;
                    }
                    yield n != INVALID;
                }
                default -> {
                    if (key.startsWith(PUZZLE_ATTEMPT_PREFIX)) {
                        yield readPuzzle(key.substring(PUZZLE_ATTEMPT_PREFIX.length()), value, lineNo);
                    }
                    others.put(key, value);
                    yield true;
                }
            };
            if (valid) {
                seen.add(key);
            }
        }

        /**
         * 쉼표 목록을 읽는다. 빈 항목은 무시하고 잘못된 ID는 경고 후 건너뛴다.
         * 명시적인 빈 목록({@code 키=})은 유효한 값이지만, 항목이 있는데 모두 잘못되었으면
         * 무효로 보고 같은 키의 다음 줄을 기다린다(중복 키는 처음 유효한 값).
         */
        private boolean readIds(String value, Set<String> into, int lineNo) {
            List<String> valid = new ArrayList<>();
            boolean hadItem = false;
            for (String item : value.split(LIST_SEPARATOR, -1)) {
                String id = item.strip();
                if (id.isEmpty()) {
                    continue;
                }
                hadItem = true;
                try {
                    valid.add(Progress.checkId(id));
                } catch (IllegalArgumentException e) {
                    warn(lineNo, e.getMessage());
                }
            }
            if (hadItem && valid.isEmpty()) {
                warn(lineNo, "유효한 ID가 하나도 없어 이 줄을 건너뜁니다");
                return false;
            }
            into.addAll(valid);
            return true;
        }

        /**
         * {@code puzzle.attempt.<id>=시도,정답}을 읽는다. ID가 잘못되었거나, 수가 두 개가 아니거나,
         * 0 이상의 정수가 아니거나, 정답이 시도보다 많으면 경고하고 무효로 본다(같은 키의 다음 줄을 쓸 수 있음).
         */
        private boolean readPuzzle(String id, String value, int lineNo) {
            try {
                Progress.checkId(id);
            } catch (IllegalArgumentException e) {
                warn(lineNo, "잘못된 퍼즐 ID: " + e.getMessage());
                return false;
            }
            String[] parts = value.split(LIST_SEPARATOR, -1);
            if (parts.length != 2) {
                warn(lineNo, "퍼즐 기록은 '시도,정답' 두 수여야 합니다: " + value);
                return false;
            }
            int attempted = readCount(parts[0].strip(), lineNo);
            int solved = readCount(parts[1].strip(), lineNo);
            if (attempted == INVALID || solved == INVALID) {
                return false;
            }
            if (solved > attempted) {
                warn(lineNo, "퍼즐 '%s'의 정답 수 %d가 시도 수 %d보다 많습니다".formatted(id, solved, attempted));
                return false;
            }
            puzzles.put(id, new AttemptStats(attempted, solved));
            return true;
        }

        /** 0 이상의 정수를 읽는다. 잘못되면 경고하고 {@link #INVALID}. */
        private int readCount(String value, int lineNo) {
            try {
                int n = Integer.parseInt(value);
                if (n >= 0) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // 아래에서 경고
            }
            warn(lineNo, "0 이상의 정수가 아닙니다: " + value);
            return INVALID;
        }

        private void warn(int lineNo, String message) {
            LOG.warning("%s:%d: %s".formatted(source, lineNo, message));
        }
    }
}
