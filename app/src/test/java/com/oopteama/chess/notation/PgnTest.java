package com.oopteama.chess.notation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PgnTest {

    private static final StandardRules RULES = new StandardRules();

    /** 기능 명세 6장의 오페라 게임(1858). SAN 파서의 기준값이다. */
    private static final String OPERA = """
        [Event "Paris"]
        [Date "1858.??.??"]
        [White "Paul Morphy"]
        [Black "Duke Karl / Count Isouard"]
        [Result "1-0"]
        [ECO "C41"]
        [Alias "오페라 게임"]
        [Level "입문"]
        1. e4 e5 2. Nf3 d6 3. d4 Bg4 {f3 나이트를 [[핀]]한다.} 4. dxe5 Bxf3
        5. Qxf3 dxe5 6. Bc4 {퀸과 비숍이 함께 f7을 노린다.} Nf6 7. Qb3 Qe7
        8. Nc3 c6 9. Bg5 b5 10. Nxb5! {전개 우위를 살리려 기물을 내준다.} cxb5
        11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. Rd1 Qe6 15. Bxd7+ Nxd7
        16. Qb8+!! {[[sacrifice|퀸 희생]]. 나이트가 d7을 비우게 만든다.} Nxb8 17. Rd8# 1-0
        """;

    private static final String OPERA_FINAL_FEN = "1n1Rkb1r/p4ppp/4q3/4p1B1/4P3/8/PPP2PPP/2K5 b k - 1 17";

    private static PgnGame read(String pgn) throws Exception {
        return Pgn.read(new StringReader(pgn), "test.pgn");
    }

    private static PgnFormatException failure(String pgn) {
        return assertThrows(PgnFormatException.class, () -> read(pgn));
    }

    private static String write(PgnGame game) throws Exception {
        StringWriter out = new StringWriter();
        Pgn.write(game, out);
        return out.toString();
    }

    @Test
    void operaGameEndsInTheReferencePosition() throws Exception {
        PgnGame game = read(OPERA);
        assertEquals(33, game.moves().size());
        assertEquals("1-0", game.result());
        assertEquals(Fen.INITIAL, game.startFen());
        assertFalse(game.customStart());

        Position pos = Fen.parse(game.startFen());
        GameHistory history = new GameHistory(game.startFen(), RULES.repetitionKey(pos));
        for (PgnMove m : game.moves()) {
            pos.make(m.move());
            history.push(m.move(), RULES.repetitionKey(pos));
        }
        assertEquals(OPERA_FINAL_FEN, Fen.write(pos));
        GameResult end = RULES.checkEnd(pos, history).orElseThrow(() -> new AssertionError("종료 판정 없음"));
        assertEquals(GameResult.Outcome.WHITE_WINS, end.outcome());
        assertEquals(GameResult.Reason.CHECKMATE, end.reason());
    }

    @Test
    void operaTagsMovesSuffixesAndCommentsAreKept() throws Exception {
        PgnGame game = read(OPERA);
        assertEquals("Paul Morphy", game.tag("White").orElseThrow());
        assertEquals("오페라 게임", game.tag("Alias").orElseThrow());
        assertTrue(game.tag("Nope").isEmpty());

        List<PgnMove> moves = game.moves();
        assertEquals("e4", moves.get(0).san());
        assertEquals("Bg4", moves.get(5).san());
        assertEquals("f3 나이트를 [[핀]]한다.", moves.get(5).comment());

        PgnMove brilliancy = moves.get(30);                  // 16. Qb8+!!
        assertEquals("Qb8+", brilliancy.san());              // 정규 SAN에는 평가 기호가 없다
        assertEquals("!!", brilliancy.suffix());
        assertEquals("[[sacrifice|퀸 희생]]. 나이트가 d7을 비우게 만든다.", brilliancy.comment());

        assertEquals("Nxb5", moves.get(18).san());
        assertEquals("!", moves.get(18).suffix());
        assertEquals("O-O-O", moves.get(22).san());
        assertEquals("Rd8#", moves.get(32).san());
        assertEquals("", moves.get(32).suffix());
        assertEquals("", moves.get(0).comment());
    }

    @Test
    void writingEveryMoveAgainGivesTheSameSan() throws Exception {
        PgnGame game = read(OPERA);
        Position pos = Fen.parse(game.startFen());
        for (PgnMove m : game.moves()) {
            assertEquals(m.san(), San.write(pos, RULES, m.move()));
            pos.make(m.move());
        }
    }

    @Test
    void writeThenReadGivesTheSameGameAndTheSameText() throws Exception {
        PgnGame game = read(OPERA);
        String text = write(game);
        PgnGame again = read(text);
        // 빠진 표준 태그(Site, Round)는 "?"로 채워지지만, 대국 내용은 그대로여야 한다.
        assertEquals(game.moves(), again.moves());
        assertEquals(game.result(), again.result());
        assertEquals(game.startFen(), again.startFen());
        for (Map.Entry<String, String> tag : game.tags().entrySet()) {
            assertEquals(tag.getValue(), again.tags().get(tag.getKey()), tag.getKey());
        }
        assertEquals("?", again.tag("Site").orElseThrow());
        // 한 번 쓴 파일은 다시 읽고 써도 글자가 같다.
        assertEquals(text, write(again));
        assertEquals(again, read(write(again)));
        assertEquals("!!", again.moves().get(30).suffix());
        String oneLine = text.replace('\n', ' ');
        assertTrue(oneLine.contains("16. Qb8+!! {[[sacrifice|퀸 희생]]. 나이트가 d7을 비우게 만든다.} 16... Nxb8"));
        for (String line : text.split("\n")) {
            assertTrue(line.length() <= 80 || line.contains("{"), "너무 긴 줄: " + line);
            assertFalse(line.matches(".*\\d+\\.+$"), "수 번호로 줄이 끝남: " + line);
        }
    }

    @Test
    void sevenStandardTagsComeFirstAndMissingOnesAreQuestionMarks() throws Exception {
        PgnGame game = read(OPERA);
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("White", "나");
        tags.put("Alias", "연습 대국");
        String text = write(new PgnGame(tags, Fen.INITIAL, game.moves().subList(0, 2), "*"));
        assertTrue(text.startsWith(
            "[Event \"?\"]\n[Site \"?\"]\n[Date \"?\"]\n[Round \"?\"]\n[White \"나\"]\n[Black \"?\"]\n[Result \"*\"]\n"
            + "[Alias \"연습 대국\"]\n\n1. e4 e5 *\n"), text);
    }

    @Test
    void resultTagIsRewrittenToMatchTheFinalToken() throws Exception {
        PgnGame game = read(OPERA);
        Map<String, String> tags = new LinkedHashMap<>(game.tags());
        tags.put("Result", "0-1"); // 잘못된 값이 들어 있어도 마지막 토큰과 같게 쓴다.
        String text = write(new PgnGame(tags, game.startFen(), game.moves(), "1-0"));
        assertTrue(text.contains("[Result \"1-0\"]"));
        assertEquals("1-0", read(text).result());
    }

    @Test
    void tagValuesAreEscaped() throws Exception {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "He said \"hi\" \\ bye");
        PgnGame game = new PgnGame(tags, Fen.INITIAL, List.of(), "*");
        String text = write(game);
        assertTrue(text.contains("[Event \"He said \\\"hi\\\" \\\\ bye\"]"), text);
        assertEquals("He said \"hi\" \\ bye", read(text).tag("Event").orElseThrow());
    }

    @Test
    void customStartPositionIsWrittenAndReadBack() throws Exception {
        String pgn = """
            [SetUp "1"]
            [FEN "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"]

            1. e4 Kd7 2. e5 *
            """;
        PgnGame game = read(pgn);
        assertTrue(game.customStart());
        assertEquals("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1", game.startFen());
        assertEquals(3, game.moves().size());

        String text = write(game);
        assertTrue(text.contains("[SetUp \"1\"]") && text.contains("[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]"));
        PgnGame again = read(text);
        assertTrue(again.customStart());
        assertEquals(game.startFen(), again.startFen());
        assertEquals(game.moves(), again.moves());
        assertEquals(text, write(again));
    }

    @Test
    void blackToMoveStartUsesTheEllipsisNumber() throws Exception {
        String pgn = """
            [SetUp "1"]
            [FEN "4k3/8/8/8/4P3/8/8/4K3 b - e3 0 17"]

            17... Kd7 18. e5 *
            """;
        PgnGame game = read(pgn);
        assertEquals(2, game.moves().size());
        assertTrue(write(game).contains("17... Kd7 18. e5 *"));
    }

    @Test
    void readsFoolsMateWithMoveNumbersGluedToTheMoves() throws Exception {
        PgnGame game = read("1.f3 e5 2.g4 Qh4# 0-1");
        assertEquals(4, game.moves().size());
        assertEquals("Qh4#", game.moves().get(3).san());
        assertEquals("0-1", game.result());
    }

    @Test
    void acceptsByteOrderMarkAndWindowsLineEnds() throws Exception {
        PgnGame game = read("\uFEFF[Event \"x\"]\r\n\r\n1. e4 e5\r\n2. Nf3 *\r\n");
        assertEquals(3, game.moves().size());
        assertEquals("x", game.tag("Event").orElseThrow());
    }

    @Test
    void unsupportedSyntaxIsAFormatError() {
        assertTrue(failure("1. e4 (1. d4) e5 *").getMessage().contains("변화수"));
        assertTrue(failure("1. e4 e5 ; 주석\n2. Nf3 *").getMessage().contains("';'"));
        assertTrue(failure("1. e4 $1 e5 *").getMessage().contains("$n"));
        assertTrue(failure("1. e4 {닫히지 않은 해설 e5 *").getMessage().contains("'}'"));
        assertTrue(failure("{첫 수 앞의 해설} 1. e4 *").getMessage().contains("첫 수 앞"));
    }

    @Test
    void illegalMoveReportsTheFileAndLine() {
        PgnFormatException e = failure("[Event \"x\"]\n\n1. e4 e5\n2. Ke3 *\n");
        assertEquals("test.pgn", e.source());
        assertEquals(4, e.line());
        assertTrue(e.getMessage().contains("3번째 수"), e.getMessage());
        assertTrue(e.getMessage().contains("Ke3"), e.getMessage());
    }

    @Test
    void ambiguousAndSyntaxErrorsInMovesAreFormatErrors() {
        assertEquals(1, failure("1. Zz9 *").line());
        PgnFormatException ambiguous = failure("""
            [SetUp "1"]
            [FEN "4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1"]

            1. Nd2 *
            """);
        assertTrue(ambiguous.getMessage().contains("출발 칸"), ambiguous.getMessage());
        assertEquals(4, ambiguous.line());
    }

    @Test
    void resultMustBePresentAndLast() {
        assertTrue(failure("1. e4 e5").getMessage().contains("결과 토큰"));
        assertTrue(failure("1. e4 e5 * 2. Nf3").getMessage().contains("뒤에"));
        assertTrue(failure("1. e4 e5 * *").getMessage().contains("뒤에"));
    }

    @Test
    void resultTagAndFinalTokenMustAgree() {
        PgnFormatException e = failure("[Result \"0-1\"]\n\n1. e4 e5 1-0\n");
        assertTrue(e.getMessage().contains("Result 태그"), e.getMessage());
    }

    @Test
    void boardResultMustMatchTheFinalToken() throws Exception {
        assertEquals("0-1", read("1. f3 e5 2. g4 Qh4# 0-1").result());
        assertTrue(failure("1. f3 e5 2. g4 Qh4# 1/2-1/2").getMessage().contains("0-1"));
        assertTrue(failure("1. f3 e5 2. g4 Qh4# *").getMessage().contains("CHECKMATE"));
        // 종료되지 않은 국면이면 어떤 결과 토큰이든(기권, 합의 무승부) 허용한다.
        assertEquals("1-0", read("1. e4 e5 1-0").result());
        assertEquals("1/2-1/2", read("1. e4 e5 1/2-1/2").result());
    }

    @Test
    void moveNumbersMustFollowTheGame() {
        assertTrue(failure("1. e4 e5 3. Nf3 *").getMessage().contains("수 번호"));
        assertTrue(failure("1. e4 e5 2. Nf3 Nc6 2... Bb5 *").getMessage().contains("수 번호"));
    }

    @Test
    void malformedTagsAreFormatErrors() {
        assertEquals(1, failure("[Event Paris]\n\n1. e4 *").line());
        assertEquals(2, failure("[Event \"a\"]\n[Event \"b\"]\n\n1. e4 *").line());
        assertTrue(failure("[Event \"a\"\n\n1. e4 *").getMessage().contains("닫는"));
        assertTrue(failure("[Event \"열리고 안 닫힘]\n\n1. e4 *").getMessage().contains("따옴표"));
        assertTrue(failure("1. e4 [Event \"x\"] *").getMessage().contains("태그는 수순 앞에"));
    }

    @Test
    void setUpAndFenTagsMustComeTogetherAndBeValid() {
        assertTrue(failure("[SetUp \"1\"]\n\n1. e4 *").getMessage().contains("[FEN]"));
        assertTrue(failure("[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]\n\n1. e4 *").getMessage().contains("SetUp"));
        PgnFormatException badFen = failure("[SetUp \"1\"]\n[FEN \"nonsense\"]\n\n1. e4 *");
        assertEquals(2, badFen.line());
        assertTrue(badFen.getMessage().contains("FEN"));
        // 킹이 없는 국면은 표준 규칙 위반이다.
        assertTrue(failure("[SetUp \"1\"]\n[FEN \"8/8/8/8/8/8/4P3/8 w - - 0 1\"]\n\n1. e4 *")
            .getMessage().contains("킹"));
    }

    @Test
    void emptyMovetextWithOnlyAResultIsAGameWithNoMoves() throws Exception {
        PgnGame game = read("[White \"a\"]\n\n*\n");
        assertTrue(game.moves().isEmpty());
        assertEquals("*", game.result());
    }

    @Test
    void famousGamesFromOutsideSourcesReplayToTheirKnownEnds() throws Exception {
        // 불멸의 게임(1851). 모든 수가 합법이고 23...Be7#로 끝난다.
        PgnGame immortal = read("""
            [Event "London"]
            [White "Anderssen"]
            [Black "Kieseritzky"]
            [Result "1-0"]

            1. e4 e5 2. f4 exf4 3. Bc4 Qh4+ 4. Kf1 b5 5. Bxb5 Nf6 6. Nf3 Qh6 7. d3 Nh5 8. Nh4 Qg5
            9. Nf5 c6 10. g4 Nf6 11. Rg1 cxb5 12. h4 Qg6 13. h5 Qg5 14. Qf3 Ng8 15. Bxf4 Qf6
            16. Nc3 Bc5 17. Nd5 Qxb2 18. Bd6 Bxg1 19. e5 Qxa1+ 20. Ke2 Na6 21. Nxg7+ Kd8
            22. Qf6+ Nxf6 23. Be7# 1-0
            """);
        assertEquals(45, immortal.moves().size());
        assertEquals("r1bk3r/p2pBpNp/n4n2/1p1NP2P/6P1/3P4/P1P1K3/q5b1 b - - 1 23", finalFen(immortal));

        // 상록수 게임(1852). 캐슬링(O-O)과 출발 표기(Nbd2, Rad1)가 들어 있다.
        PgnGame evergreen = read("""
            [Event "Berlin"]
            [White "Anderssen"]
            [Black "Dufresne"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. b4 Bxb4 5. c3 Ba5 6. d4 exd4 7. O-O d3 8. Qb3 Qf6
            9. e5 Qg6 10. Re1 Nge7 11. Ba3 b5 12. Qxb5 Rb8 13. Qa4 Bb6 14. Nbd2 Bb7 15. Ne4 Qf5
            16. Bxd3 Qh5 17. Nf6+ gxf6 18. exf6 Rg8 19. Rad1 Qxf3 20. Rxe7+ Nxe7 21. Qxd7+ Kxd7
            22. Bf5+ Ke8 23. Bd7+ Kf8 24. Bxe7# 1-0
            """);
        assertEquals(47, evergreen.moves().size());
        assertEquals("Nbd2", evergreen.moves().get(26).san());
        assertEquals("Rad1", evergreen.moves().get(36).san());
        assertEquals("O-O", evergreen.moves().get(12).san());
        // 마지막 수 Bxe7#은 나이트를 잡는 수라서 반수 카운터가 0이다.
        assertEquals("1r3kr1/pbpBBp1p/1b3P2/8/8/2P2q2/P4PPP/3R2K1 b - - 0 24", finalFen(evergreen));
    }

    private static String finalFen(PgnGame game) throws Exception {
        Position pos = Fen.parse(game.startFen());
        for (PgnMove m : game.moves()) {
            pos.make(m.move());
        }
        return Fen.write(pos);
    }

    @Test
    void nonBreakingAndFullWidthSpacesCountAsSpaces() throws Exception {
        // 웹에서 복사한 기보에는 줄바꿈 없는 공백(U+00A0)과 전각 공백(U+3000)이 흔히 섞인다.
        String nbsp = "\u00A0";
        String wide = "\u3000";
        PgnGame game = read("[Event" + nbsp + "\"x\"]\n\n1." + nbsp + "e4" + nbsp + "e5" + wide + "2." + nbsp + "Nf3"
            + wide + "{두" + nbsp + nbsp + "칸" + wide + "해설}" + nbsp + "Nc6" + nbsp + "*\n");
        assertEquals(4, game.moves().size());
        assertEquals("x", game.tag("Event").orElseThrow());
        assertEquals("두 칸 해설", game.moves().get(2).comment());
        assertEquals("e2e4", San.parse(Position.initial(), RULES, nbsp + "e4" + nbsp).uci());
    }

    @Test
    void explicitSetUpOfTheStandardPositionSurvivesARoundTrip() throws Exception {
        PgnGame game = read("[SetUp \"1\"]\n[FEN \"" + Fen.INITIAL + "\"]\n\n1. e4 *\n");
        assertTrue(game.customStart());
        PgnGame again = read(write(game));
        assertTrue(again.customStart());
        assertEquals(write(game), write(again));
    }

    @Test
    void corruptedInputEitherParsesOrFailsWithAPgnFormatException() {
        String junk = "{}[]()\";$!?+#=-/.\\ \n\t0123456789abcdefghKQRBNOxX\u00A0\u3000\uFEFF가";
        java.util.Random random = new java.util.Random(7);
        int parsed = 0;
        int rejected = 0;
        for (int i = 0; i < 1500; i++) {
            StringBuilder sb = new StringBuilder(OPERA);
            int edits = 1 + random.nextInt(4);
            for (int e = 0; e < edits; e++) {
                int at = random.nextInt(sb.length());
                switch (random.nextInt(3)) {
                    case 0 -> sb.deleteCharAt(at);
                    case 1 -> sb.insert(at, junk.charAt(random.nextInt(junk.length())));
                    default -> sb.setCharAt(at, junk.charAt(random.nextInt(junk.length())));
                }
            }
            try {
                read(sb.toString());
                parsed++;
            } catch (PgnFormatException e) {
                rejected++;
            } catch (Exception e) {
                throw new AssertionError("PgnFormatException 외의 예외: " + e + "\n" + sb, e);
            }
        }
        assertEquals(1500, parsed + rejected);
        assertTrue(rejected > 0 && parsed > 0, "정상 입력과 오류 입력이 모두 나와야 한다: " + parsed + "/" + rejected);
    }

    @Test
    void pgnMoveAndPgnGameValidateTheirArguments() {
        PgnGame game = new PgnGame(new LinkedHashMap<>(Map.of("A", "b")), Fen.INITIAL, List.of(), "*");
        assertThrows(UnsupportedOperationException.class, () -> game.tags().put("C", "d"));
        assertThrows(UnsupportedOperationException.class, () -> game.moves().clear());
        assertThrows(IllegalArgumentException.class, () -> new PgnGame(Map.of(), Fen.INITIAL, List.of(), "2-0"));
        Move anyMove = RULES.legalMoves(Position.initial()).get(0);
        assertThrows(IllegalArgumentException.class, () -> new PgnMove("e4", anyMove, "!!!", ""));
        assertThrows(NullPointerException.class, () -> new PgnMove("e4", anyMove, "", null));
    }
}
