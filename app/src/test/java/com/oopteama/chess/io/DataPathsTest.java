package com.oopteama.chess.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataPathsTest {

    @TempDir
    Path tmp;

    @Test
    void 사용자_파일_경로를_계산한다() {
        DataPaths paths = new DataPaths(tmp);
        assertEquals(tmp.resolve("profiles").resolve("index.txt"), paths.profilesIndexFile());
        assertEquals(tmp.resolve("profiles").resolve("default.txt"), paths.profileFile("default"));
        assertEquals(tmp.resolve("saves").resolve("default"), paths.savesDir("default"));
        assertEquals(tmp.resolve("prefs.txt"), paths.prefsFile());
    }

    @Test
    void 표준_폴더는_홈의_chesslearn() {
        Path expected = Path.of(System.getProperty("user.home"), ".chesslearn").toAbsolutePath().normalize();
        assertEquals(expected.resolve("prefs.txt"), DataPaths.standard().prefsFile());
    }

    @Test
    void 규칙에_맞지_않는_프로필_ID는_거부한다() {
        DataPaths paths = new DataPaths(tmp);
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        assertEquals(tmp.resolve("profiles").resolve(uuid + ".txt"), paths.profileFile(uuid));
        for (String bad : List.of("", "Default", "-a", "../x", "a/b", "a.b", "index", "a".repeat(65))) {
            assertThrows(IllegalArgumentException.class, () -> paths.profileFile(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> paths.savesDir(bad), bad);
        }
        assertThrows(NullPointerException.class, () -> paths.profileFile(null));
    }

    @Test
    void 리소스를_UTF8로_연다() throws Exception {
        DataPaths paths = new DataPaths(tmp);
        try (Reader in = paths.openResource("iotest/first.txt")) {
            BlockReader.Result r = BlockReader.read(in, "iotest/first.txt");
            assertEquals("포크", r.blocks().get(0).require("ko"));
        }
        try (Reader in = paths.openResource("/iotest/first.txt")) {
            assertEquals('@', in.read());
        }
    }

    @Test
    void 없는_리소스와_잘못된_경로() {
        DataPaths paths = new DataPaths(tmp);
        assertThrows(FileNotFoundException.class, () -> paths.openResource("iotest/none.txt"));
        assertThrows(IllegalArgumentException.class, () -> paths.openResource(""));
        assertThrows(IllegalArgumentException.class, () -> paths.openResource("iotest/"));
        assertThrows(FileNotFoundException.class, () -> paths.index("no-such-folder"));
    }

    @Test
    void index는_파일명만_순서대로_돌려준다() throws IOException {
        DataPaths paths = new DataPaths(tmp);
        assertEquals(List.of("first.txt", "second.txt"), paths.index("iotest"));
        assertEquals(List.of("first.txt", "second.txt"), paths.index("iotest/"));
    }
}
