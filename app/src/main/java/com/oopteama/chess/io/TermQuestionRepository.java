package com.oopteama.chess.io;

import com.oopteama.chess.content.Glossary;
import com.oopteama.chess.content.GlossaryEntry;
import com.oopteama.chess.content.TermQuestion;
import com.oopteama.chess.core.MalformedDataException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 용어 퀴즈 파일({@code data/term-quiz.txt})의 {@code @term-question <id>} 블록을 문항으로 읽는다(QUIZ-05).
 * 잘못된 문항은 그 문항만 건너뛰고 {@code 파일명:행번호: 설명}을 경고 로그에 남긴다. 유효 문항은 파일 순서를 유지한다.
 * <ul>
 *   <li>ID는 영문 소문자·숫자·{@code -}·{@code _}. 같은 ID가 또 나오면 처음 유효한 문항만 남긴다</li>
 *   <li>term은 용어집에 등록된 용어 ID</li>
 *   <li>question과 option.1~option.4 필수. 선택지는 정확히 네 개</li>
 *   <li>answer는 1~4. {@link TermQuestion#answerIndex()}의 0~3으로 바꾼다</li>
 *   <li>explanation이 없거나 비었으면 연결된 용어의 정의를 쓴다</li>
 * </ul>
 */
public final class TermQuestionRepository extends BlockRepository<TermQuestion> {

    private static final Logger LOG = Logger.getLogger(TermQuestionRepository.class.getName());

    private static final String QUESTION = "term-question";
    private static final String OPTION_PREFIX = "option.";
    private static final int CHOICE_COUNT = 4;

    private final Glossary glossary;

    /**
     * @param glossary 문항의 term이 가리킬 용어집
     */
    public TermQuestionRepository(Glossary glossary) {
        this.glossary = Objects.requireNonNull(glossary, "glossary");
    }

    /** 문항 ID 중복은 파일 전체를 봐야 알 수 있어 블록 묶음을 한 번에 다룬다. */
    @Override
    protected List<TermQuestion> assemble(List<Block> blocks) {
        Map<String, TermQuestion> byId = new LinkedHashMap<>();
        for (Block b : blocks) {
            try {
                TermQuestion q = question(b);
                if (byId.putIfAbsent(q.id(), q) != null) {
                    throw BlockChecks.error(b, "문항 ID '" + q.id() + "'가 중복되어 뒤의 문항을 버립니다");
                }
            } catch (MalformedDataException e) {
                LOG.warning(e.getMessage());
            }
        }
        return new ArrayList<>(byId.values());
    }

    private TermQuestion question(Block b) throws MalformedDataException {
        if (!b.type().equals(QUESTION)) {
            throw BlockChecks.error(b, "용어 퀴즈 파일에 쓸 수 없는 블록입니다: "
                    + (b.type().isEmpty() ? "@term-question 앞의 키" : "@" + b.type()));
        }
        String id = BlockChecks.id(b, "문항");
        String where = "문항 " + id;
        String termId = b.require("term");
        GlossaryEntry term = glossary.byId(termId)
                .orElseThrow(() -> BlockChecks.error(b, where + ": 용어집에 없는 용어 ID입니다: '" + termId + "'"));
        String prompt = b.require("question");
        List<String> choices = new ArrayList<>();
        for (int n = 1; n <= CHOICE_COUNT; n++) {
            choices.add(b.require(OPTION_PREFIX + n));
        }
        if (b.sub(OPTION_PREFIX).values().size() != CHOICE_COUNT) {
            throw BlockChecks.error(b, where + ": 선택지는 option.1~option.4 네 개만 씁니다: "
                    + String.join(", ", b.sub(OPTION_PREFIX).values().keySet()));
        }
        int answer = answer(b, where);
        String explanation = b.get("explanation", "");
        return new TermQuestion(id, termId, prompt, choices, answer - 1,
                explanation.isBlank() ? term.definition() : explanation);
    }

    /** 파일의 정답 번호(1~4). */
    private static int answer(Block b, String where) throws MalformedDataException {
        String value = b.require("answer");
        try {
            int n = Integer.parseInt(value);
            if (n >= 1 && n <= CHOICE_COUNT) {
                return n;
            }
        } catch (NumberFormatException e) {
            // 아래에서 같은 메시지로 처리한다
        }
        throw BlockChecks.error(b, where + ": answer는 1~" + CHOICE_COUNT + "입니다: '" + value + "'");
    }
}
