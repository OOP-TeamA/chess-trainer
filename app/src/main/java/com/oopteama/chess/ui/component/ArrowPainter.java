package com.oopteama.chess.ui.component;

import com.oopteama.chess.model.Position;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * 체스판 위에 화살표를 그립니다. 힌트 화살표와 수 설명 화살표가 함께 씁니다.
 */
final class ArrowPainter {

    private ArrowPainter() {
    }

    /**
     * from 칸 가운데에서 to 칸 가운데로 화살표를 그립니다.
     *
     * @param width    화살표 몸통 두께
     * @param progress 0~1: 화살표가 뻗어 나간 정도 (애니메이션용, 1이면 끝까지)
     * @param dashed   점선이면 true (상대의 예상 응수처럼 "아직 두지 않은 수")
     */
    static void draw(Graphics2D g2, BoardGeometry geo, Position from, Position to, Color color,
                     float width, double progress, boolean dashed) {
        double x1 = geo.centerX(from);
        double y1 = geo.centerY(from);
        double fullLength = Math.hypot(geo.centerX(to) - x1, geo.centerY(to) - y1);
        double length = fullLength * Math.min(1.0, progress);
        if (length < 1) {
            return;
        }
        double ux = (geo.centerX(to) - x1) / fullLength; // 화살표 방향 (길이 1)
        double uy = (geo.centerY(to) - y1) / fullLength;
        double tipX = x1 + ux * length;  // 지금까지 뻗어 나간 끝
        double tipY = y1 + uy * length;
        double head = Math.min(geo.cell() * 0.34, length);
        double shaftEndX = tipX - ux * head;
        double shaftEndY = tipY - uy * head;

        g2.setColor(color);
        g2.setStroke(dashed
                ? new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
                        new float[]{width * 1.6f, width * 1.1f}, 0f)
                : new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(x1, y1, shaftEndX, shaftEndY));

        double px = -uy * head * 0.65; // 화살촉 밑변 방향
        double py = ux * head * 0.65;
        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(tipX, tipY);
        arrowHead.lineTo(shaftEndX + px, shaftEndY + py);
        arrowHead.lineTo(shaftEndX - px, shaftEndY - py);
        arrowHead.closePath();
        g2.fill(arrowHead);
    }
}
