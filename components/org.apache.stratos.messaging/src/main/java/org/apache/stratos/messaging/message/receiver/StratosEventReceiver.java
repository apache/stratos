/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.receiver;

import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.listener.EventListener;

/**
 * Abstraction for Event Receivers used in Stratos
 */
public abstract class StratosEventReceiver {

    protected ThreadPoolExecutor executor;
    private static final Log log = LogFactory.getLog(StratosEventReceiver.class);

    /**
     * Thread pool information for all StratosEventReceiver implementations
     */

    public static String STRATOS_EVENT_RECEIEVER_THREAD_POOL_ID = "stratos-event-receiver-pool";
    private static String STRATOS_EVENT_RECEIEVER_THREAD_POOL_SIZE = "stratos.event.receiver.pool.size";

    // thread pool id
    protected String threadPoolId;
    // pool size
    protected static int threadPoolSize = 25;

    static {
        // check if the thread pool size is given as a system parameter
        String poolSize = System.getProperty(STRATOS_EVENT_RECEIEVER_THREAD_POOL_SIZE);
        if (poolSize != null) {
            try {
                threadPoolSize = Integer.parseInt(poolSize);
            } catch (NumberFormatException e) {
                log.error("Invalid configuration found for StratosEventReceiver thread pool size", e);
                threadPoolSize = 25;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Number of threads used in pool " + STRATOS_EVENT_RECEIEVER_THREAD_POOL_ID + " : " + threadPoolSize);
        }
    }

    public StratosEventReceiver () {
        this.threadPoolId = STRATOS_EVENT_RECEIEVER_THREAD_POOL_ID;
        this.executor = StratosThreadPool.getExecutorService(threadPoolId, (int)Math.ceil(threadPoolSize/3),
                threadPoolSize);
    }

    /**
     * Adds an EventListener to this StratosEventReceiver instance
     *
     * @param eventListener EventListener instance to add
     */
    public abstract void addEventListener(EventListener eventListener);

    /**
     * Removed an EventListener from this StratosEventReceiver instance
     *
     * @param eventListener EventListener instance to remove
     */
    public abstract void removeEventListener(EventListener eventListener);

    /**
     * Terminates this StratosEventReceiver instance
     */
    public abstract void terminate();
}
