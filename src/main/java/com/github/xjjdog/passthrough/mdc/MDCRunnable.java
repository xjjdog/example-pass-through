package com.github.xjjdog.passthrough.mdc;

import org.slf4j.MDC;

import java.util.Map;

public class MDCRunnable implements Runnable {

    private final Runnable runnable;

    private transient final Map<String, String> _cm = MDC.getCopyOfContextMap();

    public MDCRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        if (_cm != null) {
            MDC.setContextMap(_cm);
        }
        try {
            runnable.run();
        } finally {
            MDC.clear();
        }
    }

}
