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

package org.apache.stratos.autoscaler.applications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationsEventPublisher;
import org.wso2.carbon.ntask.core.Task;

import java.util.Map;

public class ApplicationSynchronizeTask implements Task {
    private static final Log log = LogFactory.getLog(ApplicationSynchronizeTask.class);

    @Override
    public void execute() {
        if (log.isDebugEnabled()) {
            log.debug("Executing topology synchronization task");
        }
        // publish to the topic
        if (ApplicationHolder.getApplications() != null) {
            //ApplicationsEventPublisher.sendCompleteApplicationsEvent(ApplicationHolder.getApplications());
            ApplicationBuilder.handleCompleteApplication(ApplicationHolder.getApplications());
        }
    }

    @Override
    public void init() {
        if(log.isDebugEnabled()) {
            log.debug("Applications Complete Event publisher task has been started...");
        }

    }

    @Override
    public void setProperties(Map<String, String> arg0) {}
}
