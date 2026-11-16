package com.oopteama.chess.ui;

import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.content.TermQuestion;
import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.progress.Progress;

import java.util.List;

public interface Services {

    // 체스 규칙
    RuleSet standardRules();
    RuleSet sandboxRules();

    // 학습 콘텐츠
    List<Lesson> lessons();
    /** data/puzzles.txt의 퍼즐(파일 순서). 파일이 아직 없으면 빈 목록. */
    List<Puzzle> puzzles();
    List<TermQuestion> termQuestions();
    Glossary glossary();

    // 학습 기록
    Progress progress();
}