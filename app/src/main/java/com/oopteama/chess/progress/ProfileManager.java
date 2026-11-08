package com.oopteama.chess.progress;

import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.io.Block;
import com.oopteama.chess.io.BlockReader;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.TextFiles;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 프로필 목록 {@code profiles/index.txt}와 선택 상태(기능 명세 6장 프로필 계약).
 *
 * <p>02단계 범위: 목록을 읽어 활성 프로필을 정하고({@link #active()}), 다른 프로필을 선택한다({@link #select}).
 * 등록·검색·삭제·전체 목록은 05단계에 추가한다.
 *
 * <p>읽기 규칙:
 * <ul>
 *   <li>{@code active=<id>}는 첫 블록 앞에, 각 프로필은 {@code @profile <id>} 블록의 {@code name}.</li>
 *   <li>ID가 규칙에 맞지 않거나({@code index} 포함) 이름이 비었거나 제어 문자를 담은 블록, 중복 ID 블록은
 *       로그에 남기고 건너뛴다.</li>
 *   <li>{@code active}가 없거나 목록에 없으면 파일 순서상 첫 유효 프로필을 쓴다.</li>
 *   <li>유효 프로필이 하나도 없으면 {@code default}/{@code 학습자}를 만들어 저장한다.</li>
 * </ul>
 * 쓰기는 {@link TextFiles#writeAtomic}으로 하며, 실패하면 메모리의 선택 상태를 바꾸지 않는다.
 */
public final class ProfileManager {

    private static final Logger LOG = Logger.getLogger(ProfileManager.class.getName());

    static final String DEFAULT_ID = "default";
    static final String DEFAULT_NAME = "학습자";
    private static final String ACTIVE_KEY = "active";
    private static final String PROFILE_TYPE = "profile";
    private static final String NAME_KEY = "name";

    private final DataPaths paths;
    /** 유효 프로필(파일 순서). 비지 않는다. */
    private final List<Profile> profiles = new ArrayList<>();
    private Profile active;

    /**
     * 프로필 목록을 읽는다. 유효 프로필이 없으면 기본 프로필을 만들어 저장한다.
     *
     * @param paths 경로 계산기(테스트는 임시 폴더)
     * @throws IOException 목록 파일이 있지만 읽지 못했거나, 기본 프로필 저장에 실패했을 때
     */
    public ProfileManager(DataPaths paths) throws IOException {
        this.paths = Objects.requireNonNull(paths, "paths");
        Path file = paths.profilesIndexFile();
        String source = file.toString();
        BlockReader.Result result = BlockReader.read(
                new StringReader(String.join("\n", TextFiles.readLines(file))), source);
        for (MalformedDataException problem : result.problems()) {
            LOG.warning(problem.getMessage());
        }
        String activeId = "";
        for (Block block : result.blocks()) {
            if (block.type().isEmpty()) {
                activeId = block.get(ACTIVE_KEY, "");
            } else if (block.type().equals(PROFILE_TYPE)) {
                addIfValid(block);
            } else {
                warn(block, "알 수 없는 블록 '@%s'를 건너뜁니다".formatted(block.type()));
            }
        }
        if (profiles.isEmpty()) {
            Profile fallback = new Profile(DEFAULT_ID, DEFAULT_NAME);
            write(List.of(fallback), fallback);
            profiles.add(fallback);
            active = fallback;
        } else {
            active = find(activeId).orElse(profiles.getFirst());
        }
    }

    /**
     * 활성 프로필. 선택 상태는 재시작 후에도 유지되며, 최초에는 {@code default}.
     *
     * @return 활성 프로필
     */
    public Profile active() {
        return active;
    }

    /**
     * 프로필을 선택하고 목록 파일의 {@code active}를 원자적으로 고친다.
     * 이미 활성인 프로필이면 아무것도 쓰지 않는다. 기록 저장·교체는 {@code Services.selectProfile}이 맡는다.
     *
     * @param id 선택할 프로필 ID
     * @throws IOException              목록 파일 쓰기에 실패했을 때(선택 상태는 그대로)
     * @throws IllegalArgumentException 목록에 없는 ID일 때
     */
    public void select(String id) throws IOException {
        Objects.requireNonNull(id, "id");
        Profile target = find(id).orElseThrow(() -> new IllegalArgumentException("없는 프로필 ID: " + id));
        if (target.equals(active)) {
            return;
        }
        write(profiles, target);
        active = target;
    }

    private void addIfValid(Block block) {
        String id = block.id();
        try {
            paths.profileFile(id); // ID 규칙의 단일 출처
        } catch (IllegalArgumentException e) {
            warn(block, "잘못된 프로필 ID '%s'를 건너뜁니다".formatted(id));
            return;
        }
        if (find(id).isPresent()) {
            warn(block, "중복 프로필 ID '%s'는 처음 블록을 씁니다".formatted(id));
            return;
        }
        String name = block.get(NAME_KEY, "").strip();
        if (name.isEmpty() || name.chars().anyMatch(Character::isISOControl)) {
            warn(block, "프로필 '%s'의 이름이 비었거나 제어 문자를 담고 있어 건너뜁니다".formatted(id));
            return;
        }
        profiles.add(new Profile(id, name));
    }

    private Optional<Profile> find(String id) {
        return profiles.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /** 목록 파일 전체를 다시 쓴다. 잘못된 블록은 남기지 않는다. */
    private void write(List<Profile> list, Profile activeProfile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(ACTIVE_KEY).append('=').append(activeProfile.id()).append('\n');
        for (Profile p : list) {
            sb.append('\n')
                    .append('@').append(PROFILE_TYPE).append(' ').append(p.id()).append('\n')
                    .append(NAME_KEY).append('=').append(p.name().replace("\\", "\\\\")).append('\n');
        }
        TextFiles.writeAtomic(paths.profilesIndexFile(), sb.toString());
    }

    private static void warn(Block block, String message) {
        LOG.warning("%s:%d: %s".formatted(block.source(), block.line(), message));
    }
}
