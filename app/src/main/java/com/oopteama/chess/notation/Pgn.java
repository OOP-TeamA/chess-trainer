package com.oopteama.chess.notation;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.StandardRules;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PGN 기보 파일 읽기와 쓰기. 파일 하나에 대국 하나를 담는다.
 * 변화수 괄호, {@code ;} 주석, {@code $n} 평가 기호는 지원하지 않고 형식 오류로 처리한다.
 */
public final class Pgn {

    private static final Pattern MOVE_NUMBER = Pattern.compile("^(\\d+)(\\.+)");
    private static final Set<String> RESULT_TOKENS = Set.of("1-0", "0-1", "1/2-1/2", "*");
    private static final List<String> SEVEN_TAGS = List.of("Event", "Site", "Date", "Round", "White", "Black", "Result");
    private static final int LINE_WIDTH = 80;

    private Pgn() {}

    /**
     * PGN 대국 하나를 읽는다. 지원: 태그, SAN, {@code {해설}}, 평가 기호 접미사, 결과 토큰,
     * {@code [SetUp "1"]}과 {@code [FEN "…"]}. 모든 수를 {@link StandardRules}로 재생해 합법 수인지
     * 확인한다. 결과 태그와 마지막 결과 토큰은 일치해야 하고, 끝 국면에서 보드가 종료를 판정하면
     * 그 결과도 일치해야 한다. 그 밖에 다음도 형식 오류로 처리한다: 같은 태그가 두 번 나옴,
     * {@code SetUp}과 {@code FEN} 중 하나만 있음, 표준 규칙에 맞지 않는 시작 국면, 실제와 다른 수 번호,
     * 첫 수 앞의 해설, 결과 토큰이 없거나 그 뒤에 내용이 더 있음. 줄바꿈 없는 공백(NBSP)과 전각 공백은
     * 공백으로 본다.
     *
     * @param in 읽을 입력(닫지 않는다)
     * @param source 오류 메시지에 쓸 파일 이름
     * @return 읽은 대국. 각 수의 SAN은 정규 SAN으로 다시 쓴 값이다
     * @throws IOException 입력을 읽지 못했을 때
     * @throws PgnFormatException 형식 오류, 불법 수, 결과 불일치. 행 번호를 담는다
     */
    public static PgnGame read(Reader in, String source) throws IOException, PgnFormatException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(source, "source");
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = in.read(buf)) >= 0) {
            sb.append(buf, 0, n);
        }
        String text = sb.toString();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        return new Parser(text, source).parse();
    }

    /**
     * 대국을 PGN으로 쓴다. 7개 표준 태그를 먼저 쓰며 Result는 마지막 토큰과 같게 쓰고 나머지 누락 값은
     * {@code ?}로 쓴다. 표준 시작 국면이 아니면 {@code [SetUp "1"]}과 {@code [FEN "…"]}도 쓴다.
     * 각 수는 정규 SAN 뒤에 평가 기호와 해설을 이어 쓴다. 출력은 닫지 않고 flush만 한다.
     *
     * @param game 쓸 대국
     * @param out 출력
     * @throws IOException 쓰지 못했을 때
     * @throws IllegalArgumentException game의 시작 FEN이 잘못되었을 때
     */
    public static void write(PgnGame game, Writer out) throws IOException {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(out, "out");

        Position start;
        try {
            start = Fen.parse(game.startFen());
        } catch (FenFormatException e) {
            throw new IllegalArgumentException("잘못된 시작 FEN: " + game.startFen(), e);
        }
        boolean customStart = game.customStart() || !Fen.write(start).equals(Fen.INITIAL);

        Map<String, String> tags = new LinkedHashMap<>();
        for (String name : SEVEN_TAGS) {
            tags.put(name, name.equals("Result") ? game.result() : game.tags().getOrDefault(name, "?"));
        }
        for (Map.Entry<String, String> e : game.tags().entrySet()) {
            String name = e.getKey();
            if (!SEVEN_TAGS.contains(name) && !name.equals("SetUp") && !name.equals("FEN")) {
                tags.put(name, e.getValue());
            }
        }
        if (customStart) {
            tags.put("SetUp", "1");
            tags.put("FEN", game.startFen());
        }
        for (Map.Entry<String, String> e : tags.entrySet()) {
            out.write("[" + e.getKey() + " \"" + escapeTag(e.getValue()) + "\"]\n");
        }
        out.write("\n");

        List<String> tokens = new ArrayList<>();
        Side side = start.sideToMove();
        int number = start.fullmoveNumber();
        boolean needsBlackNumber = true;
        for (PgnMove m : game.moves()) {
            String numberMark = "";
            if (side == Side.WHITE) {
                numberMark = number + ". ";
            } else if (needsBlackNumber) {
                numberMark = number + "... ";
            }
            // 수 번호와 수는 한 덩어리로 두어 그 사이에서 줄이 바뀌지 않게 한다.
            tokens.add(numberMark + m.san() + m.suffix());
            String comment = cleanComment(m.comment());
            needsBlackNumber = !comment.isEmpty();
            if (needsBlackNumber) {
                tokens.add("{" + comment + "}");
            }
            if (side == Side.BLACK) {
                number++;
            }
            side = side.opposite();
        }
        tokens.add(game.result());

        StringBuilder line = new StringBuilder();
        for (String token : tokens) {
            if (line.length() > 0 && line.length() + 1 + token.length() > LINE_WIDTH) {
                out.write(line + "\n");
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(token);
        }
        out.write(line + "\n");
        out.flush();
    }

    private static String escapeTag(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace('\n', ' ').replace('\r', ' ');
    }

    /** 해설 안에 닫는 중괄호를 둘 수 없고 줄바꿈은 공백 하나로 합친다. */
    private static String cleanComment(String comment) {
        return comment.replace('}', ')').replaceAll("\\s+", " ").trim();
    }

    /** 읽기 전용 상태를 가진 한 번 쓰는 파서. */
    private static final class Parser {
        private final String text;
        private final String source;
        private final int[] lineStarts;
        private int pos;

        Parser(String text, String source) {
            this.text = text;
            this.source = source;
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    starts.add(i + 1);
                }
            }
            this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
        }

        PgnGame parse() throws PgnFormatException {
            Map<String, String> tags = new LinkedHashMap<>();
            Map<String, Integer> tagLines = new LinkedHashMap<>();
            skipWhitespace();
            while (!atEnd() && text.charAt(pos) == '[') {
                readTag(tags, tagLines);
                skipWhitespace();
            }

            StandardRules rules = new StandardRules();
            String startFen = Fen.INITIAL;
            Position position = Position.initial();
            boolean setUp = "1".equals(tags.get("SetUp"));
            boolean hasFen = tags.containsKey("FEN");
            if (setUp && !hasFen) {
                throw error("[SetUp \"1\"]에는 [FEN] 태그가 필요합니다.", tagLines.get("SetUp"));
            }
            if (!setUp && hasFen) {
                throw error("[FEN] 태그에는 [SetUp \"1\"]이 필요합니다.", tagLines.get("FEN"));
            }
            if (setUp) {
                startFen = tags.get("FEN");
                int fenLine = tagLines.get("FEN");
                try {
                    position = Fen.parse(startFen);
                } catch (FenFormatException e) {
                    throw error("FEN 태그가 잘못되었습니다(" + e.field() + "번 필드): " + e.getMessage(), fenLine);
                }
                List<String> problems = rules.validate(position);
                if (!problems.isEmpty()) {
                    throw error("시작 국면이 표준 규칙에 맞지 않습니다: " + problems.get(0), fenLine);
                }
            }

            GameHistory history = new GameHistory(startFen, rules.repetitionKey(position));
            List<Move> moves = new ArrayList<>();
            List<String> sans = new ArrayList<>();
            List<String> suffixes = new ArrayList<>();
            List<String> comments = new ArrayList<>();
            String result = null;

            while (true) {
                skipWhitespace();
                if (atEnd()) {
                    break;
                }
                int line = lineAt(pos);
                if (result != null) {
                    throw error("결과 토큰 뒤에 내용이 더 있습니다.", line);
                }
                char c = text.charAt(pos);
                if (c == '{') {
                    String comment = readComment();
                    if (moves.isEmpty()) {
                        throw error("첫 수 앞의 해설은 지원하지 않습니다.", line);
                    }
                    int last = comments.size() - 1;
                    comments.set(last, comments.get(last).isEmpty() ? comment : comments.get(last) + " " + comment);
                    continue;
                }
                if (c == ';') {
                    throw error("';' 주석은 지원하지 않습니다.", line);
                }
                if (c == '$') {
                    throw error("$n 평가 기호는 지원하지 않습니다.", line);
                }
                if (c == '(' || c == ')') {
                    throw error("변화수(괄호)는 지원하지 않습니다.", line);
                }
                if (c == '[' || c == ']') {
                    throw error("태그는 수순 앞에만 쓸 수 있습니다.", line);
                }
                if (c == '}') {
                    throw error("짝이 없는 '}'입니다.", line);
                }

                String word = readWord();
                Matcher number = MOVE_NUMBER.matcher(word);
                if (number.find()) {
                    int written;
                    try {
                        written = Integer.parseInt(number.group(1));
                    } catch (NumberFormatException e) {
                        throw error("수 번호가 너무 큽니다: " + word, line);
                    }
                    if (written != position.fullmoveNumber()) {
                        throw error("수 번호가 맞지 않습니다: " + position.fullmoveNumber()
                            + "번이어야 하는데 " + written + "번입니다.", line);
                    }
                    word = word.substring(number.end());
                    if (word.isEmpty()) {
                        continue;
                    }
                }
                if (RESULT_TOKENS.contains(word)) {
                    result = word;
                    continue;
                }

                Move move;
                try {
                    move = San.parse(position, rules, word);
                } catch (SanException e) {
                    throw error((moves.size() + 1) + "번째 수 '" + word + "': " + e.getMessage(), line);
                }
                sans.add(San.write(position, rules, move));
                suffixes.add(San.suffixOf(word));
                comments.add("");
                moves.add(move);
                position.make(move);
                history.push(move, rules.repetitionKey(position));
            }

            int endLine = lineAt(text.length());
            if (result == null) {
                throw error("결과 토큰(1-0, 0-1, 1/2-1/2, *)이 없습니다.", endLine);
            }
            String tagResult = tags.get("Result");
            if (tagResult != null && !tagResult.equals(result)) {
                throw error("Result 태그(" + tagResult + ")와 마지막 결과 토큰(" + result + ")이 다릅니다.", endLine);
            }
            Optional<GameResult> end = rules.checkEnd(position, history);
            if (end.isPresent() && !end.get().pgnToken().equals(result)) {
                throw error("보드에서는 " + end.get().pgnToken() + "(" + end.get().reason()
                    + ")로 종료됐는데 결과 토큰은 " + result + "입니다.", endLine);
            }

            List<PgnMove> pgnMoves = new ArrayList<>();
            for (int i = 0; i < moves.size(); i++) {
                pgnMoves.add(new PgnMove(sans.get(i), moves.get(i), suffixes.get(i), comments.get(i)));
            }
            return new PgnGame(tags, startFen, pgnMoves, result);
        }

        private void readTag(Map<String, String> tags, Map<String, Integer> tagLines) throws PgnFormatException {
            int line = lineAt(pos);
            pos++; // '['
            skipSpaces();
            int nameStart = pos;
            while (!atEnd() && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
                pos++;
            }
            String name = text.substring(nameStart, pos);
            if (name.isEmpty()) {
                throw error("태그 이름이 없습니다. 형식: [이름 \"값\"]", line);
            }
            skipSpaces();
            if (atEnd() || text.charAt(pos) != '"') {
                throw error("태그 값은 큰따옴표로 감싸야 합니다: [" + name + " \"값\"]", line);
            }
            pos++; // 여는 따옴표
            StringBuilder value = new StringBuilder();
            while (true) {
                if (atEnd() || text.charAt(pos) == '\n') {
                    throw error("태그 값의 닫는 따옴표가 없습니다: " + name, line);
                }
                char ch = text.charAt(pos++);
                if (ch == '"') {
                    break;
                }
                if (ch == '\\' && !atEnd() && (text.charAt(pos) == '"' || text.charAt(pos) == '\\')) {
                    value.append(text.charAt(pos++));
                } else {
                    value.append(ch);
                }
            }
            skipSpaces();
            if (atEnd() || text.charAt(pos) != ']') {
                throw error("태그의 닫는 ']'가 없습니다: " + name, line);
            }
            pos++; // ']'
            if (tags.containsKey(name)) {
                throw error("같은 태그가 두 번 나왔습니다: " + name, line);
            }
            tags.put(name, value.toString());
            tagLines.put(name, line);
        }

        private String readComment() throws PgnFormatException {
            int line = lineAt(pos);
            int close = text.indexOf('}', pos + 1);
            if (close < 0) {
                throw error("해설의 닫는 '}'가 없습니다.", line);
            }
            String comment = collapse(text.substring(pos + 1, close));
            pos = close + 1;
            return comment;
        }

        /** 연속된 공백(NBSP 포함)을 공백 하나로 합치고 앞뒤를 자른다. */
        private static String collapse(String raw) {
            StringBuilder sb = new StringBuilder();
            boolean pendingSpace = false;
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (isBlank(c)) {
                    pendingSpace = sb.length() > 0;
                } else {
                    if (pendingSpace) {
                        sb.append(' ');
                        pendingSpace = false;
                    }
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private String readWord() {
            int start = pos;
            while (!atEnd() && !isWordBreak(text.charAt(pos))) {
                pos++;
            }
            return text.substring(start, pos);
        }

        /** 공백 문자. 웹에서 복사한 기보에 섞이는 줄바꿈 없는 공백(NBSP)과 전각 공백도 포함한다. */
        private static boolean isBlank(char c) {
            return Character.isWhitespace(c) || Character.isSpaceChar(c);
        }

        private static boolean isWordBreak(char c) {
            return isBlank(c) || "{}()[];$".indexOf(c) >= 0;
        }

        private void skipWhitespace() {
            while (!atEnd() && isBlank(text.charAt(pos))) {
                pos++;
            }
        }

        /** 한 줄 안의 공백(줄바꿈 제외)을 건너뛴다. */
        private void skipSpaces() {
            while (!atEnd() && text.charAt(pos) != '\n' && isBlank(text.charAt(pos))) {
                pos++;
            }
        }

        private boolean atEnd() {
            return pos >= text.length();
        }

        /** 1부터 세는 행 번호. */
        private int lineAt(int index) {
            int i = Arrays.binarySearch(lineStarts, Math.min(index, text.length()));
            return (i >= 0 ? i : -i - 2) + 1;
        }

        private PgnFormatException error(String message, int line) {
            return new PgnFormatException(message, source, line);
        }
    }
}
