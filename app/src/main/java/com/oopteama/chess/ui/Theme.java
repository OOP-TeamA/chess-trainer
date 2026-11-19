package com.oopteama.chess.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.Map;

/**
 * FlatLaf 기반 모던 다크 슬레이트 + 에메랄드 테마와 공통 컴포넌트 팩토리입니다.
 */
public final class Theme {

    public static final Color BG = new Color(0x131720);
    public static final Color SIDEBAR = new Color(0x0E1118);
    public static final Color SURFACE = new Color(0x1B212E);
    public static final Color SURFACE_ALT = new Color(0x242C3D);
    public static final Color SURFACE_HOVER = new Color(0x2A3447);
    public static final Color BORDER = new Color(0x2D3748);

    public static final Color ACCENT = new Color(0x2EB872);
    public static final Color ACCENT_HOVER = new Color(0x259B5F);
    public static final Color ACCENT_PRESSED = new Color(0x1E824F);
    public static final Color ACCENT_SOFT = new Color(0x18352A);

    public static final Color TEXT = new Color(0xF1F5F9);
    public static final Color MUTED = new Color(0x94A3B8);
    public static final Color SUCCESS = new Color(0x34D399);
    public static final Color WARNING = new Color(0xFBBF24);
    public static final Color DANGER = new Color(0xF87171);
    public static final Color DANGER_BG = new Color(0x452228);
    public static final Color DANGER_HOVER = new Color(0x5E2B33);
    public static final Color INFO = new Color(0x60A5FA);

    private static final Font REGULAR = loadFont("/fonts/Pretendard-Regular.ttf", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    private static final Font BOLD = loadFont("/fonts/Pretendard-Bold.ttf", new Font(Font.SANS_SERIF, Font.BOLD, 14));

    private static boolean installed;

    private Theme() {
    }

    public static Font font(float size) {
        return REGULAR.deriveFont(size);
    }

    public static Font bold(float size) {
        return BOLD.deriveFont(size);
    }

    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static void install() {
        if (installed) {
            return;
        }
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", hex(ACCENT),
                "@background", hex(BG),
                "@foreground", hex(TEXT)
        ));
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", font(14));
        UIManager.put("Panel.background", BG);
        UIManager.put("Panel.foreground", TEXT);
        UIManager.put("Label.foreground", TEXT);

        // 둥근 모서리 & 깔끔한 포커스 링
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);

        // 입력 컴포넌트 & 콤보박스
        UIManager.put("TextField.background", SURFACE_ALT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.placeholderForeground", new Color(0x64748B));
        UIManager.put("TextArea.background", SURFACE);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("ComboBox.background", SURFACE_ALT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.buttonBackground", SURFACE_ALT);
        UIManager.put("ComboBox.popupBackground", SURFACE);
        UIManager.put("ComboBox.selectionBackground", ACCENT);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBox.padding", new Insets(5, 10, 5, 10));

        // 슬림 모던 스크롤바
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.thumb", new Color(0x334155));
        UIManager.put("ScrollBar.hoverThumbColor", new Color(0x475569));
        UIManager.put("ScrollBar.pressedThumbColor", new Color(0x64748B));
        UIManager.put("ScrollBar.track", BG);
        UIManager.put("ScrollBar.showButtons", false);

        // 타이틀바 & 다이얼로그
        UIManager.put("TitlePane.background", SIDEBAR);
        UIManager.put("TitlePane.foreground", TEXT);
        UIManager.put("TitlePane.unifiedBackground", false);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ProgressBar.background", SURFACE_ALT);
        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("CheckBox.icon.selectedBackground", ACCENT);
        UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
        UIManager.put("CheckBox.icon.borderColor", BORDER);

        installed = true;
    }

    public static JPanel card() {
        return card(16);
    }

    public static JPanel card(int pad) {
        JPanel panel = new RoundedPanel(SURFACE, BORDER, 14);
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return panel;
    }

    public static JPanel hoverCard(int pad, Color hoverBorder) {
        RoundedPanel panel = new RoundedPanel(SURFACE, BORDER, 14, SURFACE_ALT, hoverBorder);
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(pad, pad, pad, pad));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return panel;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(bold(22));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(bold(16));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(13));
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel badge(String text, Color accent) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(bold(12));
        label.setForeground(accent);
        label.setOpaque(false);
        label.setBorder(new EmptyBorder(4, 11, 4, 11));
        return label;
    }

    public static JButton primaryButton(String text) {
        return styledButton(text, ACCENT, ACCENT_HOVER, Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        return styledButton(text, SURFACE_ALT, SURFACE_HOVER, TEXT);
    }

    public static JButton dangerButton(String text) {
        return styledButton(text, DANGER_BG, DANGER_HOVER, DANGER);
    }

    public static JButton styledButton(String text, Color normal, Color hover, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(bold(13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyButtonStyle(btn, normal, hover, fg, null);
        return btn;
    }

    /**
     * FlatLaf 네이티브 스타일 속성으로 버튼의 배경·호버·눌림·테두리 상태를 적용합니다.
     */
    public static void applyButtonStyle(JButton btn, Color normal, Color hover, Color fg, Color border) {
        StringBuilder style = new StringBuilder();
        style.append("arc: 10; focusWidth: 0; margin: 8,14,8,14; ");
        style.append("background: ").append(hex(normal)).append("; ");
        style.append("hoverBackground: ").append(hex(hover)).append("; ");
        style.append("pressedBackground: ").append(hex(hover.darker())).append("; ");
        style.append("disabledBackground: ").append(hex(SURFACE)).append("; ");
        style.append("foreground: ").append(hex(fg)).append("; ");
        style.append("disabledText: ").append(hex(new Color(0x475569))).append("; ");
        if (border != null) {
            style.append("borderWidth: 1; borderColor: ").append(hex(border)).append("; ");
            style.append("hoverBorderColor: ").append(hex(border)).append(";");
        } else {
            style.append("borderWidth: 0;");
        }
        btn.putClientProperty(FlatClientProperties.STYLE, style.toString());
        btn.setForeground(fg);
    }

    public static JScrollPane scrollPane(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(view.getBackground());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color stroke;
        private final Color hoverFill;
        private final Color hoverStroke;
        private final int radius;
        private boolean hovered;

        public RoundedPanel(Color fill, Color stroke, int radius) {
            this(fill, stroke, radius, null, null);
        }

        public RoundedPanel(Color fill, Color stroke, int radius, Color hoverFill, Color hoverStroke) {
            this.fill = fill;
            this.stroke = stroke;
            this.radius = radius;
            this.hoverFill = hoverFill;
            this.hoverStroke = hoverStroke;
            setOpaque(false);
            if (hoverFill != null || hoverStroke != null) {
                addMouseListener(new MouseAdapter() {
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
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = (hovered && hoverFill != null) ? hoverFill : fill;
            Color bd = (hovered && hoverStroke != null) ? hoverStroke : stroke;
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (bd != null) {
                g2.setColor(bd);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static Font loadFont(String path, Font fallback) {
        try (InputStream in = Theme.class.getResourceAsStream(path)) {
            if (in == null) {
                return fallback;
            }
            Font loaded = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);
            return loaded;
        } catch (Exception e) {
            return fallback;
        }
    }
}
