package com.oopteama.chess.progress;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.TextFiles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefsStoreTest {

    @TempDir
    Path tmp;

    private final Logger logger = Logger.getLogger(PrefsStore.class.getName());
    private final List<String> warnings = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            warnings.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void captureLog() {
        logger.addHandler(capture);
    }

    @AfterEach
    void releaseLog() {
        logger.removeHandler(capture);
    }

    private PrefsStore store() {
        return new PrefsStore(new DataPaths(tmp));
    }

    private Path prefsFile() {
        return new DataPaths(tmp).prefsFile();
    }

    @Test
    void 기본값은_명세_6장과_같다() {
        Prefs d = Prefs.defaults();
        assertEquals(new Prefs("walnut", true, true, true, true, true), d);
        assertEquals("""
                board.theme=walnut
                board.coordinates=true
                board.legalMoves=true
                board.lastMove=true
                ai.blunderAlert=true
                lesson.unlockAll=true
                """, PrefsStore.format(d));
    }

    @Test
    void 파일이_없으면_기본값이고_파일을_만들지_않는다() {
        assertEquals(Prefs.defaults(), store().load());
        assertFalse(Files.exists(prefsFile()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void 바꾼_설정이_재시작_후에도_유지된다() throws IOException {
        Prefs changed = new Prefs("green", false, true, false, false, true);
        store().save(changed);

        // 재시작: 새 저장소로 다시 읽는다.
        assertEquals(changed, new PrefsStore(new DataPaths(tmp)).load());
        assertEquals(List.of(
                "board.theme=green",
                "board.coordinates=false",
                "board.legalMoves=true",
                "board.lastMove=false",
                "ai.blunderAlert=false",
                "lesson.unlockAll=true"), TextFiles.readLines(prefsFile()));
    }

    @Test
    void 빠진_키는_기본값이고_주석_빈_줄_공백은_무시한다() throws IOException {
        TextFiles.writeAtomic(prefsFile(), """
                # 사용자가 손으로 고친 파일

                  board.theme = blue
                board.lastMove=FALSE
                """);
        assertEquals(new Prefs("blue", true, true, false, true, true), store().load());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void 잘못된_줄은_파일명과_행_번호를_남기고_그_항목만_기본값() throws IOException {
        TextFiles.writeAtomic(prefsFile(), """
                board.theme=
                board.coordinates=yes
                board.legalMoves=false
                이상한 줄
                board.unknown=true
                ai.blunderAlert=false
                """);
        Prefs p = store().load();
        assertEquals(new Prefs("walnut", true, false, true, false, true), p);
        assertEquals(4, warnings.size(), warnings::toString);
        assertTrue(warnings.get(0).contains("prefs.txt:1:"), warnings::toString);
        assertTrue(warnings.get(1).contains("prefs.txt:2:"), warnings::toString);
        assertTrue(warnings.get(2).contains("prefs.txt:4:"), warnings::toString);
        assertTrue(warnings.get(3).contains("prefs.txt:5:"), warnings::toString);
    }

    @Test
    void 중복_키는_처음_유효한_값을_쓴다() throws IOException {
        TextFiles.writeAtomic(prefsFile(), """
                board.coordinates=maybe
                board.coordinates=false
                board.coordinates=true
                board.theme=walnut
                board.theme=green
                """);
        Prefs p = store().load();
        assertFalse(p.coordinates());
        assertEquals("walnut", p.boardTheme());
        assertEquals(3, warnings.size(), warnings::toString);
    }

    @Test
    void 읽지_못한_파일은_기본값으로_시작하고_저장_때_원본을_보존한다() throws IOException {
        Files.createDirectories(prefsFile().getParent());
        byte[] original = "board.theme=green\n# ÿ".getBytes(StandardCharsets.ISO_8859_1); // 0xFF는 잘못된 UTF-8
        Files.write(prefsFile(), original);

        PrefsStore store = store();
        assertEquals(Prefs.defaults(), store.load());
        assertEquals(List.of(prefsFile()), listUserDir()); // 읽기만 하면 아무것도 바뀌지 않는다

        Prefs changed = new Prefs("blue", true, true, true, true, false);
        store.save(changed);
        List<Path> files = listUserDir();
        assertEquals(2, files.size());
        Path backup = files.stream().filter(f -> !f.equals(prefsFile())).findFirst().orElseThrow();
        assertTrue(backup.getFileName().toString().startsWith("prefs.txt" + PrefsStore.UNREADABLE_SUFFIX));
        assertArrayEquals(original, Files.readAllBytes(backup));
        assertEquals(changed, store().load());

        // 다시 저장해도 보존 파일이 더 생기지 않는다.
        store.save(Prefs.defaults());
        assertEquals(2, listUserDir().size());
    }

    private List<Path> listUserDir() throws IOException {
        try (Stream<Path> s = Files.list(tmp)) {
            return s.sorted().toList();
        }
    }

    @Test
    void 테마_이름을_검사한다() {
        assertThrows(NullPointerException.class, () -> new Prefs(null, true, true, true, true, true));
        for (String bad : List.of("", "  ", " walnut", "walnut ", "wal\nnut", "wal\tnut")) {
            assertThrows(IllegalArgumentException.class, () -> new Prefs(bad, true, true, true, true, true), bad);
        }
        assertEquals("다크 우드", new Prefs("다크 우드", true, true, true, true, true).boardTheme());
        assertThrows(NullPointerException.class, () -> store().save(null));
        assertThrows(NullPointerException.class, () -> new PrefsStore(null));
    }
}
