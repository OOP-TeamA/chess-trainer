package com.oopteama.chess.ui;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.content.LessonStep;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.exercise.FeedbackLadder;
import com.oopteama.chess.exercise.Verdict;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * 레슨 화면 오른쪽의 설명·피드백·단계 이동 영역(RULE-02, RULE-03).
 *
 * <p>EDT에서 사용한다. 채점과 오답 횟수는 ExerciseController 쪽에서 받은 결과를 표시한다.
 * 보드, 화면 이동, 완료 기록은 소유하지 않는다. LessonScreen에서 이 패널을 사용하고,
 * 마지막 단계의 다음 콜백에서만 Progress.markLessonDone을 호출하도록 연결한다.
 * 아직 main에 없는 공통 UI 타입에 의존하지 않는 화면 구성 요소다.
 */
final class LessonStepPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JLabel title = new JLabel();
    private final JLabel attempts = new JLabel("오답 0회");
    private final JProgressBar progress = new JProgressBar();
    private final JTextArea prompt = textArea("lesson.prompt", 5);
    private final JTextArea feedback = textArea("lesson.feedback", 4);
    private final JTextArea help = textArea("lesson.help", 3);
    private final JButton next = new JButton("다음 단계");
    private boolean stepShown;
    private boolean advanced;

    LessonStepPanel(Runnable onNext) {
        Objects.requireNonNull(onNext, "onNext");
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(330, 540));

        title.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        attempts.setName("lesson.attempts");
        progress.setName("lesson.progress");
        progress.setStringPainted(true);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.add(title, BorderLayout.NORTH);
        header.add(progress, BorderLayout.CENTER);
        header.add(attempts, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(section("현재 단계", prompt));
        content.add(Box.createVerticalStrut(12));
        content.add(section("정답·오답 안내", feedback));
        content.add(Box.createVerticalStrut(12));
        content.add(section("도움말", help));
        add(content, BorderLayout.CENTER);

        next.setName("lesson.next");
        next.setEnabled(false);
        next.addActionListener(event -> {
            // 연속 클릭이나 늦게 도착한 동일 결과로 다음 단계가 두 번 열리지 않게 한다.
            if (!next.isEnabled() || advanced) {
                return;
            }
            advanced = true;
            next.setEnabled(false);
            onNext.run();
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.add(next);
        add(controls, BorderLayout.SOUTH);
    }

    /** 단계를 열 때 이전 단계의 결과와 버튼 상태를 초기화한다. index는 0부터다. */
    void showStep(Lesson lesson, int index) {
        Objects.requireNonNull(lesson, "lesson");
        LessonStep step = lesson.steps().get(index);
        title.setText(lesson.title());
        progress.setMaximum(lesson.steps().size());
        progress.setValue(index + 1);
        progress.setString((index + 1) + " / " + lesson.steps().size() + " 단계");
        prompt.setText(step.text());
        prompt.setCaretPosition(0);
        attempts.setText("오답 0회");
        feedback.setText("체스판에서 문제를 풀어 보세요.");
        help.setText(step.goal().inputMode() == InputMode.SQUARE
                ? "말을 옮기지 말고 목표에 맞는 칸을 선택하세요."
                : "기물을 선택한 뒤 도착할 칸을 눌러 수를 두세요.");
        next.setText(index == lesson.steps().size() - 1 ? "레슨 완료" : "다음 단계");
        next.setEnabled(false);
        advanced = false;
        stepShown = true;
    }

    /** 컨트롤러가 채점하고 사다리를 갱신한 뒤 호출한다. 이 패널은 다시 채점하지 않는다. */
    void showFeedback(Verdict verdict, FeedbackLadder ladder) {
        Objects.requireNonNull(verdict, "verdict");
        Objects.requireNonNull(ladder, "ladder");
        if (!stepShown) {
            throw new IllegalStateException("단계를 먼저 표시해야 합니다");
        }
        if (advanced) {
            return;
        }
        attempts.setText("오답 " + ladder.wrongCount() + "회");
        boolean solved = verdict instanceof Verdict.Solved;
        String message;
        if (verdict instanceof Verdict.Solved result) {
            message = result.message();
        } else if (verdict instanceof Verdict.Wrong result) {
            message = result.message();
        } else {
            throw new IllegalArgumentException("레슨은 Solved 또는 Wrong 결과를 표시합니다");
        }
        feedback.setText(message.isBlank() ? (solved ? "정답입니다." : "다시 시도해 보세요.") : message);
        feedback.setCaretPosition(0);
        help.setText(solved ? "목표를 해결했습니다. 아래 버튼으로 계속 진행하세요."
                : switch (ladder.stage()) {
                    case NONE, EXPLAIN -> "설명을 읽고 다시 풀어 보세요.";
                    case HINT -> "체스판에 표시된 힌트 칸을 참고하세요.";
                    case REVEAL -> "정답이 공개되었습니다. 직접 정답을 입력하면 다음으로 진행할 수 있습니다.";
                });
        // REVEAL은 도움 표시일 뿐 완료가 아니다. 실제 Solved 결과만 이동을 허용한다.
        next.setEnabled(solved);
    }

    private static JTextArea textArea(String name, int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setName(name);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return area;
    }

    private static JPanel section(String heading, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel(heading), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }
}
