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
package org.apache.stratos.common.listeners;


import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;

public interface TenantMgtListener {
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException;

    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException;

    public void onTenantRename(int tenantId, String oldDomainName, 
                             String newDomainName)throws StratosException;
    
    public void onTenantInitialActivation(int tenantId) throws StratosException;
    
    public void onTenantActivation(int tenantId) throws StratosException;
    
    public void onTenantDeactivation(int tenantId) throws StratosException;

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, 
                                         String newPlan) throws StratosException;
    
    public int getListenerOrder();
}
