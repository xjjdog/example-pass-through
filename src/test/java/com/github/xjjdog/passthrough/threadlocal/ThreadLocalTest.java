package com.github.xjjdog.passthrough.threadlocal;


import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadLocalTest {

    @Test
    public void testThreadLocal() {
        PtClass ptClass = new PtClass();
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(new ThreadLocalRunnable(() -> {
            ptClass.print();
        }));
    }

    final static class PtClass {
//        java.lang.ThreadLocal<Object> holder = new java.lang.ThreadLocal<>();
        ThreadLocal<Object> holder = new ThreadLocal<>();

        public PtClass() {
            holder.set("Fuck you");
        }

        public void print() {
            System.out.println(holder.get());
        }
    }
}
