/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements. See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership. The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.stratos.common.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Utility class for Stratos thread pool
 */
public class StratosThreadPool {

    private static final Log log = LogFactory.getLog(StratosThreadPool.class);

    private static Map<String, ThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();
    private static Map<String, ScheduledExecutorService> scheduledServiceMap = new ConcurrentHashMap<String, ScheduledExecutorService>();
    private static Object executorServiceMapLock = new Object();
    private static Object scheduledServiceMapLock = new Object();

    /**
     * Return the executor service based on the identifier and thread pool size
     *
     * @param identifier     Thread pool identifier name
     * @param maxSize Thread pool size
     * @return ExecutorService
     */
    public static ThreadPoolExecutor getExecutorService(String identifier, int initialSize, int
            maxSize) {
        ThreadPoolExecutor executor = executorMap.get(identifier);
        if (executor == null) {
            synchronized (executorServiceMapLock) {
                if (executor == null) {
                    executor = new ThreadPoolExecutor(initialSize, maxSize, 60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(), new StratosThreadFactory(identifier));
                    executorMap.put(identifier, executor);
                    log.info(String.format("Thread pool created: [type] Executor [id] %s " +
                            "[initial size] %d [max size] %d", identifier, initialSize, maxSize));
                }
            }
        }
        return executor;
    }

    /**
     * Returns a scheduled executor for given thread pool size.
     *
     * @param identifier     Thread pool identifier name
     * @param threadPoolSize Thread pool size
     * @return
     */
    public static ScheduledExecutorService getScheduledExecutorService(String identifier, int threadPoolSize) {
        ScheduledExecutorService scheduledExecutorService = scheduledServiceMap.get(identifier);
        if (scheduledExecutorService == null) {
            synchronized (scheduledServiceMapLock) {
                if (scheduledExecutorService == null) {
                    scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolSize,
                            new StratosThreadFactory(identifier));
                    scheduledServiceMap.put(identifier, scheduledExecutorService);
                    log.info(String.format("Thread pool created: [type] Scheduled Executor Service [id] %s [size] %d",
                            identifier, threadPoolSize));
                }
            }

        }
        return scheduledExecutorService;
    }

    public static void shutDownAllThreadPoolsGracefully () {

        int threadPoolCount = executorMap.size();
        if (threadPoolCount == 0) {
            log.info("No thread pools found to shut down");
            return;
        }

        Set<Future<String>> threadPoolTerminatorFutures = new HashSet<>();
        ExecutorService threadPoolTerminator = null;

        try {
            threadPoolTerminator = Executors.newFixedThreadPool(threadPoolCount);
            for (Map.Entry<String, ThreadPoolExecutor> entry : executorMap.entrySet()) {
                threadPoolTerminatorFutures.add(threadPoolTerminator.submit(new GracefulThreadPoolTerminator(entry.getKey(),
                        entry.getValue())));
            }
            // use the Future to block until shutting down is done
            for (Future<String> threadPoolTerminatorFuture : threadPoolTerminatorFutures) {
                removeThreadPoolFromCache(threadPoolTerminatorFuture.get());
            }

        } catch (InterruptedException e) {
            log.error("Error in shutting down thread pools", e);
        } catch (ExecutionException e) {
            log.error("Error in shutting down thread pools", e);
        } finally {
            // if there are any remaining thread pools, shut down immediately
            if (!executorMap.isEmpty()) {
                for (Map.Entry<String, ThreadPoolExecutor> entry : executorMap.entrySet()) {
                    entry.getValue().shutdownNow();
                    removeThreadPoolFromCache(entry.getKey());
                }
            }

            // shut down the threadPoolTerminator threads
            threadPoolTerminator.shutdownNow();
        }
    }

    public static void shutDownAllScheduledExecutorsGracefully () {

        int threadPoolCount = scheduledServiceMap.size();
        if (threadPoolCount == 0) {
            log.info("No thread pools found to shut down");
            return;
        }

        Set<Future<String>> threadPoolTerminatorFutures = new HashSet<>();
        ExecutorService threadPoolTerminator = null;

        try {
            threadPoolTerminator = Executors.newFixedThreadPool(threadPoolCount);
            for (Map.Entry<String, ScheduledExecutorService> entry : scheduledServiceMap.entrySet()) {
                threadPoolTerminatorFutures.add(threadPoolTerminator.submit(new GracefulScheduledThreadPoolTerminator(entry.getKey(),
                        entry.getValue())));
            }
            // use the Future to block until shutting down is done
            for (Future<String> threadPoolTerminatorFuture : threadPoolTerminatorFutures) {
                removeScheduledThreadPoolFromCache(threadPoolTerminatorFuture.get());
            }

        } catch (InterruptedException e) {
            log.error("Error in shutting down thread pools", e);
        } catch (ExecutionException e) {
            log.error("Error in shutting down thread pools", e);
        } finally {
            // if there are any remaining thread pools, shut down immediately
            if (!scheduledServiceMap.isEmpty()) {
                for (Map.Entry<String, ScheduledExecutorService> entry : scheduledServiceMap.entrySet()) {
                    entry.getValue().shutdownNow();
                    removeScheduledThreadPoolFromCache(entry.getKey());
                }
            }

            // shut down the threadPoolTerminator threads
            threadPoolTerminator.shutdownNow();
        }
    }

    private static void removeThreadPoolFromCache(String terminatedPoolId) {
        if (executorMap.remove(terminatedPoolId) != null) {
            log.info("Thread pool [id] " + terminatedPoolId + " is successfully shut down" +
                    " and removed from the cache");
        }
    }

    private static void removeScheduledThreadPoolFromCache(String terminatedPoolId) {
        if (scheduledServiceMap.remove(terminatedPoolId) != null) {
            log.info("Scheduled Thread pool [id] " + terminatedPoolId + " is successfully shut down" +
                    " and removed from the cache");
        }
    }

    private static class GracefulThreadPoolTerminator implements Callable {

        private String threadPoolId;
        private ThreadPoolExecutor executor;

        public GracefulThreadPoolTerminator (String threadPoolId, ThreadPoolExecutor executor) {
            this.threadPoolId = threadPoolId;
            this.executor = executor;
        }

        @Override
        public String call() throws Exception {
            log.info("Shutting down thread pool " + threadPoolId);
            // try to shut down gracefully
            executor.shutdown();
            // wait 10 secs till terminated
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("Thread Pool [id] " + threadPoolId + " did not finish all tasks before " +
                        "timeout, forcefully shutting down");
                executor.shutdownNow();
            }
            return threadPoolId;
        }
    }

    private static class GracefulScheduledThreadPoolTerminator implements Callable {

        private String threadPoolId;
        private ScheduledExecutorService scheduledExecutor;

        public GracefulScheduledThreadPoolTerminator (String threadPoolId, ScheduledExecutorService scheduledExecutor) {
            this.threadPoolId = threadPoolId;
            this.scheduledExecutor = scheduledExecutor;
        }

        @Override
        public String call() throws Exception {
            log.info("Shutting down scheduled thread pool " + threadPoolId);
            // try to shut down gracefully
            scheduledExecutor.shutdown();
            // wait 10 secs till terminated
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("Scheduled thread Pool [id] " + threadPoolId + " did not finish all tasks before " +
                        "timeout, forcefully shutting down");
                scheduledExecutor.shutdownNow();
            }
            return threadPoolId;
        }
    }
}
