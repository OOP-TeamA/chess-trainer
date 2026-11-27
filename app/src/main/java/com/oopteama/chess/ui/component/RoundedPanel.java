package com.oopteama.chess.ui.component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;



/**
 * 모서리가 둥근 배경 패널입니다. hoverFill/hoverStroke를 주면 마우스를 올렸을 때 색이 바뀝니다.
 */
public class RoundedPanel extends JPanel {
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