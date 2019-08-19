package com.github.xjjdog.passthrough.threadlocal;

import java.util.concurrent.Callable;

public class ThreadLocalCallable<V> implements Callable<V> {

    private final Callable<V> callable;

    private transient ThreadLocalHandler.Context _cm = ThreadLocalHandler.handle();

    public ThreadLocalCallable(Callable<V> callable) {
        this.callable = callable;
    }

    @Override
    public V call() throws Exception {
        if (_cm != null) {
            _cm.set();
        }
        try {
            return this.callable.call();
        } finally {
            if (_cm != null) {
                _cm.remove();
            }
        }
    }


}
