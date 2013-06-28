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
import org.wso2.carbon.billing.core.beans.OutstandingBalanceInfoBean;
import org.wso2.carbon.billing.core.conf.BillingTaskConfiguration;
import org.wso2.carbon.billing.core.dataobjects.*;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;
import org.wso2.carbon.billing.core.scheduler.BillingScheduler;
import org.wso2.carbon.billing.core.scheduler.SchedulerContext;
import org.wso2.carbon.billing.core.utilities.CustomerUtils;
import org.wso2.carbon.email.sender.api.EmailSender;
import org.wso2.carbon.email.sender.api.EmailSenderConfiguration;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Billing engine work on billing per a seller
 */
public class BillingEngine {

    private static Log log = LogFactory.getLog(BillingEngine.class);
    private BillingTaskConfiguration billingTaskConfig;
    BillingScheduler billingScheduler;
    DataAccessObject dataAccessObject;

    public BillingEngine(BillingTaskConfiguration billingTaskConfig,
                         DataAccessObject dataAccessObject) {
        this.billingTaskConfig = billingTaskConfig;
        billingScheduler = new BillingScheduler(this, billingTaskConfig);
        this.dataAccessObject = dataAccessObject;
    }

    public void scheduleBilling() throws BillingException {
        // the logic to schedule the billing
        SchedulerContext schedulerContext = billingScheduler.createScheduleContext();
        if(schedulerContext.getCronString() != null || !schedulerContext.getCronString().equals("")) {
        	billingScheduler.scheduleNextCycle(schedulerContext);
        } else {
        	log.debug("Billing is not scheduled : due to undefined cron expression");
        }
    }

    public void generateBill() throws BillingException {
        BillingEngineContext billingEngineContext = new BillingEngineContext();
        generateBill(billingEngineContext);
    }

    public void generateBill(SchedulerContext schedulerContext) throws BillingException {
        BillingEngineContext billingEngineContext = new BillingEngineContext();
        billingEngineContext.setSchedulerContext(schedulerContext);
        generateBill(billingEngineContext);
    }

