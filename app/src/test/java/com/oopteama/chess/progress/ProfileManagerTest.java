package com.oopteama.chess.progress;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.TextFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileManagerTest {

    @TempDir
    Path tmp;

    private DataPaths paths() {
        return new DataPaths(tmp);
    }

    private void writeIndex(String content) throws IOException {
        TextFiles.writeAtomic(paths().profilesIndexFile(), content);
    }

    @Test
    void 목록이_없으면_기본_프로필을_만들어_저장한다() throws IOException {
        ProfileManager m = new ProfileManager(paths());
        assertEquals(new Profile("default", "학습자"), m.active());
        assertEquals(List.of("active=default", "", "@profile default", "name=학습자"),
                TextFiles.readLines(paths().profilesIndexFile()));
    }

    @Test
    void 명세_예시의_활성_프로필을_읽는다() throws IOException {
        writeIndex("""
                active=learner-2

                @profile default
                name=학습자

                @profile learner-2
                name=  연습생 \s
                """);
        assertEquals(new Profile("learner-2", "연습생"), new ProfileManager(paths()).active());
    }

    @Test
    void 선택은_재시작_후에도_유지된다() throws IOException {
        writeIndex("active=default\n\n@profile default\nname=학습자\n\n@profile learner-2\nname=연습생\n");
        ProfileManager m = new ProfileManager(paths());
        m.select("learner-2");
        assertEquals("learner-2", m.active().id());
        assertEquals("learner-2", new ProfileManager(paths()).active().id());
        m.select("default");
        assertEquals(new Profile("default", "학습자"), new ProfileManager(paths()).active());
    }

    @Test
    void 잘못된_블록은_건너뛰고_활성이_없으면_첫_유효_프로필() throws IOException {
        writeIndex("""
                active=ghost

                @profile index
                name=예약어

                @profile Bad_ID
                name=대문자

                @profile no-name

                @profile ctrl
                name=줄\\n바꿈

                @profile dup-key
                name=a
                name=b

                @theme x
                name=다른 종류

                @profile learner-2
                name=연습생

                @profile learner-2
                name=중복
                """);
        ProfileManager m = new ProfileManager(paths());
        assertEquals(new Profile("learner-2", "연습생"), m.active());
        assertThrows(IllegalArgumentException.class, () -> m.select("index"));
        assertThrows(IllegalArgumentException.class, () -> m.select("no-name"));
    }

    @Test
    void 유효_프로필이_없으면_기본_프로필로_다시_쓴다() throws IOException {
        writeIndex("active=index\n\n@profile index\nname=예약어\n");
        assertEquals(new Profile("default", "학습자"), new ProfileManager(paths()).active());
        assertEquals("default", new ProfileManager(paths()).active().id());
    }

    @Test
    void 역슬래시가_든_이름을_왕복한다() throws IOException {
        writeIndex("active=default\n\n@profile default\nname=a\\\\b\n\n@profile second\nname=둘\n");
        ProfileManager m = new ProfileManager(paths());
        assertEquals("a\\b", m.active().name());
        m.select("second");
        m.select("default");
        assertEquals("a\\b", new ProfileManager(paths()).active().name());
    }

    @Test
    void 없는_ID_선택은_거부하고_상태를_바꾸지_않는다() throws IOException {
        ProfileManager m = new ProfileManager(paths());
        assertThrows(IllegalArgumentException.class, () -> m.select("nobody"));
        assertThrows(NullPointerException.class, () -> m.select(null));
        assertEquals("default", m.active().id());
    }

    @Test
    void 쓰기에_실패하면_선택을_유지한다() throws IOException {
        writeIndex("active=default\n\n@profile default\nname=학습자\n\n@profile second\nname=둘\n");
        ProfileManager m = new ProfileManager(paths());
        // 목록 파일을 지우고 그 자리에 비어 있지 않은 폴더를 두어 교체가 실패하게 한다.
        Path index = paths().profilesIndexFile();
        Files.delete(index);
        Files.createDirectories(index.resolve("block"));
        assertThrows(IOException.class, () -> m.select("second"));
        assertEquals("default", m.active().id());
    }
}
