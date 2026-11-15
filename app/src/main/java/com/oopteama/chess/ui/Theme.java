package com.oopteama.chess.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * Chess Learner의 공통 색상과 글꼴을 관리한다.
 * 화면 구성 요소에서는 이 클래스의 색상과 글꼴을 사용해 디자인을 통일한다.
 * 화면 구성 예시 PDF(17페이지)의 다크 브라운 + 골드 테마를 기준으로 한다.
 */
public final class Theme {

    public static final int FONT_12 = 12;
    public static final int FONT_14 = 14;
    public static final int FONT_16 = 16;
    public static final int FONT_21 = 21;
    public static final int FONT_28 = 28;
    public static final int FONT_36 = 36;

    private Theme() {
        // 인스턴스 생성 방지
    }

    // 기본 화면과 사이드바
    public static final Color WINDOW = new Color(0x1E1915);
    public static final Color SIDEBAR = WINDOW;

    // 카드와 패널
    public static final Color PANEL = new Color(0x26201A);
    public static final Color RAISED = new Color(0x30281F);
    public static final Color LINE = new Color(0x42372C);

    // 글자 색상
    public static final Color TEXT = new Color(0xEFE6D6);
    public static final Color MUTED = new Color(0xAE9F88);

    // 체스판 색상
    public static final Color SQ_LIGHT = new Color(0xE8D3A9);
    public static final Color SQ_DARK = new Color(0xA3714A);

    // 강조 및 상태 색상
    public static final Color ACCENT = new Color(0xD9AE57);
    public static final Color OK = new Color(0x8FB98A);
    public static final Color BAD = new Color(0xE08A7C);
    public static final Color INFO = new Color(0x93A8BF);

    // 글꼴 리소스 경로
    private static final String BODY_FONT_RESOURCE =
            "/fonts/Pretendard-Regular.ttf";

    private static final String BODY_BOLD_FONT_RESOURCE =
            "/fonts/Pretendard-Bold.ttf";

    private static final String TITLE_FONT_RESOURCE =
            "/fonts/NotoSerifKR-Bold.ttf";

    // 본문 일반 글꼴
    private static final Font BODY_FONT = loadFont(
            BODY_FONT_RESOURCE,
            new Font(Font.DIALOG, Font.PLAIN, FONT_14)
    );

    // 본문 굵은 글꼴
    private static final Font BODY_BOLD_FONT = loadFont(
            BODY_BOLD_FONT_RESOURCE,
            new Font(Font.DIALOG, Font.BOLD, FONT_14)
    );

    // 제목 글꼴
    private static final Font TITLE_FONT = loadFont(
            TITLE_FONT_RESOURCE,
            new Font(Font.DIALOG, Font.BOLD, FONT_21)
    );

    private static boolean installed;

    private static final class ChessMetalTheme extends DefaultMetalTheme {

        @Override
        public String getName() {
            return "Chess Trainer";
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

    /**
     * 본문 글꼴을 반환한다.
     *
     * weight가 400이면 Pretendard Regular,
     * weight가 500이면 Pretendard Regular,
     * weight가 600 이상이면 Pretendard Bold를 사용한다.
     */
    public static Font body(int size, int weight) {

        int safeSize = Math.max(1, size);

        // 요청된 굵기에 따라 실제 폰트 파일을 선택한다.
        Font baseFont = (weight >= 600)
                ? BODY_BOLD_FONT
                : BODY_FONT;

        return baseFont.deriveFont((float) safeSize);
    }

    /**
     * 제목 글꼴을 반환한다.
     * Noto Serif KR Bold를 사용한다.
     */
    public static Font title(int size) {

        return TITLE_FONT.deriveFont(
                (float) Math.max(1, size)
        );
    }

    /**
     * Swing 기본 Look and Feel과 컴포넌트 기본값을 프로젝트 테마로 설정한다.
     * 컴포넌트를 만들기 전에 EDT에서 한 번 호출한다.
     */
    public static void install() {

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Theme.install() must be called on the Swing EDT."
            );
        }

        if (installed) {
            return;
        }

        try {
            MetalLookAndFeel.setCurrentTheme(new ChessMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());

        } catch (UnsupportedLookAndFeelException e) {
            // Metal을 사용할 수 없으면 기본 Look and Feel을 유지한다.
        }

