package ru.basmathy.pool.core;

import ru.basmathy.pool.config.PoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

public class PoolWorker implements Runnable {

    private final CustomThreadPool pool;
    private final PoolConfig config;
    private final BlockingQueue<Runnable> taskQueue;
    private final int queueNumber;
    private volatile Thread thread;
    private volatile boolean runningTask;

    public PoolWorker(CustomThreadPool pool, PoolConfig config, int queueNumber) {
        this.pool = pool;
        this.config = config;
        this.queueNumber = queueNumber;
        this.taskQueue = new ArrayBlockingQueue<Runnable>(config.getQueueSize());
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        PoolLogger.log("WORKER", "%s started with queue %d", thread.getName(), queueNumber);
        try {
            while (!pool.isShutdownRequested() || !taskQueue.isEmpty()) {
                Runnable task = taskQueue.poll(config.getKeepAliveTime(), config.getTimeUnit());
                if (task == null) {
                    if (pool.tryRetireWorker(this)) {
                        PoolLogger.log("WORKER", "%s idle timeout", thread.getName());
                        break;
                    }
                    continue;
                }

                runningTask = true;
                try {
                    PoolLogger.log("WORKER", "%s runs task %s", thread.getName(), task);
                    task.run();
                } catch (RuntimeException ex) {
                    PoolLogger.log("WORKER", "%s task failed: %s", thread.getName(), ex.getMessage());
                } finally {
                    runningTask = false;
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            PoolLogger.log("WORKER", "%s interrupted", Thread.currentThread().getName());
        } finally {
            pool.onWorkerStopped(this);
            PoolLogger.log("WORKER", "%s stopped", Thread.currentThread().getName());
        }
    }

    public boolean offer(Runnable task) {
        return taskQueue.offer(task);
    }

    public List<Runnable> drainTasks() {
        List<Runnable> removed = new ArrayList<Runnable>();
        taskQueue.drainTo(removed);
        for (Runnable runnable : removed) {
            if (runnable instanceof Future<?>) {
                ((Future<?>) runnable).cancel(false);
            }
        }
        return removed;
    }

    public void interrupt() {
        Thread currentThread = thread;
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

    public boolean isSpare() {
        return !runningTask && taskQueue.isEmpty();
    }

    public int getQueueNumber() {
        return queueNumber;
    }
}
