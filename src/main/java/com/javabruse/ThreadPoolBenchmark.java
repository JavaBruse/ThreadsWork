package com.javabruse;

import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPoolBenchmark {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolBenchmark.class);
    private static final int TASK_COUNT = 100;
    private static final int TASK_DURATION_MS = 1000;

    public static void main(String[] args) throws InterruptedException {
        log.info("Тестирование ThreadQueueMaster...");
        ParamsTest paramsCustom = testCustomPool();

        log.info("Тестирование ThreadPoolExecutor...");
        ParamsTest paramsStandard = testStandardPool();

        log.info("\n=== Итоги тестирования ===");
        log.info("ThreadQueueMaster: {}", paramsCustom);
        log.info("ThreadPoolExecutor: {}", paramsStandard);
    }

    private static ParamsTest testCustomPool() {
        ThreadQueueMaster customPool = new ThreadQueueMaster(4, 10, 10, TimeUnit.SECONDS, 5, 2);
        long startTime = System.currentTimeMillis();

        int rejectedTasks = 0;
        for (int i = 0; i < TASK_COUNT; i++) {
            try {
                customPool.execute(createTask());
            } catch (RejectedExecutionException e) {
                log.warn("Задача {} отклонена из-за переполнения очереди", i);
                rejectedTasks++;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.currentTimeMillis();
        customPool.shutdown();

        return new ParamsTest(endTime - startTime, rejectedTasks);
    }

    private static ParamsTest testStandardPool() {
        ThreadPoolExecutor standardPool = new ThreadPoolExecutor(
                4, 8, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(40),
                new ThreadPoolExecutor.AbortPolicy()
        );

        long startTime = System.currentTimeMillis();

        int rejectedTasks = 0;
        for (int i = 0; i < TASK_COUNT; i++) {
            try {
                standardPool.execute(createTask());
            } catch (RejectedExecutionException e) {
                log.warn("Задача {} отклонена из-за переполнения очереди", i);
                rejectedTasks++;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.currentTimeMillis();
        standardPool.shutdown();
        return new ParamsTest(endTime - startTime, rejectedTasks);
    }

    private static Runnable createTask() {
        return () -> {
            try {
                Thread.sleep(TASK_DURATION_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private static class ParamsTest {
        long time;
        int rejectedTasks = 0;

        public ParamsTest(long time, int rejectedTasks) {
            this.time = time;
            this.rejectedTasks = rejectedTasks;
        }

        @Override
        public String toString() {
            return "Затраченное время = " + time +
                    "мс Пропущено задач = " + rejectedTasks +
                    " Выполнения задач = " + (TASK_COUNT - rejectedTasks) +
                    " Среднее время на одну задачу = " + (double) time /(double) (TASK_COUNT - rejectedTasks) +"мс";
        }
    }
}
