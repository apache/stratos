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

import java.util.Map;
import java.util.concurrent.*;

/**
 * Utility class for Stratos thread pool
 */
public class StratosThreadPool {

    private static final Log log = LogFactory.getLog(StratosThreadPool.class);

    private static Map<String, ExecutorService> executorServiceMap = new ConcurrentHashMap<String, ExecutorService>();
    private static Map<String, ScheduledExecutorService> scheduledServiceMap = new ConcurrentHashMap<String, ScheduledExecutorService>();
    private static Object executorServiceMapLock = new Object();
    private static Object scheduledServiceMapLock = new Object();

    /**
     * Return the executor service based on the identifier and thread pool size
     *
     * @param identifier     Thread pool identifier name
     * @param threadPoolSize Thread pool size
     * @return ExecutorService
     */
    public static ExecutorService getExecutorService(String identifier, int threadPoolSize) {
        ExecutorService executorService = executorServiceMap.get(identifier);
        if (executorService == null) {
            synchronized (executorServiceMapLock) {
                if (executorService == null) {
                    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue(threadPoolSize);
                    ThreadPoolExecutor threadPool=new ThreadPoolExecutor(threadPoolSize, 1000,
                            0L, TimeUnit.MILLISECONDS,
                            queue);// Executors.newFixedThreadPool(threadPoolSize);
                    threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                            try {
                                executor.getQueue().put(r);
                            } catch (InterruptedException e) {
                                log.error("Failed to add the rejected jobs to executor service blocking queue",e);
                            }
                        }
                    });
                    executorService=threadPool;
                    executorServiceMap.put(identifier, executorService);
                    log.info(String.format("Thread pool created: [type] Executor Service [id] %s [size] %d", identifier, threadPoolSize));
                }
            }
        }
        return executorService;
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
                    scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
                    scheduledServiceMap.put(identifier, scheduledExecutorService);
                    log.info(String.format("Thread pool created: [type] Scheduled Executor Service [id] %s [size] %d",
                            identifier, threadPoolSize));
                }
            }

        }
        return scheduledExecutorService;
    }
}
