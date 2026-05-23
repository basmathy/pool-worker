package ru.basmathy.pool.routing;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinTaskRouter implements TaskRouter {

    private final AtomicInteger next = new AtomicInteger();

    @Override
    public int nextQueueIndex(int queueCount) {
        if (queueCount <= 0) {
            throw new IllegalArgumentException("queueCount must be positive");
        }
        return Math.floorMod(next.getAndIncrement(), queueCount);
    }
}
