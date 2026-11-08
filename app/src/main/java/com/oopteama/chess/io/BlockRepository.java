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
 * @param <T> 만들 객체 타입
 */
public abstract class BlockRepository<T> {

    private static final Logger LOG = Logger.getLogger(BlockRepository.class.getName());

    /**
     * 입력을 블록으로 읽고, 형식 오류는 로그에 남긴 뒤 정상 블록만 {@link #assemble(List)}에 넘긴다.
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
        return List.copyOf(assemble(r.blocks()));
    }

    /**
     * 블록 묶음을 객체로 만든다. 잘못된 항목은 LOG에 남기고 건너뛴다.
     *
     * @param blocks 형식 검사를 통과한 블록(파일 순서)
     * @return 만든 객체 목록(null 원소 불가)
     */
    protected abstract List<T> assemble(List<Block> blocks);
}
