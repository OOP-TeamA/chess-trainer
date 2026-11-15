package com.oopteama.chess.ui;

import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.content.Lesson;
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
    List<TermQuestion> termQuestions();
    Glossary glossary();

    // 학습 기록
    Progress progress();
}