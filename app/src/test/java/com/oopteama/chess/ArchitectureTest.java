package com.oopteama.chess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 의존 층 검사(인터페이스 명세 2장, 기능 명세 9장).
 *
 * <p>{@code app/src/main/java}의 {@code import com.oopteama.chess.X}(정적 import·와일드카드 포함)를 패키지 기준으로 읽어
 * 다른 계열의 동일 층·상위 층 참조를 실패로 처리한다. 같은 계열의 하위 패키지(예: {@code core}↔{@code core.piece})는 허용한다.
 * Swing({@code javax.swing})은 {@code ui}, {@code app}과 루트 진입점에서만 허용한다.
 * 루트 패키지({@code Main})는 진입점으로 취급해 층 검사에서 제외하며, 다른 패키지가 루트를 참조하면 실패다.
 */
class ArchitectureTest {

    private static final String BASE = "com.oopteama.chess";

    /** 계열별 층 번호. 다른 계열은 자기보다 번호가 낮은 층만 참조한다. */
    private static final Map<String, Integer> LAYERS = Map.ofEntries(
            Map.entry("core", 0),
            Map.entry("notation", 1),
            Map.entry("content", 2),
            Map.entry("ai", 2),
            Map.entry("exercise", 3),
            Map.entry("replay", 3),
            Map.entry("io", 3),
            Map.entry("game", 4),
            Map.entry("nav", 5),
            Map.entry("progress", 6),
            Map.entry("ui", 7),
            Map.entry("app", 8));

    /** 루트 진입점을 나타내는 계열 이름. */
    private static final String ROOT = "";

