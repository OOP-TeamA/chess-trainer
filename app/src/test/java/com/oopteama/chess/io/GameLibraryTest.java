package com.oopteama.chess.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import com.oopteama.chess.content.FamousGame;
import com.oopteama.chess.content.Level;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 번들 명국 기보와 저장 대국 읽기(GAME-01, GAME-07). */
class GameLibraryTest {

    private static final String SHORT_GAME = """
            [Event "AI 대전"]
            [White "학습자"]
            [Black "AI (입문)"]
            [Result "1-0"]

            1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6 4. Qxf7# 1-0
            """;

    @TempDir
    Path userDir;

    @Test
    void bundledGamesFollowIndexOrderWithoutWarnings() throws IOException {
        DataPaths paths = new DataPaths(userDir);
        GameLibrary library = new GameLibrary(paths, "default");
        List<FamousGame> games;
        try (TestLog log = TestLog.capture(GameLibrary.class)) {
            games = library.bundled();
            assertEquals(List.of(), log.messages());
        }
        List<String> expectedIds = paths.index("data/games").stream()
                .map(name -> name.substring(0, name.length() - ".txt".length()))
                .toList();
        assertEquals(expectedIds, games.stream().map(FamousGame::id).toList());

        FamousGame opera = games.getFirst();
        assertEquals("opera-1858", opera.id());
        assertEquals("오페라 게임", opera.alias());
        assertEquals(Level.INTRO, opera.level());
        assertEquals(33, opera.plyCount());
        assertSame(games, library.bundled(), "번들은 한 번만 읽는다");
    }

    @Test
    void savedIsEmptyWithoutFolder() throws IOException {
        assertEquals(List.of(), new GameLibrary(new DataPaths(userDir), "default").saved());
    }

    @Test
    void savedReadsNewestFirstAndSkipsBrokenFiles() throws IOException {
        DataPaths paths = new DataPaths(userDir);
        Path dir = Files.createDirectories(paths.savesDir("p1"));
        Path older = write(dir.resolve("a-older.txt"), SHORT_GAME, 1_000);
        Path newer = write(dir.resolve("b-newer.txt"), SHORT_GAME, 2_000);
        write(dir.resolve("c-broken.txt"), "1. e4 e5 2. Ke3?? *", 3_000);
        write(dir.resolve("notes.md"), "PGN 아님", 4_000);

        List<SavedGame> saved;
        try (TestLog log = TestLog.capture(GameLibrary.class)) {
            saved = new GameLibrary(paths, "p1").saved();
            assertEquals(1, log.messages().size(), String.join("\n", log.messages()));
            assertTrue(log.messages().getFirst().startsWith("c-broken.txt:"), log.messages().getFirst());
        }
        assertEquals(List.of(newer, older), saved.stream().map(SavedGame::file).toList());
        assertEquals("1-0", saved.getFirst().pgn().result());
        assertEquals(List.of(), new GameLibrary(paths, "p2").saved(), "다른 프로필의 저장 대국은 보지 않는다");
    }

    @Test
    void rejectsInvalidProfileId() {
        assertThrows(IllegalArgumentException.class, () -> new GameLibrary(new DataPaths(userDir), "../x"));
    }

    private static Path write(Path file, String content, long millis) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(file, FileTime.fromMillis(millis));
        return file;
    }
}
