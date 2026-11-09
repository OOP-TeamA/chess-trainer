package com.oopteama.chess.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public final class Theme {

    public static final int FONT_12 = 12;
    public static final int FONT_14 = 14;
    public static final int FONT_16 = 16;
    public static final int FONT_21 = 21;
    public static final int FONT_28 = 28;
    public static final int FONT_36 = 36;

    // 기본 배경 색상
    public static final Color WINDOW = new Color(0x1E1915);
    public static final Color PANEL = new Color(0x26201A);
    public static final Color RAISED = new Color(0x30281F);
    public static final Color LINE = new Color(0x42372C);

    // 글자 색상
    public static final Color TEXT = new Color(0xEFE6D6);
    public static final Color MUTED = new Color(0xAE9F88);

    // 체스판 색상
    public static final Color SQ_LIGHT = new Color(0xE8D3A9);
    public static final Color SQ_DARK = new Color(0xA3714A);

    // 강조 및 상태 표시 색상
    public static final Color ACCENT = new Color(0xD9AE57);
    public static final Color OK = new Color(0x8FB98A);
    public static final Color BAD = new Color(0xE08A7C);
    public static final Color INFO = new Color(0x93A8BF);

    // 등록된 글꼴
    private static Font bodyFont;
    private static Font titleFont;

    private static boolean installed = false;

    // 일반 글꼴
    public static Font body(int size, int weight) {

        Font base = bodyFont;

        if (base == null) {
            base = findFont("Pretendard", Font.SANS_SERIF);
        }

        return base.deriveFont(weight, (float) size);
    }

    // 제목 글꼴
    public static Font title(int size) {

        Font base = titleFont;

        if (base == null) {
            base = findFont("Noto Serif KR", Font.SERIF);
        }

        return base.deriveFont(Font.BOLD, (float) size);
    }

    // 시스템 글꼴 확인
    private static Font findFont(String preferred, String fallback) {

        Font font = new Font(preferred, Font.PLAIN, 14);

        if (!font.getFamily().equalsIgnoreCase(preferred)) {
            font = new Font(fallback, Font.PLAIN, 14);
        }

        return font;
    }

    // 글꼴 파일 등록
    private static Font registerFont(String path) {

        try (InputStream input =
                Theme.class.getResourceAsStream(path)) {

            if (input == null) {
                return null;
            }

            Font font = Font.createFont(
                Font.TRUETYPE_FONT,
                input
            );

            GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .registerFont(font);

            return font;

        } catch (Exception e) {

            System.err.println(
                "글꼴 등록 실패: " + path
            );

            return null;
        }
    }

    // Metal 테마 설정
    private static class ChessMetalTheme extends DefaultMetalTheme {

        @Override
        public String getName() {
            return "Chess Learner Theme";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(LINE);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(SQ_DARK);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(ACCENT);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(WINDOW);
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(PANEL);
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(RAISED);
        }

        @Override
        protected ColorUIResource getWhite() {
            return new ColorUIResource(TEXT);
        }

        @Override
        protected ColorUIResource getBlack() {
            return new ColorUIResource(WINDOW);
        }
    }

    // Swing 기본 UI 설정
    public static void install() {

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "Theme.install()은 EDT에서 호출해야 합니다."
            );
        }

        if (installed) {
            return;
        }

        // 글꼴 등록
        bodyFont = registerFont(
            "/fonts/Pretendard-Regular.ttf"
        );

        titleFont = registerFont(
            "/fonts/NotoSerifKR-Bold.ttf"
        );


        // Metal 테마 적용
        MetalLookAndFeel.setCurrentTheme(
            new ChessMetalTheme()
        );

        try {

            UIManager.setLookAndFeel(
                new MetalLookAndFeel()
            );

        } catch (UnsupportedLookAndFeelException e) {

            throw new IllegalStateException(
                "Metal 테마 적용에 실패했습니다.",
                e
            );
        }


        // 기본 색상 설정
        UIManager.put(
            "Panel.background",
            new ColorUIResource(WINDOW)
        );

        UIManager.put(
            "Label.foreground",
            new ColorUIResource(TEXT)
        );

        UIManager.put(
            "Button.background",
            new ColorUIResource(RAISED)
        );

        UIManager.put(
            "Button.foreground",
            new ColorUIResource(TEXT)
        );

        UIManager.put(
            "TextField.background",
            new ColorUIResource(PANEL)
        );

        UIManager.put(
            "TextField.foreground",
            new ColorUIResource(TEXT)
        );

        UIManager.put(
            "TextArea.background",
            new ColorUIResource(PANEL)
        );

        UIManager.put(
            "TextArea.foreground",
            new ColorUIResource(TEXT)
        );

        UIManager.put("ScrollBar.background", new ColorUIResource(PANEL));
        UIManager.put("ScrollBar.track", new ColorUIResource(WINDOW));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(RAISED));
        UIManager.put("Table.background", new ColorUIResource(PANEL));
        UIManager.put("Table.foreground", new ColorUIResource(TEXT));
        UIManager.put("Table.selectionBackground", new ColorUIResource(RAISED));
        UIManager.put("Table.selectionForeground", new ColorUIResource(TEXT));
        UIManager.put("TableHeader.background", new ColorUIResource(RAISED));
        UIManager.put("TableHeader.foreground", new ColorUIResource(TEXT));
        UIManager.put("ComboBox.background", new ColorUIResource(PANEL));
        UIManager.put("ComboBox.foreground", new ColorUIResource(TEXT));
        UIManager.put("OptionPane.background", new ColorUIResource(WINDOW));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("Viewport.background", new ColorUIResource(PANEL));


        // 기본 글꼴 설정
        UIManager.put(
            "Label.font",
            body(FONT_16, Font.PLAIN)
        );

        UIManager.put(
            "Button.font",
            body(FONT_16, Font.PLAIN)
        );

        UIManager.put(
            "TextField.font",
            body(FONT_16, Font.PLAIN)
        );

        UIManager.put(
            "TextArea.font",
            body(FONT_16, Font.PLAIN)
        );


        installed = true;
    }

    // 객체 생성 방지
    private Theme() {
    }
}
