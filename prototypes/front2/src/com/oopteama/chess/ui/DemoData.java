package com.oopteama.chess.ui;

import java.util.*;

public final class DemoData {
    public static final class Lesson {
        public final String title, tagline;
        public final String[] steps;
        Lesson(String title, String tagline, String... steps) { this.title=title; this.tagline=tagline; this.steps=steps; }
    }
    public static final class Question {
        public final String q, explanation;
        public final String[] options;
        public final int correct;
        Question(String q, int correct, String explanation, String... options) {
            this.q=q; this.correct=correct; this.explanation=explanation; this.options=options;
        }
    }
    public static final class Puzzle {
        public final String title, hint, explanation, board, from, to;
        Puzzle(String title, String hint, String explanation, String board, String from, String to) {
            this.title=title; this.hint=hint; this.explanation=explanation; this.board=board; this.from=from; this.to=to;
        }
    }
    public static final Lesson[] LESSONS = {
        new Lesson("체스판과 좌표", "64칸의 보드와 위치를 읽어 봅니다.",
            "체스판은 8×8로, 총 64개의 밝고 어두운 칸이 번갈아 있습니다.", "가로줄은 a부터 h까지 파일(file), 세로줄은 1부터 8까지 랭크(rank)입니다.", "백의 오른쪽 아래 칸은 밝은색(h1)입니다. 예: e4는 e파일과 4랭크의 교차점입니다."),
        new Lesson("기물의 이동", "킹·퀸·룩·비숍·나이트·폰", 
            "룩은 가로·세로, 비숍은 대각선, 퀸은 두 방식을 합쳐 이동합니다.", "킹은 한 칸씩 이동하지만 공격받는 칸으로 이동할 수 없습니다.", "나이트는 2칸+1칸의 L자로 이동하며 기물을 뛰어넘을 수 있습니다. 폰은 전진하지만 대각선으로 잡습니다."),
        new Lesson("나이트 집중 학습", "L자 이동과 포크 전술 익히기",
            "나이트(Knight)는 한 방향으로 2칸, 직각 방향으로 1칸 이동합니다.", "나이트는 다른 기물을 뛰어넘을 수 있어 복잡한 포지션에서 유용합니다.", "나이트 포크(Fork)는 한 번의 이동으로 두 개 이상의 기물을 동시에 공격하는 전술입니다."),
        new Lesson("체크와 체크메이트", "킹의 안전과 승리 조건",
            "체크(Check)는 킹이 상대의 공격을 받는 상태입니다.", "체크를 받으면 킹 이동·공격 차단·공격 기물 잡기 중 합법적인 방법으로 대응해야 합니다.", "체크메이트(Checkmate)는 체크에서 벗어날 합법적인 수가 전혀 없는 상태입니다."),
        new Lesson("특수 규칙", "캐슬링·앙파상·프로모션",
            "캐슬링은 킹과 룩을 함께 이동시키는 특별한 수입니다. 여러 제한이 있습니다.", "앙파상은 특정 상황에서 폰이 상대 폰을 지나가며 잡는 규칙입니다.", "폰이 마지막 랭크에 도달하면 퀸·룩·비숍·나이트 중 하나로 승격합니다."),
        new Lesson("기초 전술", "포크·핀·스큐어",
            "포크(Fork)는 하나의 기물이 여러 목표를 공격하는 것입니다.", "핀(Pin)은 뒤의 더 중요한 기물 때문에 앞의 기물이 움직이기 어려운 상황입니다.", "스큐어(Skewer)는 중요한 기물이 공격받아 물러나면서 뒤의 기물이 노출되는 전술입니다.")
    };
    public static final Question[] QUESTIONS = {
        new Question("체스판의 칸 수는?", 2, "8×8=64칸입니다.", "32칸", "48칸", "64칸", "81칸"),
        new Question("다른 기물을 뛰어넘을 수 있는 기물은?", 1, "나이트만 일반적인 이동에서 다른 기물을 뛰어넘습니다.", "비숍", "나이트", "룩", "퀸"),
        new Question("룩(Rook)의 이동 방향은?", 0, "룩은 가로와 세로 방향으로 이동합니다.", "가로·세로", "대각선", "L자", "한 칸만"),
        new Question("비숍(Bishop)의 이동 방향은?", 1, "비숍은 대각선으로 이동합니다.", "가로·세로", "대각선", "L자", "앞으로만"),
        new Question("체크(Check)란?", 3, "킹이 상대의 공격을 받는 상태입니다.", "퀸이 잡힌 상황", "기물이 없는 상황", "무승부 상태", "킹이 공격받는 상황"),
        new Question("체크메이트(Checkmate)란?", 2, "킹이 체크에 빠졌고 벗어날 합법적인 수가 없습니다.", "킹이 실제로 잡힌 때", "폰이 승격한 때", "체크에서 벗어날 방법이 없을 때", "시간이 남지 않을 때"),
        new Question("폰(Pawn)이 마지막 랭크에 도달하면?", 0, "퀸, 룩, 비숍 또는 나이트로 승격합니다.", "프로모션", "앙파상", "캐슬링", "스테일메이트"),
        new Question("캐슬링에서 같이 움직이는 기물은?", 1, "킹과 룩이 함께 이동합니다.", "퀸과 비숍", "킹과 룩", "폰과 나이트", "룩과 비숍"),
        new Question("나이트의 기본 이동 형태는?", 3, "한 방향 2칸과 직각 1칸을 합친 L자입니다.", "직선", "대각선", "앞 한 칸", "L자"),
        new Question("한 기물이 두 목표를 동시에 공격하는 전술은?", 2, "이 전술을 포크(Fork)라고 합니다.", "핀", "캐슬링", "포크", "앙파상")
    };
    // 간단한 목표 수 찾기 데모. FEN에서 활성 색상은 백(w)으로 고정.
    public static final Puzzle[] PUZZLES = {
        new Puzzle("01 · 나이트 포크", "백 나이트를 찾아 퀸과 룩을 동시에 공격해 보세요.", "나이트 e5→c6은 b8 퀸과 a7 룩을 동시에 위협합니다.", "1q2k3/r7/8/4N3/8/8/8/4K3", "e5", "c6"),
        new Puzzle("02 · 룩 전진", "룩을 7랭크로 보내세요.", "룩 a1→a7은 열린 파일을 활용하는 수입니다.", "4k3/8/8/8/8/8/8/R3K3", "a1", "a7"),
        new Puzzle("03 · 비숍 대각선", "비숍을 중앙으로 이동해 보세요.", "비숍 c1→g5는 긴 대각선을 활용합니다.", "4k3/8/8/8/8/8/8/2B1K3", "c1", "g5"),
        new Puzzle("04 · 퀸 중앙 장악", "퀸을 d5로 이동하세요.", "퀸 d1→d5는 중앙에서 영향력을 넓힙니다.", "4k3/8/8/8/8/8/8/3QK3", "d1", "d5"),
        new Puzzle("05 · 폰 전진", "e폰을 두 칸 전진하세요.", "폰 e2→e4는 중앙을 차지하는 대표적인 시작 수입니다.", "4k3/8/8/8/8/8/4P3/4K3", "e2", "e4"),
        new Puzzle("06 · 나이트 개발", "나이트를 f3으로 옮기세요.", "나이트 g1→f3은 중앙을 공격하며 기물을 개발합니다.", "4k3/8/8/8/8/8/8/4K1N1", "g1", "f3"),
        new Puzzle("07 · 비숍 개발", "비숍을 b5로 이동하세요.", "비숍 f1→b5는 열린 대각선을 활용한 개발 수입니다.", "4k3/8/8/8/8/8/8/4KB2", "f1", "b5"),
        new Puzzle("08 · 룩 파일 장악", "룩을 e파일로 옮기세요.", "룩 a1→e1은 열린 파일로 룩을 배치합니다.", "4k3/8/8/8/8/8/8/R5K1", "a1", "e1"),
        new Puzzle("09 · 나이트 점프", "나이트를 d4로 이동하세요.", "나이트 f3→d4는 중앙의 좋은 위치를 확보합니다.", "4k3/8/8/8/8/5N2/8/4K3", "f3", "d4"),
        new Puzzle("10 · 폰 한 칸", "d폰을 d5로 옮기세요.", "폰 d4→d5는 한 칸 전진하는 합법적인 수입니다.", "4k3/8/8/8/3P4/8/8/4K3", "d4", "d5")
    };
    private DemoData() {}
}
