package com.oopteama.chess.ui;

import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.Puzzle;
import com.oopteama.chess.content.TermQuestion;
import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.io.GameLibrary;
import com.oopteama.chess.progress.ProfileManager;
import com.oopteama.chess.progress.Progress;
import com.oopteama.chess.progress.Recommender;

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
    /** 번들 명국 기보와 현재 프로필의 저장 대국. */
    GameLibrary games();

    // 학습 기록
    Progress progress();
    /** 프로필 목록과 활성 프로필(표시 이름은 {@code profiles().active().name()}). */
    ProfileManager profiles();

    /** 홈 화면 추천과 주제별 정답률. 활성 프로필의 기록으로 계산한다. */
    Recommender recommender();
}