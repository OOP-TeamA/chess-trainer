package com.oopteama.chess.game;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Side;

import java.util.Objects;

/** AI-01: 새 대국 화면이 Route.AiGame에 실어 보내는 설정. */
public record AiSetup(Side userSide, Difficulty difficulty, String startFen, boolean blunderAlert) {

    public AiSetup {
        Objects.requireNonNull(userSide);
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(startFen);
    }

    /** 표준 시작 국면. 실수 알림 기본값은 꺼짐(화면이 Prefs.blunderAlert로 덮어쓴다). */
    public static AiSetup standard(Side userSide, Difficulty d) {
        return new AiSetup(userSide, d, Fen.INITIAL, false);
    }
}
