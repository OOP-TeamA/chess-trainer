package com.oopteama.chess.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.oopteama.chess.data.Profile;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.component.RoundedPanel;
import com.oopteama.chess.ui.controller.AiGameController;
import com.oopteama.chess.ui.controller.HomeController;
import com.oopteama.chess.ui.controller.LessonController;
import com.oopteama.chess.ui.controller.LocalGameController;
import com.oopteama.chess.ui.controller.ProfileController;
import com.oopteama.chess.ui.controller.PuzzleController;
import com.oopteama.chess.ui.controller.ScreenController;
import com.oopteama.chess.ui.controller.SettingsController;

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
 * 프로그램의 메인 창입니다. 왼쪽 메뉴와 오른쪽 화면 영역으로 이루어집니다.
 * 메뉴를 누르면 CardLayout으로 해당 화면(컨트롤러의 view)을 보여 줍니다.
 */
public final class MainFrame extends JFrame {

    private final ChessBackend backend;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<ScreenId, ScreenController> controllers = new EnumMap<>(ScreenId.class);
    private final Map<ScreenId, JButton> menuButtons = new EnumMap<>(ScreenId.class);
    private final JLabel profileNameLabel = new JLabel();
    private final JLabel profileScoreLabel = Theme.muted("");
    private ScreenId current;

    public MainFrame(ChessBackend backend) {
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
                closeWindow();
            }
        });

        // 화면 7개: 화면마다 컨트롤러가 하나씩 있고, 컨트롤러가 자기 화면(View)을 만듭니다.
        register(ScreenId.HOME, new HomeController(backend, this::navigate));
        register(ScreenId.LESSONS, new LessonController(backend));
        register(ScreenId.PUZZLES, new PuzzleController(backend));
        register(ScreenId.AI_GAME, new AiGameController(backend));
        register(ScreenId.LOCAL_GAME, new LocalGameController(backend));
        register(ScreenId.PROFILES, new ProfileController(backend));
        register(ScreenId.SETTINGS, new SettingsController(backend));

        setLayout(new BorderLayout());
        content.setBackground(Theme.BG);
        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        // 프로필이나 설정이 바뀌면 왼쪽 프로필 카드와 모든 화면에 알립니다.
        backend.addChangeListener(this::onDataChanged);
        refreshProfileCard();
        navigate(ScreenId.HOME);
    }

    private void register(ScreenId id, ScreenController controller) {
        controllers.put(id, controller);
        content.add(controller.view(), id.name());
    }

    // ---- 왼쪽 메뉴 ----

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, Theme.BORDER), new EmptyBorder(20, 14, 20, 14)));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel brand = new JLabel("ChessMate");
        brand.setFont(Theme.bold(22));
        brand.setForeground(Theme.ACCENT);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        JLabel brandSubtitle = Theme.muted("체스 학습 스튜디오");
        brandSubtitle.setAlignmentX(LEFT_ALIGNMENT);
        header.add(brand);
        header.add(Box.createVerticalStrut(2));
        header.add(brandSubtitle);
        header.add(Box.createVerticalStrut(14));
        header.add(buildProfileCard());

        JPanel mainMenu = new JPanel();
        mainMenu.setLayout(new BoxLayout(mainMenu, BoxLayout.Y_AXIS));
        mainMenu.setOpaque(false);
        for (ScreenId id : new ScreenId[]{ScreenId.HOME, ScreenId.LESSONS, ScreenId.PUZZLES, ScreenId.AI_GAME, ScreenId.LOCAL_GAME}) {
            mainMenu.add(menuButton(id));
            mainMenu.add(Box.createVerticalStrut(6));
        }
        JPanel bottomMenu = new JPanel();
        bottomMenu.setLayout(new BoxLayout(bottomMenu, BoxLayout.Y_AXIS));
        bottomMenu.setOpaque(false);
        bottomMenu.add(menuButton(ScreenId.PROFILES));
        bottomMenu.add(Box.createVerticalStrut(6));
        bottomMenu.add(menuButton(ScreenId.SETTINGS));

        sidebar.add(header, BorderLayout.NORTH);
        sidebar.add(mainMenu, BorderLayout.CENTER);
        sidebar.add(bottomMenu, BorderLayout.SOUTH);
        return sidebar;
    }

    /** 현재 프로필 이름과 총점. 누르면 프로필 화면으로 갑니다. */
    private JPanel buildProfileCard() {
        JPanel card = new RoundedPanel(Theme.SURFACE, Theme.BORDER, 12, Theme.SURFACE_ALT, Theme.ACCENT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileNameLabel.setFont(Theme.bold(14));
        profileNameLabel.setForeground(Theme.TEXT);
        card.add(profileNameLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(profileScoreLabel);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigate(ScreenId.PROFILES);
            }
        });
        return card;
    }

    private JButton menuButton(ScreenId id) {
        JButton button = Theme.styledButton(id.label(), Theme.SIDEBAR, Theme.SURFACE, Theme.MUTED);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.addActionListener(e -> navigate(id));
        menuButtons.put(id, button);
        return button;
    }

    // ---- 화면 전환 ----

    /** target 화면으로 바꿉니다. 지금 화면에서 확인이 필요하면 먼저 물어봅니다. */
    public void navigate(ScreenId target) {
        if (target == current || !confirmLeave()) {
            return;
        }
        if (current != null) {
            controllers.get(current).onHide();
        }
        current = target;
        cards.show(content, target.name());
        controllers.get(target).onShow();
        highlightMenu();
    }

    private boolean confirmLeave() {
        String warning = current == null ? null : controllers.get(current).leaveWarning();
        if (warning == null) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(this, warning, "화면 이동 확인",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.OK_OPTION;
    }

    private void highlightMenu() {
        menuButtons.forEach((id, button) -> {
            if (id == current) {
                Theme.applyButtonStyle(button, Theme.ACCENT_SOFT, Theme.ACCENT_SOFT, Theme.SUCCESS, Theme.ACCENT);
            } else {
                Theme.applyButtonStyle(button, Theme.SIDEBAR, Theme.SURFACE, Theme.MUTED, null);
            }
        });
    }

    private void onDataChanged() {
        refreshProfileCard();
        controllers.values().forEach(ScreenController::onDataChanged);
    }

    private void refreshProfileCard() {
        Profile profile = backend.activeProfile();
        profileNameLabel.setText(profile.name());
        profileScoreLabel.setText("총점 " + profile.totalScore() + "점");
    }

    /** 창을 닫을 때: 진행 중인 대국이 있으면 확인하고, 데이터를 저장한 뒤 닫습니다. */
    private void closeWindow() {
        if (!confirmLeave()) {
            return;
        }
        if (current != null) {
            controllers.get(current).onHide();
        }
        backend.save();
        dispose();
    }
}
