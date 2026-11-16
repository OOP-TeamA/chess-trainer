package com.oopteama.chess.io;

import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 퍼즐 파일 {@code data/puzzles.txt}의 {@code @puzzle <id>} 블록 하나를 퍼즐 하나로 읽는다(QUIZ-01).
 * 잘못된 퍼즐은 그 퍼즐만 건너뛰고 {@code 파일명:행번호: 설명}을 경고 로그에 남긴다(SYS-03).
 * <ul>
 *   <li>ID는 영문 소문자·숫자·{@code -}·{@code _}. 같은 ID가 또 나오면 처음 유효한 퍼즐만 남긴다</li>
 *   <li>theme은 쉼표 목록이며 {@link Puzzle#THEMES}에 있는 주제만 쓴다</li>
 *   <li>fen은 {@link StandardRules#validate}를 통과해야 한다(CORE-09)</li>
 *   <li>solution은 공백으로 구분한 UCI 수열(상대 응수 포함)이며 모든 수가 순서대로 합법이고 사용자 수로 끝난다</li>
 *   <li>메이트 주제면 마지막 국면이 체크메이트여야 하고, 메이트인N이면 사용자 수가 N개여야 한다</li>
 *   <li>explain은 선택</li>
 * </ul>
 * ID 중복은 파일 전체를 봐야 알 수 있어 {@link TermQuestionRepository}처럼 블록 묶음을 한 번에 다룬다.
 */
public final class PuzzleRepository extends BlockRepository<Puzzle> {

    private static final Logger LOG = Logger.getLogger(PuzzleRepository.class.getName());

    private static final String PUZZLE = "puzzle";
    private static final Pattern MATE_IN = Pattern.compile("메이트인(\\d+)");
    private static final StandardRules STANDARD = new StandardRules();

    @Override
    protected List<Puzzle> assemble(List<Block> blocks) {
        Map<String, Puzzle> byId = new LinkedHashMap<>();
        for (Block b : blocks) {
            try {
                Puzzle p = puzzle(b);
                if (byId.putIfAbsent(p.id(), p) != null) {
                    throw BlockChecks.error(b, "퍼즐 ID '" + p.id() + "'가 중복되어 뒤의 퍼즐을 버립니다");
                }
            } catch (MalformedDataException e) {
                LOG.warning(e.getMessage());
            }
        }
        return new ArrayList<>(byId.values());
    }

    private static Puzzle puzzle(Block b) throws MalformedDataException {
        if (!b.type().equals(PUZZLE)) {
            throw BlockChecks.error(b, "퍼즐 파일에 쓸 수 없는 블록입니다: "
                    + (b.type().isEmpty() ? "@puzzle 앞의 키" : "@" + b.type()));
        }
        String id = BlockChecks.id(b, "퍼즐");
        String where = "퍼즐 " + id;
        Set<String> themes = themes(b, where);
        String fen = b.require("fen");
        Position start = BlockChecks.fen(b, where, "fen");
        List<String> problems = STANDARD.validate(start);
        if (!problems.isEmpty()) {
            throw BlockChecks.error(b, where + ": 표준 규칙에 맞지 않는 국면입니다: " + String.join(" ", problems));
        }
        List<String> solution = List.of(b.require("solution").strip().toLowerCase(Locale.ROOT).split("\\s+"));
        if (solution.size() % 2 == 0) {
            throw BlockChecks.error(b, where + ": solution은 사용자 수로 끝나야 합니다(상대 응수 포함 홀수 개): "
                    + solution.size() + "개");
        }
        Position end = playOut(b, where, start, solution);
        Puzzle puzzle = new Puzzle(id, themes, fen, solution, b.get("explain", ""));
        checkMateThemes(b, where, puzzle, end);
        return puzzle;
    }

    private static Set<String> themes(Block b, String where) throws MalformedDataException {
        b.require("theme");
        Set<String> out = new LinkedHashSet<>();
        for (String theme : b.list("theme")) {
            if (!Puzzle.THEMES.contains(theme)) {
                throw BlockChecks.error(b, where + ": 정해진 주제가 아닙니다: '" + theme + "' (가능: "
                        + String.join(", ", Puzzle.THEMES) + ")");
            }
            out.add(theme);
        }
        if (out.isEmpty()) {
            throw BlockChecks.error(b, where + ": theme이 비었습니다");
        }
        return out;
    }

    /** 정답 수열을 순서대로 두며 합법인지 확인하고 마지막 국면을 돌려준다. */
    private static Position playOut(Block b, String where, Position start, List<String> solution)
            throws MalformedDataException {
        Position pos = start.copy();
        for (int i = 0; i < solution.size(); i++) {
            Optional<Move> move = STANDARD.fromUci(pos, solution.get(i));
            if (move.isEmpty()) {
                throw BlockChecks.error(b, where + ": solution의 %d번째 수 %s는 이 국면에서 둘 수 없습니다"
                        .formatted(i + 1, solution.get(i)));
            }
            pos.make(move.get());
        }
        return pos;
    }

    private static void checkMateThemes(Block b, String where, Puzzle puzzle, Position end)
            throws MalformedDataException {
        if (!puzzle.isMateTheme()) {
            return;
        }
        if (!(STANDARD.inCheck(end) && STANDARD.legalMoves(end).isEmpty())) {
            throw BlockChecks.error(b, where + ": 메이트 주제인데 정답 수열의 마지막 국면이 체크메이트가 아닙니다");
        }
        for (String theme : puzzle.themes()) {
            Matcher m = MATE_IN.matcher(theme);
            if (m.matches() && Integer.parseInt(m.group(1)) != puzzle.userMoveCount()) {
                throw BlockChecks.error(b, where + ": %s인데 사용자 수가 %d개입니다"
                        .formatted(theme, puzzle.userMoveCount()));
            }
        }
    }
}
