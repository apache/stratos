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
package org.wso2.carbon.billing.core.utilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.TenantManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for customer related operations
 */
public class CustomerUtils {
    private static Log log = LogFactory.getLog(CustomerUtils.class);

    /**
     * Fill the customer object with data retrieved from the tenant manager
     * @param customerId
     * @param customer
     * @throws BillingException
     */
    public static void fillCustomerData(int customerId, Customer customer) throws BillingException{
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        try{
            Tenant tenant = tenantManager.getTenant(customerId);
            customer.setId(customerId);
            customer.setName(tenant.getDomain());
            customer.setStartedDate(tenant.getCreatedDate());
            customer.setEmail(tenant.getEmail());
            //customer.setAddress(); //we dont have the address
        }catch (Exception e){
            String msg = "Failed to fill the data for customer: " +
                    customer.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }
    }

    /**
     * Get the customer by tenant id (customer id) using the realm service
     * @param customerId
     * @return
     * @throws BillingException
     */
    public static Customer getCustomer(int customerId) throws BillingException{
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        Customer customer = null;

        try{
            Tenant tenant = tenantManager.getTenant(customerId);
            if(tenant!=null){
                customer = new Customer();
                customer.setId(customerId);
                customer.setName(tenant.getDomain());
                customer.setStartedDate(tenant.getCreatedDate());
                customer.setEmail(tenant.getEmail());
                //customer.setAddress();
            }
        } catch (Exception e){
            String msg = "Failed to get customer for customer id: " + customerId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        return customer;
    }

    /**
     * Get the tenantId (customer id) of a customer from the realm service
     * @param customerName
     * @return
     * @throws BillingException
     */
    public static int getCustomerId(String customerName) throws BillingException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        int tenantId;
        try{
            tenantId = tenantManager.getTenantId(customerName);
        }catch (Exception e){
            String msg = "Failed to get tenant for domain: " + customerName + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        return tenantId;
    }

    /**
     * Get all the customers ( fill customers from tenants)
     * @return
     * @throws BillingException
     */
    public static List<Customer> getAllCustomers() throws BillingException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        List<Customer> customers = new ArrayList<Customer>();
        try{
            Tenant[] tenants = tenantManager.getAllTenants();
            if(tenants!=null && tenants.length>0){
                for(Tenant tenant : tenants){
                    Customer customer = new Customer();
                    customer.setId(tenant.getId());
                    customer.setName(tenant.getDomain());
                    customer.setStartedDate(tenant.getCreatedDate());
                    customer.setEmail(tenant.getEmail());
                    //customer.setAddress(); //no address yet
                    customers.add(customer);
                }
            }
        } catch (Exception e){
            String msg = "Failed to get all the customers.";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        return customers;
    }
}
