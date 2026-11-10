package com.oopteama.chess.io;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 블록 파일을 객체 목록으로 읽는 2단 템플릿 메서드의 1단.
 *
 * <p>레슨처럼 블록 여러 개가 객체 하나가 되는 형식은 이 클래스를 상속해 {@link #assemble(List)}를 구현한다.
 * 블록 하나가 객체 하나면 {@link PerBlockRepository}를 상속한다.
 *
 * <p>파일 전체가 객체 하나라서 일부만 읽으면 안 되는 형식(레슨)은 {@link #skipFileOnProblems()}를
 * {@code true}로 재정의한다.
 *
 * @param <T> 만들 객체 타입
 */
public abstract class BlockRepository<T> {

    private static final Logger LOG = Logger.getLogger(BlockRepository.class.getName());

    /**
     * 입력을 블록으로 읽고, 형식 오류는 로그에 남긴 뒤 정상 블록만 {@link #assemble(List)}에 넘긴다.
     *
     * <p>{@link #skipFileOnProblems()}가 {@code true}이고 형식 오류가 하나라도 있거나 읽을 블록이 없으면
     * {@code assemble}을 부르지 않고 파일명을 담은 경고를 남긴 뒤 빈 목록을 돌려준다.
     *
     * @param in     읽을 입력(UTF-8). 닫지 않는다
     * @param source 오류 메시지에 쓸 파일명
     * @return 만든 객체의 불변 목록
     * @throws IOException 입력을 읽지 못했을 때
     */
    public final List<T> loadAll(Reader in, String source) throws IOException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(source, "source");
        BlockReader.Result r = BlockReader.read(in, source);
        r.problems().forEach(p -> LOG.warning(p.getMessage()));
        if (skipFileOnProblems()) {
            if (!r.problems().isEmpty()) {
                LOG.warning(source + ": 형식 오류가 " + r.problems().size() + "건 있어 파일 전체를 건너뜁니다");
                return List.of();
            }
            if (r.blocks().isEmpty()) {
                LOG.warning(source + ": 읽을 블록이 없어 파일을 건너뜁니다");
                return List.of();
            }
        }
        return List.copyOf(assemble(r.blocks()));
    }

    /**
     * 블록 묶음을 객체로 만든다. 잘못된 항목은 LOG에 남기고 건너뛴다.
     *
     * @param blocks 형식 검사를 통과한 블록(파일 순서)
     * @return 만든 객체 목록(null 원소 불가)
     */
    protected abstract List<T> assemble(List<Block> blocks);

    /**
     * 형식 오류가 있는 파일을 통째로 건너뛸지 정한다. 기본값은 {@code false}로, 깨진 블록만 빼고 나머지를 읽는다.
     *
     * <p>파일 하나가 객체 하나인 형식(레슨: 단계 하나라도 잘못되면 레슨 전체를 건너뜀)은 {@code true}로 재정의한다.
     * 그러면 깨진 블록이 빠진 채 객체가 만들어지지 않고, 빈 파일도 {@code assemble} 전에 파일명과 함께 걸러진다.
     *
     * @return 형식 오류나 빈 파일이면 파일 전체를 건너뛸 때 {@code true}
     */
    protected boolean skipFileOnProblems() {
        return false;
    }
}
