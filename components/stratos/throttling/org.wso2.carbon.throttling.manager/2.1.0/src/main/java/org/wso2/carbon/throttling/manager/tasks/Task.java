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
package org.wso2.carbon.throttling.manager.tasks;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.throttling.manager.conf.ThrottlingTaskConfiguration;
import org.wso2.carbon.throttling.manager.conf.ThrottlingTaskDataProviderConfiguration;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataContext;
import org.wso2.carbon.throttling.manager.dataproviders.DataProvider;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.rules.RuleInvoker;
import org.wso2.carbon.throttling.manager.utils.DataProviderIterator;

public class Task {
    private static final Log log = LogFactory.getLog(Task.class);
    private static final int DEFAULT_INTERVAL = 15; // in minute
    Map<String, String> parameters;
    DataProviderIterator dataProviderIterator;
    RuleInvoker ruleInvoker;

    public Task(Map<String, String> parameters,
            List<ThrottlingTaskDataProviderConfiguration> dataProviderConfigs)
            throws ThrottlingException {
        this.parameters = parameters;
        this.dataProviderIterator = new DataProviderIterator(dataProviderConfigs);
        // initialize the rule invokers
        ruleInvoker = new RuleInvoker();
    }

    public void prepareData(ThrottlingDataContext dataContext) throws ThrottlingException {
        dataProviderIterator.reset();
        while (dataProviderIterator.hasNext()) {
            final DataProvider dataProvider = (DataProvider) dataProviderIterator.next();
            if (dataProvider == null) {
                String msg =
                        "Error in invoking the data provider. " + "dataProviderConfigs is null or "
                                + "data provider is not yet loaded";
                log.error(msg);
                throw new ThrottlingException(msg);
            }
            dataProvider.invoke(dataContext);
            if (dataContext.isProcessingComplete()) {
                break;
            }
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public int getTriggerInterval() {
        if (this.parameters == null ||
                this.parameters.get(ThrottlingTaskConfiguration.INTERVAL_PARAM_KEY) == null) {
            return DEFAULT_INTERVAL * 60 * 1000;
        }
        return Integer.parseInt(
                this.parameters.get(ThrottlingTaskConfiguration.INTERVAL_PARAM_KEY)) * 60 * 1000;
    }
    
    public int getStartDelayInterval(){
        if (this.parameters == null || 
                this.parameters.get(ThrottlingTaskConfiguration.DELAY_PARAM_KEY) == null){
            return DEFAULT_INTERVAL * 60 * 1000;
        }
        
        return Integer.parseInt(
                this.parameters.get(ThrottlingTaskConfiguration.DELAY_PARAM_KEY)) * 60 * 1000;
    }

    public RuleInvoker getRuleInvoker() {
        return ruleInvoker;
    }
}
