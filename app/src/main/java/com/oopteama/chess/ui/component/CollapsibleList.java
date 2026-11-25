package com.oopteama.chess.ui.component;

import com.oopteama.chess.ui.Theme;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * 레슨·퍼즐 목록처럼 접었다 펼 수 있는 왼쪽 패널입니다. 위쪽의 작은 버튼으로 접고 폅니다.
 */
public final class CollapsibleList extends JPanel {
    private final JButton toggle;
    private final JPanel list;
    private final String name;

    public CollapsibleList(JPanel list, String name) {
        super(new BorderLayout(0, 8));
        setOpaque(false);

        this.list = list;
        this.name = name;
        toggle = Theme.secondaryButton("<");
        toggle.setPreferredSize(new Dimension(36, 30));
        // secondaryButton이 넣어 둔 색·테두리 스타일 뒤에 덧붙임 (같은 키는 뒤의 값이 적용됨)
        Object baseStyle = toggle.getClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE);
        toggle.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, baseStyle + " arc: 8; margin: 0,0,0,0;");
        updateToggle(toggle, name, true);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        header.setOpaque(false);
        header.add(toggle);
        add(header, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);

        toggle.addActionListener(event -> setExpanded(!list.isVisible()));
    }

    /** 목록을 펼치고 접기 버튼에 키보드 포커스를 줍니다. */
    public void openAndFocus() {
        setExpanded(true);
        toggle.requestFocusInWindow();
    }

    private void setExpanded(boolean expanded) {
        list.setVisible(expanded);
        updateToggle(toggle, name, expanded);
        revalidate();
        repaint();
    }

    private static void updateToggle(JButton toggle, String name, boolean expanded) {
        String action = name + (expanded ? " 접기" : " 펼치기");
        toggle.setText(expanded ? "<" : ">");
        toggle.setToolTipText(action);
        toggle.getAccessibleContext().setAccessibleName(action);
    }
}
