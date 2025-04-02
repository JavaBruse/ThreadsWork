package com.javabruse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final int TASK_COUNT = 200;

    public static void main(String[] args) {
        ThreadQueueMaster threadPool = new ThreadQueueMaster(4, 8, 5, TimeUnit.SECONDS, 25, 2);
        long startTime = System.currentTimeMillis();
        int rejectedTasks = 0;
        for (int i = 1; i <= TASK_COUNT; i++) {
            final int taskId = i;
            try {
                threadPool.execute(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("mm минут ss секунд SSS миллисекунуд");
                    long timeStamp = Instant.now().toEpochMilli();
                    log.info("Начало выполнения задачи #{}", taskId);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    log.info("Задачя #{}, завершена за [{}]", taskId, sdf.format(Instant.now().toEpochMilli() - timeStamp));

                });
            } catch (RejectedExecutionException e) {
                rejectedTasks++;
            }
        }
        long endTime = System.currentTimeMillis();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        threadPool.shutdown();
        log.info("Пул потоков завершён");
        log.info(toConsole(endTime - startTime, rejectedTasks, TASK_COUNT));
    }

    private static String toConsole(long time, int rejectedTasks, int TASK_COUNT){
        return  "Затраченное время = " + time +
                "мс Пропущено задач = " + rejectedTasks +
                " Выполнения задач = " + (TASK_COUNT - rejectedTasks) +
                " Среднее время на одну задачу = " + (double) time /(double) (TASK_COUNT - rejectedTasks) +"мс";
    }
}
