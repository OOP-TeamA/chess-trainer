package com.oopteama.chess.proto.front2;

import java.util.*;

public final class DemoState {
    public final boolean[] doneLessons = new boolean[DemoData.LESSONS.length];
    public final LinkedHashSet<Integer> wrongQuiz = new LinkedHashSet<>();
    public final LinkedHashSet<Integer> wrongPuzzles = new LinkedHashSet<>();
    public final LinkedHashSet<Integer> solvedPuzzles = new LinkedHashSet<>();
    public int quizAttempts = 0;
    public int bestScore = 0;
    public int completedLessons() { int n=0; for (boolean b:doneLessons) if(b) n++; return n; }
}
