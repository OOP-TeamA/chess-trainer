package com.oopteama.chess.training;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 레슨의 한 단계입니다.
 *
 * @param goal          목표: "any"(아무 수나), "check"(체크 걸기), "mate"(체크메이트),
 *                      또는 "e2e3,e2e4"처럼 정답 수 목록
 * @param hintSquares   힌트로 강조할 칸
 * @param opponentReply 정답을 두면 상대가 이어서 둘 수 (오프닝 레슨, 없으면 null)
 */
public record LessonStep(Board startBoard, String goal, List<Position> hintSquares,
                         String prompt, String successText, Move opponentReply) {

    /** 둔 수(move)로 만들어진 국면(after)이 이 단계의 목표를 달성했는지. */
    public boolean isGoalMet(Board after, Move move) {
        return switch (goal) {
            case "any" -> true;
            case "check" -> Rules.isInCheck(after);
            case "mate" -> Rules.isCheckmate(after);
            default -> solutionMoves().contains(move);
        };
    }

    /** 힌트로 보여 줄 대표 정답 수 (board 국면에서 둘 수 있는 것 중 첫 번째). */
    public Optional<Move> primarySolution(Board board) {
        List<Move> legal = Rules.legalMoves(board);
        return switch (goal) {
            case "any" -> legal.stream().findFirst();
            case "check" -> legal.stream().filter(m -> Rules.isInCheck(board.apply(m))).findFirst();
            case "mate" -> legal.stream().filter(m -> Rules.isCheckmate(board.apply(m))).findFirst();
            default -> solutionMoves().stream().filter(legal::contains).findFirst();
        };
    }

    /** goal에 적힌 정답 수 목록. */
    private List<Move> solutionMoves() {
        List<Move> moves = new ArrayList<>();
        for (String text : goal.split(",")) {
            if (!text.isBlank()) {
                moves.add(Move.of(text.trim()));
            }
        }
        return moves;
    }
}
