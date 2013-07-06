/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common;

import org.apache.stratos.common.exception.StratosException;
import org.wso2.carbon.user.api.Tenant;

/**
 * The OSGI service interface that enables tenant related billing actions.
 */
public interface TenantBillingService {
    
    public void addUsagePlan(Tenant tenant, String usagePlan) throws StratosException;
    
    public String getActiveUsagePlan(String tenantDomain) throws StratosException;
    
    public void updateUsagePlan(String tenantDomain, String usagePlan) throws StratosException;
    
    public void activateUsagePlan(String tenantDomain) throws StratosException;
    
    public void deactivateActiveUsagePlan(String tenantDomain) throws StratosException;

    public void deleteBillingData(int tenantId) throws StratosException;

    
}
