/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.apache.stratos.usage.agent.persist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.apache.stratos.usage.agent.beans.BandwidthUsage;
import org.apache.stratos.usage.agent.config.UsageAgentConfiguration;
import org.apache.stratos.usage.agent.exception.UsageException;
import org.apache.stratos.usage.agent.util.PublisherUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Queue;

public class UsageDataPersistenceTask implements Runnable {

    private static final Log log = LogFactory.getLog(UsageDataPersistenceTask.class);

    private Queue<BandwidthUsage> usagePersistenceJobs;
    private UsageAgentConfiguration configuration;

    public UsageDataPersistenceTask(Queue<BandwidthUsage> jobs, UsageAgentConfiguration configuration) {
        usagePersistenceJobs = jobs;
        this.configuration = configuration;
    }

    public void run() {
        if (!usagePersistenceJobs.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Persisting Service and Web App bandwidth usage statistics");
            }
            try {
                persistUsage(usagePersistenceJobs);
            } catch (UsageException e) {
                log.error("Error when persisting usage statistics.", e);
            }
        }
    }

    /**
     * this method create a Summarizer object for each tenant and call accumulate() method to
     * accumulate usage statistics
     *
     * @param jobQueue usage data persistence jobs
     * @throws org.apache.stratos.usage.agent.exception.UsageException
     *
     */

    public void persistUsage(Queue<BandwidthUsage> jobQueue) throws UsageException {

        // create a map to hold summarizer objects against tenant id
        HashMap<Integer, Summarizer> summarizerMap = new HashMap<Integer, Summarizer>();

        // if the jobQueue is not empty
        for (int i = 0; i < configuration.getUsageTasksNumberOfRecordsPerExecution() && !jobQueue.isEmpty(); i++) {

            // get the first element from the queue, which is a BandwidthUsage object
            BandwidthUsage usage = jobQueue.poll();

            // get the tenant id
            int tenantId = usage.getTenantId();

            //get the Summarizer object corresponds to the tenant id
            Summarizer summarizer = summarizerMap.get(tenantId);

            // when tenant invoke service for the first time, no corresponding summarizer object in
            // the map
            if (summarizer == null) {
                //create a Summarizer object and put to the summarizerMap
                summarizer = new Summarizer();
                summarizerMap.put(tenantId, summarizer);
            }

            //  now accumulate usage
            summarizer.accumulate(usage);
        }

        //Finished accumulating. Now publish the events

        // get the collection view of values in summarizerMap
        Collection<Summarizer> summarizers = summarizerMap.values();

        // for each summarizer object call the publish method
        for (Summarizer summarizer : summarizers) {
            summarizer.publish();
        }
    }

    /**
     * inner class Summarizer
     * this class is used to accumulate and publish usage statistics.
     * for each tenant this keeps a map to store BandwidthUsage values
     */
    private static class Summarizer {
        private HashMap<String, BandwidthUsage> usageMap;

        public Summarizer() {
            usageMap = new HashMap<String, BandwidthUsage>();
        }

        /**
         * the method to accumulate usage data
         *
         * @param usage BandwidthUsage
         */

        public void accumulate(BandwidthUsage usage) {
            // get the measurement name of usage entry
            String key = usage.getMeasurement();

            // get the existing value of measurement
            BandwidthUsage existingUsage = usageMap.get(key);

            // if this measurement is metered earlier add the new value to the existing value
            if (existingUsage != null) {
                existingUsage.setValue(existingUsage.getValue() + usage.getValue());
            } else {
                // if this measurement is not metered previously we need to add it to the usageMap
                usageMap.put(key, usage);
            }
        }

        /**
         * this method reads usage items from the usageMap and call publish method to publish to
         * the BAM
         *
         * @throws UsageException
         */

        public void publish() throws UsageException {

            // get the collection view of values in usageMap
            Collection<BandwidthUsage> usages = usageMap.values();

            for (BandwidthUsage usage : usages) {
                try {
                    // publish the usage entry if it is not the super-tenant
                    if(MultitenantConstants.SUPER_TENANT_ID != usage.getTenantId()){
                        PublisherUtils.publish(usage);
                    }
                } catch (UsageException e) {
                    log.error("Error in publishing bandwidth usage data", e);
                }
            }
        }
    }
}
