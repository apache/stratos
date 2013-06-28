/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.billing.mgt.ui.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.mgt.stub.beans.xsd.*;
import org.wso2.carbon.billing.mgt.ui.clients.BillingServiceClient;
import org.wso2.carbon.registry.common.ui.UIException;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class BillingUtil {
    private static final Log log = LogFactory.getLog(BillingUtil.class);

    public static BillingPeriod[] getAvailableBillingPeriods(
            ServletConfig config, HttpSession session) throws UIException {
        try {
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.getAvailableBillingPeriods();
        } catch (Exception e) {
            String msg = "Failed to get available billing periods.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }
    
    public static BillingPeriod[] getAvailableBillingPeriodsBySuperTenant(
            ServletConfig config, HttpSession session, String tenantDomain) throws UIException {

        try{
            BillingServiceClient client = new BillingServiceClient(config, session);
            return client.getBillingPeriodsBySuperTenant(tenantDomain);
        }catch(Exception e){
            String msg = "Error occurred while getting available invoice dates for tenant: " +
                    tenantDomain;
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static String[] getAvailableBillingMonths(ServletConfig config,
                                 HttpSession session) throws UIException{
        return getAvailableBillingMonths(getAvailableBillingPeriods(config, session));
    }

    public static MultitenancyInvoice getPastInvoice(
            ServletConfig config, HttpSession session, int invoiceId) throws UIException {
        //int invoiceId = (Integer)session.getAttribute("invoiceId");

        try {
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.getPastInvoice(invoiceId);
        } catch (Exception e) {
            String msg = "Failed to get past invoice for invoice id:" + invoiceId + ".";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static MultitenancyInvoice getCurrentInvoice(
            ServletConfig config, HttpSession session) throws UIException {
        try {
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.getCurrentInvoice();
        } catch (java.lang.Exception e) {
            String msg = "Failed to get the current invoice.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static String[] getAvailableBillingMonths(BillingPeriod[] billingPeriods){

        if(billingPeriods==null || billingPeriods.length==0){
            return new String[0];
        }

        String[] billingMonths = new String[billingPeriods.length];
        DateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MMM-dd");
        int index = 0;
        for(BillingPeriod period : billingPeriods){
            billingMonths[index++] = yearMonthFormat.format(period.getInvoiceDate());
        }
        return billingMonths;
    }

    public static int addPaymentDetails(ServletConfig config, HttpSession session,
                                        Payment payment, String amount) throws UIException {
        try{
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.addPayment(payment, amount);
        }catch (Exception exp){
            String msg = "Failed to add the payment record " + payment.getDescription();
            log.error(msg, exp);
            throw new UIException(msg, exp);
        }
    }

    public static int makeAdjustment(ServletConfig config, HttpSession session,
                                        Payment payment, String amount) throws UIException {
        try{
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.makeAdjustment(payment, amount);
        }catch (Exception exp){
            String msg = "Failed to add the payment record " + payment.getDescription();
            log.error(msg, exp);
            throw new UIException(msg, exp);
        }
    }

    public static PaginatedBalanceInfoBean getPaginatedBalanceInfo(ServletConfig config, HttpSession session,
                                                                   int pageNumber) throws UIException{
        try{
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.getPaginatedBalanceInfo(pageNumber);
        }catch (Exception exp){
            String msg = "Failed to get paginated balance info ";
            log.error(msg, exp);
            throw new UIException(msg, exp);
        }
    }

    public static OutstandingBalanceInfoBean[] getOutstandingBalance(
            ServletConfig config, HttpSession session, String tenantDomain) throws UIException{
        try{
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.getOutstandingBalance(tenantDomain);
        }catch (Exception exp){
            String msg = "Failed to get balance info for domain: " + tenantDomain;
            log.error(msg, exp);
            throw new UIException(msg, exp);
        }
    }

    public static boolean addDiscount(
            ServletConfig config, HttpSession session, Discount discount, String tenantDomain) throws UIException{

        try{
            BillingServiceClient serviceClient = new BillingServiceClient(config, session);
            return serviceClient.addDiscount(discount, tenantDomain);
        }catch (Exception exp){
            String msg = "Failed to failed to add the discount for tenant: " + discount.getTenantId();
            log.error(msg, exp);
            throw new UIException(msg, exp);
        }
    }

   /* public static String getModifiedSubscriptionPlan(String plan){
        if("multitenancy-free".equals(plan)){
            return StratosConstants.MULTITENANCY_FREE_PLAN;
        }else if("multitenancy-small".equals(plan)){
            return StratosConstants.MULTITENANCY_SMALL_PLAN;
        }else if("multitenancy-medium".equals(plan)){
            return StratosConstants.MULTITENANCY_MEDIUM_PLAN;
        }else if("multitenancy-large".equals(plan)){
            return StratosConstants.MULTITENANCY_LARGE_PLAN;
        }else{
            return "Undefined";
        }
    }*/

}
