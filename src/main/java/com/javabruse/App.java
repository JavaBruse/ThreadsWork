package com.javabruse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        CustomExecutor treadQ = new ThreadQueueMaster(5, 10, 5, TimeUnit.SECONDS, 5, 2);

        for (int i = 0; i < 10; i++) {
            treadQ.execute(new Task(i));
        }

        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 10; i++) {
            treadQ.execute(new Task(i));
        }
    }
}