    /** Swing을 쓸 수 있는 계열. */
    private static final Set<String> SWING_ALLOWED = Set.of("ui", "app", ROOT);
    private static final String SWING_PREFIX = "javax.swing";

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+(?:\\.\\*)?)\\s*;");

    @TempDir
    Path tmp;

    @Test
    void 실제_소스는_층_규칙을_지킨다() throws IOException {
        List<String> violations = violations(mainSourceRoot());
        assertTrue(violations.isEmpty(), () -> "의존 층 위반:\n" + String.join("\n", violations));
    }

    @Test
    void 하향_참조와_같은_계열_하위_패키지는_허용한다() throws IOException {
        source("progress/A.java", "progress", "import com.oopteama.chess.nav.Route;", "import com.oopteama.chess.io.*;");
        source("app/B.java", "app", "import com.oopteama.chess.ui.Screen;", "import javax.swing.JFrame;");
        source("core/piece/C.java", "core.piece", "import com.oopteama.chess.core.Position;");
        source("core/D.java", "core", "import com.oopteama.chess.core.piece.Piece;");
        source("io/E.java", "io", "import static com.oopteama.chess.core.Side.WHITE;", "import com.oopteama.chess.ai.Difficulty;");
        source("Main.java", "", "import com.oopteama.chess.app.ChessApp;", "import javax.swing.SwingUtilities;");
        assertEquals(List.of(), violations(tmp));
    }

    @Test
    void 상향_동일층_루트_참조와_로직의_Swing을_잡는다() throws IOException {
        source("core/A.java", "core", "import com.oopteama.chess.notation.San;");
        source("io/B.java", "io", "import com.oopteama.chess.exercise.Exercise;");
        source("content/C.java", "content", "import static com.oopteama.chess.ai.Difficulty.INTRO;");
        source("game/D.java", "game", "import com.oopteama.chess.ui.*;");
        source("progress/E.java", "progress", "import javax.swing.JPanel;");
        source("nav/F.java", "nav", "import com.oopteama.chess.Main;");
        source("unknown/G.java", "unknown", "import com.oopteama.chess.core.Move;");
        List<String> v = violations(tmp);
        assertEquals(7, v.size(), String.join("\n", v));
        assertTrue(v.stream().anyMatch(s -> s.contains("A.java") && s.contains("notation")));
        assertTrue(v.stream().anyMatch(s -> s.contains("B.java") && s.contains("exercise")));
        assertTrue(v.stream().anyMatch(s -> s.contains("C.java") && s.contains("ai")));
        assertTrue(v.stream().anyMatch(s -> s.contains("D.java") && s.contains("ui")));
        assertTrue(v.stream().anyMatch(s -> s.contains("E.java") && s.contains("Swing")));
        assertTrue(v.stream().anyMatch(s -> s.contains("F.java") && s.contains("루트")));
        assertTrue(v.stream().anyMatch(s -> s.contains("G.java") && s.contains("unknown")));
    }

    /** 검사 대상 소스 폴더. Gradle 테스트의 작업 폴더는 app 모듈 폴더다. */
    private static Path mainSourceRoot() {
        for (Path candidate : List.of(Path.of("src", "main", "java"), Path.of("app", "src", "main", "java"))) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("src/main/java를 찾지 못했습니다: " + Path.of("").toAbsolutePath());
    }

    /** 폴더 아래 모든 .java 파일의 위반 목록. 항목은 "파일:행: 설명". */
    static List<String> violations(Path root) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        List<String> out = new ArrayList<>();
        for (Path file : files) {
            check(root.relativize(file).toString().replace('\\', '/'), Files.readAllLines(file, StandardCharsets.UTF_8), out);
        }
        return out;
    }

    private static void check(String name, List<String> lines, List<String> out) {
        String family = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher pkg = PACKAGE.matcher(line);
            if (family == null && pkg.find()) {
                family = familyOf(pkg.group(1));
                if (family == null) {
                    out.add("%s:%d: %s 밖의 패키지 %s".formatted(name, i + 1, BASE, pkg.group(1)));
                    return;
                }
                if (!family.equals(ROOT) && !LAYERS.containsKey(family)) {
                    out.add("%s:%d: 층이 정해지지 않은 계열 '%s'".formatted(name, i + 1, family));
                    return;
                }
                continue;
            }
            Matcher imp = IMPORT.matcher(line);
            if (!imp.find()) {
                continue;
            }
            String from = family == null ? ROOT : family;
            String target = imp.group(1);
            if (target.startsWith(SWING_PREFIX) && !SWING_ALLOWED.contains(from)) {
                out.add("%s:%d: Swing은 ui·app에서만 쓸 수 있습니다(%s → %s)".formatted(name, i + 1, from, target));
            }
            String to = familyOf(target.endsWith(".*") ? target.substring(0, target.length() - 2) : target);
            if (to == null || from.equals(ROOT) || to.equals(from)) {
                continue;  // 프로젝트 밖, 루트 진입점, 같은 계열
            }
            if (to.equals(ROOT)) {
                out.add("%s:%d: 루트 진입점을 참조할 수 없습니다(%s → %s)".formatted(name, i + 1, from, target));
            } else if (!LAYERS.containsKey(to)) {
                out.add("%s:%d: 층이 정해지지 않은 계열 '%s' 참조(%s)".formatted(name, i + 1, to, target));
            } else if (LAYERS.get(to) >= LAYERS.get(from)) {
                out.add("%s:%d: %s(L%d) → %s(L%d) 참조는 하향이 아닙니다(%s)".formatted(
                        name, i + 1, from, LAYERS.get(from), to, LAYERS.get(to), target));
            }
        }
    }

    /**
     * 이름의 계열(BASE 바로 아래 첫 패키지). BASE 밖이면 null, 루트 패키지나 루트의 클래스면 {@link #ROOT}.
     * 대문자로 시작하는 첫 조각은 클래스로 본다.
     */
    private static String familyOf(String qualified) {
        if (qualified.equals(BASE)) {
            return ROOT;
        }
        if (!qualified.startsWith(BASE + ".")) {
            return null;
        }
        String rest = qualified.substring(BASE.length() + 1);
        int dot = rest.indexOf('.');
        String first = dot < 0 ? rest : rest.substring(0, dot);
        return Character.isUpperCase(first.charAt(0)) ? ROOT : first;
    }

    private void source(String relative, String pkgSuffix, String... imports) throws IOException {
        Path file = tmp.resolve(relative);
        Files.createDirectories(file.getParent());
        String pkg = pkgSuffix.isEmpty() ? BASE : BASE + "." + pkgSuffix;
        StringBuilder sb = new StringBuilder("package ").append(pkg).append(";\n\n");
        for (String imp : imports) {
            sb.append(imp).append('\n');
        }
        String cls = file.getFileName().toString().replace(".java", "");
        sb.append("\nclass ").append(cls).append(" { }\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }
}
