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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class can be used to create a scheduled executor pool, and hand over new jobs to the pool.
 */
public class ScheduledThreadExecutor {
    private ScheduledExecutorService executor;

    private static class Holder {
        private static final ScheduledThreadExecutor INSTANCE = new ScheduledThreadExecutor();
    }
    
    public static ScheduledThreadExecutor getInstance() {
        return Holder.INSTANCE;
    }
    
    private ScheduledThreadExecutor() {
        executor = Executors.newScheduledThreadPool(50);
    }
    
    public ScheduledFuture<?> schedule(Runnable job, int interval){
        return executor.scheduleAtFixedRate(job, 0, interval, TimeUnit.MILLISECONDS);
    }
    
    public List<ScheduledFuture<?>> scheduleAll(Runnable[] jobs, int interval){
        List<ScheduledFuture<?>> list = new ArrayList<ScheduledFuture<?>>();
        for (Runnable job : jobs) {
            
            list.add(this.schedule(job, interval));
        }
        return list;
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