    public void generateBill(BillingEngineContext billingEngineContext) throws BillingException {

        try {
            beginTransaction();
            if (billingEngineContext.getSchedulerContext() == null) {
                SchedulerContext schedulerContext = billingScheduler.createScheduleContext();
                billingEngineContext.setSchedulerContext(schedulerContext);
            }

            billingEngineContext.setTaskConfiguration(billingTaskConfig);

            // now iterator through all the handlers
            List<BillingHandler> handlers = billingTaskConfig.getBillingHandlers();
            for (BillingHandler handler : handlers) {
                handler.execute(billingEngineContext);
            }
            //commit transaction
            commitTransaction();
        }catch (Exception e){
            String msg = "Error occurred while generating the bill:" + e.getMessage();
            log.error(msg, e);
            //rollback transaction
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        
    }

    public void beginTransaction() throws BillingException {
        dataAccessObject.beginTransaction();
    }

    public void commitTransaction() throws BillingException {
        dataAccessObject.commitTransaction();
    }

    public void rollbackTransaction() throws BillingException {
        dataAccessObject.rollbackTransaction();
    }

    public List<Item> getItemsWithName(String itemName) throws BillingException {
        List<Item> items;
        try {
            beginTransaction();
            items = dataAccessObject.getItemsWithName(itemName);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting item with name: " + itemName +
                        " " + e.getMessage();
            log.error(msg);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return items;
    }

    public int addItem(Item item) throws BillingException {
        int itemId = 0;
        try {
            beginTransaction();
            itemId = dataAccessObject.addItem(item);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while adding item: " + item.getName() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return itemId;
    }

    public Item getItem(int itemId) throws BillingException {
        Item item;
        try {
            beginTransaction();
            item = dataAccessObject.getItem(itemId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting item with id: " + itemId +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return item;
    }

    public List<Customer> getCustomersWithName(String customerName) throws BillingException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        List<Customer> customers = new ArrayList<Customer>();
        try{
            int tenantId = tenantManager.getTenantId(customerName);
            Tenant tenant = tenantManager.getTenant(tenantId);
            if(tenant!=null){
                Customer customer = new Customer();
                customer.setId(tenant.getId());
                customer.setName(tenant.getDomain());
                customer.setStartedDate(tenant.getCreatedDate());
                customer.setEmail(tenant.getEmail());
                //customer.setAddress();
                customers.add(customer);
            }
        }catch(Exception e){
            String msg = "Failed to get customers for customers: " + customerName + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        return customers;
    }

    public int addSubscription(Subscription subscription) throws BillingException {
        int subscriptionId = 0;
        try {
            beginTransaction();
            subscriptionId =
                    dataAccessObject.addSubscription(subscription,
                            subscription.getSubscriptionPlan());
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while adding subscription: " + subscription.getSubscriptionPlan()+
                            " for the customer " + subscription.getCustomer().getName() + " " + e.getMessage() ;
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscriptionId;
    }

    public int addPayment(Payment payment) throws BillingException {
        int paymentId = 0;
        try {
            beginTransaction();
            paymentId = dataAccessObject.addPayment(payment);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while adding payment record (transaction id): " + payment.getDescription() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return paymentId;
    }

    public int addRegistrationPayment(Payment payment, String usagePlan) throws BillingException {
        int paymentId = 0;
        try {
            beginTransaction();
            paymentId = dataAccessObject.addRegistrationPayment(payment, usagePlan);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while adding registration payment record (transaction id): " + payment.getDescription() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return paymentId;
    }

    public Invoice getLastInvoice(Customer customer) throws BillingException {
        Invoice invoice = null;
        try {
            beginTransaction();
            invoice = dataAccessObject.getLastInvoice(customer);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting last invoice for customer: " + customer.getId() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return invoice;
    }

    public List<Invoice> getAllInvoices(Customer customer) throws BillingException {
        List<Invoice> invoices = null;
        try {
            beginTransaction();
            invoices = dataAccessObject.getAllInvoices(customer);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting all invoices for customer: " + customer.getId() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return invoices;
    }

    public List<Subscription> getActiveSubscriptions(Customer customer) throws BillingException {
        List<Subscription> subscriptions;
        try {
            beginTransaction();
            subscriptions =
                    dataAccessObject.getFilteredActiveSubscriptionsForCustomer(
                            null, customer);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting active subscriptions for customer: " + customer.getId() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscriptions;
    }

    public Subscription getActiveSubscriptionOfCustomer(int customerId) throws BillingException {
        Subscription subscription;
        try {
            beginTransaction();
            subscription =
                    dataAccessObject.getActiveSubscriptionOfCustomer(customerId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting active subscription for customer: " + customerId +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscription;
    }

    public List<Invoice> getInvoices(Customer customer) throws BillingException {
        List<Invoice> invoices;
        try {
            beginTransaction();
            invoices = dataAccessObject.getInvoices(customer);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting invoices for customer: " + customer.getId() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return invoices;
    }

    public Invoice getInvoice(int invoiceId) throws BillingException {
        Invoice invoice = null;
        try {
            beginTransaction();
            invoice = dataAccessObject.getInvoice(invoiceId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting invoice with id: " + invoiceId +
                            " " + e.getMessage();                
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return invoice;
    }

    public List<Item> getBilledItems(Subscription subscription) throws BillingException {
        List<Item> billedItems;
        try {
            beginTransaction();
            billedItems = dataAccessObject.getBilledItems(subscription);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting billed items for subscription: " + subscription.getId() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return billedItems;
    }

    public List<Subscription> getInvoiceSubscriptions(int invoiceId) throws BillingException {
        List<Subscription> subscriptions;
        try {
            beginTransaction();
            subscriptions = dataAccessObject.getInvoiceSubscriptions(invoiceId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting invoice subscriptions for invoice id: " + invoiceId +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscriptions;
    }

    public Payment getLastPayment(Customer customer) throws BillingException {
        Payment payment;
        try {
            beginTransaction();
            payment = dataAccessObject.getLastPayment(customer);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting the last payment for customer: " + customer.getId() +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return payment;
    }

    public List<Customer> getAllCustomers() throws BillingException {
        return CustomerUtils.getAllCustomers();
    }

    public List<OutstandingBalanceInfoBean> getAllOutstandingBalanceInfoBeans(String tenantDomain)
            throws BillingException {
        if(tenantDomain==null || "".equals(tenantDomain)){
            return getAllOutstandingBalances(null);
        }else{
            List<Customer> customers = getCustomersWithName(tenantDomain);
            if(customers!=null && customers.size()>0){
                return getAllOutstandingBalances(customers.get(0));
            }else{
                return new ArrayList<OutstandingBalanceInfoBean>();
            }
        }
    }

    public List<OutstandingBalanceInfoBean> getAllOutstandingBalances(Customer preferredCustomer) throws BillingException{
        List<Customer> customers;// = getAllCustomers();
        List<OutstandingBalanceInfoBean> outstandingBalances = new ArrayList<OutstandingBalanceInfoBean>();
        if(preferredCustomer!=null){
            customers = new ArrayList<Customer>();
            customers.add(preferredCustomer);
        }else{
            customers = getAllCustomers();
        }
        for(Customer customer : customers){
            OutstandingBalanceInfoBean balanceBean = new OutstandingBalanceInfoBean();
            balanceBean.setCustomerName(customer.getName());
            Invoice invoice = getLastInvoice(customer);
            if(invoice!=null){
                balanceBean.setCarriedForward(invoice.getCarriedForward().toString());
                balanceBean.setLastInvoiceDate(invoice.getDate());
            }
            //setting the active usage plan
            Subscription subscription = getActiveSubscriptionOfCustomer(customer.getId());
            if(subscription!=null){
                balanceBean.setSubscription(subscription.getSubscriptionPlan());
            }else{
                balanceBean.setSubscription("Not Available");
            }

            Payment payment = getLastPayment(customer);
            if(payment!=null){
                balanceBean.setLastPaidAmount(payment.getAmount().toString());
                balanceBean.setLastPaymentDate(payment.getDate());
            }
            outstandingBalances.add(balanceBean);
        }
        return outstandingBalances;
    }

    public void sendPaymentReceivedEmail(String toAddress, String emailFile,
                                         Map<String,String> mailParameters) throws Exception {
        String emailTemplateFile = CarbonUtils.getCarbonConfigDirPath()+ File.separator
                                   + StratosConstants.EMAIL_CONFIG + File.separator + emailFile;
        EmailSenderConfiguration senderConfiguration =
                EmailSenderConfiguration.loadEmailSenderConfiguration(emailTemplateFile);
        EmailSender sender = new EmailSender(senderConfiguration);
        sender.sendEmail(toAddress, mailParameters);
    }
    
    public boolean addDiscount(Discount discount) throws Exception {
        boolean added = false;
        try {
            beginTransaction();
            added = dataAccessObject.addDiscount(discount);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while adding the discount for tenant: " + discount.getTenantId()+
                    ". " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }

        return added;
    }
}
