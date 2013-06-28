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
package org.wso2.carbon.throttling.manager.rules;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataContext;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataEntry;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.tasks.Task;
import org.wso2.carbon.throttling.manager.validation.ValidationInfoManager;

public class KnowledgeBaseManager {
    private static final Log log = LogFactory.getLog(KnowledgeBaseManager.class);

    public static ThrottlingDataContext feedKnowledgeBase(int tenantId, Task task,
            List<Object> knowledgeBase) throws ThrottlingException {
        // initialize the throttling context
        ThrottlingDataContext throttlingDataContext = new ThrottlingDataContext(tenantId);

        // prepare data from data providers
        try {
            task.prepareData(throttlingDataContext);
        } catch (ThrottlingException e) {
            String msg = "Error in preparing throttling data for tenant: " + tenantId + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }

        // add data entries with object types separately
        for (ThrottlingDataEntry dataEntry : throttlingDataContext.getData()) {
            if (dataEntry.getValueType() == 4) {
                Object object = dataEntry.getObjectValue();
                if (object != null) {
                    knowledgeBase.add(object);
                }
            }
        }
        // load the access validation data
        try {
            ValidationInfoManager.loadValidationDetails(throttlingDataContext);
        } catch (ThrottlingException e) {
            String msg = "Error in loading validation details. tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }

        // add metering data context
        knowledgeBase.add(throttlingDataContext);
        // add access validation information
        knowledgeBase.add(throttlingDataContext.getAccessValidation());

        return throttlingDataContext;
    }
}
