package com.oopteama.chess.ui.component;

import com.oopteama.chess.ai.MoveInsight;
import com.oopteama.chess.ai.MoveInsight.Reason;
import com.oopteama.chess.ai.MoveInsight.Verdict;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.ui.Theme;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 수 설명 애니메이션(InsightPainter)이 시간대마다 오류 없이 그려지고, 도착 칸이 판정 색으로 빛나는지 확인합니다.
 */
class InsightPainterTest {

    private static final Board BOARD = Board.fromFen("4k3/8/5p2/8/8/8/3Q4/4K3 w - - 0 1");
    private static final BoardGeometry GEO = new BoardGeometry(10, 10, 60, false);
    private static final MoveInsight QUEEN_BLUNDER = new MoveInsight(Move.of("d2g5"), "Qg5", Verdict.BAD, 1000,
            Move.of("f6g5"), List.of(
                    new Reason(false, "상대 폰이 이 퀸을 공짜로 잡을 수 있습니다 (−9)",
                            Position.of("f6"), List.of(Position.of("g5"))),
                    new Reason(true, "중앙(d4·e4·d5·e5)을 차지합니다", null,
                            List.of(Position.of("d4"), Position.of("e5")))));

    @Test
    void everyStageOfTheAnimationPaints() {
        Theme.install();
        for (double seconds : new double[]{0, 0.15, 0.4, 0.8, 1.2, 1.9, 2.6, 3.3, 7.0}) {
            for (boolean dragging : new boolean[]{false, true}) {
                assertDoesNotThrow(() -> render(seconds, dragging), "seconds=" + seconds);
            }
        }
    }

    @Test
    void targetSquareGlowsInTheVerdictColor() {
        Theme.install();
        BufferedImage image = render(0.6, false);
        Position target = Position.of("g5");
        Color pixel = new Color(image.getRGB(GEO.x(target) + 8, GEO.y(target) + 8), true);
        assertTrue(pixel.getAlpha() > 0 && pixel.getRed() > pixel.getGreen() + 60 && pixel.getRed() > pixel.getBlue() + 60,
                "나쁜 수의 도착 칸은 빨갛게 빛나야 합니다: " + pixel);
    }

    private static BufferedImage render(double seconds, boolean dragging) {
        BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        InsightPainter.paint(g2, GEO, BOARD, QUEEN_BLUNDER, seconds, dragging);
        g2.dispose();
        return image;
    }
}
