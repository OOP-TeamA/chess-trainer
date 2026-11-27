package com.oopteama.chess.ui.component;

import com.oopteama.chess.ai.MoveInsight;
import com.oopteama.chess.ai.MoveInsight.Reason;
import com.oopteama.chess.ai.MoveInsight.Verdict;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.ui.Theme;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 수 설명(MoveInsight)을 체스판 위에 애니메이션으로 덧그립니다. BoardPanel이 화면을 그릴 때마다 부릅니다.
 * 설명을 보여 주기 시작한 뒤 지난 시간(초)에 따라 차례로 그립니다.
 * <ol>
 *   <li>0초부터: 도착 칸이 판정 색으로 빛나고, 움직일 기물의 반투명 미리보기가 도착 칸으로 미끄러집니다.
 *       잡는 수면 도착하는 순간 "+점수"가 떠오릅니다.</li>
 *   <li>0.3초부터: 이유마다 화살표나 칸 강조 고리가 하나씩 그려지고, 카드의 이유 글도 함께 나타납니다.</li>
 *   <li>0.9초부터: 좋지 않은 수면 상대의 예상 응수가 점선 화살표와 상대 기물 미리보기로 되풀이됩니다.</li>
 * </ol>
 * 설명 카드는 체스판 위쪽과 아래쪽 중 중요한 칸을 덜 가리는 쪽에 보여 줍니다.
 */
final class InsightPainter {

    private static final Color GOOD = new Color(0x2EB872);
    private static final Color BAD = new Color(0xEF4444);
    private static final Color CARD_BG = new Color(0x1E1A17);
    private static final double SLIDE_TIME = 0.3;   // 기물 미리보기가 도착 칸까지 가는 시간(초)
    private static final double REASON_START = 0.3; // 첫 이유를 그리기 시작하는 시각(초)
    private static final double REASON_GAP = 0.22;  // 이유 사이 간격(초)
    private static final double REPLY_START = 0.9;  // 상대 응수 재생을 시작하는 시각(초)
    private static final double REPLY_CYCLE = 2.2;  // 상대 응수를 되풀이하는 주기(초)
    private static final double REPLY_LAND = 0.85;  // 한 주기 안에서 상대 기물이 도착하는 시각(초)

    private InsightPainter() {
    }

    /** 판정마다 쓰는 색: 최선 초록, 좋음 파랑, 아쉬움 주황, 나쁨 빨강. */
    static Color verdictColor(Verdict verdict) {
        return switch (verdict) {
            case BEST -> GOOD;
            case GOOD -> new Color(0x3B82F6);
            case INACCURATE -> new Color(0xF59E0B);
            case BAD -> BAD;
        };
    }

    /**
     * 수 설명을 그립니다.
     *
     * @param board    수를 두기 전 국면
     * @param seconds  설명을 보여 주기 시작한 뒤 지난 시간(초)
     * @param dragging 기물을 끌고 있으면 true (끌고 있는 기물이 미리보기 역할을 하므로 미리보기를 생략)
     */
    static void paint(Graphics2D g2, BoardGeometry geo, Board board, MoveInsight insight,
                      double seconds, boolean dragging) {
        Move move = insight.move();
        Color tone = verdictColor(insight.verdict());
        boolean showReply = !insight.verdict().isGood() && insight.reply() != null;

        drawTargetGlow(g2, geo, move.to(), tone, seconds);
        drawReasonMarks(g2, geo, insight.reasons(), seconds);
        if (!dragging) {
            // 상대 응수가 이 기물을 잡는 순간에는 내 기물 미리보기를 흐리게 해서 "잡힌다"는 걸 보여 줍니다.
            double captured = showReply && insight.reply().to().equals(move.to()) ? replyLanding(seconds) : 0;
            drawGhostMove(g2, geo, board, move, seconds, (float) (0.62 * (1 - 0.8 * captured)));
        }
        if (showReply) {
            drawReply(g2, geo, board.apply(move), insight.reply(), seconds);
        }
        drawCard(g2, geo, insight, tone, seconds);
    }

