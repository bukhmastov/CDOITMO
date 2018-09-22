package com.bukhmastov.cdoitmo.interfaces;

@FunctionalInterface
public interface ThrowingRunnable<T extends Throwable> {
    void run() throws T;
}