package com.oopteama.chess.model;

import com.oopteama.chess.engine.Board;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece.Side;

import java.util.List;
import java.util.Optional;

/**
 * 레슨(기본 튜토리얼 및 유명 오프닝)과 각 학습 단계를 정의합니다.
 */
public record Lesson(
        String id,
        Category category,
        String title,
        String summary,
        Side playerSide,
        List<Step> steps
) {
    public enum Category {
        TUTORIAL("기본 튜토리얼"),
        OPENING("유명 오프닝");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Step(
            Board startBoard,
            String goalSpec,
            List<Integer> hintSquares,
            String prompt,
            String successText,
            Move opponentReply
    ) {
        public boolean matches(Board before, Board after, Move move) {
            return switch (goalSpec) {
                case "any" -> true;
                case "check" -> after.isInCheck();
                case "mate" -> after.isCheckmate();
                default -> {
                    for (String token : goalSpec.split(",")) {
                        String trimmed = token.trim();
                        if (!trimmed.isEmpty() && Move.fromUci(trimmed).equals(move)) {
                            yield true;
                        }
                    }
                    yield false;
                }
            };
        }

        public Optional<Move> primarySolution(Board board) {
            List<Move> legal = board.legalMoves();
            return switch (goalSpec) {
                case "any" -> legal.isEmpty() ? Optional.empty() : Optional.of(legal.getFirst());
                case "check" -> legal.stream().filter(m -> board.applyMove(m).isInCheck()).findFirst();
                case "mate" -> legal.stream().filter(m -> board.applyMove(m).isCheckmate()).findFirst();
                default -> {
                    String first = goalSpec.split(",")[0].trim();
                    Move m = Move.fromUci(first);
                    yield legal.contains(m) ? Optional.of(m) : Optional.empty();
                }
            };
        }
    }
}