    // ---- 체스판 위 표시 ----

    /** 도착 칸: 판정 색으로 숨 쉬듯 빛나고, 물결 고리가 바깥으로 퍼집니다. */
    private static void drawTargetGlow(Graphics2D g2, BoardGeometry geo, Position to, Color tone, double seconds) {
        int cell = geo.cell();
        double breath = 0.5 + 0.5 * Math.sin(seconds * 2 * Math.PI / 1.4);
        g2.setColor(withAlpha(tone, (int) (70 + 60 * breath)));
        g2.fillRect(geo.x(to), geo.y(to), cell, cell);
        g2.setColor(withAlpha(tone, 235));
        g2.setStroke(new BasicStroke(Math.max(2f, cell * 0.06f)));
        g2.drawRect(geo.x(to) + 1, geo.y(to) + 1, cell - 3, cell - 3);

        double ripple = (seconds % 1.4) / 1.4; // 0 → 1을 되풀이
        double radius = cell * (0.35 + 0.4 * ripple);
        g2.setColor(withAlpha(tone, (int) (190 * (1 - ripple))));
        g2.setStroke(new BasicStroke(Math.max(2f, cell * 0.045f)));
        g2.draw(circle(geo.centerX(to), geo.centerY(to), radius));
    }

    /** 이유마다 화살표(또는 칸 강조 고리)를 차례로 그립니다. 좋은 점은 초록, 나쁜 점은 빨강입니다. */
    private static void drawReasonMarks(Graphics2D g2, BoardGeometry geo, List<Reason> reasons, double seconds) {
        for (int i = 0; i < reasons.size(); i++) {
            Reason reason = reasons.get(i);
            double t = easeInOut(clamp((seconds - reasonStart(i)) / 0.35));
            if (t <= 0) {
                continue;
            }
            Color color = reason.good() ? GOOD : BAD;
            for (Position target : reason.targets()) {
                if (reason.from() != null) {
                    ArrowPainter.draw(g2, geo, reason.from(), target, withAlpha(color, 205), geo.cell() * 0.1f, t, false);
                } else {
                    drawRing(g2, geo, target, color, t, seconds);
                }
            }
        }
    }

    /** 칸을 둘러싸는 고리: 커지면서 나타난 뒤 살짝 맥박 칩니다. */
    private static void drawRing(Graphics2D g2, BoardGeometry geo, Position square, Color color,
                                 double t, double seconds) {
        double pulse = 1 + 0.05 * Math.sin(seconds * 2 * Math.PI / 1.1);
        double radius = geo.cell() * 0.42 * (0.6 + 0.4 * t) * pulse;
        g2.setColor(withAlpha(color, (int) (225 * t)));
        g2.setStroke(new BasicStroke(Math.max(2.5f, geo.cell() * 0.07f)));
        g2.draw(circle(geo.centerX(square), geo.centerY(square), radius));
    }

    /** 움직일 기물의 반투명 미리보기가 도착 칸으로 미끄러집니다. 잡는 수면 도착하는 순간 "+점수"가 떠오릅니다. */
    private static void drawGhostMove(Graphics2D g2, BoardGeometry geo, Board board, Move move,
                                      double seconds, float alpha) {
        Piece piece = board.pieceAt(move.from());
        if (piece == null) {
            return;
        }
        double t = easeOut(clamp(seconds / SLIDE_TIME));
        if (t >= 1 && move.isPromotion()) {
            piece = Piece.create(move.promotion(), piece.side()); // 도착하면 승진한 기물로 바뀝니다.
        }
        PiecePainter.draw(g2, piece, lerp(geo.x(move.from()), geo.x(move.to()), t),
                lerp(geo.y(move.from()), geo.y(move.to()), t), geo.cell(), alpha);

        Piece captured = Rules.capturedPiece(board, move);
        if (captured != null && seconds >= SLIDE_TIME) {
            drawPop(g2, geo, capturedSquare(board, move), "+" + points(captured), GOOD, seconds - SLIDE_TIME, 1);
        }
    }

