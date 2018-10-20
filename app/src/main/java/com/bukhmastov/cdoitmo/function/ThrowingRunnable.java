package com.bukhmastov.cdoitmo.function;

@FunctionalInterface
public interface ThrowingRunnable<T extends Throwable> {
    void run() throws T;
}