package com.oopteama.chess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.content.TermQuestion;
import com.oopteama.chess.exercise.TermAnswer;
import com.oopteama.chess.exercise.TermQuizSession;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.io.GlossaryRepository;
import com.oopteama.chess.io.TermQuestionRepository;
import com.oopteama.chess.progress.AttemptStats;
import com.oopteama.chess.progress.Progress;
import com.oopteama.chess.progress.ProgressStore;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 11/13 첫 시연 절차(기능 명세 6장 공용 샘플)의 퀴즈·기록 부분을 화면 없이 재현한다.
 * "4지선다에서 파일 순서대로 1,2,3,4,2,3,1,1,1,1을 고르면 결과는 7/10입니다. … 종료 후 같은 폴더로 재실행합니다.
 * 레슨 knight 완료, 퀴즈 10회 시도·7회 정답, 정답 ID q0001~q0007, Progress.score() 17점이 유지됩니다.
 * 같은 정답 ID를 다시 맞히면 시도·정답 횟수만 늘고 점수는 유지됩니다."
 * 화면이 할 일(첫 10문항으로 세션 생성, 문항마다 기록 한 번)은 테스트가 대신한다. 레슨 부분은 KnightLessonFlowTest가 다룬다.
 */
class FirstDemoRecordTest {

    private static final String PROFILE = "default";
    private static final int QUIZ_SET_SIZE = 10;
    /** 화면의 1~4 번호로 적은 시연 답안. */
    private static final int[] PICKS = {1, 2, 3, 4, 2, 3, 1, 1, 1, 1};

    @TempDir
    Path userDir;

    @Test
    void quizResultAndScoreSurviveRestart() throws IOException {
        DataPaths paths = new DataPaths(userDir);
        List<TermQuestion> questions = bundledQuestions(paths);
        ProgressStore store = new ProgressStore(paths);
        Progress progress = store.load(PROFILE);
        progress.addChangeListener(() -> save(store, progress)); // AppContext가 연결할 자동 저장

        progress.markLessonDone("knight");
        TermQuizSession session = new TermQuizSession(questions.subList(0, QUIZ_SET_SIZE));
        for (int pick : PICKS) {
            TermAnswer answer = session.submit(pick - 1);
            progress.recordTermQuiz(answer.questionId(), answer.correct());
            session.next();
        }
        assertEquals(7, session.correctCount());
        assertEquals(10, session.totalCount());

        Progress restarted = store.load(PROFILE);
        assertEquals(Set.of("knight"), restarted.lessonsDone());
        assertEquals(new AttemptStats(10, 7), restarted.termQuizAttempts());
        assertEquals(Set.of("q0001", "q0002", "q0003", "q0004", "q0005", "q0006", "q0007"),
                restarted.termQuizCorrectIds());
        assertEquals(17, restarted.score());

        restarted.recordTermQuiz("q0001", true);
        assertEquals(new AttemptStats(11, 8), restarted.termQuizAttempts());
        assertEquals(17, restarted.score());
    }

    private static List<TermQuestion> bundledQuestions(DataPaths paths) throws IOException {
        Glossary glossary;
        try (Reader in = paths.openResource("data/glossary.txt")) {
            glossary = new Glossary(new GlossaryRepository().loadAll(in, "glossary.txt"));
        }
        try (Reader in = paths.openResource("data/term-quiz.txt")) {
            return new TermQuestionRepository(glossary).loadAll(in, "term-quiz.txt");
        }
    }

    private static void save(ProgressStore store, Progress progress) {
        try {
            store.save(PROFILE, progress);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
