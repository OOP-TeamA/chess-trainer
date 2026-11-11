package com.oopteama.chess.progress;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Side;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        assertEquals(new AttemptStats(3, 1), p.puzzleAttempts("p0001"));
        assertEquals(PuzzleState.SOLVED, p.puzzleState("p0001"));
        assertEquals(PuzzleState.FAILED, p.puzzleState("p0002"));
        assertEquals(PuzzleState.UNSEEN, p.puzzleState("p0003"));
        assertEquals(22, p.score()); // 명세 예시: 레슨 10 + 퍼즐 5 + 문항 7

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
    void 퍼즐_기록_기준_흐름이_재시작_후에도_같다() throws IOException {
        // 기능 명세 9장: HINT만 보고 완주 → REVEAL 후 완주 → 공개 없이 재완주
        Progress p = store().load("default");
        p.recordPuzzle("p0001", true);
        p.recordPuzzle("p0001", false);
        p.recordPuzzle("p0001", true);
        store().save("default", p);

        Progress again = store().load("default");
        assertEquals(new AttemptStats(3, 2), again.puzzleAttempts("p0001"));
        assertEquals(PuzzleState.SOLVED, again.puzzleState("p0001"));
        assertEquals(5, again.score());
        assertEquals(List.of("puzzle.attempt.p0001=3,2", "quiz.attempts=0", "quiz.correct.count=0"),
                TextFiles.readLines(new DataPaths(tmp).profileFile("default")));
    }

    @Test
    void 잘못된_퍼즐_줄은_건너뛰고_같은_키의_첫_유효값을_쓴다() throws IOException {
        writeProfile("default", """
                puzzle.attempt.p0001=1,2
                puzzle.attempt.p0001=2,1
                puzzle.attempt.p0001=5,5
                puzzle.attempt.p0002=-1,0
                puzzle.attempt.p0003=1
                puzzle.attempt.p0004=1,0,0
                puzzle.attempt.p0005=a,b
                puzzle.attempt.=1,1
                puzzle.attempt.p 6=1,1
                puzzle.attempt.p0007=0,0
                puzzle.attempt.p0008= 4 , 0
                """);
        Progress p = store().load("default");
        assertEquals(new AttemptStats(2, 1), p.puzzleAttempts("p0001"));
        for (String bad : List.of("p0002", "p0003", "p0004", "p0005")) {
            assertEquals(PuzzleState.UNSEEN, p.puzzleState(bad), bad);
        }
        assertEquals(PuzzleState.UNSEEN, p.puzzleState("p0007"));
        assertEquals(new AttemptStats(4, 0), p.puzzleAttempts("p0008"));
        assertEquals(PuzzleState.FAILED, p.puzzleState("p0008"));
        assertEquals(5, p.score());

        store().save("default", p);
        List<String> saved = TextFiles.readLines(new DataPaths(tmp).profileFile("default"));
        assertEquals(List.of("puzzle.attempt.p0001=2,1", "puzzle.attempt.p0007=0,0", "puzzle.attempt.p0008=4,0"),
                saved.stream().filter(l -> l.startsWith("puzzle.")).toList());
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
    void ID가_전부_잘못된_목록은_무효라_뒤의_정상_값을_쓴다() throws IOException {
        writeProfile("default", """
                lesson.done=ro ok
                lesson.done=knight
                quiz.correct.unique=q 1, ,q\t2
                quiz.correct.unique=q0001
                """);
        Progress p = store().load("default");
        assertEquals(Set.of("knight"), p.lessonsDone());
        assertEquals(Set.of("q0001"), p.termQuizCorrectIds());
    }

    @Test
    void 명시적인_빈_목록은_유효한_값이라_뒤의_줄을_쓰지_않는다() throws IOException {
        writeProfile("default", "lesson.done=\nlesson.done=knight\nquiz.correct.unique= , \nquiz.correct.unique=q0001\n");
        Progress p = store().load("default");
        assertTrue(p.lessonsDone().isEmpty());
        assertTrue(p.termQuizCorrectIds().isEmpty());
    }

    @Test
    void 읽지_못한_원본은_저장_전에_옆으로_옮겨_보존한다() throws IOException {
        Path file = new DataPaths(tmp).profileFile("default");
        Files.createDirectories(file.getParent());
        byte[] original = "lesson.done=knight\nquiz.attempts=10\nquiz.correct.count=7\n# ÿ"
                .getBytes(StandardCharsets.ISO_8859_1); // 0xFF는 잘못된 UTF-8 바이트
        Files.write(file, original);

        // 읽기만 하면 아무것도 바뀌지 않는다(순위 계산 등에서 여러 번 읽어도 안전).
        assertEquals(0, store().load("default").score());
        assertEquals(0, store().load("default").score());
        assertEquals(List.of(file), listProfiles());

        // 빈 기록에서 새 레슨을 완료하고 저장하면 원본은 보존 파일로 옮겨진다.
        Progress p = store().load("default");
        p.markLessonDone("rook");
        store().save("default", p);
        List<Path> files = listProfiles();
        assertEquals(2, files.size());
        Path backup = files.stream().filter(f -> !f.equals(file)).findFirst().orElseThrow();
        assertTrue(backup.getFileName().toString().startsWith("default.txt" + ProgressStore.UNREADABLE_SUFFIX));
        assertArrayEquals(original, Files.readAllBytes(backup));
        assertEquals(Set.of("rook"), store().load("default").lessonsDone());

        // 같은 기록을 다시 저장해도 보존 파일이 더 생기지 않는다.
        p.markLessonDone("bishop");
        store().save("default", p);
        assertEquals(2, listProfiles().size());
    }

    @Test
    void 읽지_못한_기록을_다른_프로필로_저장하면_원본을_옮기지_않는다() throws IOException {
        Path file = new DataPaths(tmp).profileFile("default");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {(byte) 0xFF});
        Progress p = store().load("default");
        store().save("learner-2", p);
        assertTrue(Files.exists(file));
        assertEquals(2, listProfiles().size());
    }

    private List<Path> listProfiles() throws IOException {
        try (Stream<Path> s = Files.list(new DataPaths(tmp).profileFile("default").getParent())) {
            return s.sorted().toList();
        }
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

    @Test
    void AI_전적이_재시작_후에도_유지되고_난이도_key로_저장된다() throws IOException {
        Progress p = store().load("default");
        GameResult blackMates = new GameResult(GameResult.Outcome.BLACK_WINS, GameResult.Reason.CHECKMATE);
        p.recordAiGame(Difficulty.BEGINNER, blackMates, Side.BLACK);
        p.recordAiGame(Difficulty.BEGINNER, blackMates, Side.WHITE);
        p.recordAiGame(Difficulty.INTRO, GameResult.resignation(Side.BLACK), Side.WHITE);
        store().save("default", p);

        Progress again = store().load("default");
        assertEquals(new AiRecord(1, 0, 0), again.aiRecord(Difficulty.INTRO));
        assertEquals(new AiRecord(1, 1, 0), again.aiRecord(Difficulty.BEGINNER));
        assertEquals(new AiRecord(0, 0, 0), again.aiRecord(Difficulty.ADVANCED));
        assertEquals(List.of("quiz.attempts=0", "quiz.correct.count=0",
                        "ai.record.intro=1,0,0", "ai.record.beginner=1,1,0"),
                TextFiles.readLines(new DataPaths(tmp).profileFile("default")));
    }

    @Test
    void 잘못된_AI_전적_줄은_건너뛰고_모르는_난이도는_보존한다() throws IOException {
        writeProfile("default", """
                ai.record.intro=3,1
                ai.record.intro=3,-1,0
                ai.record.intro=3,1,0
                ai.record.intro=9,9,9
                ai.record.beginner=a,b,c
                ai.record.expert=2,0,0
                ai.record.INTRO=5,5,5
                """);
        Progress p = store().load("default");
        assertEquals(new AiRecord(3, 1, 0), p.aiRecord(Difficulty.INTRO)); // 세 수가 아니거나 음수인 줄 뒤의 첫 유효값
        assertEquals(new AiRecord(0, 0, 0), p.aiRecord(Difficulty.BEGINNER));

        store().save("default", p);
        List<String> saved = TextFiles.readLines(new DataPaths(tmp).profileFile("default"));
        assertTrue(saved.contains("ai.record.intro=3,1,0"));
        assertTrue(saved.contains("ai.record.expert=2,0,0"));  // 모르는 난이도 key는 그대로
        assertTrue(saved.contains("ai.record.INTRO=5,5,5"));   // key는 대소문자까지 같아야 해석
        assertTrue(saved.stream().noneMatch(l -> l.startsWith("ai.record.beginner")));
    }

    @Test
    void 기보_맞히기_최고값이_재시작_후에도_같은_값으로_유지된다() throws IOException {
        Progress p = store().load("default");
        p.recordGuess("opera-1858", 0.82);
        p.recordGuess("immortal", 2.0 / 3);
        p.recordGuess("tiny", 1.0 / 1001);  // Double.toString이면 지수 표기가 되는 값
        p.recordGuess("perfect", 1.0);
        store().save("default", p);

        List<String> lines = TextFiles.readLines(new DataPaths(tmp).profileFile("default"));
        assertTrue(lines.contains("replay.best.opera-1858=0.82"));
        assertTrue(lines.contains("replay.best.perfect=1.0"));
        assertTrue(lines.stream().noneMatch(l -> l.contains("E")), lines.toString());

        Progress again = store().load("default");
        assertEquals(0.82, again.bestGuess("opera-1858").getAsDouble());
        assertEquals(2.0 / 3, again.bestGuess("immortal").getAsDouble());
        assertEquals(1.0 / 1001, again.bestGuess("tiny").getAsDouble());
        assertEquals(1.0, again.bestGuess("perfect").getAsDouble());
    }

    @Test
    void 잘못된_기보_최고값_줄은_건너뛰고_같은_키의_첫_유효값을_쓴다() throws IOException {
        writeProfile("default", """
                replay.best.opera-1858=1.5
                replay.best.opera-1858=-0.1
                replay.best.opera-1858=8.2E-1
                replay.best.opera-1858=0.75
                replay.best.opera-1858=0.9
                replay.best.a,b=0.5
                replay.best.immortal=abc
                replay.best.whole=1
                """);
        Progress p = store().load("default");
        assertEquals(0.75, p.bestGuess("opera-1858").getAsDouble());
        assertTrue(p.bestGuess("immortal").isEmpty());
        assertEquals(1.0, p.bestGuess("whole").getAsDouble());

        store().save("default", p);
        List<String> saved = TextFiles.readLines(new DataPaths(tmp).profileFile("default"));
        assertTrue(saved.contains("replay.best.opera-1858=0.75"));
        assertTrue(saved.stream().noneMatch(l -> l.startsWith("replay.best.immortal") || l.startsWith("replay.best.a,b")));
    }
}
