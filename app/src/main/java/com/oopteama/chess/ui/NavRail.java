package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JSeparator;
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


    public NavRail(Navigator navigator) {

        this.navigator = Objects.requireNonNull(navigator);

        setLayout(new BorderLayout());

        setBackground(Theme.SIDEBAR);
        setOpaque(true);

        setPreferredSize(new Dimension(76, 0));
        setMinimumSize(new Dimension(76, 0));

        setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(
                                0, 0, 0, 1,
                                Theme.LINE
                        ),
                                new EmptyBorder(12, 6, 12, 6)
                )
        );

        // 상단 프로필
        add(createHeader(), BorderLayout.NORTH);

        // 중앙 메뉴
        add(createMainMenu(), BorderLayout.CENTER);

        // 하단 메뉴
        add(createFooterMenu(), BorderLayout.SOUTH);

        setActiveRoute(new Route.Home());
    }


    // =========================================
    // 상단 프로필 영역
    // =========================================

    private JPanel createHeader() {

        JPanel header = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        0,
                        8
                )
        );

        header.setBackground(Theme.SIDEBAR);
        header.setOpaque(true);

        header.setBorder(
                new EmptyBorder(0, 2, 17, 2)
        );

        // 프로필 아이콘
        AvatarLabel avatar = new AvatarLabel();

        header.add(avatar);

        return header;
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
                "⌂",
                "홈",
                new Route.Home()
        );

        addMenuItem(
                menu,
                "lessons",
                "▤",
                "룰학습",
                new Route.Lessons()
        );

        addMenuItem(
                menu,
                "quiz",
                "♟",
                "체스 퀴즈",
                new Route.Puzzles()
        );

        addMenuItem(
                menu,
                "library",
                "♛",
                "명국 기보",
                new Route.Library()
        );

        addMenuItem(
                menu,
                "ai",
                "♞",
                "AI 대전",
                new Route.NewGame()
        );

        addMenuItem(
                menu,
                "glossary",
                "◇",
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
                "▥",
                "학습 기록",
                new Route.LearningRecord()
        );

        addMenuItem(
                footer,
                "prefs",
                "⚙",
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
            String icon,
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
        parent.add(Box.createVerticalStrut(5));
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


        NavItemButton(String icon, String text) {

            super(icon);

            setFont(
                    Theme.body(Theme.FONT_21, 1)
            );

            setForeground(Theme.MUTED);

            setHorizontalAlignment(SwingConstants.CENTER);

            setIconTextGap(8);

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
                            8, 8, 8, 8
                    )
            );

            setPreferredSize(
                    new Dimension(48, 48)
            );

            setMaximumSize(
                    new Dimension(48, 48)
            );

            setMinimumSize(
                    new Dimension(48, 48)
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

            g2.dispose();

            super.paintComponent(g);
        }
    }


    // =========================================
    // 프로필 아이콘 디자인
    // =========================================

    private static final class AvatarLabel extends JLabel {

        AvatarLabel() {

            super("♞", SwingConstants.CENTER);

            setFont(
                    Theme.title(Theme.FONT_21)
            );

            setForeground(Theme.ACCENT);

            setPreferredSize(
                    new Dimension(42, 42)
            );

            setMinimumSize(
                    new Dimension(42, 42)
            );

            setMaximumSize(
                    new Dimension(42, 42)
            );

            setOpaque(false);
        }


        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(Theme.RAISED);

            g2.fillOval(
                    1,
                    1,
                    getWidth() - 2,
                    getHeight() - 2
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }
}
