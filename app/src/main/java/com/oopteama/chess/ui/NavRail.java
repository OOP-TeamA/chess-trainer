package com.oopteama.chess.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;

public final class NavRail extends JPanel {

    private final Navigator navigator;

    // 메뉴 버튼 관리
    private final Map<String, NavItemButton> menuButtons =
            new LinkedHashMap<>();

    private final ButtonGroup buttonGroup = new ButtonGroup();

    // 현재 선택된 메뉴
    private String activeKey = "home";
    private JLabel scoreLabel;


    public NavRail(Navigator navigator) {

        this.navigator = Objects.requireNonNull(navigator);

        setLayout(new BorderLayout());

        setBackground(Theme.SIDEBAR);
        setOpaque(true);

        setPreferredSize(new Dimension(208, 0));
        setMinimumSize(new Dimension(208, 0));

        setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(
                                0, 0, 0, 1,
                                Theme.LINE
                        ),
                                new EmptyBorder(12, 6, 12, 6)
                )
        );

        // 프로필 영역
        add(createProfile(), BorderLayout.NORTH);

        // 중앙 메뉴
        add(createMainMenu(), BorderLayout.CENTER);

        // 하단 메뉴
        add(createFooterMenu(), BorderLayout.SOUTH);

        setActiveRoute(new Route.Home());
    }


    private JPanel createProfile() {
        JPanel profile = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        profile.setBackground(Theme.SIDEBAR);
        profile.setOpaque(true);
        profile.setBorder(new EmptyBorder(8, 8, 22, 0));

        profile.add(new PlayerAvatar());

        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.setOpaque(false);

        JLabel name = new JLabel("학습자");
        name.setFont(Theme.body(Theme.FONT_16, 1));
        name.setForeground(Theme.TEXT);

        scoreLabel = new JLabel("점수 —");
        scoreLabel.setFont(Theme.body(Theme.FONT_14, 1));
        scoreLabel.setForeground(new Color(0x342316));
        scoreLabel.setOpaque(true);
        scoreLabel.setBackground(Theme.ACCENT);
        scoreLabel.setBorder(new EmptyBorder(4, 9, 4, 9));

        labels.add(name);
        labels.add(Box.createVerticalStrut(7));
        labels.add(scoreLabel);
        profile.add(labels);
        return profile;
    }


    public void setScore(int score) {
        scoreLabel.setText("점수 " + Math.max(0, score));
    }


    // =========================================
    // 중앙 메뉴 영역
    // =========================================

    private JPanel createMainMenu() {

        JPanel menu = new JPanel();

        menu.setLayout(
                new BoxLayout(
                        menu,
                        BoxLayout.Y_AXIS
                )
        );

        menu.setBackground(Theme.SIDEBAR);
        menu.setOpaque(true);

        addMenuItem(
                menu,
                "home",
                NavIcon.HOME,
                "홈",
                new Route.Home()
        );

        addMenuItem(
                menu,
                "lessons",
                NavIcon.LESSONS,
                "체스 레슨",
                new Route.Lessons()
        );

        addMenuItem(
                menu,
                "quiz",
                NavIcon.QUIZ,
                "체스 퀴즈",
                new Route.Puzzles()
        );

        addMenuItem(
                menu,
                "library",
                NavIcon.LIBRARY,
                "명국 기보",
                new Route.Library()
        );

        addMenuItem(
                menu,
                "ai",
                NavIcon.AI,
                "AI 대전",
                new Route.NewGame()
        );

        addMenuItem(
                menu,
                "glossary",
                NavIcon.GLOSSARY,
                "용어집",
                new Route.Glossary("")
        );

        menu.add(Box.createVerticalGlue());

        return menu;
    }


    // =========================================
    // 하단 메뉴 영역
    // =========================================

    private JPanel createFooterMenu() {

        JPanel footer = new JPanel();

        footer.setLayout(
                new BoxLayout(
                        footer,
                        BoxLayout.Y_AXIS
                )
        );

        footer.setBackground(Theme.SIDEBAR);
        footer.setOpaque(true);

        JSeparator separator = new JSeparator(
                SwingConstants.HORIZONTAL
        );

        separator.setForeground(Theme.LINE);
        separator.setBackground(Theme.LINE);

        separator.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1)
        );

        footer.add(separator);
        footer.add(Box.createVerticalStrut(12));

        addMenuItem(
                footer,
                "learningRecord",
                NavIcon.PROGRESS,
                "학습 기록",
                new Route.LearningRecord()
        );

        addMenuItem(
                footer,
                "prefs",
                NavIcon.SETTINGS,
                "설정",
                new Route.Prefs()
        );

        return footer;
    }


    // =========================================
    // 메뉴 버튼 생성
    // =========================================

    private void addMenuItem(
            JPanel parent,
            String key,
            NavIcon icon,
            String text,
            Route route
    ) {

        NavItemButton button = new NavItemButton(
                icon,
                text
        );

        button.addActionListener(e -> {
            navigator.go(route);
            // 이동이 취소된 경우 실제 활성 메뉴로 선택을 되돌린다.
            setActive(activeKey);
        });

        buttonGroup.add(button);
        menuButtons.put(key, button);

        button.setAlignmentX(LEFT_ALIGNMENT);

        parent.add(button);
        parent.add(Box.createVerticalStrut(8));
    }


    // =========================================
    // 현재 화면에 해당하는 메뉴 선택
    // =========================================

    public void setActiveRoute(Route route) {

        Objects.requireNonNull(route);

        String key = null;

        if (route instanceof Route.Home) {

            key = "home";

        } else if (
                route instanceof Route.Lessons ||
                route instanceof Route.Lesson
        ) {

            key = "lessons";

        } else if (
                route instanceof Route.Puzzles ||
                route instanceof Route.Puzzle ||
                route instanceof Route.TermQuiz ||
                route instanceof Route.WrongNotes
        ) {

            key = "quiz";

        } else if (
                route instanceof Route.Library ||
                route instanceof Route.Replay
        ) {

            key = "library";

        } else if (
                route instanceof Route.NewGame ||
                route instanceof Route.AiGame
        ) {

            key = "ai";

        } else if (route instanceof Route.Glossary) {

            key = "glossary";

        } else if (
                route instanceof Route.LearningRecord ||
                route instanceof Route.Profiles ||
                route instanceof Route.Ranking ||
                route instanceof Route.Favorites
        ) {

            key = "learningRecord";

        } else if (route instanceof Route.Prefs) {

            key = "prefs";
        }

        setActive(key);
    }


    // =========================================
    // 메뉴 선택 상태 갱신
    // =========================================

    private void setActive(String key) {

        activeKey = key;

        for (Map.Entry<String, NavItemButton> entry
                : menuButtons.entrySet()) {

            boolean selected =
                    entry.getKey().equals(activeKey);

            entry.getValue().setMenuSelected(selected);
        }

        repaint();
    }


    // =========================================
    // 메뉴 버튼 디자인
    // =========================================

    private static final class NavItemButton extends JToggleButton {

        private boolean selected;
        private boolean hovered;


        NavItemButton(NavIcon icon, String text) {

            super(text, new NavGlyphIcon(icon));

            setFont(
                    Theme.body(Theme.FONT_16, 1)
            );

            setForeground(Theme.MUTED);

            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(14);

            setFocusPainted(false);
            setBorderPainted(false);

            setContentAreaFilled(false);
            setOpaque(false);
            setToolTipText(text);
            getAccessibleContext().setAccessibleName(text);

            setCursor(
                    java.awt.Cursor.getPredefinedCursor(
                            java.awt.Cursor.HAND_CURSOR
                    )
            );

            setBorder(
                    new EmptyBorder(
                            8, 12, 8, 8
                    )
            );

            setPreferredSize(
                    new Dimension(190, 56)
            );

            setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 56)
            );

            setMinimumSize(
                    new Dimension(150, 56)
            );

            // 마우스 호버 효과
            addMouseListener(
                    new MouseAdapter() {

                        @Override
                        public void mouseEntered(MouseEvent e) {

                            hovered = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {

                            hovered = false;
                            repaint();
                        }
                    }
            );
        }


        // 선택 상태 변경
        void setMenuSelected(boolean selected) {

            this.selected = selected;
            setSelected(selected);

            setForeground(
                    selected
                            ? Theme.TEXT
                            : Theme.MUTED
            );

            repaint();
        }


        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (selected) {

                g2.setColor(Theme.RAISED);

            } else if (hovered) {

                g2.setColor(
                        new Color(
                                Theme.RAISED.getRed(),
                                Theme.RAISED.getGreen(),
                                Theme.RAISED.getBlue(),
                                120
                        )
                );
            }

            if (selected || hovered) {

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        10,
                        10
                );
            }

            if (selected) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(
                        0,
                        8,
                        3,
                        Math.max(1, getHeight() - 16),
                        3,
                        3
                );
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }


    private static final class PlayerAvatar extends JPanel {

        private PlayerAvatar() {
            setOpaque(false);
            setPreferredSize(new Dimension(72, 72));
            setMinimumSize(new Dimension(72, 72));
            setMaximumSize(new Dimension(72, 72));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(0x594128));
            g2.fillOval(1, 1, 70, 70);
            g2.setColor(new Color(0xF3BD47));
            g2.fillOval(5, 5, 62, 62);

            // 머리카락과 얼굴
            g2.setColor(new Color(0x6E3F27));
            g2.fillRoundRect(18, 17, 36, 27, 15, 15);
            g2.setColor(new Color(0xF5C996));
            g2.fillOval(20, 24, 32, 32);
            g2.fillOval(16, 36, 8, 10);
            g2.fillOval(48, 36, 8, 10);

            // 앞머리, 눈, 미소
            g2.setColor(new Color(0x6E3F27));
            g2.fillArc(19, 15, 34, 22, 0, 180);
            g2.setColor(new Color(0x342316));
            g2.fillOval(28, 36, 3, 3);
            g2.fillOval(41, 36, 3, 3);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(31, 39, 11, 8, 200, 140);

            // 상의
            g2.setColor(new Color(0x8B5637));
            Path2D shirt = new Path2D.Double();
            shirt.moveTo(10, 67);
            shirt.curveTo(13, 56, 22, 53, 28, 53);
            shirt.curveTo(35, 59, 39, 59, 46, 53);
            shirt.curveTo(53, 54, 60, 59, 62, 67);
            shirt.closePath();
            g2.fill(shirt);
            g2.dispose();
        }
    }


    private enum NavIcon {
        HOME,
        LESSONS,
        QUIZ,
        LIBRARY,
        AI,
        GLOSSARY,
        PROGRESS,
        SETTINGS
    }


    /** Draws navigation symbols with Java2D so they render consistently without font glyphs. */
    private static final class NavGlyphIcon implements Icon {

        private static final int SIZE = 22;

        private final NavIcon kind;

        private NavGlyphIcon(NavIcon kind) {
            this.kind = Objects.requireNonNull(kind);
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.translate(x, y);
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.setColor(component.getForeground());
            g2.setStroke(new BasicStroke(
                    1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));

            switch (kind) {
                case HOME -> drawHome(g2);
                case LESSONS -> drawBook(g2, false);
                case QUIZ -> drawQuiz(g2);
                case LIBRARY -> drawCrown(g2);
                case AI -> drawRobot(g2);
                case GLOSSARY -> drawBook(g2, true);
                case PROGRESS -> drawProgress(g2);
                case SETTINGS -> drawSettings(g2);
            }

            g2.dispose();
        }

        private static void drawHome(Graphics2D g2) {
            Path2D roof = new Path2D.Double();
            roof.moveTo(3, 10);
            roof.lineTo(11, 3.5);
            roof.lineTo(19, 10);
            g2.draw(roof);
            g2.drawRoundRect(5, 9, 12, 10, 2, 2);
            g2.drawRoundRect(9, 13, 4, 6, 2, 2);
        }

        private static void drawBook(Graphics2D g2, boolean open) {
            Path2D pages = new Path2D.Double();
            pages.moveTo(11, 6);
            pages.curveTo(8, 4, 5, 4, 3, 5);
            pages.lineTo(3, 17);
            pages.curveTo(6, 16, 9, 16, 11, 18);
            pages.curveTo(13, 16, 16, 16, 19, 17);
            pages.lineTo(19, 5);
            pages.curveTo(16, 4, 13, 4, 11, 6);
            pages.closePath();
            g2.draw(pages);
            g2.drawLine(11, 6, 11, 18);
            if (open) {
                g2.drawLine(5, 8, 8, 8);
                g2.drawLine(14, 8, 17, 8);
                g2.drawLine(5, 11, 8, 11);
                g2.drawLine(14, 11, 17, 11);
            }
        }

        private static void drawQuiz(Graphics2D g2) {
            // Quiz checklist: a small clipboard with answer rows.
            g2.drawRoundRect(4, 4, 14, 16, 2, 2);
            g2.drawRoundRect(8, 2, 6, 4, 2, 2);

            for (int y : new int[] {9, 13, 17}) {
                g2.drawRoundRect(6, y - 1, 3, 3, 1, 1);
                g2.drawLine(12, y, 16, y);
            }
        }

        private static void drawCrown(Graphics2D g2) {
            Path2D crown = new Path2D.Double();
            crown.moveTo(4, 8);
            crown.lineTo(6, 17);
            crown.lineTo(16, 17);
            crown.lineTo(18, 8);
            crown.lineTo(14.5, 11);
            crown.lineTo(11, 5);
            crown.lineTo(7.5, 11);
            crown.closePath();
            g2.draw(crown);
            g2.drawLine(6, 19, 16, 19);
            g2.fill(new Ellipse2D.Double(3, 6.5, 2, 2));
            g2.fill(new Ellipse2D.Double(10, 3.5, 2, 2));
            g2.fill(new Ellipse2D.Double(17, 6.5, 2, 2));
        }

        private static void drawRobot(Graphics2D g2) {
            g2.draw(new Line2D.Double(11, 2.5, 11, 5));
            g2.fill(new Ellipse2D.Double(9.7, 1.5, 2.6, 2.6));
            g2.drawRoundRect(3, 5, 16, 13, 3, 3);
            g2.draw(new Line2D.Double(1.5, 9, 3, 9));
            g2.draw(new Line2D.Double(19, 9, 20.5, 9));
            g2.draw(new Ellipse2D.Double(6.5, 9, 2, 2));
            g2.draw(new Ellipse2D.Double(13.5, 9, 2, 2));
            g2.draw(new Line2D.Double(8, 14.5, 14, 14.5));
        }

        private static void drawProgress(Graphics2D g2) {
            g2.drawLine(3, 19, 19, 19);
            g2.drawLine(3, 19, 3, 4);
            g2.draw(new RoundRectangle2D.Double(6, 12, 2.5, 6, 1, 1));
            g2.draw(new RoundRectangle2D.Double(10, 9, 2.5, 9, 1, 1));
            g2.draw(new RoundRectangle2D.Double(14, 5, 2.5, 13, 1, 1));
        }

        private static void drawSettings(Graphics2D g2) {
            g2.draw(new Ellipse2D.Double(7, 7, 8, 8));
            g2.draw(new Ellipse2D.Double(9.5, 9.5, 3, 3));
            g2.drawLine(11, 2, 11, 6);
            g2.drawLine(11, 16, 11, 20);
            g2.drawLine(2, 11, 6, 11);
            g2.drawLine(16, 11, 20, 11);
            g2.draw(new Line2D.Double(4.5, 4.5, 7.2, 7.2));
            g2.draw(new Line2D.Double(14.8, 14.8, 17.5, 17.5));
            g2.draw(new Line2D.Double(4.5, 17.5, 7.2, 14.8));
            g2.draw(new Line2D.Double(14.8, 7.2, 17.5, 4.5));
        }
    }

}
