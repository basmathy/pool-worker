package ru.basmathy.pool.task;

public class DemoTask implements Runnable {

    private final int id;
    private final long workTimeMillis;

    public DemoTask(int id, long workTimeMillis) {
        this.id = id;
        this.workTimeMillis = workTimeMillis;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(workTimeMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "DemoTask#" + id;
    }
}
