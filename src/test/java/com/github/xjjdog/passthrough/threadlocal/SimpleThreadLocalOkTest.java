package com.github.xjjdog.passthrough.threadlocal;

import org.junit.jupiter.api.Test;

public class SimpleThreadLocalOkTest {
    @Test
    void testThreadLocal() {
        InheritableThreadLocal<Object> threadLocal = new InheritableThreadLocal<>();
        threadLocal.set("ok");
        new Thread(() -> {
            System.out.println(threadLocal.get());
        }).start();
    }
}
