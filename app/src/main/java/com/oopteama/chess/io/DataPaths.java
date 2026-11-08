package com.oopteama.chess.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 경로와 인코딩의 단일 출처. 콘텐츠(클래스패스 {@code data/…})와 사용자 데이터 폴더의 경로를 계산한다.
 *
 * <p>사용자 폴더 구조:
 * <pre>
 * &lt;userDir&gt;/profiles/index.txt     프로필 목록과 선택 상태
 * &lt;userDir&gt;/profiles/&lt;id&gt;.txt      프로필별 기록
 * &lt;userDir&gt;/saves/&lt;id&gt;/            프로필별 저장 대국
 * &lt;userDir&gt;/prefs.txt
 * </pre>
 * 이 클래스는 경로만 계산하며 폴더를 만들지 않는다. 테스트는 생성자에 임시 폴더를 넣는다.
 */
public final class DataPaths {

    private static final Logger LOG = Logger.getLogger(DataPaths.class.getName());

    /** 표준 사용자 폴더 이름(홈 폴더 아래). */
    private static final String STANDARD_DIR_NAME = ".chesslearn";
    private static final String PROFILES_DIR = "profiles";
    private static final String SAVES_DIR = "saves";
    private static final String INDEX_FILE = "index.txt";
    private static final String PREFS_FILE = "prefs.txt";
    private static final String TEXT_EXTENSION = ".txt";
    /** 프로필 ID 규칙(기능 명세 6장). */
    private static final Pattern PROFILE_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    /** profiles/index.txt와 겹치므로 프로필 ID로 쓸 수 없는 이름. */
    private static final String RESERVED_PROFILE_ID = "index";

    private final Path userDir;

    /**
     * 지정한 사용자 폴더를 쓰는 경로 계산기를 만든다.
     *
     * @param userDir 사용자 데이터 폴더(없어도 됨)
     */
    public DataPaths(Path userDir) {
        this.userDir = Objects.requireNonNull(userDir, "userDir").toAbsolutePath().normalize();
    }

    /**
     * 표준 사용자 폴더 {@code ~/.chesslearn}을 쓰는 경로 계산기.
     *
     * @return 새 DataPaths
     */
    public static DataPaths standard() {
        return new DataPaths(Path.of(System.getProperty("user.home"), STANDARD_DIR_NAME));
    }

    /**
     * 클래스패스의 콘텐츠 파일을 UTF-8로 연다. 호출한 쪽이 닫는다.
     *
     * @param path 클래스패스 기준 경로(예: {@code "data/glossary.txt"})
     * @return UTF-8 Reader
     * @throws FileNotFoundException    리소스가 없을 때
     * @throws IOException              열지 못했을 때
     * @throws IllegalArgumentException 경로가 비었거나 '/'로 끝날 때
     */
    public Reader openResource(String path) throws IOException {
        String name = resourceName(path);
        if (name.isEmpty() || name.endsWith("/")) {
            throw new IllegalArgumentException("파일 경로가 아닙니다: " + path);
        }
        InputStream in = DataPaths.class.getClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new FileNotFoundException("콘텐츠 리소스가 없습니다: " + name);
        }
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * 콘텐츠 폴더의 {@code index.txt}에 적힌 파일명 목록을 순서대로 돌려준다.
     * 빈 줄과 {@code #} 주석은 무시하고, 폴더 구분자나 {@code ..}이 든 줄은 로그에 남기고 건너뛴다.
     *
     * @param folder 클래스패스 기준 폴더(예: {@code "data/lessons"})
     * @return 파일명의 불변 목록
     * @throws FileNotFoundException {@code index.txt}가 없을 때
     * @throws IOException           읽지 못했을 때
     */
    public List<String> index(String folder) throws IOException {
        String dir = resourceName(folder);
        while (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        String indexPath = dir.isEmpty() ? INDEX_FILE : dir + "/" + INDEX_FILE;
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(openResource(indexPath))) {
            String raw;
            int lineNo = 0;
            while ((raw = reader.readLine()) != null) {
                lineNo++;
                String name = (lineNo == 1 ? raw.replace("﻿", "") : raw).strip();
                if (name.isEmpty() || name.startsWith("#")) {
                    continue;
                }
                if (name.contains("/") || name.contains("\\") || name.equals("..") || name.equals(".")) {
                    LOG.warning("%s:%d: 파일명만 적어야 합니다: %s".formatted(indexPath, lineNo, name));
                    continue;
                }
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    /**
     * 프로필 기록 파일 {@code profiles/<id>.txt}.
     *
     * @param profile 프로필 ID
     * @return 경로
     * @throws IllegalArgumentException ID가 규칙({@code [a-z0-9][a-z0-9-]{0,63}}, {@code index} 제외)에 맞지 않을 때
     */
    public Path profileFile(String profile) {
        return userDir.resolve(PROFILES_DIR).resolve(checkProfileId(profile) + TEXT_EXTENSION);
    }

    /**
     * 프로필 목록 파일 {@code profiles/index.txt}(프로필 ID·표시 이름·선택 상태).
     *
     * @return 경로
     */
    public Path profilesIndexFile() {
        return userDir.resolve(PROFILES_DIR).resolve(INDEX_FILE);
    }

    /**
     * 프로필별 저장 대국 폴더 {@code saves/<id>/}.
     *
     * @param profileId 프로필 ID
     * @return 경로
     * @throws IllegalArgumentException ID가 규칙에 맞지 않을 때
     */
    public Path savesDir(String profileId) {
        return userDir.resolve(SAVES_DIR).resolve(checkProfileId(profileId));
    }

    /**
     * 설정 파일 {@code prefs.txt}.
     *
     * @return 경로
     */
    public Path prefsFile() {
        return userDir.resolve(PREFS_FILE);
    }

    /** 경로 조작을 막기 위해 프로필 ID를 검사한다. */
    private static String checkProfileId(String id) {
        Objects.requireNonNull(id, "profileId");
        if (!PROFILE_ID.matcher(id).matches() || id.equals(RESERVED_PROFILE_ID)) {
            throw new IllegalArgumentException("잘못된 프로필 ID: " + id);
        }
        return id;
    }

    /** 클래스패스 리소스 이름으로 바꾼다. 앞의 '/'는 떼고 역슬래시는 '/'로 바꾼다. */
    private static String resourceName(String path) {
        Objects.requireNonNull(path, "path");
        String name = path.strip().replace('\\', '/');
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name;
    }
}
