package com.javabruse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class ThreadQueueMaster implements CustomExecutor {

    private static final Logger log = LoggerFactory.getLogger(ThreadQueueMaster.class);

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    private final List<BlockingQueue<Runnable>> queues;
    private final Set<Worker> workers = new HashSet<>();
    private final MasterThreadFactory masterThreadFactory;
    private volatile boolean isShutdown = false;
    private int queueIndex = 0;

    public ThreadQueueMaster(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit, int queueSize, int minSpareThreads) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = Math.max(maxPoolSize, corePoolSize);
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.queues = new ArrayList<>();
        this.masterThreadFactory = new MasterThreadFactory("Master");

        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            log.warn("Задача отклонена, так как пул потоков завершает работу.");
            return;
        }

        synchronized (this) {
            int startIndex = queueIndex;
            do {
                if (queues.get(queueIndex).offer(command)) {
                    log.info("Задача добавлена в очередь {}", queueIndex);
                    return;
                }
                queueIndex = (queueIndex + 1) % queues.size();
            } while (queueIndex != startIndex);

            if (workers.size() < maxPoolSize) {
                log.info("Очереди заполнены. Добавляем новый поток.");
                addWorker();
                queues.get(queueIndex).offer(command);
            } else {
                log.warn("Очереди заполнены. Задача будет отклонена.");
                throw new RejectedExecutionException();
            }

        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        log.info("Исполнитель завершает работу...");
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        workers.forEach(Worker::stopWorker);
        log.info("Исполнитель принудительно завершает работу.");
    }

    private void addWorker() {
        if (workers.size() < maxPoolSize) {
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
            queues.add(queue);
            Worker worker = new Worker(queue);
            workers.add(worker);
            Thread thread = masterThreadFactory.newThread(worker);
            thread.start();
            log.info("Создан новый рабочий поток: {}", thread.getName());
        }
    }

    private synchronized void maintainMinSpareThreads() {
        long idleThreads = workers.stream().filter(worker -> worker.isIdle()).count();
        while (idleThreads < minSpareThreads && workers.size() < maxPoolSize) {
            addWorker();
            idleThreads++;
        }
    }

    private class Worker implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private volatile boolean running = true;
        private long nowDate = 0;
        private boolean firstTry = false;

        public Worker(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (running && !isShutdown) {
                try {
                    Runnable task = queue.poll(keepAliveTime, timeUnit);
                    if (task != null) {
                        log.info("Выполнение задачи в потоке {}", Thread.currentThread().getName());
                        task.run();
                        nowDate = 0;
                    } else if (workers.size() > corePoolSize) {
                        if (firstTry) {
                            if (Instant.now().toEpochMilli() >= nowDate + timeUnit.toMillis(keepAliveTime)) {
                                running = false;
                                workers.remove(this);
                                log.info("Рабочий поток {} остановлен из-за простоя.", Thread.currentThread().getName());
                            }
                        } else {
                            nowDate = Instant.now().toEpochMilli();
                            firstTry = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                maintainMinSpareThreads();
            }
        }

        public void stopWorker() {
            running = false;
        }

        public boolean isIdle() {
            return queue.isEmpty();
        }
    }

    private static class MasterThreadFactory implements ThreadFactory {
        private final String name;
        private int count = 0;

        public MasterThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, name + " - " + (++count));
            log.info("Создан новый поток: {}", thread.getName());
            return thread;
        }
    }
}
