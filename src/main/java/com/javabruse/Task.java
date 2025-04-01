package com.javabruse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class Task implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Task.class);
    private int taskNumber;

    public Task(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    @Override
    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat("mm минут ss секунд SSS миллисекунуд");

        long timeStamp = Instant.now().toEpochMilli();
        log.info("Начало выполнения задачи #{}", taskNumber);
        try {
            Thread.sleep(200 + (long) (Math.random() * (1000 - 200)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Задачя #{}, завершена за [{}]", taskNumber, sdf.format(Instant.now().toEpochMilli() - timeStamp));
    }

}
