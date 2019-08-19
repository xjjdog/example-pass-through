package com.github.xjjdog.passthrough.threadlocal;

public class ThreadLocalRunnable implements Runnable {

    private final Runnable runnable;

    private transient final ThreadLocalHandler.Context _cm = ThreadLocalHandler.handle();

    public ThreadLocalRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        if (_cm != null) {
            _cm.set();
        }
        try {
            runnable.run();
        } finally {
            if (_cm != null) {
                _cm.remove();
            }
        }
    }
}
