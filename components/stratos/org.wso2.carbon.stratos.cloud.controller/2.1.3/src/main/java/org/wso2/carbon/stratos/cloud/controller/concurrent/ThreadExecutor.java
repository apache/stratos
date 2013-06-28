/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class can be used to create a thread pool, and hand over new jobs to the pool.
 */
public class ThreadExecutor {
    private ExecutorService executor;

    public ThreadExecutor(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
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
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finished
        while (!executor.isTerminated()) {}
    }
    
    
}
