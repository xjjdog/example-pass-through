package com.github.xjjdog.passthrough.threadlocal;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transmissible {
}
