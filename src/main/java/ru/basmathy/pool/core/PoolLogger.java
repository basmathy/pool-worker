package ru.basmathy.pool.core;

public final class PoolLogger {

    private PoolLogger() {
    }

    public static synchronized void log(String source, String message, Object... args) {
        System.out.printf("%s | %s%n", source, String.format(message, args));
    }
}
