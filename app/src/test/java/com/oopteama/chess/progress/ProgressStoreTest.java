package com.oopteama.chess.progress;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.TextFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressStoreTest {

    @TempDir
    Path tmp;

    private ProgressStore store() {
        return new ProgressStore(new DataPaths(tmp));
    }

    private void writeProfile(String id, String content) throws IOException {
        TextFiles.writeAtomic(new DataPaths(tmp).profileFile(id), content);
    }

    @Test
    void 파일이_없으면_빈_기록() {
        Progress p = store().load("default");
        assertTrue(p.lessonsDone().isEmpty());
        assertEquals(new AttemptStats(0, 0), p.termQuizAttempts());
        assertEquals(0, p.score());
    }

    @Test
    void 첫_시연_기록이_재실행_후에도_유지된다() throws IOException {
        // 홈 → 나이트 레슨 완료 → 용어 퀴즈 10문제 중 7개 정답
        Progress first = store().load("default");
        first.markLessonDone("knight");
        for (int i = 1; i <= 10; i++) {
            first.recordTermQuiz("q%04d".formatted(i), i <= 7);
        }
        store().save("default", first);

        // 재실행: 새 저장소로 다시 읽는다.
        Progress again = new ProgressStore(new DataPaths(tmp)).load("default");
        assertEquals(Set.of("knight"), again.lessonsDone());
        assertEquals(new AttemptStats(10, 7), again.termQuizAttempts());
        assertEquals(List.of("q0001", "q0002", "q0003", "q0004", "q0005", "q0006", "q0007"),
                List.copyOf(again.termQuizCorrectIds()));
        assertEquals(17, again.score());
        assertEquals(List.of(
                "lesson.done=knight",
                "quiz.correct.unique=q0001,q0002,q0003,q0004,q0005,q0006,q0007",
                "quiz.attempts=10",
                "quiz.correct.count=7"), TextFiles.readLines(new DataPaths(tmp).profileFile("default")));
    }

    @Test
    void 명세_예시_파일을_읽고_해석하지_않는_키도_보존한다() throws IOException {
        String example = """
                lesson.done=knight
                puzzle.attempt.p0001=3,1
                puzzle.attempt.p0002=1,0
                quiz.correct.unique=q0001,q0002,q0003,q0004,q0005,q0006,q0007
                quiz.attempts=10
                quiz.correct.count=7
                wrong.notes=term:q0008,puzzle:p0002
                term.favorites=fork
                ai.record.intro=3,1,0
                replay.best.opera-1858=0.82
                """;
        writeProfile("default", example);
        Progress p = store().load("default");
        assertEquals(Set.of("knight"), p.lessonsDone());
        assertEquals(new AttemptStats(10, 7), p.termQuizAttempts());
        assertEquals(17, p.score()); // 퍼즐 5점은 04단계에서 반영(명세 예시 22점)

        p.markLessonDone("rook");
        store().save("default", p);
        List<String> saved = TextFiles.readLines(new DataPaths(tmp).profileFile("default"));
        for (String kept : List.of("puzzle.attempt.p0001=3,1", "puzzle.attempt.p0002=1,0",
                "wrong.notes=term:q0008,puzzle:p0002", "term.favorites=fork",
                "ai.record.intro=3,1,0", "replay.best.opera-1858=0.82")) {
            assertTrue(saved.contains(kept), kept);
        }
        assertTrue(saved.contains("lesson.done=knight,rook"));
    }

    @Test
    void 잘못된_줄은_건너뛰고_나머지와_중복_키의_첫_유효값을_복구한다() throws IOException {
        writeProfile("default", """
                이건 키값이 아님
                =값만
                quiz.attempts=abc
                quiz.attempts=-3
                quiz.attempts=5
                quiz.attempts=9
                lesson.done=knight, ,ro ok,bishop
                lesson.done=queen
                quiz.correct.unique=q0001
                quiz.correct.count=2
                """);
        Progress p = store().load("default");
        assertEquals(List.of("knight", "bishop"), List.copyOf(p.lessonsDone()));
        assertEquals(new AttemptStats(5, 2), p.termQuizAttempts());
        assertEquals(Set.of("q0001"), p.termQuizCorrectIds());
    }

    @Test
    void 정답_수가_시도보다_많으면_정답_수만_버린다() throws IOException {
        writeProfile("default", "quiz.attempts=3\nquiz.correct.count=4\nlesson.done=knight\n");
        Progress p = store().load("default");
        assertEquals(new AttemptStats(3, 0), p.termQuizAttempts());
        assertEquals(Set.of("knight"), p.lessonsDone());
    }

    @Test
    void 프로필별로_다른_파일에_저장한다() throws IOException {
        Progress a = store().load("default");
        a.markLessonDone("knight");
        store().save("default", a);
        Progress b = store().load("learner-2");
        b.recordTermQuiz("q0001", true);
        store().save("learner-2", b);

        assertEquals(Set.of("knight"), store().load("default").lessonsDone());
        assertTrue(store().load("default").termQuizCorrectIds().isEmpty());
        assertTrue(store().load("learner-2").lessonsDone().isEmpty());
        assertEquals(Set.of("q0001"), store().load("learner-2").termQuizCorrectIds());
    }

    @Test
    void 읽을_수_없는_기록_파일이면_빈_기록() throws IOException {
        Files.createDirectories(new DataPaths(tmp).profileFile("default")); // 파일 자리에 폴더
        assertEquals(0, store().load("default").score());
    }

    @Test
    void BOM과_CRLF가_있어도_읽는다() throws IOException {
        Path file = new DataPaths(tmp).profileFile("default");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "﻿lesson.done=knight\r\nquiz.attempts=1\r\n", StandardCharsets.UTF_8);
        Progress p = store().load("default");
        assertEquals(Set.of("knight"), p.lessonsDone());
        assertEquals(1, p.termQuizAttempts().attempted());
    }

    @Test
    void 잘못된_인자를_거부한다() {
        assertThrows(IllegalArgumentException.class, () -> store().load("../x"));
        assertThrows(IllegalArgumentException.class, () -> store().load("index"));
        assertThrows(NullPointerException.class, () -> store().save("default", null));
        assertThrows(NullPointerException.class, () -> new ProgressStore(null));
    }
}
