package com.oopteama.chess.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 용어집(GLOS-01, GLOS-02). 용어 목록을 담고 ID 조회, 링크 이름 해석, 검색을 제공한다. 불변이다.
 *
 * <p>이름 비교는 모두 대소문자와 공백을 무시한다({@code "En passant"}와 {@code "enpassant"}는 같다).
 */
public final class Glossary {

    private static final Logger LOG = Logger.getLogger(Glossary.class.getName());

    /** 한글 음절의 첫 글자와 끝 글자, 초성 하나에 속한 음절 수. */
    private static final char HANGUL_FIRST = '가';
    private static final char HANGUL_LAST = '힣';
    private static final int SYLLABLES_PER_CHOSEONG = 21 * 28;
    /** 초성 19자(유니코드 음절 순서). 초성 검색의 입력도 이 글자들이다. */
    private static final String CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";

    private final List<GlossaryEntry> sorted;
    private final Map<String, GlossaryEntry> byId = new HashMap<>();
    private final Map<String, GlossaryEntry> byName = new HashMap<>();

    /**
     * ID가 중복되면 뒤의 항목을 버리고 경고를 남긴다. 한 용어의 이름(ko, en, aliases)이 다른 용어의 ID나
     * 먼저 나온 용어의 이름과 겹치면 먼저 등록된 용어가 그 이름을 갖고 경고를 남긴다.
     *
     * @param entries 용어 목록(파일 순서)
     */
    public Glossary(List<GlossaryEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<String, GlossaryEntry> unique = new LinkedHashMap<>();
        for (GlossaryEntry e : entries) {
            if (unique.putIfAbsent(e.id(), e) != null) {
                LOG.warning("용어 ID '%s'가 중복되어 뒤의 항목을 버립니다".formatted(e.id()));
            }
        }
        byId.putAll(unique);
        // ID가 이름보다 우선하도록 먼저 모두 등록한다.
        for (GlossaryEntry e : unique.values()) {
            registerName(e.id(), e);
        }
        for (GlossaryEntry e : unique.values()) {
            registerName(e.ko(), e);
            registerName(e.en(), e);
            e.aliases().forEach(alias -> registerName(alias, e));
        }
        List<GlossaryEntry> list = new ArrayList<>(unique.values());
        list.sort(Comparator.comparing(GlossaryEntry::ko).thenComparing(GlossaryEntry::id));
        this.sorted = List.copyOf(list);
    }

    private void registerName(String name, GlossaryEntry entry) {
        String key = normalize(name);
        if (key.isEmpty()) {
            return;
        }
        GlossaryEntry owner = byName.putIfAbsent(key, entry);
        if (owner != null && owner != entry) {
            LOG.warning("용어 '%s'의 이름 '%s'가 용어 '%s'와 겹쳐 '%s'로 해석합니다"
                    .formatted(entry.id(), name, owner.id(), owner.id()));
        }
    }

    /**
     * 모든 용어. 한글명의 가나다순(문자 코드 순서라 영문·숫자로 시작하는 이름이 앞에 온다)이며 같으면 ID 순이다.
     *
     * @return 불변 목록
     */
    public List<GlossaryEntry> all() {
        return sorted;
    }

    /**
     * ID가 정확히 같은 용어.
     *
     * @param id 용어 ID
     * @return 없으면 empty
     */
    public Optional<GlossaryEntry> byId(String id) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id")));
    }

    /**
     * 링크 대상 이름을 용어로 해석한다. id, ko, en, aliases 중 하나와 일치하면 된다(대소문자와 공백 무시).
     *
     * @param text {@code [[용어]]}의 대상 이름
     * @return 일치하는 용어가 없으면 empty
     */
    public Optional<GlossaryEntry> resolve(String text) {
        return Optional.ofNullable(byName.get(normalize(Objects.requireNonNull(text, "text"))));
    }

    /**
     * 한글명과 영문명의 부분 일치로 찾는다(GLOS-02). query가 비었으면 분류로만 거른다.
     * 한글 초성만 입력하면 한글명의 초성과도 비교한다(GLOS-05, {@code ㅍㅋ} → 포크).
     *
     * @param query    검색어. 대소문자와 공백은 무시한다
     * @param category 분류. empty면 전체
     * @return {@link #all()}과 같은 순서의 불변 목록
     */
    public List<GlossaryEntry> search(String query, Optional<TermCategory> category) {
        Objects.requireNonNull(category, "category");
        String q = normalize(Objects.requireNonNull(query, "query"));
        boolean choseongOnly = !q.isEmpty() && q.chars().allMatch(c -> CHOSEONG.indexOf(c) >= 0);
        List<GlossaryEntry> out = new ArrayList<>();
        for (GlossaryEntry e : sorted) {
            if (category.isPresent() && e.category() != category.get()) {
                continue;
            }
            String ko = normalize(e.ko());
            if (q.isEmpty() || ko.contains(q) || normalize(e.en()).contains(q)
                    || (choseongOnly && choseongOf(ko).contains(q))) {
                out.add(e);
            }
        }
        return List.copyOf(out);
    }

    /** 공백을 모두 지우고 소문자로 바꾼다. */
    private static String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /** 한글 음절을 초성으로 바꾼다. 그 밖의 글자는 그대로 둔다. */
    private static String choseongOf(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c >= HANGUL_FIRST && c <= HANGUL_LAST
                    ? CHOSEONG.charAt((c - HANGUL_FIRST) / SYLLABLES_PER_CHOSEONG)
                    : c);
        }
        return sb.toString();
    }
}
