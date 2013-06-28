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
import org.wso2.carbon.billing.core.*;
import org.wso2.carbon.billing.core.dataobjects.*;
import org.wso2.carbon.billing.core.internal.Util;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates overusage charges, CF value, total payments etc.
 */
public class InvoiceCalculationHandler implements BillingHandler {

    Log log = LogFactory.getLog(InvoiceCalculationHandler.class);
    Map<Integer, Discount> discountMap = new HashMap<Integer, Discount>();

    public void init(Map<String, String> handlerConfig) throws BillingException {
        //we are getting the discount list and put them in to a map
        List<Discount> discountList = BillingManager.getInstance().getDataAccessObject().getAllActiveDiscounts();
        for(Discount discount : discountList){
            discountMap.put(discount.getTenantId(), discount);
        }
    }

    public void execute(BillingEngineContext handlerContext) throws BillingException {
        // calculate the bill
        calculateInvoice(handlerContext);
    }

    private void calculateInvoice(BillingEngineContext handlerContext) throws BillingException {
        List<Subscription> subscriptions = handlerContext.getSubscriptions();
        Map<Integer, Invoice> invoiceMap = new HashMap<Integer, Invoice>();
       
        for (Subscription subscription : subscriptions) {
            Customer customer = subscription.getCustomer();

            // invoice should be already set..
            Invoice invoice = customer.getActiveInvoice();
            Cash totalCost = invoice.getTotalCost();
            if (totalCost == null) {
                totalCost = new Cash("$0");
            }
        
            Item item = subscription.getItem();
            //prorate the subscription cost
            //prorateItemCosts(item, invoice, subscription);
            calculateItemCost(item, invoice, subscription);
            Cash itemCost = getItemCost(item);
            totalCost = Cash.add(totalCost, itemCost);
            invoice.setTotalCost(totalCost);
            if (invoiceMap.get(customer.getId()) == null) {
                invoiceMap.put(customer.getId(), invoice);
            }
        }

        // from the invoice set we are calculating the payments       purchase orders
        for (Invoice invoice : invoiceMap.values()) {
            Cash totalPayment = invoice.getTotalPayment();
            if (totalPayment == null) {
                totalPayment = new Cash("$0");
            }
            List<Payment> payments = invoice.getPayments();
            if (payments != null) {
                for (Payment payment : payments) {
                    Cash paymentCash = payment.getAmount();
                    totalPayment = Cash.add(paymentCash, totalPayment);
                }
            }
            invoice.setTotalPayment(totalPayment);

            // setting the carried forward
            Cash boughtForward = invoice.getBoughtForward();
            if (boughtForward == null) {
                boughtForward = new Cash("$0");
            }
            Cash totalCost = invoice.getTotalCost();
            Cash carriedForward = Cash.subtract(Cash.add(boughtForward, totalCost), totalPayment);
            invoice.setCarriedForward(carriedForward);
        }

        log.info("Invoice calculation phase completed. " + invoiceMap.size() + " invoices were calculated");
    }

    private Cash getItemCost(Item item) throws BillingException {
        Cash itemCost = item.getCost();
        if (itemCost == null) {
            itemCost = new Cash("$0");
        }
        if (item.getChildren() != null) {
            // and iterate through all the item children
            for (Item subItem : item.getChildren()) {
                Cash subItemCost = subItem.getCost();
                if (subItemCost != null) {
                    itemCost = Cash.add(itemCost, subItemCost);
                }
            }
        }
        return itemCost;
    }

    private void calculateItemCost(Item item, Invoice invoice, Subscription subscription) throws BillingException {
        if(item.getChildren()!=null){
            for(Item subItem : item.getChildren()){
                if((BillingConstants.BANDWIDTH_SUBITEM.equals(subItem.getName()) ||
                    BillingConstants.STORAGE_SUBITEM.equals(subItem.getName()) ||
                    BillingConstants.CARTRIDGE_SUBITEM.equals(subItem.getName())) && subscription.isActive()){
                    calculateOverUseCharges(item, subItem, subscription);
                }else if(BillingConstants.SUBSCRIPTION_SUBITEM.equals(subItem.getName())){
                    prorateItemCosts(subItem, invoice, subscription);
                }
            }
        }
    }

