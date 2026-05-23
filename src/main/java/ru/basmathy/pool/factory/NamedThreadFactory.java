package ru.basmathy.pool.factory;

import ru.basmathy.pool.core.PoolLogger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String namePrefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedThreadFactory(String namePrefix) {
        if (namePrefix == null || namePrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("namePrefix must not be blank");
        }
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = namePrefix + "-" + counter.getAndIncrement();
        PoolLogger.log("THREAD_FACTORY", "created %s", name);
        return new Thread(runnable, name);
    }
}
