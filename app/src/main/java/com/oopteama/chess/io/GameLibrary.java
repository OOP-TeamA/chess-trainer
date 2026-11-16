package com.oopteama.chess.io;

import com.oopteama.chess.content.FamousGame;
import com.oopteama.chess.content.Level;
import com.oopteama.chess.notation.Pgn;
import com.oopteama.chess.notation.PgnFormatException;
import com.oopteama.chess.notation.PgnGame;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * 번들 명국 기보와 프로필별 저장 대국(GAME-01, GAME-07). 인터페이스 명세 4.3의 계약 중 읽기 쪽만 구현한다.
 *
 * <ul>
 *   <li>{@link #bundled()}: {@code data/games/index.txt} 순서로 PGN을 읽는다. 번들 ID는 파일명에서 {@code .txt}를 뺀 값이다.
 *       {@code [Alias]}·{@code [Level]} 태그가 필요하다. 읽지 못한 기보는 {@code 파일명:행번호: 설명}을 경고 로그에
 *       남기고 건너뛴다. 콘텐츠는 바뀌지 않으므로 처음 한 번만 읽어 둔다.</li>
 *   <li>{@link #saved()}: {@code saves/<id>/}의 {@code *.txt} PGN을 최신순(수정 시각, 같으면 파일명 역순)으로 읽는다.
 *       폴더가 없으면 빈 목록이고, 깨진 파일은 경고 후 건너뛴다.</li>
 * </ul>
 * 저장({@code save}, AI-08)과 검색·삭제({@code searchSaved}·{@code deleteSaved}, GAME-09)는 해당 단계에서 추가한다.
 * {@code *.uci.txt} 대안({@code UciGameReader})도 아직 읽지 않는다.
 */
public final class GameLibrary {

    private static final Logger LOG = Logger.getLogger(GameLibrary.class.getName());

    static final String GAMES_DIR = "data/games";
    private static final String PGN_EXTENSION = ".txt";
    private static final String UCI_EXTENSION = ".uci.txt";
    private static final String ALIAS_TAG = "Alias";
    private static final String LEVEL_TAG = "Level";

    private final DataPaths paths;
    private final String profileId;
    /** 처음 {@link #bundled()}를 부를 때 채운다. */
    private List<FamousGame> bundled;

    /**
     * @param paths     경로 계산기
     * @param profileId 저장 대국을 읽을 프로필
     * @throws IllegalArgumentException 프로필 ID가 규칙에 맞지 않을 때
     */
    public GameLibrary(DataPaths paths, String profileId) {
        this.paths = Objects.requireNonNull(paths, "paths");
        paths.savesDir(profileId); // ID 검사의 단일 출처
        this.profileId = profileId;
    }

    /**
     * 번들 명국 기보.
     *
     * @return {@code data/games/index.txt} 순서의 불변 목록
     * @throws IOException 목록 파일이 없거나 읽지 못했을 때
     */
    public List<FamousGame> bundled() throws IOException {
        if (bundled == null) {
            bundled = loadBundled();
        }
        return bundled;
    }

    private List<FamousGame> loadBundled() throws IOException {
        List<FamousGame> out = new ArrayList<>();
        for (String name : paths.index(GAMES_DIR)) {
            if (name.endsWith(UCI_EXTENSION)) {
                LOG.warning("%s/%s: UCI 대안 기보는 아직 읽지 않아 건너뜁니다".formatted(GAMES_DIR, name));
                continue;
            }
            if (!name.endsWith(PGN_EXTENSION)) {
                LOG.warning("%s/%s: 기보 파일은 .txt여야 해서 건너뜁니다".formatted(GAMES_DIR, name));
                continue;
            }
            String id = name.substring(0, name.length() - PGN_EXTENSION.length());
            try (Reader in = paths.openResource(GAMES_DIR + "/" + name)) {
                famous(id, Pgn.read(in, name), name).ifPresent(out::add);
            } catch (FileNotFoundException e) {
                LOG.warning("%s/index.txt에 적힌 기보 파일이 없어 건너뜁니다: %s".formatted(GAMES_DIR, name));
            } catch (PgnFormatException e) {
                warn(e);
            }
        }
        return List.copyOf(out);
    }

    /** 팀 정의 태그를 확인해 번들 기보로 만든다. 빠지거나 잘못되면 경고 후 empty. */
    private static Optional<FamousGame> famous(String id, PgnGame pgn, String source) {
        String alias = pgn.tag(ALIAS_TAG).map(String::strip).orElse("");
        if (alias.isEmpty()) {
            LOG.warning("%s: [Alias] 태그가 없어 건너뜁니다".formatted(source));
            return Optional.empty();
        }
        Level level;
        try {
            level = Level.fromLabel(pgn.tag(LEVEL_TAG).orElse(""));
        } catch (IllegalArgumentException e) {
            LOG.warning("%s: [Level] 태그가 잘못되어 건너뜁니다: %s".formatted(source, e.getMessage()));
            return Optional.empty();
        }
        return Optional.of(new FamousGame(id, alias, level, pgn));
    }

    /**
     * 현재 프로필의 저장 대국.
     *
     * @return 최신순 불변 목록. 저장 폴더가 없으면 빈 목록
     * @throws IOException 폴더 목록을 읽지 못했을 때
     */
    public List<SavedGame> saved() throws IOException {
        Path dir = paths.savesDir(profileId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(PGN_EXTENSION))
                    .filter(Files::isRegularFile)
                    .toList();
        }
        List<SavedGame> out = new ArrayList<>();
        for (Path file : newestFirst(files)) {
            String source = file.getFileName().toString();
            try {
                String text = String.join("\n", TextFiles.readLines(file));
                out.add(new SavedGame(file, Pgn.read(new StringReader(text), source)));
            } catch (PgnFormatException e) {
                warn(e);
            } catch (IOException e) {
                LOG.warning("%s: 저장 대국을 읽지 못해 건너뜁니다: %s".formatted(file, e));
            }
        }
        return List.copyOf(out);
    }

    /** 수정 시각 내림차순, 같으면 파일명 내림차순. 시각을 못 읽은 파일은 가장 오래된 것으로 본다. */
    private static List<Path> newestFirst(List<Path> files) {
        Comparator<Path> byTime = Comparator.comparingLong(GameLibrary::modifiedMillis);
        Comparator<Path> byName = Comparator.comparing(p -> p.getFileName().toString());
        return files.stream().sorted(byTime.thenComparing(byName).reversed()).toList();
    }

    private static long modifiedMillis(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private static void warn(PgnFormatException e) {
        LOG.warning("%s:%d: %s (기보를 건너뜁니다)".formatted(e.source(), e.line(), e.getMessage()));
    }
}
