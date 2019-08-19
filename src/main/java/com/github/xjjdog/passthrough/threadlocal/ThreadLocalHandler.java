package com.github.xjjdog.passthrough.threadlocal;

import java.lang.ThreadLocal;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

public class ThreadLocalHandler {

    private static Field THREAD_LOCALS;

    private static Field THREAD_LOCAL_MAP_TABLE;

    private static Method THREAD_LOCAL_MAP_ENTRY_GET_METHOD;

    private static Field THREAD_LOCAL_MAP_ENTRY_VALUE;

    private static volatile boolean PREPARED = false;

    static {
        try {
            //ThreadLocal is java native-lib,loaded by bootstrap.
            //they(fields) can be cached safely.
            THREAD_LOCALS = Thread.class.getDeclaredField("threadLocals");
            THREAD_LOCALS.setAccessible(true);

            Class mapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            THREAD_LOCAL_MAP_TABLE = mapClass.getDeclaredField("table");
            THREAD_LOCAL_MAP_TABLE.setAccessible(true);
            Class entryClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry");
            THREAD_LOCAL_MAP_ENTRY_VALUE = entryClass.getDeclaredField("value");
            THREAD_LOCAL_MAP_ENTRY_VALUE.setAccessible(true);

            THREAD_LOCAL_MAP_ENTRY_GET_METHOD = entryClass.getMethod("get");
            THREAD_LOCAL_MAP_ENTRY_GET_METHOD.setAccessible(true);

            PREPARED = true;
        } catch (NoSuchFieldException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            //
        }
    }


    /**
     * @return null if no threadLocals
     * @throws Exception
     */
    public static Context handle() {
        if (!PREPARED) {
            return null;
        }

        Thread thread = Thread.currentThread();
        try {
            //also private，reflection
            Object table = THREAD_LOCAL_MAP_TABLE.get(THREAD_LOCALS.get(thread));
            if (table == null) {
                return null;
            }

            int count = Array.getLength(table);
            if (count == 0) {
                return null;
            }

            //
            Map<ThreadLocal, Object> variables = new WeakHashMap<>();
            for (int i = 0; i < count; i++) {
                Object entry = Array.get(table, i);
                if (entry != null) {
                    ThreadLocal key = (ThreadLocal) THREAD_LOCAL_MAP_ENTRY_GET_METHOD.invoke(entry);
                    if (transmissible(key)) {
                        Object value = THREAD_LOCAL_MAP_ENTRY_VALUE.get(entry);
                        variables.put(key, value);
                    }
                }
            }
            return new Context(variables);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            //
        }
        return null;
    }


    private static boolean transmissible(ThreadLocal target) {
        return target == null ? false : target.getClass().isAnnotationPresent(Transmissible.class);
    }


    public static class Context {
        final Map<ThreadLocal, Object> variables;
        final Thread parent;

        protected Context(Map<ThreadLocal, Object> variables) {
            this.variables = variables;
            parent = Thread.currentThread();
        }

        public void set() {
            if (variables == null || variables.isEmpty() || parent == Thread.currentThread()) {
                return;
            }
            variables.forEach((threadLocal, value) -> {
                if (threadLocal != null) {
                    threadLocal.set(value);
                }
            });
        }

        public void remove() {
            if (variables == null || variables.isEmpty()) {
                return;
            }
            variables.forEach((threadLocal, value) -> {
                if (threadLocal != null) {
                    threadLocal.remove();
                }
            });
            variables.clear();
        }
    }

}
