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
package org.wso2.carbon.throttling.test.utils;

import org.wso2.carbon.billing.mgt.dataobjects.MultitenancyPackage;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataContext;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataEntryConstants;
import org.wso2.carbon.throttling.manager.dataproviders.DataProvider;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;

public class CustomDataProvider extends DataProvider {
    public void invoke(ThrottlingDataContext dataContext) throws ThrottlingException {
        long dataSize = dataContext.getDataLong(ThrottlingDataEntryConstants.TENANT_CAPACITY);

        if (dataSize % 2 == 0) {
            MultitenancyPackage multiPackage = new MultitenancyPackage();
            multiPackage.setName("test1");
            multiPackage.setBandwidthLimit(50);

            dataContext.addDataObject(ThrottlingDataEntryConstants.PACKAGE, multiPackage);
        } else {
            MultitenancyPackage multiPackage = new MultitenancyPackage();
            multiPackage.setName("test2");

            dataContext.addDataObject(ThrottlingDataEntryConstants.PACKAGE, multiPackage);
            multiPackage.setBandwidthLimit(50);
        }

    }
}
