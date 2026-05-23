package ru.basmathy.pool.rejection;

import ru.basmathy.pool.core.PoolLogger;

import java.util.concurrent.RejectedExecutionException;

public class AbortRejectPolicy implements RejectPolicy {

    @Override
    public void reject(Runnable task) {
        PoolLogger.log("REJECT", "task rejected because pool is overloaded: %s", task);
        throw new RejectedExecutionException("Task rejected because custom pool is overloaded");
    }
}
