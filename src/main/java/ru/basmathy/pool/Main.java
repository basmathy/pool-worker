package ru.basmathy.pool;

import ru.basmathy.pool.config.PoolConfig;
import ru.basmathy.pool.core.CustomThreadPool;
import ru.basmathy.pool.task.DemoTask;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        PoolConfig config = new PoolConfig(
                2,
                4,
                2,
                TimeUnit.SECONDS,
                2,
                1
        );

        CustomThreadPool pool = new CustomThreadPool(config);

        System.out.println("DEMO | normal execution and submit");
        Future<String> result = pool.submit(new java.util.concurrent.Callable<String>() {
            @Override
            public String call() {
                return "callable result";
            }
        });
        System.out.println("DEMO | submit result: " + result.get());

        System.out.println("DEMO | overload: extra workers and rejection");
        for (int i = 1; i <= 14; i++) {
            try {
                pool.execute(new DemoTask(i, 1_500));
            } catch (RejectedExecutionException ex) {
                System.out.println("DEMO | rejection observed for DemoTask#" + i);
            }
        }

        Thread.sleep(5_000);
        System.out.println("DEMO | waiting for idle timeout");
        Thread.sleep(3_500);

        System.out.println("DEMO | graceful shutdown");
        pool.execute(new DemoTask(100, 500));
        pool.execute(new DemoTask(101, 500));
        pool.shutdown();

        Thread.sleep(1_500);

        try {
            pool.execute(new DemoTask(102, 100));
        } catch (RejectedExecutionException ex) {
            System.out.println("DEMO | task after shutdown rejected");
        }
    }
}
