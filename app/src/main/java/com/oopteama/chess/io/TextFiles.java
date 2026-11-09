package com.oopteama.chess.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * 사용자 텍스트 파일의 UTF-8 읽기와 원자적 쓰기.
 *
 * <p>사용자 데이터 파일(프로필 기록·프로필 목록·설정·저장 대국)은 이 클래스로만 쓴다.
 * 쓰는 도중 프로그램이 멈춰도 기존 파일이 반쯤 쓰인 상태로 남지 않게, 같은 폴더의 임시 파일에
 * 다 쓴 뒤 한 번에 바꿔 끼운다.
 */
public final class TextFiles {

    /** 임시 파일 이름의 접미사. */
    private static final String TEMP_SUFFIX = ".tmp";
    /** UTF-8 BOM이 디코딩된 문자. */
    private static final char BOM = '﻿';

    private TextFiles() {
    }

    /**
     * 파일 전체를 UTF-8 줄 목록으로 읽는다. 첫 줄의 BOM은 제거한다.
     *
     * @param p 읽을 파일
     * @return 줄의 불변 목록. 파일이 없으면 빈 목록
     * @throws IOException 파일이 있지만 읽지 못했을 때(폴더인 경우 포함)
     */
    public static List<String> readLines(Path p) throws IOException {
        Objects.requireNonNull(p, "p");
        if (Files.notExists(p)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        if (!lines.isEmpty() && !lines.getFirst().isEmpty() && lines.getFirst().charAt(0) == BOM) {
            lines.set(0, lines.getFirst().substring(1));
        }
        return List.copyOf(lines);
    }

    /**
     * 내용을 UTF-8로 원자적으로 쓴다. 상위 폴더가 없으면 만든다.
     * 같은 폴더의 임시 파일에 쓴 뒤 {@code ATOMIC_MOVE}로 바꿔 끼우고,
     * 파일 시스템이 지원하지 않으면 {@code REPLACE_EXISTING} 이동으로 대체한다.
     * 실패하면 임시 파일을 지우고 기존 파일은 그대로 둔다.
     *
     * @param p       쓸 파일
     * @param content 파일 전체 내용
     * @throws IOException 폴더 생성·쓰기·교체에 실패했을 때
     */
    public static void writeAtomic(Path p, String content) throws IOException {
        Objects.requireNonNull(p, "p");
        Objects.requireNonNull(content, "content");
        Path target = p.toAbsolutePath();
        Path dir = target.getParent();
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, target.getFileName().toString() + ".", TEMP_SUFFIX);
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
    }
}
