package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 블록 하나가 객체 하나인 형식의 저장소(2단 템플릿 메서드의 2단).
 *
 * <p>하위 클래스는 {@link #map(Block)}만 구현한다. 변환에 실패한 블록은 로그에 남기고 건너뛰므로
 * 깨진 항목 하나가 나머지 항목의 로딩을 막지 않는다(SYS-03).
 *
 * @param <T> 만들 객체 타입
 */
public abstract class PerBlockRepository<T> extends BlockRepository<T> {

    private static final Logger LOG = Logger.getLogger(PerBlockRepository.class.getName());

    /**
     * 블록마다 {@link #map(Block)}을 호출해 모은다. 실패한 블록은 건너뛴다.
     *
     * @param blocks 형식 검사를 통과한 블록(파일 순서)
     * @return 변환에 성공한 객체 목록(파일 순서)
     */
    @Override
    protected final List<T> assemble(List<Block> blocks) {
        List<T> out = new ArrayList<>();
        for (Block b : blocks) {
            try {
                out.add(map(b));
            } catch (MalformedDataException e) {
                LOG.warning(e.getMessage());
            }
        }
        return out;
    }

    /**
     * 블록 하나를 객체로 바꾼다.
     *
     * @param block 블록
     * @return 만든 객체(null 불가)
     * @throws MalformedDataException 필수 키가 없거나 값이 잘못됐을 때. 메시지에 파일명·행 번호를 담는다
     */
    protected abstract T map(Block block) throws MalformedDataException;
}
