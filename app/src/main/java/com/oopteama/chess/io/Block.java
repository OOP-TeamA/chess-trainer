package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 공통 블록 형식의 블록 하나. {@code @종류 아이디} 줄과 그 아래의 {@code 키=값} 줄을 담는다.
 *
 * <p>첫 {@code @} 이전의 이름 없는 기본 블록은 {@code type}과 {@code id}가 빈 문자열이다.
 * 아이디 없이 {@code @step}처럼 시작한 블록도 {@code id}가 빈 문자열이다.
 * 값은 {@link BlockReader}가 이스케이프({@code \n}, {@code \\})를 풀고 앞뒤 공백을 제거한 상태다.
 *
 * @param type   블록 종류({@code @} 뒤 첫 단어). 기본 블록이면 빈 문자열
 * @param id     블록 아이디. 없으면 빈 문자열
 * @param values 키와 값. 파일에 나온 순서를 유지하는 불변 사본으로 저장된다
 * @param source 파일명(오류 메시지용)
 * @param line   블록이 시작된 행 번호(1부터)
 */
public record Block(String type, String id, Map<String, String> values, String source, int line) {

    /** 목록 값 구분자. */
    private static final String LIST_SEPARATOR = ",";
    /** 대안 값 구분자(정규식으로 사용하므로 이스케이프). */
    private static final String ALTERNATIVE_SEPARATOR = "\\|";

    /** 모든 필드는 null 불가. 값 맵은 순서를 유지한 불변 사본으로 바꾼다. */
    public Block {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(source, "source");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        values.forEach((k, v) -> copy.put(Objects.requireNonNull(k, "key"), Objects.requireNonNull(v, "value")));
        values = Collections.unmodifiableMap(copy);
    }

    /**
     * 필수 키의 값을 돌려준다.
     *
     * @param key 키
     * @return 값(빈 문자열이 아님)
     * @throws MalformedDataException 키가 없거나 값이 비어 있을 때. 메시지에 파일명·행 번호를 담는다
     */
    public String require(String key) throws MalformedDataException {
        Objects.requireNonNull(key, "key");
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            throw new MalformedDataException(
                    "%s:%d: %s 블록에 필수 키 '%s'가 없습니다".formatted(source, line, label(), key), source, line);
        }
        return value;
    }

    /**
     * 키의 값을 돌려주고, 키가 없으면 기본값을 돌려준다.
     *
     * @param key      키
     * @param fallback 키가 없을 때 돌려줄 값(null 불가)
     * @return 값 또는 기본값
     */
    public String get(String key, String fallback) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fallback, "fallback");
        return values.getOrDefault(key, fallback);
    }

    /**
     * 쉼표로 구분한 목록 값을 돌려준다. 항목의 앞뒤 공백은 제거하고 빈 항목은 버린다.
     *
     * @param key 키
     * @return 불변 목록. 키가 없으면 빈 목록
     */
    public List<String> list(String key) {
        return split(key, LIST_SEPARATOR);
    }

    /**
     * {@code |}로 구분한 대안 값을 돌려준다(예: {@code move:d4f5|d4f3}). 항목의 앞뒤 공백은 제거하고 빈 항목은 버린다.
     *
     * @param key 키
     * @return 불변 목록. 키가 없으면 빈 목록
     */
    public List<String> alternatives(String key) {
        return split(key, ALTERNATIVE_SEPARATOR);
    }

    /**
     * 접두어로 시작하는 키만 모아 접두어를 뗀 블록을 돌려준다.
     * 예: {@code sub("try.")}는 {@code try.fen}을 {@code fen}으로 담는다. 종류·아이디·파일명·행 번호는 그대로다.
     *
     * @param prefix 접두어(점까지 포함해 넘긴다)
     * @return 새 블록. 맞는 키가 없으면 값이 빈 블록
     */
    public Block sub(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        LinkedHashMap<String, String> picked = new LinkedHashMap<>();
        values.forEach((k, v) -> {
            if (k.startsWith(prefix) && k.length() > prefix.length()) {
                picked.put(k.substring(prefix.length()), v);
            }
        });
        return new Block(type, id, picked, source, line);
    }

    private List<String> split(String key, String separatorRegex) {
        Objects.requireNonNull(key, "key");
        String value = values.get(key);
        if (value == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : value.split(separatorRegex)) {
            String item = part.strip();
            if (!item.isEmpty()) {
                out.add(item);
            }
        }
        return List.copyOf(out);
    }

    /** 오류 메시지용 블록 표기. 예: {@code @term fork}, {@code 기본} */
    private String label() {
        if (type.isEmpty()) {
            return "기본";
        }
        return id.isEmpty() ? "@" + type : "@" + type + " " + id;
    }
}
