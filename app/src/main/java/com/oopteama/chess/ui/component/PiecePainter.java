package com.oopteama.chess.ui.component;

import com.oopteama.chess.model.Side;
import com.oopteama.chess.model.piece.Piece;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * 체스 기물을 벡터 도형으로 그립니다. 이미지 파일 없이 어떤 크기로도 선명하게 그려집니다.
 * 도형은 100×100 좌표계로 정의하고, 칸 크기에 맞게 늘이거나 줄여 그립니다.
 */
public final class PiecePainter {

    private static final Color WHITE_FILL = new Color(0xFCFBF7);
    private static final Color WHITE_STROKE = new Color(0x1E242E);
    private static final Color WHITE_DETAIL = new Color(0x475569);
    private static final Color BLACK_FILL = new Color(0x232730);
    private static final Color BLACK_STROKE = new Color(0xE8ECF2);
    private static final Color BLACK_DETAIL = new Color(0x94A3B8);

    private PiecePainter() {
    }

    /**
     * (x, y) 위치의 cell 크기 칸에 기물을 그립니다.
     *
     * @param alpha 불투명도 (1 = 그대로, 0 = 투명). 잡혀서 사라지는 기물에 씁니다.
     */
    public static void draw(Graphics2D g2, Piece piece, int x, int y, int cell, float alpha) {
        Graphics2D pg = (Graphics2D) g2.create();
        if (alpha < 0.99f) {
            pg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
        transform.scale(cell / 100.0, cell / 100.0);
        pg.transform(transform);

        boolean white = piece.side() == Side.WHITE;

        // 바닥 그림자
        pg.setColor(new Color(0, 0, 0, 48));
        pg.fill(new Ellipse2D.Double(20, 82, 60, 11));

        // 몸체
        Path2D body = silhouette(piece.symbol());
        pg.setColor(white ? WHITE_FILL : BLACK_FILL);
        pg.fill(body);
        pg.setColor(white ? WHITE_STROKE : BLACK_STROKE);
        pg.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pg.draw(body);

        // 기물별 장식선
        pg.setColor(white ? WHITE_DETAIL : BLACK_DETAIL);
        pg.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (piece.symbol()) {
            case 'K' -> {
                pg.drawLine(50, 12, 50, 27);
                pg.drawLine(43, 19, 57, 19);
                pg.drawLine(32, 76, 68, 76);
            }
            case 'Q' -> {
                pg.drawLine(31, 75, 69, 75);
                for (int[] dot : new int[][]{{19, 25}, {33, 19}, {47, 16}, {61, 19}, {75, 25}}) {
                    pg.fill(new Ellipse2D.Double(dot[0], dot[1], 6, 6));
                }
            }
            case 'R' -> {
                pg.drawLine(33, 38, 67, 38);
                pg.drawLine(31, 74, 69, 74);
            }
            case 'B' -> {
                pg.drawLine(44, 34, 56, 46);
                pg.drawLine(33, 74, 67, 74);
            }
            case 'N' -> pg.fill(new Ellipse2D.Double(37, 33, 5, 5));
            default -> pg.drawLine(34, 75, 66, 75); // 폰
        }
        pg.dispose();
    }

    /** 기물 기호별 몸체 윤곽선. */
    private static Path2D silhouette(char symbol) {
        Path2D p = new Path2D.Double();
        switch (symbol) {
            case 'R' -> {
                p.moveTo(27, 22);
                p.lineTo(37, 22);
                p.lineTo(37, 30);
                p.lineTo(45, 30);
                p.lineTo(45, 22);
                p.lineTo(55, 22);
                p.lineTo(55, 30);
                p.lineTo(63, 30);
                p.lineTo(63, 22);
                p.lineTo(73, 22);
                p.lineTo(73, 38);
                p.lineTo(66, 44);
                p.lineTo(68, 72);
                p.lineTo(76, 84);
                p.lineTo(24, 84);
                p.lineTo(32, 72);
                p.lineTo(34, 44);
                p.lineTo(27, 38);
                p.closePath();
            }
            case 'N' -> {
                p.moveTo(28, 84);
                p.lineTo(35, 68);
                p.curveTo(38, 56, 46, 54, 48, 46);
                p.lineTo(30, 50);
                p.lineTo(25, 42);
                p.lineTo(35, 26);
                p.lineTo(41, 16);
                p.lineTo(52, 22);
                p.curveTo(69, 25, 76, 44, 71, 70);
                p.lineTo(76, 84);
                p.closePath();
            }
            case 'B' -> {
                p.append(new Ellipse2D.Double(45, 13, 10, 10), false);
                p.moveTo(50, 22);
                p.curveTo(66, 30, 68, 48, 59, 60);
                p.lineTo(66, 73);
                p.lineTo(75, 84);
                p.lineTo(25, 84);
                p.lineTo(34, 73);
                p.lineTo(41, 60);
                p.curveTo(32, 48, 34, 30, 50, 22);
                p.closePath();
            }
            case 'Q' -> {
                p.moveTo(25, 84);
                p.lineTo(31, 72);
                p.lineTo(21, 31);
                p.lineTo(36, 53);
                p.lineTo(36, 25);
                p.lineTo(46, 51);
                p.lineTo(50, 22);
                p.lineTo(54, 51);
                p.lineTo(64, 25);
                p.lineTo(64, 53);
                p.lineTo(79, 31);
                p.lineTo(69, 72);
                p.lineTo(75, 84);
                p.closePath();
            }
            case 'K' -> {
                p.moveTo(25, 84);
                p.lineTo(31, 72);
                p.curveTo(20, 55, 24, 32, 40, 32);
                p.lineTo(44, 28);
                p.lineTo(56, 28);
                p.lineTo(60, 32);
                p.curveTo(76, 32, 80, 55, 69, 72);
                p.lineTo(75, 84);
                p.closePath();
            }
            default -> { // 폰
                p.append(new Ellipse2D.Double(36, 20, 28, 28), false);
                p.moveTo(39, 46);
                p.lineTo(61, 46);
                p.lineTo(67, 72);
                p.lineTo(74, 84);
                p.lineTo(26, 84);
                p.lineTo(33, 72);
                p.closePath();
            }
        }
        return p;
    }
}