    /**
     * 상대의 예상 응수를 되풀이해 보여 줍니다: 점선 화살표가 뻗고 → 상대 기물 미리보기가 그 칸으로 가고 →
     * 그 칸이 빨갛게 번쩍이며, 내 기물을 잡으면 "−점수"가 떠오릅니다.
     */
    private static void drawReply(Graphics2D g2, BoardGeometry geo, Board after, Move reply, double seconds) {
        if (seconds < REPLY_START) {
            return;
        }
        double phase = (seconds - REPLY_START) % REPLY_CYCLE;
        double fade = 1 - clamp((phase - (REPLY_CYCLE - 0.4)) / 0.4); // 주기 끝에서 서서히 사라졌다가 다시 시작
        ArrowPainter.draw(g2, geo, reply.from(), reply.to(), withAlpha(BAD, (int) (215 * fade)),
                geo.cell() * 0.09f, easeInOut(clamp(phase / 0.4)), true);

        double landed = phase - REPLY_LAND; // 상대 기물이 도착한 뒤 지난 시간
        if (landed >= 0) {
            g2.setColor(withAlpha(BAD, (int) (120 * (1 - clamp(landed / 0.6)) * fade)));
            g2.fillRect(geo.x(reply.to()), geo.y(reply.to()), geo.cell(), geo.cell());
        }
        Piece piece = after.pieceAt(reply.from());
        double slide = easeInOut(clamp((phase - 0.4) / (REPLY_LAND - 0.4)));
        if (piece != null && slide > 0) {
            PiecePainter.draw(g2, piece, lerp(geo.x(reply.from()), geo.x(reply.to()), slide),
                    lerp(geo.y(reply.from()), geo.y(reply.to()), slide), geo.cell(), (float) (0.8 * fade));
        }
        Piece lost = Rules.capturedPiece(after, reply);
        if (lost != null && landed >= 0) {
            drawPop(g2, geo, capturedSquare(after, reply), "−" + points(lost), BAD, landed, fade);
        }
    }

    /** 상대 응수가 도착 칸에 내려앉은 정도 (0~1): 내 기물 미리보기를 흐리게 할 때 씁니다. */
    private static double replyLanding(double seconds) {
        if (seconds < REPLY_START) {
            return 0;
        }
        double phase = (seconds - REPLY_START) % REPLY_CYCLE;
        double fade = 1 - clamp((phase - (REPLY_CYCLE - 0.4)) / 0.4);
        return clamp((phase - REPLY_LAND) / 0.15) * fade;
    }

    /**
     * 칸에서 터지는 고리와 위로 떠오르며 사라지는 점수 글자 ("+3", "−9").
     *
     * @param elapsed 터지기 시작한 뒤 지난 시간(초)
     * @param fade    전체 불투명도 (0~1)
     */
    private static void drawPop(Graphics2D g2, BoardGeometry geo, Position square, String text, Color color,
                                double elapsed, double fade) {
        int cell = geo.cell();
        double burst = clamp(elapsed / 0.45);
        if (burst < 1) {
            g2.setColor(withAlpha(color, (int) (220 * (1 - burst) * fade)));
            g2.setStroke(new BasicStroke(Math.max(1f, cell * 0.08f * (float) (1 - burst))));
            g2.draw(circle(geo.centerX(square), geo.centerY(square), cell * (0.3 + 0.5 * easeOut(burst))));
        }
        double rise = clamp(elapsed / 1.2);
        if (rise < 1) {
            g2.setFont(Theme.bold(Math.max(14f, cell * 0.34f)));
            FontMetrics fm = g2.getFontMetrics();
            int x = (int) Math.round(geo.centerX(square) - fm.stringWidth(text) / 2.0);
            int y = (int) Math.round(geo.y(square) + cell * 0.35 - cell * 0.55 * easeOut(rise));
            int alpha = (int) (255 * fade * (rise < 0.6 ? 1 : 1 - (rise - 0.6) / 0.4));
            g2.setColor(withAlpha(Color.BLACK, alpha * 2 / 3)); // 글자 그림자
            g2.drawString(text, x + 1, y + 2);
            g2.setColor(withAlpha(color.brighter(), alpha));
            g2.drawString(text, x, y);
        }
    }

