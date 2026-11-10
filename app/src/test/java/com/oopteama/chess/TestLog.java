package com.oopteama.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** 테스트에서 로그 메시지를 모은다. try-with-resources로 쓰거나 테스트가 끝날 때 close()를 부른다. */
public final class TestLog implements AutoCloseable {

    private static final String APP_ROOT = "com.oopteama.chess";

    private final Logger logger;
    private final List<String> messages = new ArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord r) {
            messages.add(r.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private TestLog(String loggerName) {
        this.logger = Logger.getLogger(loggerName);
        logger.addHandler(handler);
    }

    /** owner 클래스의 로거에 남는 메시지를 모은다. */
    public static TestLog capture(Class<?> owner) {
        return new TestLog(owner.getName());
    }

    /** 앱의 모든 로거에 남는 메시지를 모은다. */
    public static TestLog captureAll() {
        return new TestLog(APP_ROOT);
    }

    /** 지금까지 모은 메시지(기록 순서). */
    public List<String> messages() {
        return List.copyOf(messages);
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
    }
}
