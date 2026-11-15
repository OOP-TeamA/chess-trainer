package com.oopteama.chess.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 설명 글 안의 용어 링크(RULE-06, GLOS-04).
 *
 * <p>문법: {@code [[핀]]}은 표시문 그대로 용어를 찾고, {@code [[sacrifice|퀸 희생]]}은 {@code |} 앞을 대상으로,
 * 뒤를 표시문으로 쓴다. 닫히지 않았거나 대상·표시문이 빈 괄호는 링크로 보지 않고 글자 그대로 둔다.
 */
public final class TermLinks {

    private static final String OPEN = "[[";
    private static final String CLOSE = "]]";
    private static final char SEPARATOR = '|';
    /** 링크 주소 접두어. 주소는 {@code term:<용어 ID>}다. */
    private static final String HREF_PREFIX = "term:";

    private TermLinks() {
    }

    /** 글을 나눈 조각. 닫힌 계층이라 switch로 빠짐없이 처리한다. */
    public sealed interface Segment {

        /**
         * 링크가 아닌 글.
         *
         * @param text 원문 그대로(이스케이프하지 않음). 비어 있지 않다
         */
        record Text(String text) implements Segment {
            /** text는 null 불가. */
            public Text {
                Objects.requireNonNull(text, "text");
            }
        }

        /**
         * 용어 링크. 대상이 실제 용어인지는 {@link Glossary#resolve}로 확인한다.
         *
         * @param target 찾을 용어의 ID나 이름
         * @param label  화면에 보일 글
         */
        record Link(String target, String label) implements Segment {
            /** 두 필드 모두 null 불가. */
            public Link {
                Objects.requireNonNull(target, "target");
                Objects.requireNonNull(label, "label");
            }
        }
    }

    /**
     * 글을 일반 글과 링크 조각으로 나눈다. 조각을 순서대로 이으면 링크 괄호를 뺀 원문이 된다.
     *
     * @param text 링크 문법이 섞인 글
     * @return 불변 목록. 빈 글이면 빈 목록
     */
    public static List<Segment> parse(String text) {
        Objects.requireNonNull(text, "text");
        List<Segment> out = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            int open = text.indexOf(OPEN, pos);
            int close = open < 0 ? -1 : text.indexOf(CLOSE, open + OPEN.length());
            if (close < 0) {
                break;
            }
            // "[[a [[b]]"처럼 여는 괄호가 겹치면 닫는 괄호에 가장 가까운 것을 짝으로 본다.
            open = text.lastIndexOf(OPEN, close - OPEN.length());
            plain.append(text, pos, open);
            Optional<Segment.Link> link = link(text.substring(open + OPEN.length(), close));
            if (link.isPresent()) {
                flush(plain, out);
                out.add(link.get());
            } else {
                plain.append(text, open, close + CLOSE.length());
            }
            pos = close + CLOSE.length();
        }
        plain.append(text, pos, text.length());
        flush(plain, out);
        return List.copyOf(out);
    }

    private static Optional<Segment.Link> link(String body) {
        int bar = body.indexOf(SEPARATOR);
        String target = (bar < 0 ? body : body.substring(0, bar)).strip();
        String label = (bar < 0 ? body : body.substring(bar + 1)).strip();
        if (target.isEmpty() || label.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Segment.Link(target, label));
    }

    private static void flush(StringBuilder plain, List<Segment> out) {
        if (plain.length() > 0) {
            out.add(new Segment.Text(plain.toString()));
            plain.setLength(0);
        }
    }

    /**
     * JEditorPane용 HTML 조각을 만든다. 용어집에서 해석되는 링크만 {@code <a href="term:용어ID">}로 감싸고,
     * 해석되지 않는 링크는 표시문만 남긴다. 글은 이스케이프하고 줄바꿈은 {@code <br>}로 바꾼다.
     *
     * @param text     링크 문법이 섞인 글
     * @param glossary 링크 대상을 찾을 용어집
     * @return {@code <html>} 태그 없는 HTML 조각
     */
    public static String toHtml(String text, Glossary glossary) {
        Objects.requireNonNull(glossary, "glossary");
        StringBuilder sb = new StringBuilder();
        for (Segment segment : parse(text)) {
            switch (segment) {
                case Segment.Text t -> sb.append(escape(t.text()));
                case Segment.Link l -> {
                    Optional<GlossaryEntry> entry = glossary.resolve(l.target());
                    if (entry.isPresent()) {
                        sb.append("<a href=\"").append(HREF_PREFIX).append(escape(entry.get().id())).append("\">")
                                .append(escape(l.label())).append("</a>");
                    } else {
                        sb.append(escape(l.label()));
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\n' -> sb.append("<br>");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
