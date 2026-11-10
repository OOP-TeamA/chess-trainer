package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Polygon;
import java.awt.geom.Path2D;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;
import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.progress.AttemptStats;
import com.oopteama.chess.progress.Progress;


public final class HomeScreen extends JPanel implements Screen {

    private final Navigator navigator;
    private final Services services;
    private final Map<String, JLabel> metricLabels = new HashMap<>();
    private final Map<String, JProgressBar> metricBars = new HashMap<>();
    private final Runnable progressChangeListener = this::refreshSummary;

    private JLabel resumeTitle;
    private JLabel resumeDescription;
    private JLabel lessonProgressText;
    private JProgressBar lessonProgressBar;
    private JButton continueButton;
    private Route recommendedRoute = new Route.Lessons();
    private Progress observedProgress;


    public HomeScreen(Navigator navigator, Services services) {

        this.navigator = Objects.requireNonNull(navigator);
        this.services = Objects.requireNonNull(services);

        setLayout(new BorderLayout());

        setBackground(Theme.WINDOW);

        setBorder(
                new EmptyBorder(24, 24, 24, 24)
        );

        // 전체 홈 화면 내용
        JPanel content = new JPanel();

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        content.setBackground(Theme.WINDOW);

        // 1. 메인 배너
        content.add(createHero());

        content.add(Box.createVerticalStrut(18));

        // 2. 이어서 하기
        content.add(createContinueCard());

        content.add(Box.createVerticalStrut(22));

        // 3. 기능별 카드
        content.add(createFeatureCards());
        add(content, BorderLayout.CENTER);
        refreshSummary();
    }


    private void refreshSummary() {
        Progress progress = services.progress();
        List<Lesson> lessons = services.lessons() == null
                ? List.of()
                : services.lessons();

        if (progress == null) {
            updateMetric("lessons", "기록 연동 대기", 0);
            updateMetric("quiz", "기록 연동 대기", 0);
            updateMetric("library", "기록 API 연동 대기", 0);
            updateMetric("ai", "기록 API 연동 대기", 0);
            if (resumeTitle != null) {
                resumeTitle.setText("학습 기록을 불러올 수 없습니다");
                resumeDescription.setText("Progress 서비스 연결 후 추천을 표시합니다.");
                lessonProgressBar.setValue(0);
                lessonProgressText.setText("기록 대기");
                recommendedRoute = new Route.Lessons();
            }
            return;
        }

        int totalLessons = lessons.size();
        int completedLessons = progress.lessonsDone().size();
        int lessonPercent = totalLessons == 0
                ? 0
                : Math.min(100, completedLessons * 100 / totalLessons);
        updateMetric(
                "lessons",
                totalLessons == 0
                        ? "레슨 콘텐츠 없음"
                        : completedLessons + " / " + totalLessons + " 레슨",
                lessonPercent
        );

        AttemptStats quizStats = progress.termQuizAttempts();
        int attempted = quizStats == null ? 0 : quizStats.attempted();
        int correct = quizStats == null ? 0 : quizStats.solved();
        int accuracy = attempted == 0 ? 0 : Math.min(100, correct * 100 / attempted);
        updateMetric(
                "quiz",
                attempted == 0
                        ? "아직 퀴즈 기록 없음"
                        : attempted + "문제, 정답률 " + accuracy + "%",
                accuracy
        );

        // GameLibrary 데이터 접근 API가 Services에 추가되면 실제 기보 수치로 연결합니다.
        updateMetric("library", "기보 기록 API 연동 대기", 0);
        // AI 대국 기록은 AI 서비스 계약 통합 후 표시합니다.
        updateMetric("ai", "AI 기록 API 연동 대기", 0);

        if (resumeTitle != null) {
            Lesson nextLesson = lessons.stream()
                    .sorted(Comparator.comparingInt(Lesson::order))
                    .filter(lesson -> !progress.lessonsDone().contains(lesson.id()))
                    .findFirst()
                    .orElse(null);

            if (nextLesson != null) {
                resumeTitle.setText("레슨 " + nextLesson.order() + ". " + nextLesson.title());
                resumeDescription.setText("다음 추천 · 미완료 첫 레슨");
                recommendedRoute = new Route.Lesson(nextLesson.id(), 0);
            } else if (!lessons.isEmpty()) {
                resumeTitle.setText("모든 레슨을 완료했어요");
                resumeDescription.setText("다른 학습 메뉴에서 계속 연습해 보세요.");
                recommendedRoute = new Route.Lessons();
            } else {
                resumeTitle.setText("레슨 콘텐츠를 기다리고 있어요");
                resumeDescription.setText("레슨이 추가되면 다음 학습을 추천합니다.");
                recommendedRoute = new Route.Lessons();
            }

            lessonProgressBar.setValue(lessonPercent);
            lessonProgressText.setText(totalLessons == 0
                    ? "0 / 0 레슨"
                    : completedLessons + " / " + totalLessons + " 레슨 · "
                            + lessonPercent + "%");
        }
    }


