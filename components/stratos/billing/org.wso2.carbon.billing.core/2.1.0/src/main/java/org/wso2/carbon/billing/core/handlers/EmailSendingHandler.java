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
package org.wso2.carbon.billing.core.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingConstants;
import org.wso2.carbon.billing.core.BillingEngineContext;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingHandler;
import org.wso2.carbon.billing.core.dataobjects.Cash;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Invoice;
import org.wso2.carbon.billing.core.dataobjects.Item;
import org.wso2.carbon.billing.core.dataobjects.Payment;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.ClaimsMgtUtil;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.email.sender.api.BulkEmailSender;
import org.wso2.carbon.email.sender.api.EmailDataHolder;
import org.wso2.carbon.email.sender.api.EmailSender;
import org.wso2.carbon.email.sender.api.EmailSenderConfiguration;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the email notifications after the bill generation
 */
public class EmailSendingHandler implements BillingHandler {
    private static Log log = LogFactory.getLog(EmailSendingHandler.class);
    private static String DEFAULT_EMAIL_NOTIFICATION_TEMPLATE_FILE = "email-billing-notification.xml";
    private static String DEFAULT_REPORT_EMAIL_TEMPLATE_FILE = "email-bill-generated.xml";
    private static String EMAIL_SENDING_CONF_KEY = "file";
    private static String REPORT_EMAIL_TO_ADDRESS ="cloudservice@wso2.com";
    BulkEmailSender bulkEmailSender;
    EmailSender reportMailSender;

    public void init(Map<String, String> handlerConfig) throws BillingException {
        if(CommonUtil.getStratosConfig()!=null){
            REPORT_EMAIL_TO_ADDRESS = CommonUtil.getStratosConfig().getNotificationEmail();
        }
        
        String confFileName = handlerConfig.get(EMAIL_SENDING_CONF_KEY);
        if (confFileName == null) {
            confFileName = DEFAULT_EMAIL_NOTIFICATION_TEMPLATE_FILE;
        }
        confFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator
                       +StratosConstants.EMAIL_CONFIG+ File.separator + confFileName;

        EmailSenderConfiguration emailSenderConfig =
                EmailSenderConfiguration.loadEmailSenderConfiguration(confFileName);
        bulkEmailSender = new BulkEmailSender(emailSenderConfig);

        //this is the email sent to the admin after bill generation
        String reportMailFileName = DEFAULT_REPORT_EMAIL_TEMPLATE_FILE;
        reportMailFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator
                             +StratosConstants.EMAIL_CONFIG+File.separator+ reportMailFileName;

        EmailSenderConfiguration reportEmailSenderConfig =
                EmailSenderConfiguration.loadEmailSenderConfiguration(reportMailFileName);
        reportMailSender = new EmailSender(reportEmailSenderConfig);

    }

    public void execute(BillingEngineContext handlerContext) throws BillingException {
        List<Subscription> subscriptions = handlerContext.getSubscriptions();
        Map<Integer, Invoice> invoiceMap = new HashMap<Integer, Invoice>();
        List<Integer> creditLimitExceededCustomers = new ArrayList<Integer>();
        //this holds the data to be sent to bulk email bulkEmailSender
        List<EmailDataHolder> emailDataList = new ArrayList<EmailDataHolder>();
        
        for (Subscription subscription : subscriptions) {
            Customer customer = subscription.getCustomer();
            Invoice invoice = customer.getActiveInvoice();
            if (invoiceMap.get(customer.getId()) == null) {
                invoiceMap.put(customer.getId(), invoice);
            }
        }

        for (Invoice invoice : invoiceMap.values()) {

            //checkinh whether the carried forward is $0 because we dont want to
            //send emails for $0 bills
            Cash diff = Cash.subtract(new Cash("$0"), invoice.getCarriedForward());
            if(diff.getSign().equals(Cash.Sign.NEGATIVE)){
                Map<String, String> mailParameters = deriveInvoiceMailParameters(invoice);
                Customer customer = invoice.getCustomer();
                String emailAddress = customer.getEmail();
                EmailDataHolder emailData = new EmailDataHolder();
                emailData.setEmail(emailAddress);
                emailData.setEmailParameters(mailParameters);
                //we keep the data in the list to be sent as bulk
                emailDataList.add(emailData);
            }

            //adding the customers who have exceeded the credit limit
            // to a list to be informed to the admin
            if(isExceedsCreditLimit(invoice)){
                log.debug("Customer " + invoice.getCustomer().getName() + " needs to be reported");
                creditLimitExceededCustomers.add(invoice.getCustomer().getId());
            }

        }

        try {
            log.info("Sending emails to the customers: " + emailDataList.size());
            bulkEmailSender.sendBulkEmails(emailDataList);
            log.info("Email (invoices) sending completed");
        } catch (Exception e) {
            String msg = "Error in sending the invoices to the customers";
            log.error(msg, e);
        }
        //now sending the email with customers who have exceeded the credit limit
        Map<String, String> reportEmailParameters = deriveReportEmailParameters(creditLimitExceededCustomers, invoiceMap);
        try {
            reportMailSender.sendEmail(REPORT_EMAIL_TO_ADDRESS, reportEmailParameters);
            log.info("Email sent to the admin.");
        } catch (Exception e) {
            String msg = "Error in sending the bill generation completed email";
            log.error(msg, e);
        }

    }

