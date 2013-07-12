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
package org.apache.stratos.throttling.manager.dataproviders;

import org.apache.stratos.throttling.manager.dataobjects.ThrottlingDataContext;
import org.apache.stratos.throttling.manager.dataobjects.ThrottlingDataEntryConstants;
import org.apache.stratos.throttling.manager.exception.ThrottlingException;
import org.apache.stratos.throttling.manager.utils.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.billing.mgt.dataobjects.MultitenancyPackage;

public class BillingDataProvider extends DataProvider {
    private static Log log = LogFactory.getLog(BillingDataProvider.class);

    
    public void invoke(ThrottlingDataContext dataContext) throws ThrottlingException {
        int tenantId = dataContext.getTenantId();
        try {
            Customer customer = Util.getCurrentBillingCustomer(tenantId);
            dataContext.addDataObject(ThrottlingDataEntryConstants.CUSTOMER, customer);
        } catch (RegistryException e) {
            String msg = "Error in getting the current customer. tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
        // getting the package
        try {
            MultitenancyPackage mtPackage = Util.getCurrentBillingPackage(tenantId);
            dataContext.addDataObject(ThrottlingDataEntryConstants.PACKAGE, mtPackage);
        } catch (RegistryException e) {
            String msg = "Error in getting the multi-tenancy package. tenant id: " + tenantId + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
    }
}
