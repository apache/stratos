/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.usage.agent.persist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.agent.beans.BandwidthUsage;
import org.wso2.carbon.usage.agent.config.UsageAgentConfiguration;

import java.util.Queue;
import java.util.concurrent.*;

public class UsageDataPersistenceManager {
    private static final Log log = LogFactory.getLog(UsageDataPersistenceManager.class);

    // queue to store Bandwidth usage statistics.
    // usage of  LinkedBlockingQueue ensures operations on the queue to wait for the queue to be non
    // empty when retrieving and wait for space when storing element.
    private Queue<BandwidthUsage> persistenceJobs = new LinkedBlockingQueue<BandwidthUsage>();

    private final ScheduledExecutorService scheduler;

    private UsageAgentConfiguration configuration;

    public UsageDataPersistenceManager(UsageAgentConfiguration configuration) {
        scheduler = Executors.newScheduledThreadPool(2, new UsageDataPersistenceThreadFactory());
        this.configuration = configuration;
    }

    /**
     * this method add bandwidth usage entries to the jobQueue
     *
     * @param usage Bandwidth usage
     */

    public void addToQueue(BandwidthUsage usage) {
        persistenceJobs.add(usage);
    }

    public void scheduleUsageDataPersistenceTask() {
        //we will schedule the usage data persistence task only if interval is not -1
        if(configuration.getUsageTasksExecutionIntervalInMilliSeconds()>0){
            scheduler.scheduleWithFixedDelay(new UsageDataPersistenceTask(persistenceJobs, configuration),
                    configuration.getUsageTasksStartupDelayInMilliSeconds(),
                    configuration.getUsageTasksExecutionIntervalInMilliSeconds(),
                    TimeUnit.MILLISECONDS);
            log.debug("Usage data persistence task was scheduled");
        }else{
            log.debug("Usage data persistence task is disabled");
        }
    }


    public void scheduleBandwidthUsageDataRetrievalTask() {
        //we will schedule the usage data retrieval task only if interval is not -1
        if(configuration.getUsageTasksExecutionIntervalInMilliSeconds()>0){
            scheduler.scheduleWithFixedDelay(new BandwidthUsageDataRetrievalTask(configuration),
                    configuration.getUsageTasksStartupDelayInMilliSeconds(),
                    configuration.getUsageTasksExecutionIntervalInMilliSeconds(),
                    TimeUnit.MILLISECONDS);
            log.debug("Bandwidth Usage data retrieval task was scheduled");
        }else {
            log.debug("Bandwidth Usage data retrieval task was disabled");
        }
    }

    class UsageDataPersistenceThreadFactory implements ThreadFactory {
        private int counter = 0;

        public Thread newThread(Runnable r) {
            return new Thread(r, "UsageDataPersistenceThread-" + counter++);
        }
    }
}
