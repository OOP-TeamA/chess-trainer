package com.oopteama.chess.api;

import java.util.Objects;

/** 홈 화면과 좌측 점수 표시를 위한 백엔드 요약 뷰 모델. */
public record HomeSummary(
        int userScore,
        String resumeTitle,
        String resumeDescription,
        int resumeProgressPercent,
        String resumeProgressText,
        ContinueTarget continueTarget,
        String continueId,
        CardMetric lessons,
        CardMetric quiz,
        CardMetric library,
        CardMetric ai
) {

    public enum ContinueTarget {
        LESSONS,
        LESSON_DETAIL,
        PUZZLES,
        AI_GAME,
        LOCAL_GAME
    }

    public record CardMetric(String statusText, int progressPercent) {
        public CardMetric {
            statusText = statusText == null ? "" : statusText;
            progressPercent = Math.max(0, Math.min(100, progressPercent));
        }
    }

    public HomeSummary {
        resumeTitle = resumeTitle == null ? "" : resumeTitle;
        resumeDescription = resumeDescription == null ? "" : resumeDescription;
        resumeProgressPercent = Math.max(0, Math.min(100, resumeProgressPercent));
        resumeProgressText = resumeProgressText == null ? "" : resumeProgressText;
        Objects.requireNonNull(continueTarget, "continueTarget");
        continueId = continueId == null ? "" : continueId;
        Objects.requireNonNull(lessons, "lessons");
        Objects.requireNonNull(quiz, "quiz");
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(ai, "ai");
    }
}
