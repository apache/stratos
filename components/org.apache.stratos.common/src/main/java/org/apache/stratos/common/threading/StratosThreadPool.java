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

    private static volatile Map<String, ThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();
    private static volatile Map<String, ScheduledThreadPoolExecutor> scheduledExecutorMap = new ConcurrentHashMap<>();
    private static final Object executorServiceMapLock = new Object();
    private static final Object scheduledServiceMapLock = new Object();

    /**
     * Return the executor based on the identifier and thread pool size
     *
     * @param identifier     Thread pool identifier name
     * @param maxSize Thread pool size
     * @return ThreadPoolExecutor
     */
    public static ThreadPoolExecutor getExecutorService(String identifier, int initialSize, int maxSize) {
        ThreadPoolExecutor executor = executorMap.get(identifier);
        if (executor == null) {
            synchronized (executorServiceMapLock) {
                if (executor == null) {
                    int taskQueueSize = initialSize > 3 ? (int)Math.ceil(initialSize/3) : 1;
                    executor = new ThreadPoolExecutor(initialSize, maxSize, 60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(taskQueueSize), new StratosThreadFactory(identifier));
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
     * @return ScheduledThreadPoolExecutor
     */
    public static ScheduledThreadPoolExecutor getScheduledExecutorService(String identifier, int threadPoolSize) {
        ScheduledThreadPoolExecutor scheduledExecutor = scheduledExecutorMap.get(identifier);
        if (scheduledExecutor == null) {
            synchronized (scheduledServiceMapLock) {
                if (scheduledExecutor == null) {
                    scheduledExecutor = new ScheduledThreadPoolExecutor(threadPoolSize,
                            new StratosThreadFactory(identifier));
                    scheduledExecutorMap.put(identifier, scheduledExecutor);
                    log.info(String.format("Thread pool created: [type] Scheduled Executor [id] %s [size] %d",
                            identifier, threadPoolSize));
                }
            }

        }
        return scheduledExecutor;
    }

    /**
     * Stops the executor with the specified id in a graceful manner
     *
     * @param threadPoolId thread pool id
     */
    public static void shutDownThreadPoolGracefully (String threadPoolId) {

        ThreadPoolExecutor executor = executorMap.get(threadPoolId);
        if (executor == null) {
            log.warn("No thread pool found for id " + threadPoolId + ", unable to shut down");
            return;
        }

        new GracefulThreadPoolTerminator(threadPoolId, executor).call();
        removeThreadPoolFromCache(threadPoolId);
    }

    /**
     * Stops the scheduled executor with the specified id in a graceful manner
     *
     * @param threadPoolId thread pool id
     */
    public static void shutDownScheduledThreadPoolGracefully (String threadPoolId) {

        ScheduledThreadPoolExecutor scheduledExecutor = scheduledExecutorMap.get(threadPoolId);
        if (scheduledExecutor == null) {
            log.warn("No scheduled thread pool found for id " + threadPoolId + ", unable to shut down");
            return;
        }

        new GracefulThreadPoolTerminator(threadPoolId, scheduledExecutor).call();
        removeScheduledThreadPoolFromCache(threadPoolId);
    }

    /**
     * Stop all executors in a graceful manner
     */
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
                threadPoolTerminatorFutures.add(threadPoolTerminator.submit(new
                        GracefulThreadPoolTerminator(entry.getKey(),
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

    /**
     * Stop all scheduled executors in a graceful manner
     */
    public static void shutDownAllScheduledExecutorsGracefully () {

        int threadPoolCount = scheduledExecutorMap.size();
        if (threadPoolCount == 0) {
            log.info("No thread pools found to shut down");
            return;
        }

        Set<Future<String>> threadPoolTerminatorFutures = new HashSet<>();
        ExecutorService threadPoolTerminator = null;

        try {
            threadPoolTerminator = Executors.newFixedThreadPool(threadPoolCount);
            for (Map.Entry<String, ScheduledThreadPoolExecutor> entry : scheduledExecutorMap.entrySet())
                threadPoolTerminatorFutures.add(threadPoolTerminator.submit(new GracefulThreadPoolTerminator(entry.getKey(),
                        entry.getValue())));
            // use the Future to block until shutting down is done
            for (Future<String> threadPoolTerminatorFuture : threadPoolTerminatorFutures) {
                removeScheduledThreadPoolFromCache(threadPoolTerminatorFuture.get());
            }

        } catch (InterruptedException e) {
            log.error("Error in shutting down thread pools", e);
        } catch (ExecutionException e) {
            log.error("Error in shutting down thread pools", e);
        } catch (Exception e) {
            log.error("Error in shutting down thread pools", e);
        } finally {
            // if there are any remaining thread pools, shut down immediately
            if (!scheduledExecutorMap.isEmpty()) {
                for (Map.Entry<String, ScheduledThreadPoolExecutor> entry : scheduledExecutorMap.entrySet()) {
                    entry.getValue().shutdownNow();
                    removeScheduledThreadPoolFromCache(entry.getKey());
                }
            }

            // shut down the threadPoolTerminator threads
            threadPoolTerminator.shutdownNow();
        }
    }

    /**
     * Removes the thread pool with id terminatedPoolId from the executorMap
     *
     * @param terminatedPoolId thread pool id
     */
    private static void removeThreadPoolFromCache(String terminatedPoolId) {
        if (executorMap.remove(terminatedPoolId) != null) {
            log.info("Thread pool [id] " + terminatedPoolId + " is successfully shut down" +
                    " and removed from the cache");
        }
    }

    /**
     * Removes the scheduled thread pool with id terminatedPoolId from the scheduledExecutorMap
     *
     * @param terminatedPoolId thread pool id
     */
    private static void removeScheduledThreadPoolFromCache(String terminatedPoolId) {
        if (scheduledExecutorMap.remove(terminatedPoolId) != null) {
            log.info("Scheduled Thread pool [id] " + terminatedPoolId + " is successfully shut down" +
                    " and removed from the cache");
        }
    }

    public static void shutdown (String identifier) {

        ExecutorService executorService = executorMap.get(identifier);
        if (executorService == null) {
            log.warn("No executor service found for id " + identifier + ", unable to shut down");
            return;
        }

        // try to shut down gracefully
        executorService.shutdown();
        // wait 10 secs till terminated
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("Thread Pool [id] " + identifier + " did not finish all tasks before " +
                        "timeout, forcefully shutting down");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            // interrupted, shutdown now
            executorService.shutdownNow();
        }

        log.info("Successfully shutdown thread pool associated with id: " + identifier);
    }
}