        applyUiDefaults();

        installed = true;
    }

    /** Swing 컴포넌트의 기본 색상과 글꼴을 설정한다. */
    private static void applyUiDefaults() {

        ColorUIResource window = new ColorUIResource(WINDOW);
        ColorUIResource panel = new ColorUIResource(PANEL);
        ColorUIResource raised = new ColorUIResource(RAISED);
        ColorUIResource line = new ColorUIResource(LINE);
        ColorUIResource text = new ColorUIResource(TEXT);
        ColorUIResource muted = new ColorUIResource(MUTED);
        ColorUIResource accent = new ColorUIResource(ACCENT);

        // 명세의 글꼴 굵기 기준 사용
        FontUIResource normalFont =
                new FontUIResource(body(FONT_14, 400));

        FontUIResource boldFont =
                new FontUIResource(body(FONT_14, 600));

        // 전체 컴포넌트 기본값
        UIManager.put("control", panel);
        UIManager.put("Panel.background", window);
        UIManager.put("Panel.foreground", text);
        UIManager.put("Label.foreground", text);
        UIManager.put("Label.font", normalFont);

        // 버튼
        UIManager.put("Button.background", raised);
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.font", boldFont);
        UIManager.put("Button.focus", accent);
        UIManager.put("Button.select", line);
        UIManager.put("Button.margin", new Insets(8, 14, 8, 14));

        UIManager.put(
                "Button.border",
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LINE),
                        BorderFactory.createEmptyBorder(7, 13, 7, 13)
                )
        );

        // 입력 컴포넌트
        UIManager.put("TextField.background", panel);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.caretForeground", text);
        UIManager.put("TextField.selectionBackground", accent);
        UIManager.put("TextField.selectionForeground", window);
        UIManager.put("TextField.font", normalFont);

        UIManager.put("TextArea.background", panel);
        UIManager.put("TextArea.foreground", text);
        UIManager.put("TextArea.caretForeground", text);
        UIManager.put("TextArea.selectionBackground", accent);
        UIManager.put("TextArea.selectionForeground", window);
        UIManager.put("TextArea.font", normalFont);

        UIManager.put("ComboBox.background", raised);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.font", normalFont);

        // 목록
        UIManager.put("List.background", panel);
        UIManager.put("List.foreground", text);
        UIManager.put("List.selectionBackground", accent);
        UIManager.put("List.selectionForeground", window);

        // 메뉴
        UIManager.put("Menu.background", SIDEBAR);
        UIManager.put("Menu.foreground", text);
        UIManager.put("Menu.font", normalFont);

        UIManager.put("MenuItem.background", SIDEBAR);
        UIManager.put("MenuItem.foreground", text);
        UIManager.put("MenuItem.font", normalFont);

        // 스크롤 영역
        UIManager.put("ScrollPane.background", window);
        UIManager.put("Viewport.background", window);

        UIManager.put("ScrollBar.background", panel);
        UIManager.put("ScrollBar.thumb", raised);
        UIManager.put("ScrollBar.track", window);

        // 기타 상태 표시
        UIManager.put("OptionPane.background", panel);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("OptionPane.messageFont", normalFont);

        UIManager.put("ProgressBar.background", line);
        UIManager.put("ProgressBar.foreground", accent);
        UIManager.put("ProgressBar.font", boldFont);

        UIManager.put("ToolTip.background", raised);
        UIManager.put("ToolTip.foreground", text);
        UIManager.put("ToolTip.font", normalFont);

        UIManager.put("Separator.foreground", line);
        UIManager.put("Separator.background", line);

        UIManager.put("TitledBorder.titleColor", muted);
    }

    /**
     * 리소스 폴더에서 글꼴을 불러온다.
     * 글꼴 파일이 없거나 읽을 수 없으면 기본 글꼴을 사용한다.
     */
    private static Font loadFont(
            String resourcePath,
            Font fallback
    ) {

        try (InputStream input =
                     Theme.class.getResourceAsStream(resourcePath)) {

            if (input == null) {
                return fallback;
            }

            Font loaded = Font.createFont(
                    Font.TRUETYPE_FONT,
                    input
            );

            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .registerFont(loaded);

            return loaded;

        } catch (IOException | java.awt.FontFormatException e) {
            return fallback;
        }
    }
}