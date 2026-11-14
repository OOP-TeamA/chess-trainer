package com.oopteama.chess.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;
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
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.WINDOW);

        content.add(createHero());
        content.add(Box.createVerticalStrut(18));
        content.add(createContinueCard());
        content.add(Box.createVerticalStrut(22));
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

        HeroPanel panel = new HeroPanel();
        panel.setLayout(new BorderLayout(12, 0));
        panel.setBorder(new EmptyBorder(0, 28, 12, 24));
        panel.setPreferredSize(new Dimension(800, 210));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        // 왼쪽 제목과 설명
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel title = new JLabel(
                "<html>체스로 배우는<br>사고력, 한 수씩</html>"
        );
        title.setFont(
                Theme.title(Theme.FONT_36).deriveFont(Font.BOLD, 36f)
        );
        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(
                "<html>기본부터 AI 대전까지,<br>"
                        + "체계적으로 배우는 체스 학습 프로그램</html>"
        );
        subtitle.setFont(Theme.body(13, Font.BOLD));
        subtitle.setForeground(Theme.MUTED);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(subtitle);
        panel.add(textPanel, BorderLayout.WEST);

        JPanel piecesPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 16, 0)
        );
        piecesPanel.setOpaque(false);
        piecesPanel.setBorder(new EmptyBorder(8, 0, 0, 4));

        // 기물 크기
        piecesPanel.add(createHeroPiece("♟", 90, 90));
        piecesPanel.add(createHeroPiece("♝", 100, 100));
        piecesPanel.add(createHeroPiece("♚", 120, 120));
        piecesPanel.add(createHeroPiece("♞", 125, 125));
        piecesPanel.add(createHeroPiece("♟", 90, 90));

        panel.add(piecesPanel, BorderLayout.CENTER);

        // 친구와 대국하기 버튼
        JButton localGameButton = createOutlineButton("친구와 대국하기");
        localGameButton.addActionListener(e ->
                navigator.go(new Route.LocalGame())
        );

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionPanel.add(localGameButton);
        panel.add(actionPanel, BorderLayout.NORTH);

        return panel;
    }

    private JLabel createHeroPiece(
            String symbol,
            int fontSize,
            int width
    ) {
        JLabel label = new JLabel(symbol) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D shadow = (Graphics2D) graphics.create();
                shadow.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                );
                shadow.setFont(getFont());
                shadow.setColor(new Color(35, 20, 12, 45));

                FontMetrics metrics = shadow.getFontMetrics();
                int textX = (getWidth() - metrics.stringWidth(getText())) / 2;
                int baseline = getHeight() - metrics.getDescent();
                shadow.drawString(getText(), textX + 3, baseline + 4);
                shadow.dispose();

                super.paintComponent(graphics);
            }
        };

        label.setFont(new Font("Serif", Font.PLAIN, fontSize));
        label.setForeground(
                symbol.equals("♞")
                        ? new Color(0x24150F)
                        : Theme.ACCENT
        );
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.BOTTOM);
        label.setPreferredSize(new Dimension(width, 150));
        label.setOpaque(false);
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

        panel.setLayout(new BorderLayout(15, 10));
        panel.setBorder(new EmptyBorder(15, 18, 15, 18));
        panel.setPreferredSize(new Dimension(800, 165));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel badge = new JLabel("이어서 하기");
        badge.setFont(Theme.body(Theme.FONT_12, 1));
        badge.setForeground(Theme.ACCENT);
        badge.setOpaque(true);
        badge.setBackground(Theme.RAISED);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        resumeTitle = new JLabel("학습 기록을 불러오는 중");
        resumeTitle.setFont(Theme.title(Theme.FONT_28));
        resumeTitle.setForeground(Theme.TEXT);
        resumeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        resumeDescription = new JLabel("다음 학습을 확인하고 있어요.");
        resumeDescription.setFont(Theme.body(Theme.FONT_12, 0));
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

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 8)
        );
        buttonPanel.setOpaque(false);

        continueButton = createAccentButton("▶  레슨 이어 하기");
        continueButton.addActionListener(e -> navigator.go(recommendedRoute));

        JButton listButton = createOutlineButton("☷  레슨 목록");
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
    // 3. 기능별 카드
    // =================================================

    private JPanel createFeatureCards() {

        JPanel panel = new JPanel(new GridLayout(1, 4, 13, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(800, 300));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 315));

        panel.add(createFeatureCard(
                "lessons",
                "BOOK",
                "룰학습",
                "체스의 기본 규칙과\n핵심 개념 배우기",
                "기록 불러오는 중",
                0,
                new Color(0xF3DCC0),
                new Color(0x754B2B),
                () -> navigator.go(new Route.Lessons())
        ));

        panel.add(createFeatureCard(
                "quiz",
                "QUIZ",
                "체스 퀴즈",
                "풀면서 익히는\n실전 감각",
                "기록 불러오는 중",
                0,
                new Color(0xDCE8F8),
                new Color(0x315D91),
                () -> navigator.go(new Route.Puzzles())
        ));

        panel.add(createFeatureCard(
                "library",
                "CROWN",
                "명국 기보",
                "명국으로 배우는\n전략과 전술",
                "기보 기록 API 연동 대기",
                0,
                new Color(0xE2EDD9),
                new Color(0x527448),
                () -> navigator.go(new Route.Library())
        ));

        panel.add(createFeatureCard(
                "ai",
                "ROBOT",
                "AI 대전",
                "나만의 난이도로\n대국 연습하기",
                "AI 기록 API 연동 대기",
                0,
                new Color(0xEAE0F6),
                new Color(0x70509A),
                () -> navigator.go(new Route.NewGame())
        ));

        return panel;
    }


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

        card.setLayout(new BorderLayout(5, 10));
        card.setBorder(new EmptyBorder(15, 12, 15, 12));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(new FeatureIcon(icon, foreground));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.title(Theme.FONT_28));
        titleLabel.setForeground(foreground);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(iconLabel);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(titleLabel);

        JLabel descriptionLabel = new JLabel(
                "<html><div style='text-align:center;'>"
                        + description.replace("\n", "<br>")
                        + "</div></html>"
        );
        descriptionLabel.setFont(Theme.body(14, 0));
        descriptionLabel.setForeground(foreground);
        descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setOpaque(false);
        descriptionPanel.setBorder(new EmptyBorder(22, 0, 0, 0));
        descriptionPanel.add(descriptionLabel, BorderLayout.NORTH);

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
        card.add(descriptionPanel, BorderLayout.CENTER);
        card.add(statisticPanel, BorderLayout.SOUTH);

        makeClickable(card, action);

        return card;
    }


    // =================================================
    // 공통 버튼 디자인
    // =================================================

    private JButton createAccentButton(String text) {
        return createRoundedButton(
                text,
                Theme.ACCENT,
                new Color(0x342316),
                null
        );
    }


    private JButton createOutlineButton(String text) {
        return createRoundedButton(
                text,
                Theme.PANEL,
                Theme.TEXT,
                Theme.LINE
        );
    }


    private JButton createRoundedButton(
            String text,
            Color background,
            Color foreground,
            Color borderColor
    ) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                Color fill = getModel().isRollover()
                        ? background.brighter()
                        : background;
                g2.setColor(fill);
                g2.fillRoundRect(
                        0, 0,
                        getWidth() - 1,
                        getHeight() - 1,
                        26, 26
                );
                g2.dispose();

                super.paintComponent(graphics);
            }

            @Override
            protected void paintBorder(Graphics graphics) {
                if (borderColor == null) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(borderColor);
                g2.drawRoundRect(
                        0, 0,
                        getWidth() - 1,
                        getHeight() - 1,
                        26, 26
                );
                g2.dispose();
            }
        };

        button.setFont(Theme.body(Theme.FONT_12, 1));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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


    // =================================================
    // 기능별 아이콘
    // =================================================

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
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
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
            g2.setStroke(new BasicStroke(
                    3f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));
            g2.drawLine(32, 19, 32, 49);
        }


        // 체스판 + 나이트 아이콘
        private static void drawQuiz(Graphics2D g2) {

            Color boardColor = new Color(0x315D91);
            Color squareColor = new Color(0xDCE8F8);

            // 체스판을 오른쪽 뒤에 배치
            int boardX = 27;
            int boardY = 7;
            int cell = 8;
            int boardSize = cell * 4;

            // 둥근 체스판 바탕
            g2.setColor(boardColor);
            g2.fillRoundRect(
                    boardX - 2,
                    boardY - 2,
                    boardSize + 4,
                    boardSize + 4,
                    6,
                    6
            );

            // 4 x 4 체크 무늬
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if ((row + col) % 2 == 0) {
                        g2.setColor(squareColor);
                    } else {
                        g2.setColor(boardColor);
                    }

                    g2.fillRect(
                            boardX + col * cell,
                            boardY + row * cell,
                            cell,
                            cell
                    );
                }
            }

            Graphics2D knightG = (Graphics2D) g2.create();
            knightG.translate(32, 32);
            knightG.scale(0.85, 0.85);
            knightG.translate(-32, -32);

            knightG.setColor(boardColor);

            Path2D knight = new Path2D.Double();
            knight.moveTo(8, 55);
            knight.lineTo(8, 49);
            knight.curveTo(8, 43, 13, 38, 18, 33);
            knight.curveTo(21, 30, 24, 27, 24, 24);
            knight.curveTo(24, 21, 21, 20, 17, 21);
            knight.lineTo(13, 23);
            knight.curveTo(10, 24, 8, 22, 8, 19);
            knight.curveTo(8, 16, 11, 13, 15, 11);
            knight.curveTo(18, 7, 22, 5, 28, 5);
            knight.lineTo(25, 1);
            knight.curveTo(32, 4, 38, 10, 40, 18);
            knight.curveTo(42, 25, 41, 34, 39, 42);
            knight.lineTo(39, 55);
            knight.closePath();
            knightG.fill(knight);

            // 나이트 목 아래쪽의 받침
            knightG.fillRoundRect(5, 57, 37, 5, 3, 3);

            // 눈
            knightG.setColor(squareColor);
            knightG.fillOval(23, 12, 3, 3);
            knightG.dispose();
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
            g2.setStroke(new BasicStroke(
                    6f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));

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

    /**
     * 홈 화면 상단 배너 전용 그라데이션 패널입니다.
     * 다른 카드에서 사용하는 RoundedPanel의 디자인은 변경하지 않습니다.
     */
    private static final class HeroPanel extends JPanel {

        private static final int RADIUS = 18;

        private HeroPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width = getWidth();
            int height = getHeight();

            if (width <= 0 || height <= 0) {
                g2.dispose();
                return;
            }

            // 그라데이션
            float[] fractions = {0.0f, 0.55f, 1.0f};
            Color[] colors = {
                    Theme.WINDOW,
                    new Color(0x38271D),
                    new Color(0x503522)
            };

            g2.setPaint(new LinearGradientPaint(
                    0,
                    0,
                    width,
                    0,
                    fractions,
                    colors
            ));

            g2.fillRoundRect(
                    0,
                    0,
                    width - 1,
                    height - 1,
                    RADIUS,
                    RADIUS
            );

            // 배너 외곽선
            g2.setColor(new Color(Theme.LINE.getRed(),
                    Theme.LINE.getGreen(),
                    Theme.LINE.getBlue(),
                    115));
            g2.drawRoundRect(
                    0,
                    0,
                    width - 1,
                    height - 1,
                    RADIUS,
                    RADIUS
            );

            g2.dispose();
        }
    }


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
