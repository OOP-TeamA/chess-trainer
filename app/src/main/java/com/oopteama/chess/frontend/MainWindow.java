package com.oopteama.chess.frontend;

import com.formdev.flatlaf.FlatClientProperties;
import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.service.Profile;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;

/**
 * 단일 메인 윈도우: 좌측 사이드바와 CardLayout 기반 7개 화면을 Screen 인터페이스로 다형 관리합니다.
 */
public final class MainWindow extends JFrame {

    private final ChessBackend backend;
    private final CardLayout cards = new CardLayout();
    private final JPanel contentPane = new JPanel(cards);
    private final Map<Screen.Id, Screen> screens = new EnumMap<>(Screen.Id.class);
    private final Map<Screen.Id, JButton> navButtons = new EnumMap<>(Screen.Id.class);

    private final JLabel profileNameLabel = new JLabel();
    private final JLabel profileScoreLabel = Theme.muted("");
    private Screen.Id activeId;

    public MainWindow(ChessBackend backend) {
        this.backend = backend;
        setTitle("ChessMate");
        setMinimumSize(new Dimension(1024, 680));
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, Theme.SIDEBAR);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_FOREGROUND, Theme.TEXT);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmLeaveCurrent()) {
                    if (activeId != null) {
                        screens.get(activeId).onHide();
                    }
                    backend.save();
                    dispose();
                }
            }
        });

        setLayout(new BorderLayout());
        contentPane.setBackground(Theme.BG);

        registerScreens();
        add(buildSidebar(), BorderLayout.WEST);
        add(contentPane, BorderLayout.CENTER);

        backend.addChangeListener(this::onBackendChanged);
        refreshProfileBadge();
        navigate(Screen.Id.HOME);
    }

    private void registerScreens() {
        register(Screen.Id.HOME, new HomePanel(backend, this::navigate));
        register(Screen.Id.LESSONS, new LessonPanel(backend));
        register(Screen.Id.PUZZLES, new PuzzlePanel(backend));
        register(Screen.Id.AI_GAME, new AiGamePanel(backend));
        register(Screen.Id.LOCAL_GAME, new LocalGamePanel(backend));
        register(Screen.Id.PROFILES, new ProfilePanel(backend));
        register(Screen.Id.SETTINGS, new SettingsPanel(backend));
    }

    private void register(Screen.Id id, Screen screen) {
        screens.put(id, screen);
        contentPane.add(screen.view(), id.name());
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, Theme.BORDER),
                new EmptyBorder(20, 14, 20, 14)
        ));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel brand = new JLabel("ChessMate");
        brand.setFont(Theme.bold(22));
        brand.setForeground(Theme.ACCENT);
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JLabel brandSub = Theme.muted("체스 학습 스튜디오");
        brandSub.setAlignmentX(LEFT_ALIGNMENT);

        JPanel profileCard = new Theme.RoundedPanel(
                Theme.SURFACE, Theme.BORDER, 12, Theme.SURFACE_ALT, Theme.ACCENT
        );
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        profileCard.setAlignmentX(LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        profileCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileNameLabel.setFont(Theme.bold(14));
        profileNameLabel.setForeground(Theme.TEXT);
        profileCard.add(profileNameLabel);
        profileCard.add(Box.createVerticalStrut(2));
        profileCard.add(profileScoreLabel);
        profileCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate(Screen.Id.PROFILES);
            }
        });

        header.add(brand);
        header.add(Box.createVerticalStrut(2));
        header.add(brandSub);
        header.add(Box.createVerticalStrut(14));
        header.add(profileCard);

        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        for (Screen.Id id : new Screen.Id[]{
                Screen.Id.HOME, Screen.Id.LESSONS, Screen.Id.PUZZLES, Screen.Id.AI_GAME, Screen.Id.LOCAL_GAME
        }) {
            navList.add(createNavButton(id));
            navList.add(Box.createVerticalStrut(6));
        }

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.add(createNavButton(Screen.Id.PROFILES));
        footer.add(Box.createVerticalStrut(6));
        footer.add(createNavButton(Screen.Id.SETTINGS));

        sidebar.add(header, BorderLayout.NORTH);
        sidebar.add(navList, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton createNavButton(Screen.Id id) {
        JButton btn = Theme.styledButton(id.label(), Theme.SIDEBAR, Theme.SURFACE, Theme.MUTED);
        btn.setHorizontalAlignment(JButton.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.addActionListener(e -> navigate(id));
        navButtons.put(id, btn);
        return btn;
    }

    public void navigate(Screen.Id target) {
        if (target == activeId || !confirmLeaveCurrent()) {
            return;
        }
        if (activeId != null) {
            screens.get(activeId).onHide();
        }
        activeId = target;
        cards.show(contentPane, target.name());
        screens.get(target).onShow();
        updateNavHighlight();
    }

    private boolean confirmLeaveCurrent() {
        if (activeId == null) {
            return true;
        }
        String warning = screens.get(activeId).leaveWarning();
        if (warning == null) {
            return true;
        }
        int res = JOptionPane.showConfirmDialog(
                this, warning, "화면 이동 확인", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
        );
        return res == JOptionPane.OK_OPTION;
    }

    private void updateNavHighlight() {
        for (Map.Entry<Screen.Id, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey() == activeId;
            JButton btn = e.getValue();
            if (active) {
                Theme.applyButtonStyle(btn, Theme.ACCENT_SOFT, Theme.ACCENT_SOFT, Theme.SUCCESS, Theme.ACCENT);
            } else {
                Theme.applyButtonStyle(btn, Theme.SIDEBAR, Theme.SURFACE, Theme.MUTED, null);
            }
        }
    }

    private void onBackendChanged() {
        refreshProfileBadge();
        for (Screen screen : screens.values()) {
            screen.onSettingsChanged();
        }
    }

    private void refreshProfileBadge() {
        Profile p = backend.activeProfile();
        profileNameLabel.setText(p.name());
        profileScoreLabel.setText("총점 " + p.totalScore() + "점");
    }
}
