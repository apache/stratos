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
package org.wso2.carbon.billing.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.conf.BillingConfiguration;
import org.wso2.carbon.billing.core.conf.BillingTaskConfiguration;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to handle multiple billing engines
 */
public class BillingManager {
    private static final Log log = LogFactory.getLog(BillingManager.class);
    private static BillingManager billingManager;
    private BillingConfiguration billingConfiguration;
    private DataAccessObject dataAccessObject;
    private Map<String, BillingEngine> billingEngines = new HashMap<String, BillingEngine>();

    public static BillingManager getInstance() throws BillingException {
        if (billingManager == null) {
            String msg = "Billing Manager is not initialized.";
            log.error(msg);
            throw new BillingException(msg);
        }
        return billingManager;
    }

    public static void destroyInstance() {
        billingManager = null;
    }

    public BillingManager(BillingConfiguration billingConfiguration) throws BillingException {
        if (billingManager != null) {
            String msg = "Billing Manager should not be initialized twice";
            log.error(msg);
            throw new BillingException(msg);
        }
        
        this.billingConfiguration = billingConfiguration;
        this.dataAccessObject = new DataAccessObject(billingConfiguration.getDataSource());
        //Create billing engine corresponds to given billingTaskConfigurations
        Map<String, BillingTaskConfiguration> billingTaskConfigs =
                billingConfiguration.getBillingTaskConfigs();
        for (Map.Entry<String, BillingTaskConfiguration> entry : billingTaskConfigs.entrySet()) {
            String billingTaskName = entry.getKey();
            BillingTaskConfiguration billingTaskConfiguration = entry.getValue();
            BillingEngine billingEngine =
                    new BillingEngine(billingTaskConfiguration, dataAccessObject);
            billingEngines.put(billingTaskName, billingEngine);
        }
        billingManager = this;
    }

    public BillingConfiguration getBillingConfiguration() {
        return billingConfiguration;
    }

    public DataAccessObject getDataAccessObject() {
        return dataAccessObject;
    }

    public BillingEngine getBillingEngine(String billingTaskName) {
        return billingEngines.get(billingTaskName);
    }
}