    // ---- 설명 카드 ----

    /** 설명 카드: 첫 줄에 [판정] 수 · 손해 점수, 그 아래에 이유 목록 (이유 글은 체스판 표시와 같은 때에 나타남). */
    private static void drawCard(Graphics2D g2, BoardGeometry geo, MoveInsight insight, Color tone, double seconds) {
        double appear = easeOut(clamp(seconds / 0.25));
        if (appear <= 0) {
            return;
        }
        int cell = geo.cell();
        float textSize = (float) Math.max(11, Math.min(15, cell * 0.23));
        Font titleFont = Theme.bold(textSize + 1);
        Font textFont = Theme.font(textSize);
        FontMetrics titleMetrics = g2.getFontMetrics(titleFont);
        FontMetrics textMetrics = g2.getFontMetrics(textFont);

        int margin = Math.max(6, cell / 8);
        int pad = Math.max(8, cell / 7);
        int titleHeight = titleMetrics.getHeight() + 8;
        int lineHeight = textMetrics.getHeight() + 3;
        int width = geo.size() - margin * 2;
        int height = pad * 2 + titleHeight + insight.reasons().size() * lineHeight;
        int x = geo.left() + margin;
        int y = cardTop(geo, insight, height, margin) + (int) Math.round((1 - appear) * 10); // 살짝 떠오르며 나타남

        Composite saved = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) appear));
        RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, width, height, 16, 16);
        g2.setColor(withAlpha(CARD_BG, 240));
        g2.fill(shape);
        g2.setColor(withAlpha(tone, 230));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(shape);

        // 첫 줄: 판정 배지, 수 이름, 오른쪽 끝에 손해 점수
        String label = insight.verdict().label();
        int badgeWidth = titleMetrics.stringWidth(label) + 18;
        int badgeHeight = titleMetrics.getHeight() + 2;
        int baseline = y + pad + titleMetrics.getAscent() + 2;
        g2.setColor(tone);
        g2.fill(new RoundRectangle2D.Double(x + pad, y + pad, badgeWidth, badgeHeight, badgeHeight, badgeHeight));
        g2.setFont(titleFont);
        g2.setColor(new Color(0x16120E));
        g2.drawString(label, x + pad + 9, baseline);
        g2.setColor(Theme.TEXT);
        g2.drawString(insight.san(), x + pad + badgeWidth + 10, baseline);
        String loss = lossText(insight);
        g2.setFont(textFont);
        g2.setColor(insight.verdict().isGood() ? Theme.MUTED : tone);
        g2.drawString(loss, x + width - pad - textMetrics.stringWidth(loss), baseline);

        // 이유 목록: 체스판에 그 이유의 화살표가 그려질 때 왼쪽에서 밀려 들어오며 나타납니다.
        int bulletWidth = titleMetrics.stringWidth("✓") + 4;
        int lineY = y + pad + titleHeight + textMetrics.getAscent();
        for (int i = 0; i < insight.reasons().size(); i++) {
            Reason reason = insight.reasons().get(i);
            double shown = clamp((seconds - reasonStart(i) + 0.1) / 0.3);
            g2.setComposite(AlphaComposite.SrcOver.derive((float) (appear * shown)));
            int left = x + pad + (int) Math.round((1 - shown) * 10);
            String bullet = reason.good() ? "✓" : "!";
            g2.setFont(titleFont);
            g2.setColor(reason.good() ? GOOD.brighter() : BAD.brighter());
            g2.drawString(bullet, left + (bulletWidth - titleMetrics.stringWidth(bullet)) / 2, lineY);
            g2.setFont(textFont);
            g2.setColor(Theme.TEXT);
            int textX = left + bulletWidth + 6;
            g2.drawString(fit(reason.text(), textMetrics, x + width - pad - textX), textX, lineY);
            lineY += lineHeight;
        }
        g2.setComposite(saved);
    }

    /** 카드를 체스판 위쪽과 아래쪽 중 중요한 칸(수, 이유 표시, 예상 응수)을 덜 가리는 쪽에 둡니다. */
    private static int cardTop(BoardGeometry geo, MoveInsight insight, int height, int margin) {
        int top = geo.top() + margin;
        int bottom = geo.top() + geo.size() - margin - height;
        int topCost = 0;
        int bottomCost = 0;
        for (Position square : importantSquares(insight)) {
            if (geo.y(square) < top + height) {
                topCost++;
            }
            if (geo.y(square) + geo.cell() > bottom) {
                bottomCost++;
            }
        }
        if (topCost != bottomCost) {
            return topCost < bottomCost ? top : bottom;
        }
        boolean targetInTopHalf = geo.y(insight.move().to()) < geo.top() + geo.size() / 2;
        return targetInTopHalf ? bottom : top; // 비기면 도착 칸의 반대쪽
    }

    /**
     * 카드가 가리면 안 되는 칸들. 중요한 칸일수록 여러 번 넣어 무겁게 셉니다:
     * 도착 칸 3번, 이유 표시와 상대 응수의 칸 2번, 출발 칸 1번.
     */
    private static List<Position> importantSquares(MoveInsight insight) {
        Move move = insight.move();
        List<Position> squares = new ArrayList<>(List.of(move.from(), move.to(), move.to(), move.to()));
        if (!insight.verdict().isGood() && insight.reply() != null) {
            squares.addAll(List.of(insight.reply().from(), insight.reply().from(), insight.reply().to(), insight.reply().to()));
        }
        for (Reason reason : insight.reasons()) {
            if (reason.from() != null) {
                squares.addAll(List.of(reason.from(), reason.from()));
            }
            for (Position target : reason.targets()) {
                squares.addAll(List.of(target, target));
            }
        }
        return squares;
    }

    private static String lossText(MoveInsight insight) {
        if (insight.verdict() == Verdict.BEST) {
            return "가장 좋은 수";
        }
        if (insight.loss() >= 5_000) {
            return "큰 손해";
        }
        return String.format("최선보다 −%.1f", insight.loss() / 100.0);
    }

    /** 글이 maxWidth보다 길면 뒤를 잘라 "…"를 붙입니다. */
    private static String fit(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && fm.stringWidth(cut + "…") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    // ---- 작은 도우미 ----

    private static double reasonStart(int index) {
        return REASON_START + index * REASON_GAP;
    }

    /** 잡힌 기물이 있던 칸 (앙파상이면 도착 칸 옆). */
    private static Position capturedSquare(Board board, Move move) {
        return Rules.isEnPassant(board, move) ? new Position(move.to().col(), move.from().row()) : move.to();
    }

    private static long points(Piece piece) {
        return Math.round(piece.value() / 100.0);
    }

    private static int lerp(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    private static Ellipse2D circle(double centerX, double centerY, double radius) {
        return new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /** 0~1 사이로 자릅니다. */
    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** 처음에 빠르고 끝에서 천천히 멈추는 움직임. */
    private static double easeOut(double t) {
        double rest = 1 - t;
        return 1 - rest * rest * rest;
    }

    /** 천천히 출발해 가운데서 빠르고 끝에서 천천히 멈추는 움직임. */
    private static double easeInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }
}
