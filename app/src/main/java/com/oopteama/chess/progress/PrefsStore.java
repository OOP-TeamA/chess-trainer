package com.oopteama.chess.progress;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.TextFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 설정 파일 {@code prefs.txt}의 읽기·쓰기(SYS-04, 기능 명세 6장).
 *
 * <pre>
 * board.theme=walnut
 * board.coordinates=true
 * board.legalMoves=true
 * board.lastMove=true
 * ai.blunderAlert=true
 * lesson.unlockAll=true
 * </pre>
 *
 * <p>읽을 때:
 * <ul>
 *   <li>파일이 없거나 키가 빠지면 그 항목은 {@link Prefs#defaults()}의 값이다. 파일은 첫 저장 때 생긴다.</li>
 *   <li>잘못된 줄(형식·값 오류, 알 수 없는 키)은 파일명·행 번호를 로그에 남기고 건너뛴다.
 *       참·거짓 값은 {@code true}/{@code false}만 받는다(대소문자 무시).</li>
 *   <li>같은 키가 다시 나오면 처음 유효한 값을 쓴다.</li>
 *   <li>파일이 있지만 읽지 못하면(잘못된 UTF-8 등) 기본값으로 시작하고, 그 뒤 이 저장소로 처음 저장할 때
 *       원본을 {@code prefs.txt.unreadable-<시각>}으로 옮겨 덮어쓰지 않는다({@link ProgressStore}와 같은 규칙).</li>
 * </ul>
 * 쓰기는 {@link TextFiles#writeAtomic}으로 파일 전체를 원자적으로 바꾸며, 여섯 키를 항상 위 순서로 쓴다.
 */
public final class PrefsStore {

    private static final Logger LOG = Logger.getLogger(PrefsStore.class.getName());

    static final String BOARD_THEME = "board.theme";
    static final String COORDINATES = "board.coordinates";
    static final String LEGAL_MOVES = "board.legalMoves";
    static final String LAST_MOVE = "board.lastMove";
    static final String BLUNDER_ALERT = "ai.blunderAlert";
    static final String UNLOCK_ALL = "lesson.unlockAll";

    private static final char KEY_VALUE_SEPARATOR = '=';
    private static final String COMMENT_PREFIX = "#";
    /** 읽지 못한 원본을 보존할 때 파일명 뒤에 붙이는 접미사(뒤에 시각이 붙는다). */
    static final String UNREADABLE_SUFFIX = ".unreadable-";
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final DataPaths paths;
    /** 마지막 {@link #load()}가 파일을 읽지 못했는지. 다음 저장 때 원본을 먼저 보존한다. */
    private boolean unreadableSource;

    /**
     * 사용자 폴더의 설정 파일을 다루는 저장소를 만든다.
     *
     * @param paths 경로 계산기(테스트는 임시 폴더)
     */
    public PrefsStore(DataPaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    /**
     * 설정을 읽는다. 파일이 없거나 읽지 못하면 기본값을, 깨진 줄은 건너뛰고 나머지를 돌려준다.
     *
     * @return 설정(빠지거나 잘못된 항목은 기본값)
     */
    public Prefs load() {
        Path file = paths.prefsFile();
        List<String> lines;
        try {
            lines = TextFiles.readLines(file);
        } catch (IOException e) {
            LOG.warning("%s: 설정 파일을 읽지 못해 기본값으로 시작합니다. 저장 시 원본은 '%s…'로 보존합니다: %s"
                    .formatted(file, UNREADABLE_SUFFIX, e));
            unreadableSource = true;
            return Prefs.defaults();
        }
        unreadableSource = false;
        return new Parser(file.toString()).parse(lines);
    }

    /**
     * 설정을 원자적으로 저장한다. 폴더가 없으면 만든다.
     *
     * @param prefs 저장할 설정
     * @throws IOException 원본 보존이나 쓰기에 실패했을 때(기존 파일은 그대로)
     */
    public void save(Prefs prefs) throws IOException {
        Objects.requireNonNull(prefs, "prefs");
        Path file = paths.prefsFile();
        if (unreadableSource) {
            preserveUnreadable(file);
            unreadableSource = false;
        }
        TextFiles.writeAtomic(file, format(prefs));
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
            throw new IOException("%s: 읽지 못한 설정 원본을 보존하지 못해 저장하지 않습니다".formatted(file), e);
        }
        LOG.warning("%s: 읽지 못한 설정 원본을 %s로 옮겼습니다".formatted(file, backup.getFileName()));
    }

    /** 설정을 파일 내용으로 만든다. */
    static String format(Prefs p) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, BOARD_THEME, p.boardTheme());
        appendLine(sb, COORDINATES, Boolean.toString(p.coordinates()));
        appendLine(sb, LEGAL_MOVES, Boolean.toString(p.legalMoves()));
        appendLine(sb, LAST_MOVE, Boolean.toString(p.lastMove()));
        appendLine(sb, BLUNDER_ALERT, Boolean.toString(p.blunderAlert()));
        appendLine(sb, UNLOCK_ALL, Boolean.toString(p.unlockAllLessons()));
        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, String key, String value) {
        sb.append(key).append(KEY_VALUE_SEPARATOR).append(value).append('\n');
    }

    /** 한 파일을 읽는 동안의 상태. 기본값에서 시작해 유효한 줄만 덮어쓴다. */
    private static final class Parser {
        private final String source;
        /** 이미 유효한 값을 받은 키. */
        private final Set<String> seen = new HashSet<>();
        private final Prefs d = Prefs.defaults();
        private String theme = d.boardTheme();
        private boolean coordinates = d.coordinates();
        private boolean legalMoves = d.legalMoves();
        private boolean lastMove = d.lastMove();
        private boolean blunderAlert = d.blunderAlert();
        private boolean unlockAll = d.unlockAllLessons();

        Parser(String source) {
            this.source = source;
        }

        Prefs parse(List<String> lines) {
            for (int i = 0; i < lines.size(); i++) {
                accept(lines.get(i).strip(), i + 1);
            }
            return new Prefs(theme, coordinates, legalMoves, lastMove, blunderAlert, unlockAll);
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
            try {
                switch (key) {
                    case BOARD_THEME -> theme = Prefs.checkBoardTheme(value);
                    case COORDINATES -> coordinates = bool(value);
                    case LEGAL_MOVES -> legalMoves = bool(value);
                    case LAST_MOVE -> lastMove = bool(value);
                    case BLUNDER_ALERT -> blunderAlert = bool(value);
                    case UNLOCK_ALL -> unlockAll = bool(value);
                    default -> {
                        warn(lineNo, "알 수 없는 설정 키 '%s'를 건너뜁니다".formatted(key));
                        return;
                    }
                }
                seen.add(key);
            } catch (IllegalArgumentException e) {
                warn(lineNo, "'%s' 값이 잘못되어 건너뜁니다: %s".formatted(key, e.getMessage()));
            }
        }

        /** {@code true}/{@code false}만 받는다(대소문자 무시). {@link Boolean#parseBoolean}은 오타를 거짓으로 삼켜 쓰지 않는다. */
        private static boolean bool(String value) {
            if (value.equalsIgnoreCase("true")) {
                return true;
            }
            if (value.equalsIgnoreCase("false")) {
                return false;
            }
            throw new IllegalArgumentException("true 또는 false가 아닙니다: " + value);
        }

        private void warn(int lineNo, String message) {
            LOG.warning("%s:%d: %s".formatted(source, lineNo, message));
        }
    }
}
