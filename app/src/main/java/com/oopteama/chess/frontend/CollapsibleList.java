package com.oopteama.chess.frontend;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

final class CollapsibleList extends JPanel {
    private final JButton toggle;
    private final JPanel list;
    private final String name;

    CollapsibleList(JPanel list, String name) {
        super(new BorderLayout(0, 8));
        setOpaque(false);

        this.list = list;
        this.name = name;
        toggle = Theme.secondaryButton("<");
        toggle.setPreferredSize(new Dimension(36, 30));
        toggle.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 8; margin: 0,0,0,0");
        updateToggle(toggle, name, true);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        header.setOpaque(false);
        header.add(toggle);
        add(header, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);

        toggle.addActionListener(event -> setExpanded(!list.isVisible()));
    }

    void openAndFocus() {
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
