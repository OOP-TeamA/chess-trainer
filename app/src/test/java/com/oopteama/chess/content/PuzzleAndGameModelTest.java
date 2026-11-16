package com.oopteama.chess.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.notation.PgnGame;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** QUIZ-01·02, GAME-01: 퍼즐·명국 기보 모델과 난이도 표시명. */
class PuzzleAndGameModelTest {

    private static final String BACK_RANK = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1";

    private static Puzzle puzzle(Set<String> themes, List<String> solution) {
        return new Puzzle("p0001", themes, BACK_RANK, solution, "");
    }

    @ParameterizedTest
    @CsvSource({"입문, INTRO", "초급, BEGINNER", "중급, INTERMEDIATE", "상급, ADVANCED", "고급, ADVANCED"})
    void levelIsReadFromItsKoreanLabel(String label, Level expected) {
        assertEquals(expected, Level.fromLabel(label));
        assertEquals(expected, Level.fromLabel("  " + label + " "));
    }

    @Test
    void levelLabelsRoundTrip() {
        for (Level level : Level.values()) {
            assertEquals(level, Level.fromLabel(level.label()));
        }
    }

    @Test
    void unknownLevelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Level.fromLabel("초보"));
        assertThrows(IllegalArgumentException.class, () -> Level.fromLabel(""));
    }

    @Test
    void mateThemeIsAnyThemeStartingWithMate() {
        assertTrue(puzzle(Set.of("메이트인1", "백랭크"), List.of("d1d8")).isMateTheme());
        assertTrue(puzzle(Set.of("메이트인2"), List.of("a1a2", "a7a6", "a2a3")).isMateTheme());
        assertFalse(puzzle(Set.of("포크"), List.of("d1d8")).isMateTheme());
    }

    @Test
    void themesKeepFileOrderAndCollectionsAreCopied() {
        Set<String> themes = new LinkedHashSet<>(List.of("백랭크", "메이트인1"));
        List<String> solution = new ArrayList<>(List.of("d1d8"));
        Puzzle p = puzzle(themes, solution);
        themes.add("포크");
        solution.add("g8h8");

        assertEquals(List.of("백랭크", "메이트인1"), List.copyOf(p.themes()));
        assertEquals(List.of("d1d8"), p.solutionUci());
        assertThrows(UnsupportedOperationException.class, () -> p.themes().add("핀"));
        assertThrows(UnsupportedOperationException.class, () -> p.solutionUci().add("a1a2"));
    }

    @Test
    void userMoveCountSkipsOpponentReplies() {
        assertEquals(1, puzzle(Set.of("메이트인1"), List.of("d1d8")).userMoveCount());
        assertEquals(2, puzzle(Set.of("메이트인2"), List.of("a1a2", "a7a6", "a2a3")).userMoveCount());
    }

    @Test
    void puzzleNeedsThemesAndASolutionEndingWithTheUsersMove() {
        assertThrows(IllegalArgumentException.class, () -> puzzle(Set.of(), List.of("d1d8")));
        assertThrows(IllegalArgumentException.class, () -> puzzle(Set.of("백랭크"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> puzzle(Set.of("백랭크"), List.of("d1d8", "g8h8")));
    }

    @Test
    void famousGameCarriesTheParsedGame() {
        PgnGame pgn = new PgnGame(Map.of("Alias", "빈 기보"), Fen.INITIAL, List.of(), "*");
        FamousGame game = new FamousGame("empty", "빈 기보", Level.INTRO, pgn);
        assertEquals(0, game.plyCount());
        assertEquals(Level.INTRO, game.level());

        assertThrows(IllegalArgumentException.class, () -> new FamousGame(" ", "빈 기보", Level.INTRO, pgn));
        assertThrows(IllegalArgumentException.class, () -> new FamousGame("empty", "", Level.INTRO, pgn));
        assertThrows(NullPointerException.class, () -> new FamousGame("empty", "빈 기보", null, pgn));
    }
}
