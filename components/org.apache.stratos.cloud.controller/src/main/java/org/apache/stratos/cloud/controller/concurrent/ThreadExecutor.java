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
package org.apache.stratos.cloud.controller.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class can be used to create a thread pool, and hand over new jobs to the pool.
 */
public class ThreadExecutor {
    private ExecutorService executor;

    private static class Holder {
        private static final ThreadExecutor INSTANCE = new ThreadExecutor();
    }
    
    public static ThreadExecutor getInstance() {
        return Holder.INSTANCE;
    }
    
    private ThreadExecutor() {
        executor = Executors.newFixedThreadPool(50);
    }
    
    public void execute(Runnable job){
        executor.execute(job);
    }
    
    public void executeAll(Runnable[] jobs){
        for (Runnable job : jobs) {
            
            executor.execute(job);
        }
    }
    
    public void shutdown() {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
          // Wait a while for existing tasks to terminate
          if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                System.err.println("Pool did not terminate");
          }
        } catch (InterruptedException ie) {
          // (Re-)Cancel if current thread also interrupted
          executor.shutdownNow();
          // Preserve interrupt status
          Thread.currentThread().interrupt();
        }
    }
    
    
}
