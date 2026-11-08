package com.oopteama.chess.io;

import java.nio.file.Files;
import java.nio.file.Path;

/** 테스트에서 저장소의 공용 샘플(docs/fixtures/first-demo/data)을 찾는다. */
final class TestFixtures {

    private static final Path FIRST_DEMO = Path.of("docs", "fixtures", "first-demo", "data");

    private TestFixtures() {
    }

    /** 작업 폴더(Gradle은 app 모듈 폴더)에서 위로 올라가며 공용 샘플 폴더를 찾는다. */
    static Path firstDemo() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(FIRST_DEMO);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("공용 샘플 폴더를 찾지 못했습니다: " + FIRST_DEMO);
    }
}
