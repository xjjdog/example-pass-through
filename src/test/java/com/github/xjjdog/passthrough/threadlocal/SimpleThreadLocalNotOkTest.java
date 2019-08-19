package com.github.xjjdog.passthrough.threadlocal;

import org.junit.jupiter.api.Test;

public class SimpleThreadLocalNotOkTest {
    @Test
    void testThreadLocal(){
        ThreadLocal<Object> threadLocal = new ThreadLocal<>();
        threadLocal.set("not ok");
        new Thread(()->{
            System.out.println(threadLocal.get());
        }).start();
    }
}
