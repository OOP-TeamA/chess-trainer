package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 공통 블록 형식(기능 명세 6장)의 유일한 파서.
 *
 * <ul>
 *   <li>{@code #}으로 시작하는 줄은 주석, 빈 줄은 무시한다(앞쪽 공백은 무시하고 판단).</li>
 *   <li>{@code @종류} 또는 {@code @종류 아이디} 줄이 새 블록을 시작한다. 첫 {@code @} 이전의 키 줄은 이름 없는 기본 블록이 된다.</li>
 *   <li>블록 안은 {@code 키=값}. 첫 번째 {@code =}만 구분자이며 키와 값의 앞뒤 공백은 제거한다.</li>
 *   <li>값 안의 {@code \n}은 줄바꿈, {@code \\}는 역슬래시로 푼다. 그 밖의 역슬래시는 그대로 둔다.</li>
 *   <li>한 블록에서 같은 키가 두 번 나오면 형식 오류다.</li>
 * </ul>
 *
 * <p>형식이 깨진 블록은 결과에서 빼고 오류를 {@link Result#problems()}에 모은다. 예외로 전체를 멈추지 않는다.
 * 오류 메시지는 {@code 파일명:행번호: 설명} 형태다. 파일 첫 줄의 BOM은 허용해 제거한다.
 */
public final class BlockReader {

    /** 주석 시작 문자. */
    private static final String COMMENT_PREFIX = "#";
    /** 블록 머리 시작 문자. */
    private static final String HEADER_PREFIX = "@";
    /** 키와 값 구분자. */
    private static final char KEY_VALUE_SEPARATOR = '=';
    /** UTF-8 BOM이 디코딩된 문자. */
    private static final char BOM = '﻿';

    /**
     * 읽기 결과.
     *
     * @param blocks   정상 블록(파일 순서)
     * @param problems 건너뛴 블록의 형식 오류(파일명·행 번호 포함)
     */
    public record Result(List<Block> blocks, List<MalformedDataException> problems) {
        /** 두 목록을 불변 사본으로 저장한다. */
        public Result {
            blocks = List.copyOf(blocks);
            problems = List.copyOf(problems);
        }
    }

    private BlockReader() {
    }

    /**
     * 입력 전체를 블록 목록으로 읽는다. 입력은 닫지 않는다.
     *
     * @param in     읽을 입력(UTF-8로 연 Reader)
     * @param source 오류 메시지에 쓸 파일명
     * @return 정상 블록과 형식 오류 목록
     * @throws IOException 입력을 읽지 못했을 때
     */
    public static Result read(Reader in, String source) throws IOException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(source, "source");
        Parser parser = new Parser(source);
        BufferedReader reader = in instanceof BufferedReader b ? b : new BufferedReader(in);
        String raw;
        int lineNo = 0;
        while ((raw = reader.readLine()) != null) {
            lineNo++;
            if (lineNo == 1 && !raw.isEmpty() && raw.charAt(0) == BOM) {
                raw = raw.substring(1);
            }
            parser.accept(raw, lineNo);
        }
        parser.finish();
        return new Result(parser.blocks, parser.problems);
    }

    /** 한 번의 읽기 상태. 블록 하나를 모으다가 다음 머리나 파일 끝에서 확정한다. */
    private static final class Parser {
        private final String source;
        private final List<Block> blocks = new ArrayList<>();
        private final List<MalformedDataException> problems = new ArrayList<>();

        /** 모으는 중인 블록. 첫 키 줄이나 머리를 만나기 전에는 null. */
        private Pending current;

        Parser(String source) {
            this.source = source;
        }

        void accept(String raw, int lineNo) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                return;
            }
            if (line.startsWith(HEADER_PREFIX)) {
                startBlock(line, lineNo);
            } else {
                addEntry(line, lineNo);
            }
        }

        void finish() {
            flush();
        }

        private void startBlock(String line, int lineNo) {
            flush();
            String[] tokens = line.substring(HEADER_PREFIX.length()).strip().split("\\s+");
            if (tokens[0].isEmpty()) {
                current = Pending.broken(lineNo);
                problem(lineNo, "블록 종류가 없습니다: " + line);
            } else if (tokens.length > 2) {
                current = Pending.broken(lineNo);
                problem(lineNo, "블록 머리는 '@종류 [아이디]' 형식이어야 합니다: " + line);
            } else {
                current = new Pending(tokens[0], tokens.length == 2 ? tokens[1] : "", lineNo);
            }
        }

        private void addEntry(String line, int lineNo) {
            if (current == null) {
                current = new Pending("", "", lineNo);  // 첫 @ 이전의 기본 블록
            }
            int sep = line.indexOf(KEY_VALUE_SEPARATOR);
            if (sep < 0) {
                current.broken = true;
                problem(lineNo, "'키=값' 형식이 아닙니다: " + line);
                return;
            }
            String key = line.substring(0, sep).strip();
            if (key.isEmpty()) {
                current.broken = true;
                problem(lineNo, "키가 비어 있습니다: " + line);
                return;
            }
            if (current.values.containsKey(key)) {
                current.broken = true;
                problem(lineNo, "같은 블록에 키 '%s'가 두 번 나옵니다".formatted(key));
                return;
            }
            current.values.put(key, unescape(line.substring(sep + 1).strip()));
        }

        private void flush() {
            if (current != null && !current.broken) {
                blocks.add(new Block(current.type, current.id, current.values, source, current.line));
            }
            current = null;
        }

        private void problem(int lineNo, String message) {
            problems.add(new MalformedDataException("%s:%d: %s".formatted(source, lineNo, message), source, lineNo));
        }
    }

    /** 모으는 중인 블록. 깨진 블록은 끝까지 키를 받되 결과에서 뺀다. */
    private static final class Pending {
        final String type;
        final String id;
        final int line;
        final Map<String, String> values = new LinkedHashMap<>();
        boolean broken;

        Pending(String type, String id, int line) {
            this.type = type;
            this.id = id;
            this.line = line;
        }

        static Pending broken(int line) {
            Pending p = new Pending("", "", line);
            p.broken = true;
            return p;
        }
    }

    /** {@code \n}을 줄바꿈으로, {@code \\}를 역슬래시로 푼다. 다른 역슬래시 조합은 그대로 둔다. */
    static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    sb.append('\\');
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
