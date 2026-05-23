package ru.basmathy.pool.rejection;

public interface RejectPolicy {

    void reject(Runnable task);
}
