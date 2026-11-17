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
 * UI-백엔드 분리 및 패키지 계층 의존성 검사.
 *
 * <ul>
 *   <li>{@code api}: JDK 표준 라이브러리만 의존한다.</li>
 *   <li>{@code nav}: {@code api}만 참조할 수 있다.</li>
 *   <li>{@code backend.core}: JDK 표준 라이브러리만 의존한다(UI·api 비의존).</li>
 *   <li>{@code backend.game}: {@code api}와 {@code backend.core}만 참조할 수 있다.</li>
 *   <li>{@code ui}: {@code api}, {@code nav}와 Swing/AWT만 참조할 수 있다({@code backend.*} 금지).</li>
 *   <li>{@code app}: 조립 루트로서 전체를 연결하되 Swing은 {@code SwingUtilities}만 쓴다.</li>
 * </ul>
 */
class ArchitectureTest {

    private static final String BASE = "com.oopteama.chess";
    private static final String ROOT = "";
    private static final String APP = "app";
    private static final String APP_GUI_CLASS = "javax.swing.SwingUtilities";
    private static final List<String> GUI_PREFIXES = List.of("javax.swing", "java.awt");

    private static final Map<String, Set<String>> ALLOWED_DEPS = Map.of(
            "api", Set.of(),
            "nav", Set.of("api"),
            "backend.core", Set.of(),
            "backend.game", Set.of("api", "backend.core"),
            "ui", Set.of("api", "nav"),
            "app", Set.of("api", "backend.core", "backend.game", "nav", "ui")
    );

    private static final Set<String> GUI_ALLOWED = Set.of("ui", ROOT);

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+(?:\\.\\*)?)\\s*;");

    @TempDir
    Path tmp;

    @Test
    void 실제_소스는_UI_백엔드_분리_규칙을_지킨다() throws IOException {
        List<String> violations = violations(mainSourceRoot());
        assertTrue(violations.isEmpty(), () -> "아키텍처 규칙 위반:\n" + String.join("\n", violations));
    }

    @Test
    void ui가_backend를_참조하거나_api가_외부를_참조하면_잡는다() throws IOException {
        source("ui/BadScreen.java", "ui", "import com.oopteama.chess.backend.core.ChessCore;");
        source("api/BadApi.java", "api", "import com.oopteama.chess.nav.Route;");
        source("backend/core/BadCore.java", "backend.core", "import com.oopteama.chess.api.GameSession;");
        source("backend/game/BadGame.java", "backend.game", "import javax.swing.JPanel;");
        source("nav/BadRoute.java", "nav", "import com.oopteama.chess.backend.game.CoreGameSession;");
        source("app/BadApp.java", "app", "import javax.swing.JFrame;");

        List<String> v = violations(tmp);
        assertEquals(6, v.size(), String.join("\n", v));
    }

    private static Path mainSourceRoot() {
        for (Path candidate : List.of(Path.of("src", "main", "java"), Path.of("app", "src", "main", "java"))) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("src/main/java를 찾지 못했습니다");
    }

    static List<String> violations(Path root) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        List<String> out = new ArrayList<>();
        for (Path file : files) {
            check(root.relativize(file).toString().replace('\\', '/'),
                    Files.readAllLines(file, StandardCharsets.UTF_8), out);
        }
        return out;
    }

    private static void check(String name, List<String> lines, List<String> out) {
        String layer = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher pkg = PACKAGE.matcher(line);
            if (layer == null && pkg.find()) {
                layer = layerOf(pkg.group(1));
                if (layer == null) {
                    out.add("%s:%d: %s 밖의 패키지 %s".formatted(name, i + 1, BASE, pkg.group(1)));
                    return;
                }
                if (!layer.equals(ROOT) && !ALLOWED_DEPS.containsKey(layer)) {
                    out.add("%s:%d: 허용되지 않은 패키지 계층 '%s'".formatted(name, i + 1, layer));
                    return;
                }
                continue;
            }
            Matcher imp = IMPORT.matcher(line);
            if (!imp.find()) {
                continue;
            }
            String from = layer == null ? ROOT : layer;
            String target = imp.group(1);

            if (isGui(target) && !GUI_ALLOWED.contains(from)) {
                if (!from.equals(APP)) {
                    out.add("%s:%d: Swing/AWT는 ui에서만 쓸 수 있습니다(%s -> %s)".formatted(name, i + 1, from, target));
                } else if (!isClassOrMember(target, APP_GUI_CLASS)) {
                    out.add("%s:%d: app은 SwingUtilities만 쓸 수 있습니다(%s)".formatted(name, i + 1, target));
                }
            }

            String to = layerOf(target.endsWith(".*") ? target.substring(0, target.length() - 2) : target);
            if (to == null || from.equals(ROOT) || to.equals(from)) {
                continue;
            }
            if (to.equals(ROOT)) {
                out.add("%s:%d: 루트 진입점을 참조할 수 없습니다(%s -> %s)".formatted(name, i + 1, from, target));
            } else if (!ALLOWED_DEPS.getOrDefault(from, Set.of()).contains(to)) {
                out.add("%s:%d: 금지된 의존성 %s -> %s (%s)".formatted(name, i + 1, from, to, target));
            }
        }
    }

    private static boolean isGui(String target) {
        return GUI_PREFIXES.stream().anyMatch(p -> isClassOrMember(target, p));
    }

    private static boolean isClassOrMember(String target, String name) {
        return target.equals(name) || target.startsWith(name + ".");
    }

    private static String layerOf(String qualified) {
        if (qualified.equals(BASE)) {
            return ROOT;
        }
        if (!qualified.startsWith(BASE + ".")) {
            return null;
        }
        String rest = qualified.substring(BASE.length() + 1);
        String[] parts = rest.split("\\.");
        if (parts.length == 0 || Character.isUpperCase(parts[0].charAt(0))) {
            return ROOT;
        }
        if (parts[0].equals("backend") && parts.length >= 2 && !Character.isUpperCase(parts[1].charAt(0))) {
            return parts[0] + "." + parts[1];
        }
        return parts[0];
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
