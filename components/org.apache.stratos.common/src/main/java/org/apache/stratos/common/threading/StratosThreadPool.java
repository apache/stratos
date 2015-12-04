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
}