    private void updateMetric(String key, String text, int progress) {
        JLabel label = metricLabels.get(key);
        if (label != null) {
            label.setText(text);
        }
        JProgressBar bar = metricBars.get(key);
        if (bar != null) {
            bar.setValue(Math.max(0, Math.min(100, progress)));
        }
    }


    // =================================================
    // 1. 상단 메인 배너
    // =================================================

    private JPanel createHero() {

        RoundedPanel panel = new RoundedPanel(
                new Color(0x48301F),
                Theme.LINE,
                18
        );

        panel.setLayout(
                new BorderLayout(15, 10)
        );

        panel.setBorder(
                new EmptyBorder(8, 25, 8, 25)
        );

        panel.setPreferredSize(
                new Dimension(800, 210)
        );

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 220)
        );

        // 왼쪽 제목 영역
        JPanel textPanel = new JPanel();

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        textPanel.setOpaque(false);

        JLabel title = new JLabel(
                "<html>체스로 배우는<br>사고력, 한 수씩</html>"
        );

        title.setFont(
                Theme.title(Theme.FONT_36)
        );

        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(
                "<html>기본부터 AI 대전까지,<br>"
                        + "체계적으로 배우는 체스 학습 프로그램</html>"
        );

        subtitle.setFont(
                Theme.body(Theme.FONT_12, 0)
        );

        subtitle.setForeground(Theme.MUTED);

        textPanel.add(title);

        textPanel.add(Box.createVerticalStrut(10));

        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.CENTER);


        // 오른쪽 체스 기물 장식
        JPanel piecesPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        10,
                        10
                )
        );

        piecesPanel.setOpaque(false);

        JLabel pawn = createPieceLabel("♟", 48);
        JLabel bishop = createPieceLabel("♝", 60);
        JLabel king = createPieceLabel("♚", 86);
        JLabel knight = createPieceLabel("♞", 76);
        JLabel pawn2 = createPieceLabel("♟", 48);

        piecesPanel.add(pawn);
        piecesPanel.add(bishop);
        piecesPanel.add(king);
        piecesPanel.add(knight);
        piecesPanel.add(pawn2);

        panel.add(piecesPanel, BorderLayout.EAST);


        // 로컬 대국 바로가기
        JButton localGameButton = createOutlineButton(
                "친구와 대국하기"
        );

        localGameButton.addActionListener(e ->
                navigator.go(new Route.LocalGame())
        );

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );

        actionPanel.setOpaque(false);

        actionPanel.add(localGameButton);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }


    // 체스 기물 장식 생성
    private JLabel createPieceLabel(
            String symbol,
            int size
    ) {

        JLabel label = new JLabel(symbol);

        label.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        size
                )
        );

        label.setForeground(Theme.ACCENT);

        label.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        return label;
    }


    // =================================================
    // 2. 이어서 하기 카드
    // =================================================

    private JPanel createContinueCard() {

        RoundedPanel panel = new RoundedPanel(
                Theme.PANEL,
                Theme.LINE,
                15
        );

        panel.setLayout(
                new BorderLayout(15, 10)
        );

        panel.setBorder(
                new EmptyBorder(15, 18, 15, 18)
        );

        panel.setPreferredSize(new Dimension(800, 155));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));


        // 왼쪽 학습 정보
        JPanel infoPanel = new JPanel();

        infoPanel.setLayout(
                new BoxLayout(
                        infoPanel,
                        BoxLayout.Y_AXIS
                )
        );

        infoPanel.setOpaque(false);


        JLabel badge = new JLabel("이어서 하기");

        badge.setFont(
                Theme.body(Theme.FONT_12, 1)
        );

        badge.setForeground(Theme.ACCENT);
        badge.setOpaque(true);
        badge.setBackground(Theme.RAISED);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);


        resumeTitle = new JLabel("학습 기록을 불러오는 중");

        resumeTitle.setFont(
                Theme.title(Theme.FONT_28)
        );

        resumeTitle.setForeground(Theme.TEXT);
        resumeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);


        resumeDescription = new JLabel("다음 학습을 확인하고 있어요.");

        resumeDescription.setFont(
                Theme.body(Theme.FONT_12, 0)
        );

        resumeDescription.setForeground(Theme.MUTED);
        resumeDescription.setAlignmentX(Component.LEFT_ALIGNMENT);


        infoPanel.add(badge);

        infoPanel.add(Box.createVerticalStrut(7));

        infoPanel.add(resumeTitle);

        infoPanel.add(Box.createVerticalStrut(6));

        infoPanel.add(resumeDescription);
        infoPanel.add(Box.createVerticalStrut(9));

        JPanel progressRow = new JPanel(new BorderLayout(12, 0));
        progressRow.setOpaque(false);
        progressRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        lessonProgressBar = new JProgressBar(0, 100);
        lessonProgressBar.setBorderPainted(false);
        lessonProgressBar.setBackground(Theme.RAISED);
        lessonProgressBar.setForeground(Theme.ACCENT);
        lessonProgressBar.setPreferredSize(new Dimension(300, 12));
        lessonProgressText = new JLabel("기록 대기");
        lessonProgressText.setFont(Theme.body(Theme.FONT_14, 0));
        lessonProgressText.setForeground(Theme.MUTED);
        progressRow.add(lessonProgressBar, BorderLayout.CENTER);
        progressRow.add(lessonProgressText, BorderLayout.EAST);
        infoPanel.add(progressRow);


        // 오른쪽 버튼
        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        8,
                        8
                )
        );

        buttonPanel.setOpaque(false);


        continueButton = createAccentButton(
                "▶  레슨 이어 하기"
        );

        continueButton.addActionListener(e -> navigator.go(recommendedRoute));


        JButton listButton = createOutlineButton(
                "☷  레슨 목록"
        );

        listButton.addActionListener(e ->
                navigator.go(new Route.Lessons())
        );


        buttonPanel.add(continueButton);

        buttonPanel.add(listButton);


        panel.add(infoPanel, BorderLayout.CENTER);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }


    // =================================================
    // 4. 기능별 카드
    // =================================================

    private JPanel createFeatureCards() {

        JPanel panel = new JPanel(
                new GridLayout(1, 4, 13, 0)
        );

        panel.setOpaque(false);

        panel.setPreferredSize(
                new Dimension(800, 315)
        );

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 330)
        );


        // 룰학습
        panel.add(
                createFeatureCard(
                        "lessons",
                        "BOOK",
                        "룰학습",
                        "체스의 기본 규칙과\n핵심 개념 배우기",
                        "기록 불러오는 중",
                        0,
                        new Color(0xF3DCC0),
                        new Color(0x754B2B),
                        () -> navigator.go(new Route.Lessons())
                )
        );


        // 체스 퀴즈
        panel.add(
                createFeatureCard(
                        "quiz",
                        "QUIZ",
                        "체스 퀴즈",
                        "풀면서 익히는\n실전 감각",
                        "기록 불러오는 중",
                        0,
                        new Color(0xDCE8F8),
                        new Color(0x315D91),
                        () -> navigator.go(new Route.Puzzles())
                )
        );


        // 명국 기보
        panel.add(
                createFeatureCard(
                        "library",
                        "CROWN",
                        "명국 기보",
                        "명국으로 배우는\n전략과 전술",
                        "기보 기록 API 연동 대기",
                        0,
                        new Color(0xE2EDD9),
                        new Color(0x527448),
                        () -> navigator.go(new Route.Library())
                )
        );


        // AI 대전
        panel.add(
                createFeatureCard(
                        "ai",
                        "ROBOT",
                        "AI 대전",
                        "나만의 난이도로\n대국 연습하기",
                        "AI 기록 API 연동 대기",
                        0,
                        new Color(0xEAE0F6),
                        new Color(0x70509A),
                        () -> navigator.go(new Route.NewGame())
                )
        );


        return panel;
    }


    // 개별 기능 카드
    private JPanel createFeatureCard(
            String metricKey,
            String icon,
            String title,
            String description,
            String statistic,
            int progressValue,
            Color background,
            Color foreground,
            Runnable action
    ) {

        RoundedPanel card = new RoundedPanel(
                background,
                background.darker(),
                16
        );

        card.setLayout(
                new BorderLayout(5, 10)
        );

        card.setBorder(
                new EmptyBorder(15, 12, 15, 12)
        );


        // 아이콘과 제목
        JPanel topPanel = new JPanel();

        topPanel.setLayout(
                new BoxLayout(
                        topPanel,
                        BoxLayout.Y_AXIS
                )
        );

        topPanel.setOpaque(false);


        JLabel iconLabel = new JLabel(new FeatureIcon(icon, foreground));

        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(
                Theme.title(Theme.FONT_28)
        );

        titleLabel.setForeground(foreground);

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        topPanel.add(iconLabel);

        topPanel.add(Box.createVerticalStrut(8));

        topPanel.add(titleLabel);


        // 설명
        JLabel descriptionLabel = new JLabel(
                "<html><div style='text-align:center;'>"
                        + description.replace("\n", "<br>")
                        + "</div></html>"
        );

        descriptionLabel.setFont(
                Theme.body(Theme.FONT_12, 0)
        );

        descriptionLabel.setForeground(foreground);

        descriptionLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );


        JPanel statisticPanel = new JPanel();
        statisticPanel.setLayout(new BoxLayout(statisticPanel, BoxLayout.Y_AXIS));
        statisticPanel.setOpaque(false);

        JLabel statisticLabel = new JLabel(statistic);
        statisticLabel.setFont(Theme.body(Theme.FONT_16, 1));
        statisticLabel.setForeground(foreground.darker());
        statisticLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(progressValue);
        progress.setBorderPainted(false);
        progress.setBackground(new Color(
                Math.min(255, background.getRed() + 15),
                Math.min(255, background.getGreen() + 15),
                Math.min(255, background.getBlue() + 15)
        ));
        progress.setForeground(foreground);
        progress.setPreferredSize(new Dimension(150, 12));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));

        metricLabels.put(metricKey, statisticLabel);
        metricBars.put(metricKey, progress);

        statisticPanel.add(statisticLabel);
        statisticPanel.add(Box.createVerticalStrut(10));
        statisticPanel.add(progress);


        card.add(topPanel, BorderLayout.NORTH);

        card.add(descriptionLabel, BorderLayout.CENTER);

        card.add(statisticPanel, BorderLayout.SOUTH);


        // 카드 전체를 눌러도 이동
        makeClickable(card, action);


        return card;
    }


    // =================================================
    // 공통 버튼 디자인
    // =================================================

    private JButton createAccentButton(String text) {

        JButton button = new JButton(text);

        button.setFont(
                Theme.body(Theme.FONT_12, 1)
        );

        button.setForeground(
                new Color(0x342316)
        );

        button.setBackground(Theme.ACCENT);

        button.setOpaque(true);

        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(9, 14, 9, 14)
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    private JButton createOutlineButton(String text) {

        JButton button = new JButton(text);

        button.setFont(
                Theme.body(Theme.FONT_12, 1)
        );

        button.setForeground(Theme.TEXT);

        button.setBackground(Theme.PANEL);

        button.setOpaque(true);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.LINE),
                        new EmptyBorder(8, 12, 8, 12)
                )
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    private void makeClickable(Component component, Runnable action) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                action.run();
            }
        });
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                makeClickable(child, action);
            }
        }
    }


    private static final class FeatureIcon implements Icon {

        private final String kind;
        private final Color color;

        private FeatureIcon(String kind, Color color) {
            this.kind = kind;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return 64;
        }

        @Override
        public int getIconHeight() {
            return 64;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);

            switch (kind) {
                case "BOOK" -> drawBook(g2);
                case "QUIZ" -> drawQuiz(g2);
                case "CROWN" -> drawCrown(g2);
                case "ROBOT" -> drawRobot(g2);
                default -> { }
            }
            g2.dispose();
        }

        private static void drawBook(Graphics2D g2) {
            Path2D page = new Path2D.Double();
            page.moveTo(32, 18);
            page.curveTo(23, 13, 14, 14, 8, 18);
            page.lineTo(8, 50);
            page.curveTo(17, 46, 25, 47, 32, 52);
            page.curveTo(39, 47, 47, 46, 56, 50);
            page.lineTo(56, 18);
            page.curveTo(50, 14, 41, 13, 32, 18);
            page.closePath();
            g2.fill(page);
            g2.setColor(new Color(0xF3DCC0));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.drawLine(32, 19, 32, 49);
        }

        private static void drawQuiz(Graphics2D g2) {
            int[] xs = {32, 38, 45, 45, 52, 45, 45, 38, 32, 26, 19, 19,
                    12, 19, 19, 26};
            int[] ys = {7, 17, 13, 22, 32, 42, 51, 47, 57, 47, 51, 42,
                    32, 22, 13, 17};
            g2.fillPolygon(new Polygon(xs, ys, xs.length));
            g2.setColor(new Color(0xDCE8F8));
            g2.fillOval(25, 25, 14, 14);
        }

        private static void drawCrown(Graphics2D g2) {
            int[] xs = {8, 19, 27, 32, 38, 47, 56, 51, 13};
            int[] ys = {20, 30, 17, 31, 17, 30, 20, 49, 49};
            g2.fillPolygon(new Polygon(xs, ys, xs.length));
            g2.fillRoundRect(15, 52, 34, 5, 3, 3);
            g2.fillOval(5, 16, 8, 8);
            g2.fillOval(28, 9, 8, 8);
            g2.fillOval(51, 16, 8, 8);
        }

        private static void drawRobot(Graphics2D g2) {
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.drawLine(32, 8, 32, 16);
            g2.drawRoundRect(13, 17, 38, 34, 10, 10);
            g2.drawLine(8, 27, 13, 27);
            g2.drawLine(51, 27, 56, 27);
            g2.drawLine(8, 40, 13, 40);
            g2.drawLine(51, 40, 56, 40);
            g2.fillOval(23, 29, 5, 5);
            g2.fillOval(37, 29, 5, 5);
            g2.drawArc(25, 34, 14, 9, 200, 140);
        }
    }


    // =================================================
    // 둥근 모서리 패널
    // =================================================

    private static class RoundedPanel extends JPanel {

        private final Color backgroundColor;
        private final Color borderColor;
        private final int radius;


        RoundedPanel(
                Color backgroundColor,
                Color borderColor,
                int radius
        ) {

            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.radius = radius;

            setOpaque(false);
        }


        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(backgroundColor);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            g2.setColor(borderColor);

            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            g2.dispose();
        }
    }


    // =================================================
    // Screen 인터페이스 구현
    // =================================================

    @Override
    public String cardName() {

        return "home";
    }


    @Override
    public JPanel component() {

        return this;
    }


    @Override
    public void onShow(Route route) {
        Progress current = services.progress();
        if (observedProgress != current) {
            if (observedProgress != null) {
                observedProgress.removeChangeListener(progressChangeListener);
            }
            observedProgress = current;
            if (observedProgress != null) {
                observedProgress.addChangeListener(progressChangeListener);
            }
        }
        refreshSummary();
    }


    @Override
    public void onHide() {
        if (observedProgress != null) {
            observedProgress.removeChangeListener(progressChangeListener);
            observedProgress = null;
        }
    }
}
