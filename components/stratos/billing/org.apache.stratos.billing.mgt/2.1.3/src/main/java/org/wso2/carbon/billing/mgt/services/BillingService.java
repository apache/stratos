/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.billing.mgt.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingConstants;
import org.wso2.carbon.billing.core.BillingEngine;
import org.wso2.carbon.billing.core.BillingEngineContext;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.billing.core.beans.OutstandingBalanceInfoBean;
import org.wso2.carbon.billing.core.beans.PaginatedBalanceInfoBean;
import org.wso2.carbon.billing.core.dataobjects.*;
import org.wso2.carbon.billing.mgt.beans.*;
import org.wso2.carbon.billing.mgt.util.Util;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.ClaimsMgtUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.DataPaginator;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(BillingService.class);

    /**
     * Gets the available billing periods of the available invoices for the
     * current customer. Tenant id is taken from registry
     * @return  an array of BillingPeriod objects
     * @throws Exception Exception
     */
    public BillingPeriod[] getAvailableBillingPeriods() throws Exception {
        UserRegistry registry = (UserRegistry) getGovernanceUserRegistry();
        return getAvailableBillingPeriods(registry);
    }

    /**
     * Gets the available billigs dates of a given tenant
     * @param tenantDomain is the tenant domain
     * @return an array of BillingPeriods
     * @throws Exception if an error occurs during the process
     */
    public BillingPeriod[] getAvailableBillingPeriodsBySuperTenant(String tenantDomain) throws Exception{

        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        
        List<Customer> customers = billingEngine.getCustomersWithName(tenantDomain);
        if(customers.size()>0){
            return getBillingPeriodsFromInvoices(billingEngine.getInvoices(customers.get(0)));
        }else{
            return new BillingPeriod[0];
        }
    }

    /**
     * Gets the invoice with a given invoice id
     * @param invoiceId is the id of the invoice expected
     * @return  a MultitenancyInvoice object
     * @throws Exception Exception
     */
    public MultitenancyInvoice getPastInvoice(int invoiceId) throws Exception {
        UserRegistry registry = (UserRegistry) getGovernanceUserRegistry();
        return getPastInvoiceById(registry, invoiceId);
    }

    /**
     * Gets the current invoice (interim invoice) of the current customer.
     * Tenant id is taken from the registry
     * @return a MultitenancyInvoice object
     * @throws Exception Exception
     */
    public MultitenancyInvoice getCurrentInvoice() throws Exception {
        UserRegistry registry = (UserRegistry) getGovernanceUserRegistry();
        return getCurrentInvoiceOfCustomer(registry);
    }

    /**
     * Adds a payment record to the BC_PAYMENT table. Sends a notification email
     * after adding the record
     * @param payment is the Payment object which contains the payment record details
     * @param amount is the paid amount (had to pass this as a string)
     * @return  the payment id for the added record
     * @throws Exception if an error occurs during the operation
     */
    public int addPayment(Payment payment, String amount) throws Exception {
        int paymentId = addPaymentRecord(payment, amount);
        if(paymentId>0){
            payment.setId(paymentId);
            sendPaymentReceivedEmail(payment);
        }
        return paymentId;
    }

    /**
     * Adds a payment record to the BC_REGISTRATION_PAYMENT table. Sends a notification email
     * after adding the record
     *
     * @param payment   the Payment object which contains the payment record details
     * @param amount    the registration fee paid
     * @param usagePlan the registered usage plan
     * @return the payment id for the added record
     * @throws Exception thrown if an error occurs while adding the record
     */
    public int addRegistrationPayment(Payment payment, String amount, String usagePlan)
            throws Exception {
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        Cash cashAmount = new Cash(amount);
        payment.setAmount(cashAmount);
        int paymentId = billingEngine.addRegistrationPayment(payment, usagePlan);
        if (paymentId > 0) {
            payment.setId(paymentId);
            sendRegistrationPaymentReceivedEmail(payment);
        }
        return paymentId;
    }

    /**
     * Adds a payment record for invoice adjustment purposes
     * @param payment is the Payment object which contains the adjustment details
     * @param amount is the adjustment amount (had to pass this as a string)
     * @return the payment id for the added adjustment record
     * @throws Exception if an error occurs during the operation
     */
    public int makeAdjustment(Payment payment, String amount) throws Exception {
        return addPaymentRecord(payment, amount);
    }
    
    private int addPaymentRecord(Payment payment, String amount) throws Exception{
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        Cash cashAmount = new Cash(amount);
        payment.setAmount(cashAmount);
        if(payment.getInvoice()!=null){
            payment.setSubscriptions(billingEngine.getInvoiceSubscriptions(payment.getInvoice().getId()));
        }
        int paymentId = billingEngine.addPayment(payment);
        return paymentId;
    }

    /**
     * Gets the paginated BalanceInfoBean to be shown for the super tenant in the paginated mode
     * @param pageNumber is the expected page number
     * @return a PaginatedBalanceInfoBean object
     * @throws Exception Exception
     */
    public PaginatedBalanceInfoBean getPaginatedBalances(int pageNumber) throws Exception {
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        List<OutstandingBalanceInfoBean> balanceBeans = billingEngine.getAllOutstandingBalances(null); //no tenant domain
        PaginatedBalanceInfoBean paginatedBalanceBean = new PaginatedBalanceInfoBean();
        DataPaginator.doPaging(pageNumber, balanceBeans, paginatedBalanceBean);

        return paginatedBalanceBean;
    }

    /**
     * Gets OutstandingBalanceInfo bean(s).
     * @param tenantDomain  is the domain of the expected tenant which the super-tenant
     * wants to view the balance. If this is null, balance info of all the tenants will be shown
     * @return  an array of OutstandingBalanceInfoBeans
     * @throws Exception Exception
     */
    public OutstandingBalanceInfoBean[] getOutstandingBalance(String tenantDomain) throws Exception {
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        List<OutstandingBalanceInfoBean> balanceBeans = billingEngine.getAllOutstandingBalanceInfoBeans(tenantDomain);
        return balanceBeans.toArray(new OutstandingBalanceInfoBean[balanceBeans.size()]);
    }

    /**
     * Adds a discount entry
     * @param discount is the discount object which contains discount information
     * @param tenantDomain is passed to get the tenant id and set to the discount object
     * @return true or false based on the result of the operation
     * @throws Exception if an error occurs during the operation
     */
    public boolean addDiscount (Discount discount, String tenantDomain) throws Exception {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        int tenantId = tenantManager.getTenantId(tenantDomain);
        if(tenantId== MultitenantConstants.INVALID_TENANT_ID){
            throw new Exception("Invalid tenant domain submitted for a discount");
        }
        discount.setTenantId(tenantId);

        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine = billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);

        boolean added = billingEngine.addDiscount(discount);
        if(added){
            log.info("Discount entry added for tenant: " + discount.getTenantId());
        }
        return added;
    }
    
    /**
     * Gets the past invoice for a given invoice id
     * @param registry  is the GovernanceUserRegistry
     * @param invoiceId is the expected invoice id
     * @return  a MultitenancyInvoice object
     * @throws Exception Exception
     */
    private MultitenancyInvoice getPastInvoiceById(UserRegistry registry,
                                               int invoiceId) throws Exception {
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);

        Invoice invoice = billingEngine.getInvoice(invoiceId);
        if (invoice == null) {
            return null;
        }
        
        Customer customer = getCurrentCustomer(registry, billingEngine);
        if (customer == null || customer.getId() != invoice.getCustomer().getId()) {
            String msg = "Trying to looking at an invoice of another customer, customer: " +
                            (customer == null ? "unknown" : customer.getId()) + ", invoice: " +
                            invoice.getId() + ".";
            log.error(msg);
            throw new Exception(msg);
        }
        
        MultitenancyInvoice multitenancyInvoice = new MultitenancyInvoice();
        multitenancyInvoice.setInvoiceId(invoice.getId());
        multitenancyInvoice.setBillingDate(invoice.getDate());
        multitenancyInvoice.setBoughtForward(invoice.getBoughtForward().serializeToString());
        multitenancyInvoice.setCarriedForward(invoice.getCarriedForward().serializeToString());
        multitenancyInvoice.setStartDate(invoice.getStartDate());
        multitenancyInvoice.setEndDate(invoice.getEndDate());
        multitenancyInvoice.setTotalCost(invoice.getTotalCost().serializeToString());
        multitenancyInvoice.setTotalPayments(invoice.getTotalPayment().serializeToString());

        List<Subscription> subscriptions = invoice.getSubscriptions();
        MultitenancySubscription[] multitenancySubscriptions =
                new MultitenancySubscription[subscriptions.size()];
        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription subscription = subscriptions.get(i);
            MultitenancySubscription multitenancySubscription = new MultitenancySubscription();
            multitenancySubscription.setSubscribedPackage(subscription.getItem().getName());
            multitenancySubscription.setActiveSince(subscription.getActiveSince());
            multitenancySubscription.setActiveUntil(subscription.getActiveUntil());
            multitenancySubscription.setActive(subscription.isActive());

            // now iterating the items
            List<Item> billedItems = billingEngine.getBilledItems(subscription);
            BilledEntry[] itemEntries = new BilledEntry[billedItems.size()];
            for (int j = 0; j < billedItems.size(); j++) {
                Item billedItem = billedItems.get(j);
                if (billedItem.getName().equals(multitenancySubscription.getSubscribedPackage())) {
                    // ignoring the parent item..
                    continue;
                }
                BilledEntry itemEntry = new BilledEntry();
                itemEntry.setName(billedItem.getDescription());
                itemEntry.setCost(billedItem.getCost().serializeToString());
                itemEntries[j] = itemEntry;
            }
            multitenancySubscription.setBilledEntries(itemEntries);
            multitenancySubscriptions[i] = multitenancySubscription;
        }

        multitenancyInvoice.setSubscriptions(multitenancySubscriptions);

        // getting the purchase orders
        List<Payment> payments = invoice.getPayments();
        if (payments != null) {
            MultitenancyPurchaseOrder[] multitenancyPurchaseOrders =
                    new MultitenancyPurchaseOrder[payments.size()];

            for (int i = 0; i < payments.size(); i++) {
                Payment payment = payments.get(i);
                MultitenancyPurchaseOrder multitenancyPurchaseOrder =
                        new MultitenancyPurchaseOrder();

                multitenancyPurchaseOrder.setId(payment.getId());
                multitenancyPurchaseOrder.setPaymentDate(payment.getDate());
                multitenancyPurchaseOrder.setPayment(payment.getAmount().serializeToString());
                multitenancyPurchaseOrder.setTransactionId(payment.getDescription());
                multitenancyPurchaseOrders[i] = multitenancyPurchaseOrder;
            }
            multitenancyInvoice.setPurchaseOrders(multitenancyPurchaseOrders);
        }

        return multitenancyInvoice;
    }

    /**
     * Gets the interim invoice of the current customer
     * @param registry is the GovernanceUserRegistry
     * @return an MultiTenancyInvoice object
     * @throws Exception Exception
     */
    private MultitenancyInvoice getCurrentInvoiceOfCustomer(UserRegistry registry) throws Exception {
        // we have to generate the invoice for this.
        

        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngineViewer =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        Customer customer = getCurrentCustomer(registry, billingEngineViewer);
        if (customer == null) {
            // no customer => no invoices
            return null;
        }
        
        BillingEngineContext billingEngineContext = new BillingEngineContext();
        billingEngineContext.setCustomer(customer);
        billingEngineViewer.generateBill(billingEngineContext);

        // reloading the customer with new updates
        customer = billingEngineContext.getCustomer();
        Invoice invoice = customer.getActiveInvoice();

        // convert it and return
        if (invoice == null) {
            return null;
        }

        if (customer.getId() != invoice.getCustomer().getId()) {
            String msg = "Trying to looking at an invoice of another customer, customer: " +
                            customer.getId() + ", invoice: " + invoice.getId() + ".";
            log.error(msg);
            throw new Exception(msg);
        }
        
        MultitenancyInvoice multitenancyInvoice = new MultitenancyInvoice();
        multitenancyInvoice.setBillingDate(invoice.getDate());
        multitenancyInvoice.setBoughtForward(invoice.getBoughtForward().serializeToString());
        multitenancyInvoice.setCarriedForward(invoice.getCarriedForward().serializeToString());
        multitenancyInvoice.setEndDate(invoice.getEndDate());
        multitenancyInvoice.setInvoiceId(invoice.getId());
        multitenancyInvoice.setStartDate(invoice.getStartDate());
        
        // getting the purchase orders
        List<Payment> payments = invoice.getPayments();
        MultitenancyPurchaseOrder[] multitenancyPurchaseOrders =
                new MultitenancyPurchaseOrder[payments.size()];
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            MultitenancyPurchaseOrder multitenancyPurchaseOrder = new MultitenancyPurchaseOrder();
            multitenancyPurchaseOrder.setId(payment.getId());
            multitenancyPurchaseOrder.setPaymentDate(payment.getDate());
            multitenancyPurchaseOrder.setPayment(payment.getAmount().serializeToString());
            multitenancyPurchaseOrder.setTransactionId(payment.getDescription());
            multitenancyPurchaseOrders[i] = multitenancyPurchaseOrder;
        }
        multitenancyInvoice.setPurchaseOrders(multitenancyPurchaseOrders);

        List<Subscription> subscriptions = invoice.getSubscriptions();
        MultitenancySubscription[] multitenancySubscriptions =
                new MultitenancySubscription[subscriptions.size()];
        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription subscription = subscriptions.get(i);
            MultitenancySubscription multitenancySubscription = new MultitenancySubscription();
            multitenancySubscription.setSubscribedPackage(subscription.getItem().getName());
            multitenancySubscription.setActiveSince(subscription.getActiveSince());
            multitenancySubscription.setActiveUntil(subscription.getActiveUntil());
            multitenancySubscription.setActive(subscription.isActive());

            BilledEntry[] itemEntries;
            List<? extends Item> subItems = subscription.getItem().getChildren();
            if(subItems!=null){
                itemEntries = new BilledEntry[subItems.size()];
                for(int j=0; j<subItems.size(); j++){
                    BilledEntry billedEntry = new BilledEntry();
                    Item billedItem = subItems.get(j);
                    billedEntry.setName(billedItem.getDescription()); //description
                    if(billedItem.getCost()!=null){
                        billedEntry.setCost(billedItem.getCost().toString());   //cost
                    }else{
                        billedEntry.setCost(new Cash("$0").toString());
                    }
                    itemEntries[j] = billedEntry;
                }
            }else{
                itemEntries = new BilledEntry[0];
            }


            multitenancySubscription.setBilledEntries(itemEntries);
            multitenancySubscriptions[i] = multitenancySubscription;
        }
        multitenancyInvoice.setSubscriptions(multitenancySubscriptions);

        Cash totalCost = invoice.getTotalCost();
        if (totalCost == null) {
            totalCost = new Cash("$0");
        }
        multitenancyInvoice.setTotalCost(totalCost.serializeToString());
        
        Cash totalPaymentCash = invoice.getTotalPayment();
        if (totalPaymentCash == null) {
            totalPaymentCash = new Cash("$0");
        }
        multitenancyInvoice.setTotalPayments(totalPaymentCash.serializeToString());

        return multitenancyInvoice;
    }

    /**
     * Gets the tenant is and then fills the customer details
     * @param userRegistry to get the tenant id
     * @param billingEngine to fill the customer details
     * @return   a customer object
     * @throws Exception Exception
     */
    private Customer getCurrentCustomer(UserRegistry userRegistry,
                                        BillingEngine billingEngine) throws Exception {
        int currentTenantId = userRegistry.getTenantId();
        TenantManager tenantManger = Util.getRealmService().getTenantManager();
        Tenant currentTenant = (Tenant) tenantManger.getTenant(currentTenantId);

        List<Customer> customers = billingEngine.getCustomersWithName(currentTenant.getDomain());
        if (customers == null || customers.isEmpty()) {
            return null;
        }
        return customers.get(0);
    }

    private BillingPeriod[] getAvailableBillingPeriods(UserRegistry registry) throws Exception {
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        
        Customer customer = getCurrentCustomer(registry, billingEngine);
        if (customer == null) {
            return new BillingPeriod[0];
        }
        
        List<Invoice> invoices = billingEngine.getInvoices(customer);
        if (invoices == null || invoices.size() == 0) {
            return new BillingPeriod[0];
        }
        
        return getBillingPeriodsFromInvoices(invoices);
    }

    /**
     * Get the billing period details when given the invoices
     * @param invoices is list of invoices
     * @return an array of billing periods
     */
    private BillingPeriod[] getBillingPeriodsFromInvoices(List<Invoice> invoices){
        BillingPeriod[] billingPeriods = new BillingPeriod[invoices.size()];
        int index = 0;
        for (Invoice invoice : invoices) {
            BillingPeriod billingPeriod = new BillingPeriod();
            billingPeriod.setInvoiceId(invoice.getId());
            billingPeriod.setStartDate(invoice.getStartDate());
            billingPeriod.setEndDate(invoice.getEndDate());
            billingPeriod.setInvoiceDate(invoice.getDate());
            billingPeriods[index++] = billingPeriod;
        }
        return billingPeriods;
    }

    /**
     * Sends the payment received email to the customer
     * @param payment is the payment object with the payment details 
     * @throws Exception Exception
     */
    private void sendPaymentReceivedEmail(Payment payment) throws Exception{
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        if(payment.getInvoice()!=null){
            Invoice invoice = billingEngine.getInvoice(payment.getInvoice().getId());
            if(invoice!=null){
                Customer customer = invoice.getCustomer();
                if(customer!=null){
                    Map<String, String> mailParameters = new HashMap<String, String>();
                    mailParameters.put("date",
                            new SimpleDateFormat("dd-MMM-yyyy").format(payment.getDate()));
                    mailParameters.put("transaction-id", payment.getDescription());
                    mailParameters.put("amount", payment.getAmount().toString());
                    mailParameters.put("invoice-id", String.valueOf(payment.getInvoice().getId()));

                    try{
                        String customerName =
                                ClaimsMgtUtil.getFirstName(Util.getRealmService(), customer.getId());
                        if(customerName!=null){
                            mailParameters.put("customer-name", customerName);
                        }else{
                            mailParameters.put("customer-name", "");
                        }

                    }catch(Exception e){
                        log.error("Could not get tenant information for tenant: " +
                                customer.getName() + "\n" + e.getMessage());
                        mailParameters.put("customer-name", "");
                    }

                    //sending the mail to the customer
                    billingEngine.sendPaymentReceivedEmail(
                            customer.getEmail(),
                            BillingConstants.PAYMENT_RECEIVED_EMAIL_CUSTOMER_FILE,
                            mailParameters);

                    String financeEmail = CommonUtil.getStratosConfig().getFinanceNotificationEmail();
                    //customer's first name is not important to finance team. Therefore it is
                    //being replace with the domain name
                    mailParameters.put("customer-name", customer.getName());
                    billingEngine.sendPaymentReceivedEmail(
                            financeEmail,
                            BillingConstants.PAYMENT_RECEIVED_EMAIL_WSO2_FILE,
                            mailParameters
                    );
                }else{
                    String msg = "Cannot send email to customer. Customer details not available";
                    log.error(msg);
                    throw new Exception(msg);
                }
            }else{
                String msg = "Cannot send email to customer. Invoice details not available";
                log.error(msg);
                throw new Exception(msg);
            }
        }else{
            String msg = "Cannot send email to customer. Invoice Id is not available";
            log.error(msg);
            throw new Exception(msg);
        }
    }


    private void sendRegistrationPaymentReceivedEmail(Payment payment) throws Exception {
        BillingManager billingManager = Util.getBillingManager();
        BillingEngine billingEngine = billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);

        String tenantDomain = payment.getDescription().split(" ")[0];
        int tenantId = Util.getTenantManager().getTenantId(tenantDomain);
        Tenant tenant = (Tenant) Util.getTenantManager().getTenant(tenantId);

        Map<String, String> mailParameters = new HashMap<String, String>();
        mailParameters.put("date", new SimpleDateFormat("dd-MMM-yyyy").format(payment.getDate()));
        mailParameters.put("transaction-id", payment.getDescription().split(" ")[1]);
        mailParameters.put("amount", payment.getAmount().toString());
        mailParameters.put("invoice-id", "Registration - " + tenantDomain);
        mailParameters.put("tenant-domain", tenantDomain);

        String customerName = null;
        String customerEmail = tenant.getEmail();
        try {
            customerName = ClaimsMgtUtil.getFirstName(Util.getRealmService(), tenantId);
            if (customerName != null) {
                mailParameters.put("customer-name", customerName);
            } else {
                mailParameters.put("customer-name", "");
            }

        } catch (Exception e) {
            log.error("Could not get tenant information for tenant: " +
                      customerName + "\n" + e.getMessage());
            mailParameters.put("customer-name", "");
        }

        //sending the mail to the customer
        billingEngine.sendPaymentReceivedEmail(
                customerEmail,
                BillingConstants.REGISTRATION_PAYMENT_RECEIVED_EMAIL_CUSTOMER_FILE,
                mailParameters);

        String financeEmail = CommonUtil.getStratosConfig().getFinanceNotificationEmail();
        //customer's first name is not important to finance team. Therefore it is
        //being replace with the domain name
        mailParameters.put("customer-name", customerName);
        billingEngine.sendPaymentReceivedEmail(
                financeEmail,
                BillingConstants.PAYMENT_RECEIVED_EMAIL_WSO2_FILE,
                mailParameters
        );


    }

}
