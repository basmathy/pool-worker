package ru.basmathy.pool.config;

import java.util.concurrent.TimeUnit;

public final class PoolConfig {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    public PoolConfig(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueSize,
            int minSpareThreads
    ) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize must be non-negative");
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
        if (corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException("corePoolSize must not exceed maxPoolSize");
        }
        if (keepAliveTime <= 0) {
            throw new IllegalArgumentException("keepAliveTime must be positive");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit must not be null");
        }
        if (queueSize <= 0) {
            throw new IllegalArgumentException("queueSize must be positive");
        }
        if (minSpareThreads < 0) {
            throw new IllegalArgumentException("minSpareThreads must be non-negative");
        }
        if (minSpareThreads > maxPoolSize) {
            throw new IllegalArgumentException("minSpareThreads must not exceed maxPoolSize");
        }

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }
}
