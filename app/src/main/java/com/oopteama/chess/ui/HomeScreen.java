package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;


public final class HomeScreen extends JPanel implements Screen {

    private final Navigator navigator;


    public HomeScreen(Navigator navigator) {

        this.navigator = Objects.requireNonNull(navigator);

        setLayout(new BorderLayout());

        setBackground(Theme.WINDOW);

        setBorder(
                new EmptyBorder(25, 30, 25, 30)
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

        // 3. 주요 기능 안내
        content.add(createSectionTitle());

        content.add(Box.createVerticalStrut(12));

        // 4. 기능별 카드
        content.add(createFeatureCards());

        // 화면 크기가 작을 때 스크롤 가능하도록 설정
        JScrollPane scrollPane = new JScrollPane(content);

        scrollPane.setBorder(null);

        scrollPane.setBackground(Theme.WINDOW);

        scrollPane.getViewport().setBackground(Theme.WINDOW);

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        add(scrollPane, BorderLayout.CENTER);
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
                new EmptyBorder(23, 25, 23, 25)
        );

        panel.setPreferredSize(
                new Dimension(800, 145)
        );

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 160)
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
                Theme.title(25)
        );

        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(
                "<html>기본부터 AI 대전까지,<br>"
                        + "체계적으로 배우는 체스 학습 프로그램</html>"
        );

        subtitle.setFont(
                Theme.body(12, 0)
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

        JLabel pawn = createPieceLabel("♟", 38);
        JLabel bishop = createPieceLabel("♝", 43);
        JLabel king = createPieceLabel("♚", 65);
        JLabel knight = createPieceLabel("♞", 53);
        JLabel pawn2 = createPieceLabel("♟", 38);

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

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 125)
        );


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
                Theme.body(10, 1)
        );

        badge.setForeground(Theme.ACCENT);


        JLabel title = new JLabel(
                "레슨 학습을 이어서 진행해 보세요."
        );

        title.setFont(
                Theme.title(17)
        );

        title.setForeground(Theme.TEXT);


        JLabel description = new JLabel(
                "최근 학습 진행 정보는 데이터 연결 후 표시됩니다."
        );

        description.setFont(
                Theme.body(11, 0)
        );

        description.setForeground(Theme.MUTED);


        infoPanel.add(badge);

        infoPanel.add(Box.createVerticalStrut(7));

        infoPanel.add(title);

        infoPanel.add(Box.createVerticalStrut(6));

        infoPanel.add(description);


        // 오른쪽 버튼
        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        8,
                        15
                )
        );

        buttonPanel.setOpaque(false);


        JButton continueButton = createAccentButton(
                "레슨 이어 하기"
        );

        continueButton.addActionListener(e ->
                navigator.go(new Route.Lessons())
        );


        JButton listButton = createOutlineButton(
                "레슨 목록"
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
    // 3. 주요 기능 제목
    // =================================================

    private JPanel createSectionTitle() {

        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setOpaque(false);

        JLabel title = new JLabel("주요 학습 메뉴");

        title.setFont(
                Theme.title(19)
        );

        title.setForeground(Theme.TEXT);

        JLabel subtitle = new JLabel(
                "원하는 메뉴를 선택해 체스를 배워보세요."
        );

        subtitle.setFont(
                Theme.body(11, 0)
        );

        subtitle.setForeground(Theme.MUTED);


        JPanel textPanel = new JPanel();

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        textPanel.setOpaque(false);

        textPanel.add(title);

        textPanel.add(Box.createVerticalStrut(4));

        textPanel.add(subtitle);


        panel.add(textPanel, BorderLayout.CENTER);

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 55)
        );

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
                new Dimension(800, 225)
        );

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 240)
        );


        // 룰학습
        panel.add(
                createFeatureCard(
                        "▤",
                        "룰학습",
                        "체스의 기본 규칙과\n핵심 개념 배우기",
                        "기초부터 차근차근",
                        new Color(0xF3DCC0),
                        new Color(0x754B2B),
                        () -> navigator.go(new Route.Lessons())
                )
        );


        // 체스 퀴즈
        panel.add(
                createFeatureCard(
                        "♟",
                        "체스 퀴즈",
                        "풀면서 익히는\n실전 감각",
                        "문제로 실력 확인",
                        new Color(0xDCE8F8),
                        new Color(0x315D91),
                        () -> navigator.go(new Route.Puzzles())
                )
        );


        // 명국 기보
        panel.add(
                createFeatureCard(
                        "♛",
                        "명국 기보",
                        "명국으로 배우는\n전략과 전술",
                        "명국 감상하기",
                        new Color(0xE2EDD9),
                        new Color(0x527448),
                        () -> navigator.go(new Route.Library())
                )
        );


        // AI 대전
        panel.add(
                createFeatureCard(
                        "♞",
                        "AI 대전",
                        "나만의 난이도로\n대국 연습하기",
                        "AI와 대국하기",
                        new Color(0xEAE0F6),
                        new Color(0x70509A),
                        () -> navigator.go(new Route.NewGame())
                )
        );


        return panel;
    }


    // 개별 기능 카드
    private JPanel createFeatureCard(
            String icon,
            String title,
            String description,
            String actionText,
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


        JLabel iconLabel = new JLabel(icon);

        iconLabel.setFont(
                new Font(
                        "Serif",
                        Font.PLAIN,
                        39
                )
        );

        iconLabel.setForeground(foreground);

        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(
                Theme.title(17)
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
                Theme.body(11, 0)
        );

        descriptionLabel.setForeground(foreground);

        descriptionLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );


        // 바로가기 버튼
        JButton button = new JButton(
                actionText + "  →"
        );

        button.setFont(
                Theme.body(11, 1)
        );

        button.setForeground(foreground);

        button.setBackground(background);

        button.setOpaque(false);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                foreground.darker()
                        ),
                        new EmptyBorder(7, 5, 7, 5)
                )
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.addActionListener(e -> action.run());


        card.add(topPanel, BorderLayout.NORTH);

        card.add(descriptionLabel, BorderLayout.CENTER);

        card.add(button, BorderLayout.SOUTH);


        // 카드 전체를 눌러도 이동
        card.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        card.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            java.awt.event.MouseEvent e
                    ) {

                        if (e.getSource() == card) {
                            action.run();
                        }
                    }
                }
        );


        return card;
    }


    // =================================================
    // 공통 버튼 디자인
    // =================================================

    private JButton createAccentButton(String text) {

        JButton button = new JButton(text);

        button.setFont(
                Theme.body(12, 1)
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
                Theme.body(12, 1)
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

        // 홈 화면 진입 시 필요한 작업
    }


    @Override
    public void onHide() {

        // 홈 화면 이탈 시 정리할 작업
    }
}