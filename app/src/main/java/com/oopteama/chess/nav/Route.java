package com.oopteama.chess.nav;

import com.oopteama.chess.game.AiSetup;

public sealed interface Route {

    // 기본 화면
    record Home() implements Route { }

    // 룰학습
    record Lessons() implements Route { }
    record Lesson(String lessonId, int step) implements Route { }

    // 체스 퀴즈
    record Puzzles() implements Route { }
    record Puzzle(String theme, String puzzleId) implements Route { }

    // 용어 퀴즈
    record TermQuiz() implements Route { }

    // 대국
    record LocalGame() implements Route { }
    record NewGame() implements Route { }
    record AiGame(AiSetup setup) implements Route { }

    // 기보 및 용어집
    record Library() implements Route { }
    record Replay(String gameId, boolean saved) implements Route { }
    record Glossary(String termId) implements Route { }

    // 기타 화면
    record Favorites() implements Route { }
    record WrongNotes() implements Route { }
    record Profiles() implements Route { }
    record Ranking() implements Route { }
    record LearningRecord() implements Route { }
    record Prefs() implements Route { }
}