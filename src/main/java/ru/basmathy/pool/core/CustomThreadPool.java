package ru.basmathy.pool.core;

import ru.basmathy.pool.api.CustomExecutor;
import ru.basmathy.pool.config.PoolConfig;
import ru.basmathy.pool.factory.NamedThreadFactory;
import ru.basmathy.pool.rejection.AbortRejectPolicy;
import ru.basmathy.pool.rejection.RejectPolicy;
import ru.basmathy.pool.routing.RoundRobinTaskRouter;
import ru.basmathy.pool.routing.TaskRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

public class CustomThreadPool implements CustomExecutor {

    private final PoolConfig config;
    private final ThreadFactory threadFactory;
    private final TaskRouter taskRouter;
    private final RejectPolicy rejectPolicy;
    private final List<PoolWorker> workers = new ArrayList<PoolWorker>();
    private volatile boolean shutdownRequested;
    private int nextQueueNumber = 1;

    public CustomThreadPool(PoolConfig config) {
        this(config, new NamedThreadFactory("classic-pool-worker"), new RoundRobinTaskRouter(), new AbortRejectPolicy());
    }

    public CustomThreadPool(
            PoolConfig config,
            ThreadFactory threadFactory,
            TaskRouter taskRouter,
            RejectPolicy rejectPolicy
    ) {
        if (config == null || threadFactory == null || taskRouter == null || rejectPolicy == null) {
            throw new IllegalArgumentException("pool dependencies must not be null");
        }
        this.config = config;
        this.threadFactory = threadFactory;
        this.taskRouter = taskRouter;
        this.rejectPolicy = rejectPolicy;
        synchronized (this) {
            for (int i = 0; i < config.getCorePoolSize(); i++) {
                addWorker();
            }
            ensureMinSpareThreads();
        }
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException("command must not be null");
        }

        synchronized (this) {
            if (shutdownRequested) {
                rejectPolicy.reject(command);
                return;
            }

            if (workers.isEmpty() && canAddWorker()) {
                addWorker();
            }

            if (tryOfferToExistingQueues(command)) {
                ensureMinSpareThreads();
                return;
            }

            if (canAddWorker()) {
                PoolWorker worker = addWorker();
                if (worker.offer(command)) {
                    PoolLogger.log("POOL", "task accepted by new queue %d", worker.getQueueNumber());
                    ensureMinSpareThreads();
                    return;
                }
            }

            rejectPolicy.reject(command);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException("callable must not be null");
        }
        FutureTask<T> futureTask = new FutureTask<T>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public synchronized void shutdown() {
        shutdownRequested = true;
        PoolLogger.log("POOL", "shutdown requested; accepted tasks will finish");
    }

    @Override
    public synchronized void shutdownNow() {
        shutdownRequested = true;
        int removed = 0;
        for (PoolWorker worker : snapshotWorkers()) {
            removed += worker.drainTasks().size();
            worker.interrupt();
        }
        PoolLogger.log("POOL", "shutdownNow requested; %d queued tasks removed", removed);
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    public synchronized boolean tryRetireWorker(PoolWorker worker) {
        if (shutdownRequested) {
            return false;
        }
        if (workers.size() > config.getCorePoolSize()) {
            workers.remove(worker);
            return true;
        }
        return false;
    }

    public synchronized void onWorkerStopped(PoolWorker worker) {
        workers.remove(worker);
    }

    private boolean tryOfferToExistingQueues(Runnable command) {
        int workerCount = workers.size();
        if (workerCount == 0) {
            return false;
        }

        int startIndex = taskRouter.nextQueueIndex(workerCount);
        for (int offset = 0; offset < workerCount; offset++) {
            int index = (startIndex + offset) % workerCount;
            PoolWorker worker = workers.get(index);
            if (worker.offer(command)) {
                PoolLogger.log("POOL", "task accepted by queue %d", worker.getQueueNumber());
                return true;
            }
        }
        return false;
    }

    private PoolWorker addWorker() {
        PoolWorker worker = new PoolWorker(this, config, nextQueueNumber++);
        workers.add(worker);
        Thread thread = threadFactory.newThread(worker);
        thread.start();
        PoolLogger.log("POOL", "worker added; total workers %d", workers.size());
        return worker;
    }

    private void ensureMinSpareThreads() {
        while (!shutdownRequested && countSpareWorkers() < config.getMinSpareThreads() && canAddWorker()) {
            addWorker();
        }
    }

    private int countSpareWorkers() {
        int spare = 0;
        for (PoolWorker worker : workers) {
            if (worker.isSpare()) {
                spare++;
            }
        }
        return spare;
    }

    private boolean canAddWorker() {
        return workers.size() < config.getMaxPoolSize();
    }

    private synchronized List<PoolWorker> snapshotWorkers() {
        return new ArrayList<PoolWorker>(workers);
    }
}
