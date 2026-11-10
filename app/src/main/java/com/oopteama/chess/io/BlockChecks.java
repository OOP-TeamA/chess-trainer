package com.oopteama.chess.io;

import com.oopteama.chess.content.goal.CheckGoal;
import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.content.goal.MateGoal;
import com.oopteama.chess.content.goal.MoveGoal;
import com.oopteama.chess.content.goal.StepGoal;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.MalformedDataException;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 콘텐츠 저장소들이 함께 쓰는 블록 값 읽기와 로드 시점 검증. 오류는 모두 {@code 파일명:행번호: 설명} 형태다.
 * {@code where}는 "레슨 knight 2단계"처럼 설명 앞에 붙일 항목 이름이다.
 */
final class BlockChecks {

    /** 콘텐츠 ID(레슨, 용어, 용어 퀴즈 문항): 영문 소문자·숫자로 시작하고 {@code -}, {@code _}를 쓸 수 있다. */
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    private BlockChecks() {
    }

    /** 블록 위치를 붙인 형식 오류. */
    static MalformedDataException error(Block b, String message) {
        return new MalformedDataException("%s:%d: %s".formatted(b.source(), b.line(), message), b.source(), b.line());
    }

    /** 블록 ID가 콘텐츠 ID 규칙에 맞는지 확인하고 돌려준다. {@code kind}는 "레슨"처럼 오류에 쓸 종류 이름. */
    static String id(Block b, String kind) throws MalformedDataException {
        if (!ID.matcher(b.id()).matches()) {
            throw error(b, kind + " ID는 영문 소문자·숫자·-·_로 씁니다: '" + b.id() + "'");
        }
        return b.id();
    }

    /** 필수 키의 FEN을 읽어 국면으로 바꾼다. */
    static Position fen(Block b, String where, String key) throws MalformedDataException {
        try {
            return Fen.parse(b.require(key));
        } catch (FenFormatException e) {
            throw error(b, where + ": " + key + " 형식 오류: " + e.getMessage());
        }
    }

    /** 필수 키의 목표 문자열을 읽는다. */
    static StepGoal goal(Block b, String where, String key) throws MalformedDataException {
        String spec = b.require(key);
        try {
            return StepGoal.parse(spec);
        } catch (MalformedDataException e) {
            throw error(b, where + ": " + e.getMessage());
        }
    }

    /** 쉼표로 구분한 칸 목록. 키가 없으면 빈 집합. */
    static Set<Square> squares(Block b, String where, String key) throws MalformedDataException {
        Set<Square> out = new LinkedHashSet<>();
        for (String name : b.list(key)) {
            try {
                out.add(Square.of(name.toLowerCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw error(b, where + ": " + key + "의 칸 이름이 잘못됐습니다: '" + name + "'");
            }
        }
        return out;
    }

    /**
     * 시작 국면에서 목표를 이룰 수 없는 이유를 찾는다(P1, G7). 풀 수 없는 문제가 화면에 나오지 않게 로드 시점에 거른다.
     * 체크·메이트 목표는 상대 킹이 있어야 하고, {@code move:}는 적힌 모든 수가 합법이어야 하며,
     * 그 밖의 이동 목표는 정답 수가 하나 이상 있어야 한다.
     *
     * @return 이룰 수 없는 이유. 이룰 수 있으면 empty
     */
    static Optional<String> unreachable(Position start, RuleSet rules, StepGoal goal) {
        if ((goal instanceof CheckGoal || goal instanceof MateGoal)
                && start.kingSquare(start.sideToMove().opposite()).isEmpty()) {
            return Optional.of("체크·메이트 목표인데 상대 킹이 국면에 없습니다");
        }
        if (goal instanceof MoveGoal move) {
            for (String uci : move.uci()) {
                if (rules.fromUci(start, uci).isEmpty()) {
                    return Optional.of("정답 수 " + uci + "는 이 국면에서 둘 수 없습니다");
                }
            }
        } else if (goal.inputMode() == InputMode.MOVE && goal.solution(start, rules).isEmpty()) {
            return Optional.of("이 국면에는 목표를 이루는 수가 없습니다");
        }
        return Optional.empty();
    }
}
