package com.oopteama.chess.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public final class DemoTheme {
    public static final Color BG = new Color(26, 24, 23);
    public static final Color SIDEBAR = new Color(36, 31, 28);
    public static final Color PANEL = new Color(47, 41, 36);
    public static final Color PANEL2 = new Color(58, 50, 43);
    public static final Color GOLD = new Color(224, 182, 105);
    public static final Color CREAM = new Color(246, 234, 212);
    public static final Color MUTED = new Color(184, 170, 150);
    public static final Color GREEN = new Color(125, 189, 143);
    public static final Color RED = new Color(223, 136, 128);
    private DemoTheme() {}
    public static Font font(int size, boolean bold) { return new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, size); }
    public static JLabel label(String value, int size, Color color, boolean bold) {
        JLabel l = new JLabel(value); l.setFont(font(size, bold)); l.setForeground(color); return l;
    }
    public static JPanel panel(LayoutManager layout) { JPanel p = new JPanel(layout); p.setOpaque(true); p.setBackground(PANEL); return p; }
    public static JPanel transparent(LayoutManager layout) { JPanel p = new JPanel(layout); p.setOpaque(false); return p; }
    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(font(14, true)); b.setForeground(primary ? BG : CREAM);
        b.setBackground(primary ? GOLD : PANEL2); b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(primary ? GOLD : new Color(86, 73, 59), 1, true), new EmptyBorder(10, 15, 10, 15)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true); return b;
    }
    public static void inset(JComponent c, int top, int left, int bottom, int right) {
        c.setBorder(new EmptyBorder(top, left, bottom, right));
    }
    public static JPanel card(LayoutManager layout) {
        JPanel p = panel(layout);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(77, 65, 54), 1, true), new EmptyBorder(20, 22, 20, 22)));
        return p;
    }
    public static JScrollPane scroll(Component content) {
        JScrollPane s = new JScrollPane(content);
        s.setBorder(null); s.getViewport().setBackground(BG);
        s.getVerticalScrollBar().setUnitIncrement(20); return s;
    }
    public static void setDefaults() {
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("OptionPane.messageForeground", CREAM);
        UIManager.put("Label.foreground", CREAM);
        UIManager.put("Button.font", font(13, false));
    }
}
