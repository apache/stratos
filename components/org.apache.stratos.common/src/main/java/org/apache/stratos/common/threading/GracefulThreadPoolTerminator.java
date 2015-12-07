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

package org.apache.stratos.common.threading;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GracefulThreadPoolTerminator implements Callable<String> {

    private static final Log log = LogFactory.getLog(GracefulThreadPoolTerminator.class);

    private String threadPoolId;
    private ThreadPoolExecutor executor;

    public GracefulThreadPoolTerminator (String threadPoolId, ThreadPoolExecutor executor) {
        this.threadPoolId = threadPoolId;
        this.executor = executor;
    }

    @Override
    public String call() {
        // try to shut down gracefully
        executor.shutdown();
        // wait 10 secs till terminated
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("Thread Pool [id] " + threadPoolId + " did not finish all tasks before " +
                        "timeout, forcefully shutting down");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // interrupted, shutdown now
            executor.shutdownNow();
        }
        return threadPoolId;
    }
}