    private void calculateOverUseCharges(Item item, Item subItem, Subscription subscription) throws BillingException {
        //calculating cost for bandwidth overuse
        if(BillingConstants.BANDWIDTH_SUBITEM.equals(subItem.getName())){
            long bandwidthUsage = subscription.getCustomer().getTotalBandwidth()/(1024 * 1024L);
            long bandwidthOveruse = 0;
            if(bandwidthUsage > item.getBandwidthLimit()){
                bandwidthOveruse = bandwidthUsage - item.getBandwidthLimit();
                subItem.setCost(item.getBandwidthOveruseCharge().multiply(bandwidthOveruse));
            }
            StringBuffer description = new StringBuffer();
            description.append(subItem.getDescription());
            description.append(": ").append(bandwidthOveruse).append("MB");
            subItem.setDescription(description.toString());
        //calculating cost for storage overuse    
        }else if(BillingConstants.STORAGE_SUBITEM.equals(subItem.getName())){
            long storageUsage = subscription.getCustomer().getTotalStorage()/(1024 * 1024L);
            long storageOveruse = 0;
            if(storageUsage > item.getResourceVolumeLimit()){
                storageOveruse = storageUsage - item.getResourceVolumeLimit();
                subItem.setCost(item.getResourceVolumeOveruseCharge().multiply(storageOveruse));
            }
            StringBuffer description = new StringBuffer();
            description.append(subItem.getDescription());
            description.append(": ").append(storageOveruse).append("MB");
            subItem.setDescription(description.toString());
        //calculating the cost for cartridge overuse
        }else if(BillingConstants.CARTRIDGE_SUBITEM.equals(subItem.getName())){
            long cartridgeCpuUsage = subscription.getCustomer().getTotalCartridgeCPUHours();
            long cartridgeCpuOveruse = 0;
            if(cartridgeCpuUsage > item.getCartridgeCPUHourLimit()){
                cartridgeCpuOveruse = cartridgeCpuUsage - item.getCartridgeCPUHourLimit();
                subItem.setCost(item.getCartridgeCPUOveruseCharge().multiply(cartridgeCpuOveruse));
            }

            StringBuffer description = new StringBuffer();
            description.append(subItem.getDescription());
            description.append(": ").append(cartridgeCpuOveruse).append("Hours");
            subItem.setDescription(description.toString());
        }

    }

    //by looking at the start and end dates of the invoice, subscription item's cost is interpolated
    private void prorateItemCosts(Item subItem, Invoice invoice, Subscription subscription) throws BillingException {
        long milisecondsPerDay = 24*60*60*1000L;
        NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);

        int tenantId = invoice.getCustomer().getId();
        Discount discount = discountMap.get(tenantId);

        long period;
        long days;

        if(subscription.isActive()){
            if(subscription.getActiveSince().before(invoice.getStartDate())){
                period = invoice.getEndDate().getTime() - invoice.getStartDate().getTime();
            }else{
                period = invoice.getEndDate().getTime() - subscription.getActiveSince().getTime();
            }
        }else{ 
            if(subscription.getActiveSince().before(invoice.getStartDate())){
                period = subscription.getActiveUntil().getTime() - invoice.getStartDate().getTime();
            }else{
                period = subscription.getActiveUntil().getTime() - subscription.getActiveSince().getTime();
            }
        }

        //I am considering 28 days or more as a complete month
        days = period/milisecondsPerDay;
        if(days<28){
            float multiplyingFactor = (float)days/30;
            multiplyingFactor = Float.parseFloat(nf.format(multiplyingFactor));

            //prorate the subscription fee...
            if(subItem.getCost()!=null){
                subItem.setCost(subItem.getCost().multiply(multiplyingFactor));
            }

            //prorating the discount too (if the discount is defined as a raw amount)...
            if(discount!=null && !discount.isPercentageType()){
                discount.setAmount(Float.parseFloat(nf.format(discount.getAmount()*multiplyingFactor)));
            }
        }
        
        //Check whether the customer is offered any discounts and reduce the subscription fee
        
        if(discount!=null){
            
            if(discount.isPercentageType()){
                subItem.setCost(subItem.getCost().multiply(1 - (discount.getPercentage()/100)));
                subItem.setDescription(subItem.getDescription() + " (with " + discount.getPercentage() + "% discount)");
                log.info("Customer: " + tenantId + " was qualified for a discount of " +
                        discount.getPercentage() + "% for subscription:" + subscription.getSubscriptionPlan() +
                        " which started from:" + subscription.getActiveSince().toString());
            }else{
                subItem.setCost(Cash.subtract(subItem.getCost(), new Cash(String.valueOf(discount.getAmount()))));
                subItem.setDescription(subItem.getDescription() + " (with " + new Cash(String.valueOf(discount.getAmount())).toString() + " discount)");
                log.info("Customer: " + tenantId + " was qualified for a discount of " +
                        new Cash(String.valueOf(discount.getAmount())).toString() + " for subscription:" + subscription.getSubscriptionPlan() +
                        " which started from:" + subscription.getActiveSince().toString());
            }

        }
    }
}
