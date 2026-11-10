package com.oopteama.chess.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.content.RulesKind;
import com.oopteama.chess.content.goal.AnyMoveGoal;
import com.oopteama.chess.content.goal.CheckGoal;
import com.oopteama.chess.content.goal.MateGoal;
import com.oopteama.chess.content.goal.MoveGoal;
import com.oopteama.chess.content.goal.SquareGoal;
import com.oopteama.chess.core.Square;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** RULE-01·RULE-02: 레슨 파일 로드와 로드 시점 검증(P1, G7). */
class LessonRepositoryTest {

    /** 정상 레슨. 줄 번호: 1 @lesson, 6 @step, 7 fen, 10 goal. */
    private static final String VALID = """
            @lesson rook
            order=3
            title=룩
            summary=가로와 세로로 움직입니다.

            @step
            fen=8/8/8/8/8/8/8/R7 w - - 0 1
            rules=sandbox
            text=룩을 움직여 보세요.
            goal=any-move
            """;

    private final Logger log = Logger.getLogger(LessonRepository.class.getName());
    private final List<String> warnings = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord r) {
            warnings.add(r.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @TempDir
    Path userDir;

    @BeforeEach
    void captureLog() {
        log.addHandler(capture);
    }

    @AfterEach
    void releaseLog() {
        log.removeHandler(capture);
    }

    private static List<Lesson> load(String text) throws IOException {
        return new LessonRepository().loadAll(new StringReader(text), "t.txt");
    }

    private static Set<Square> squares(String... names) {
        return Stream.of(names).map(Square::of).collect(Collectors.toSet());
    }

    @Test
    void readsSharedKnightSample() throws IOException {
        Path file = TestFixtures.firstDemo().resolve("lessons").resolve("07_knight.txt");
        List<Lesson> lessons;
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            lessons = new LessonRepository().loadAll(in, "07_knight.txt");
        }
        assertEquals(1, lessons.size());
        Lesson knight = lessons.get(0);
        assertEquals("knight", knight.id());
        assertEquals(7, knight.order());
        assertEquals("나이트", knight.title());
        assertEquals(3, knight.steps().size());

        LessonStep any = knight.steps().get(0);
        assertEquals(RulesKind.SANDBOX, any.rules());
        assertEquals(new AnyMoveGoal(), any.goal());
        assertEquals(squares("b4", "b6", "c3", "c7", "e3", "e7", "f4", "f6"), any.highlight());
        assertEquals(Set.of(), any.hint());
        assertEquals("", any.fail());

        LessonStep move = knight.steps().get(1);
        assertEquals(new MoveGoal(List.of("d4f5")), move.goal());
        assertEquals(squares("d4", "f5"), move.hint());
        assertTrue(move.fail().startsWith("나이트는 직선으로"));

        LessonStep square = knight.steps().get(2);
        assertEquals(new SquareGoal(Set.of(Square.of("c7"))), square.goal());
        assertEquals(squares("d5"), square.hint());
        assertEquals(List.of(), warnings);
    }

    @Test
    void indexOrderDecidesListAndBrokenFileIsSkipped() throws IOException {
        // RULE-01: 파일을 추가하고 index.txt에 한 줄 넣으면 코드 수정 없이 목록에 나타난다
        DataPaths paths = new DataPaths(userDir);
        List<String> ids = new ArrayList<>();
        for (String name : paths.index("lessontest")) {
            try (Reader in = paths.openResource("lessontest/" + name)) {
                new LessonRepository().loadAll(in, name).forEach(l -> ids.add(l.id()));
            }
        }
        assertEquals(List.of("rook", "pawn"), ids);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).startsWith("broken.txt:7:"), warnings.get(0));
    }

    @Test
    void optionalKeysHaveDefaults() throws IOException {
        Lesson lesson = load("""
                @lesson start
                order=1
                title=시작
                summary=첫 수

                @step
                fen=rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
                text=아무 수나 두어 보세요.
                goal=any-move
                """).get(0);
        LessonStep step = lesson.steps().get(0);
        assertEquals(RulesKind.STANDARD, step.rules());
        assertEquals(Set.of(), step.highlight());
        assertEquals(Set.of(), step.hint());
        assertEquals("", step.fail());
        assertEquals("", step.success());
    }

    @Test
    void acceptsReachableCheckMateAndSquareGoals() throws IOException {
        List<Lesson> lessons = load("""
                @lesson goals
                order=10
                title=목표
                summary=여러 목표

                @step
                fen=4k3/8/8/8/8/8/8/R7 w - - 0 1
                rules=sandbox
                text=흑 킹에게 체크를 걸어 보세요.
                goal=check

                @step
                fen=6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1
                text=한 수로 메이트해 보세요.
                goal=mate
                highlight=A1, G8

                @step
                fen=8/8/8/8/8/8/8/8 w - - 0 1
                rules=sandbox
                text=e4 칸을 골라 보세요.
                goal=square:e4
                """);
        assertEquals(1, lessons.size(), () -> String.join("\n", warnings));
        List<LessonStep> steps = lessons.get(0).steps();
        assertEquals(new CheckGoal(), steps.get(0).goal());
        assertEquals(new MateGoal(), steps.get(1).goal());
        assertEquals(RulesKind.STANDARD, steps.get(1).rules());
        assertEquals(squares("a1", "g8"), steps.get(1).highlight());
        assertEquals(new SquareGoal(Set.of(Square.of("e4"))), steps.get(2).goal());
    }

    static Stream<Arguments> brokenLessons() {
        return Stream.of(
            Arguments.of("빈 파일", "# 주석만\n", "레슨 파일에 읽을 블록이 없습니다"),
            Arguments.of("@lesson 없음", VALID.replace("@lesson rook", "@step"), "t.txt:1: @lesson 블록이 없습니다"),
            Arguments.of("@lesson 두 개", VALID + "@lesson other\ntitle=x\n", "t.txt:11: @lesson 블록은 파일에 하나만"),
            Arguments.of("@lesson 앞의 키", "order=1\n" + VALID, "t.txt:1: 레슨 파일에 쓸 수 없는 블록입니다: @lesson 앞의 키"),
            Arguments.of("알 수 없는 블록", VALID + "@stpe\nfen=x\n", "t.txt:11: 레슨 파일에 쓸 수 없는 블록입니다: @stpe"),
            Arguments.of("ID 없음", VALID.replace("@lesson rook", "@lesson"), "t.txt:1: 레슨 ID는"),
            Arguments.of("ID 대문자", VALID.replace("@lesson rook", "@lesson Rook"), "'Rook'"),
            Arguments.of("title 없음", VALID.replace("title=룩\n", ""), "필수 키 'title'"),
            Arguments.of("summary 비어 있음", VALID.replace("summary=가로와 세로로 움직입니다.", "summary="), "필수 키 'summary'"),
            Arguments.of("order 0", VALID.replace("order=3", "order=0"), "t.txt:1: 레슨 rook: order는 양의 정수"),
            Arguments.of("order 숫자 아님", VALID.replace("order=3", "order=셋"), "order는 양의 정수"),
            Arguments.of("@step 없음", VALID.substring(0, VALID.indexOf("@step")), "t.txt:1: 레슨 rook: @step 블록이 하나 이상"),
            Arguments.of("goal 없음(P1)", VALID.replace("goal=any-move\n", ""), "t.txt:6: @step 블록에 필수 키 'goal'"),
            Arguments.of("fen 없음", VALID.replace("fen=8/8/8/8/8/8/8/R7 w - - 0 1\n", ""), "필수 키 'fen'"),
            Arguments.of("text 없음", VALID.replace("text=룩을 움직여 보세요.\n", ""), "필수 키 'text'"),
            Arguments.of("fen 형식 오류", VALID.replace("8/8/8/8/8/8/8/R7", "8/8/R7"), "t.txt:6: 레슨 rook 1단계: fen 형식 오류"),
            Arguments.of("goal 문법 오류", VALID.replace("goal=any-move", "goal=jump"), "1단계: 잘못된 목표 'jump'"),
            Arguments.of("rules 오타", VALID.replace("rules=sandbox", "rules=classic"), "rules는 standard 또는 sandbox"),
            Arguments.of("highlight 칸 오류", VALID + "highlight=a1,z9\n", "highlight의 칸 이름이 잘못됐습니다: 'z9'"),
            Arguments.of("표준 규칙인데 킹 없음", VALID.replace("rules=sandbox\n", ""), "표준 규칙에 맞지 않는 국면"),
            Arguments.of("체크 목표인데 상대 킹 없음(G7)", VALID.replace("goal=any-move", "goal=check"), "상대 킹이 국면에 없습니다"),
            Arguments.of("메이트 목표인데 상대 킹 없음(G7)", VALID.replace("goal=any-move", "goal=mate"), "상대 킹이 국면에 없습니다"),
            Arguments.of("둘 수 없는 정답 수", VALID.replace("goal=any-move", "goal=move:a1a8|a1b2"), "정답 수 a1b2는 이 국면에서 둘 수 없습니다"),
            Arguments.of("메이트가 불가능한 국면",
                VALID.replace("8/8/8/8/8/8/8/R7 w", "4k3/8/8/8/8/8/8/R7 w").replace("goal=any-move", "goal=mate"),
                "목표를 이루는 수가 없습니다"),
            Arguments.of("두 번째 단계만 잘못돼도 레슨 전체를 건너뜀",
                VALID + "\n@step\nfen=8/8/8/8/8/8/8/R7 w - - 0 1\nrules=sandbox\ntext=t\ngoal=move:a1h8\n",
                "t.txt:12: 레슨 rook 2단계: 정답 수 a1h8"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenLessons")
    void brokenLessonIsSkippedWithLocatedWarning(String name, String text, String expected) throws IOException {
        assertEquals(List.of(), load(text));
        assertEquals(1, warnings.size(), () -> String.join("\n", warnings));
        assertTrue(warnings.get(0).contains(expected), warnings.get(0));
    }

    @Test
    void lessonRecordsCheckInvariants() throws IOException {
        Lesson lesson = load(VALID).get(0);
        assertThrows(UnsupportedOperationException.class, () -> lesson.steps().add(lesson.steps().get(0)));
        assertThrows(IllegalArgumentException.class, () -> new Lesson("x", 0, "t", "s", lesson.steps()));
        assertThrows(IllegalArgumentException.class, () -> new Lesson("x", 1, "t", "s", List.of()));
        assertEquals(RulesKind.SANDBOX.rules(), lesson.steps().get(0).rules().rules());
    }
}