    public static Map<String, String> deriveInvoiceMailParameters(Invoice invoice) {
        Map<String, String> mailParameters = new HashMap<String, String>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        mailParameters.put("start-date", dateFormat.format(invoice.getStartDate()));
        mailParameters.put("end-date", dateFormat.format(invoice.getEndDate()));

        Customer customer = invoice.getCustomer();

        try{
            TenantManager tenantManager = Util.getRealmService().getTenantManager();
            Tenant tenant = (Tenant) tenantManager.getTenant(customer.getId());
            mailParameters.put("tenant-domain", tenant.getDomain());
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

        List<Subscription> subscriptions = invoice.getSubscriptions();
        if (subscriptions != null) {
            StringBuffer subscriptionText = new StringBuffer();
            for (Subscription subscription : subscriptions) {

                if(subscription.isActive()){
                    mailParameters.put("current-subscription", subscription.getSubscriptionPlan());
                }
                Item item = subscription.getItem();
                String itemName = item.getDescription();
                Cash itemCost = item.getCost();

                String subscriptionPlan = subscription.getSubscriptionPlan();
                if(itemCost!=null){
                    subscriptionText.append(subscriptionPlan).append("\t\t\t").append(itemCost.toString()).append("\n");
                }else{
                    subscriptionText.append(subscriptionPlan).append("\n");
                }

                List<? extends Item> children = item.getChildren();
                if (children != null) {
                    for (Item childItem : children) {
                        String childItemName = childItem.getDescription();
                        Cash childItemCost = childItem.getCost();
                        String childItemCostStr;
                        if(childItemCost!=null){
                            childItemCostStr = childItemCost.toString();
                        }else{
                            childItemCostStr = "$0.00";
                        }
                        subscriptionText.append("\t").append(childItemName).append("\t\t")
                                .append(childItemCostStr).append("\n");
                    }
                }
                subscriptionText.append("-------------------------------------------").append("\n");
            }
            mailParameters.put("subscription-charges", subscriptionText.toString());
        }

        StringBuffer paymentText = new StringBuffer();
        if (invoice.getPayments() != null && invoice.getPayments().size()>0) {
            for (Payment payment : invoice.getPayments()) {
                Date paymentDate = payment.getDate();
                Cash paymentAmount = payment.getAmount();

                paymentText.append(dateFormat.format(paymentDate)).append("\t\t")
                        .append(paymentAmount.toString()).append("\n");
            }
        }else{
            paymentText.append("No payment details during this period");
        }
        mailParameters.put("payment-details", paymentText.toString());

        if (invoice.getBoughtForward() != null) {
            mailParameters.put("bought-forward", invoice.getBoughtForward().toString());
        } else {
            mailParameters.put("bought-forward", "$0");
        }
        if (invoice.getTotalCost() != null) {
            mailParameters.put("total-cost", invoice.getTotalCost().toString());
        } else {
            mailParameters.put("total-cost", "$0");
        }
        if (invoice.getTotalPayment() != null) {
            mailParameters.put("total-payments", invoice.getTotalPayment().toString());
        } else {
            mailParameters.put("total-payments", "$0");
        }
        if (invoice.getCarriedForward() != null) {
            mailParameters.put("carried-forward", invoice.getCarriedForward().toString());
        } else {
            mailParameters.put("carried-forward", "$0");
        }

        return mailParameters;
    }

    private boolean isExceedsCreditLimit(Invoice invoice) throws BillingException{
        boolean exceedsCreditLimit = false;
        Cash creditLimit = new Cash("$0");
        List<Subscription> subscriptions = invoice.getSubscriptions();
        for(Subscription subscription : subscriptions){
            if(subscription.isActive()){
                List<? extends Item> subItems = subscription.getItem().getChildren();
                for (Item item : subItems){
                    if(BillingConstants.SUBSCRIPTION_SUBITEM.equals(item.getName())){
                        if(item.getCreditLimit()!=null){
                            creditLimit = Cash.add(creditLimit, item.getCreditLimit());
                        }else{
                            creditLimit = Cash.add(creditLimit, new Cash("$0"));
                        }
                        break;
                    }
                }
                break;
            }
        }

        Cash difference = Cash.subtract(invoice.getCarriedForward(), creditLimit);
        if(Cash.Sign.POSITIVE == difference.getSign() && difference.getWholeNumber()>0){
            exceedsCreditLimit = true;
        }
        return exceedsCreditLimit;
    }

    public Map<String, String> deriveReportEmailParameters(List<Integer> creditExceededCustomers, Map<Integer, Invoice> invoiceMap){
        Map<String, String> mailParameters = new HashMap<String, String>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        //note that I haven't considered the timezone here
        mailParameters.put("date", dateFormat.format(new Date()));

        StringBuffer reportedCustomers = new StringBuffer();
        if(creditExceededCustomers.isEmpty()){
            reportedCustomers.append("No customers to be reported");
        }else{
            for(Integer customerId : creditExceededCustomers){
                Invoice invoice = invoiceMap.get(customerId);
                List<Subscription> subscriptions = invoice.getSubscriptions();
                String activeSubscriptionName = "";
                for(Subscription subscription : subscriptions){
                    if(subscription.isActive()){
                        activeSubscriptionName = subscription.getSubscriptionPlan();
                        break;
                    }
                }
                reportedCustomers.append(invoice.getCustomer().getName()).append("\t\t").
                        append(activeSubscriptionName).append("\t\t").append(invoice.getCarriedForward().toString()).
                        append("\n");
            }
        }
        mailParameters.put("reported-customers", reportedCustomers.toString());

        return mailParameters;
    }    
}
