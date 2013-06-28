/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.db.keep.alive.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.db.keep.alive.actions.AccessingBillingAction;
import org.wso2.carbon.db.keep.alive.actions.AccessingRegistryAction;
import org.wso2.carbon.db.keep.alive.actions.UserMgtAction;

public class KeepAliveJob implements Job {
    private static final Log log = LogFactory.getLog(KeepAliveJob.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.debug("-------------Running the db keep alive job------------------");
        RegistryService registryService = Util.getRegistryService();
        try {
            new AccessingRegistryAction(registryService.getLocalRepository()).execute();
            new AccessingRegistryAction(registryService.getConfigSystemRegistry()).execute();
            new AccessingRegistryAction(registryService.getGovernanceSystemRegistry()).execute();
        } catch (Exception e) {
            String msg = "Error in running registry actions.";
            log.error(msg, e);
            throw new JobExecutionException(msg, e);
        }
        try {
            new AccessingBillingAction(Util.getBillingEngine()).execute();
        } catch (Exception e) {
            String msg = "Error in running billing engine action.";
            log.error(msg, e);
            throw new JobExecutionException(msg, e);
        }
        try {
            new UserMgtAction(registryService).execute();
        } catch (Exception e) {
            String msg = "Error in running user manager actions.";
            log.error(msg, e);
            throw new JobExecutionException(msg, e);
        }
    }
}