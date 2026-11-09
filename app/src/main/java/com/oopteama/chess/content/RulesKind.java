package com.oopteama.chess.content;

import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.core.rules.SandboxRules;
import com.oopteama.chess.core.rules.StandardRules;

/** 학습 단계에 적용할 규칙. 레슨 파일의 {@code rules=standard|sandbox}. */
public enum RulesKind {
    /** 표준 체스 규칙. 양쪽 킹이 정확히 하나씩 있어야 한다. */
    STANDARD(new StandardRules()),
    /** 킹이 없거나 한쪽만 있는 교육용 국면의 규칙(RULE-05). */
    SANDBOX(new SandboxRules());

    /** 두 규칙 모두 상태가 없어 인스턴스를 공유한다. */
    private final RuleSet rules;

    RulesKind(RuleSet rules) {
        this.rules = rules;
    }

    /**
     * 이 종류의 규칙 객체.
     *
     * @return 공유 RuleSet
     */
    public RuleSet rules() {
        return rules;
    }
}
