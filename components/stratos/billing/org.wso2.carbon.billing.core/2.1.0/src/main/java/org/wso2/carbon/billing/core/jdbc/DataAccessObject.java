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
package org.wso2.carbon.billing.core.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.dataobjects.*;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.billing.core.utilities.CustomerUtils;
import org.wso2.carbon.billing.core.utilities.DataSourceHolder;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreException;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class DataAccessObject {
    public static final Log log = LogFactory.getLog(DataAccessObject.class);
    public static final int INVALID = -1;
    //following timezone will be changed according to the one defined in the billing-config.xml
    //It is done in the MonthlyScheduleHelper
    public static String TIMEZONE="GMT-8:00";

    DataSource dataSource;

    public DataAccessObject(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataAccessObject() {
        this.dataSource = DataSourceHolder.getDataSource();
    }
    // transaction handling

    public void beginTransaction() throws BillingException {
        if (Transaction.getNestedDepth() != 0) {
            if (log.isTraceEnabled()) {
                log.trace("The transaction was not started, because it is called within a "
                        + "transaction, nested depth: " + Transaction.getNestedDepth() + ".");
            }
            Transaction.incNestedDepth();
            return;
        }

        Connection conn;
        try {
            conn = dataSource.getConnection();
            if (conn.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
            conn.setAutoCommit(false);
            Transaction.incNestedDepth();
        } catch (SQLException e) {
            String msg = "Failed to start new billing transaction. " + e.getMessage();
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        Transaction.setConnection(conn);
    }

    public void rollbackTransaction() throws BillingException {
        Transaction.setRollbacked(true);
        if (Transaction.getNestedDepth() != 1) {
            if (log.isTraceEnabled()) {
                log.trace("The transaction was not rollbacked, because it is called within a "
                        + "transaction, nested depth: " + Transaction.getNestedDepth() + ".");
            }

            Transaction.decNestedDepth();
            return;
        }

        Connection conn = Transaction.getConnection();
        try {
            conn.rollback();

        } catch (SQLException e) {
            String msg = "Failed to rollback transaction. " + e.getMessage();
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            endTransaction();
            Transaction.decNestedDepth();
        }
    }

    public void commitTransaction() throws BillingException {
        if (Transaction.getNestedDepth() != 1) {
            if (log.isTraceEnabled()) {
                log.trace("The transaction was not commited, because it is called within a "
                        + "transaction, nested depth: " + Transaction.getNestedDepth() + ".");
            }
            Transaction.decNestedDepth();
            return;
        }

        if (Transaction.isRollbacked()) {
            String msg = "The transaction is already rollbacked, you can not commit a transaction "
                    + "already rollbacked, nested depth: " + Transaction.getNestedDepth() + ".";
            log.debug(msg);
            Transaction.decNestedDepth();
            throw new BillingException(msg);
        }

        Connection conn = Transaction.getConnection();
        try {
            conn.commit();

        } catch (SQLException e) {
            String msg = "Failed to commit transaction. " + e.getMessage();
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            endTransaction();
            Transaction.decNestedDepth();
        }
    }

    private void endTransaction() throws BillingException {

        if (Transaction.isStarted()) {
            Connection conn = Transaction.getConnection();
            try {
                conn.close();
                //log.info("Database connection closed: ");
            } catch (SQLException e) {
                String msg = "Failed to close transaction. " + e.getMessage();
                log.error(msg, e);
                throw new BillingException(msg, e);

            } finally {
                Transaction.setStarted(false);
                Transaction.setConnection(null);
            }
        }
    }

    public int getItemIdWithName(String itemName) throws BillingException {

        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int id = INVALID;
        try {
            String sql = "SELECT BC_ID FROM BC_ITEM WHERE BC_NAME=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, itemName);

            result = ps.executeQuery();

            if (result.next()) {
                id = result.getInt("BC_ID");
            }
        } catch (SQLException e) {
            String msg = "Failed to check the existence of the items with name " + itemName + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
            
        }

        return id;
    }

    public int getItemId(String itemName, int parentId) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int id = INVALID;
        try {
            String sql = "SELECT BC_ID FROM BC_ITEM WHERE BC_NAME=? AND BC_PARENT_ITEM_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, itemName);
            ps.setInt(2, parentId);

            result = ps.executeQuery();

            if (result.next()) {
                id = result.getInt("BC_ID");
            }
        } catch (SQLException e) {
            String msg = "Failed to check the existence of the items with name " + itemName + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }

        return id;
    }

    public List<Item> getItemsWithName(String itemName) throws BillingException {
        /*Connection conn = null;
        try {
              conn = DataSourceHolder.getDataSource().getConnection();
                } catch (SQLException e) {
                    String msg = "Failed to establish data connection";
                    log.error(msg, e);
                }
        */
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Item> items = new ArrayList<Item>();

        try {
            String sql = "SELECT BC_ID, BC_COST, BC_DESCRIPTION, BC_PARENT_ITEM_ID " +
                    " FROM BC_ITEM WHERE BC_NAME=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, itemName);

            result = ps.executeQuery();

            while (result.next()) {
                Item item = new Item();
                item.setName(itemName);
                int id = result.getInt("BC_ID");
                item.setId(id);
                String costStr = result.getString("BC_COST");
                Cash cost = new Cash(costStr);
                item.setCost(cost);
                item.setDescription(result.getString("BC_DESCRIPTION"));
                int parentId = result.getInt("BC_PARENT_ITEM_ID");
                if (parentId > 0) {
                    Item parentItem;
                    if (id == parentId) {
                        parentItem = item;
                    } else {
                        parentItem = getItem(parentId);
                    }
                    item.setParent(parentItem);
                }
                items.add(item);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the items with name " + itemName + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
            /*try{
                if(conn !=null){
                    conn.close();
                }
            }
            catch (SQLException e){
                String msg = "Error while closing connection";
                log.error(msg, e);
            }*/
        }

        return items;
    }

    public int addInvoice(Invoice invoice) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int invoiceId = INVALID;

        try {
            String sql = "INSERT INTO BC_INVOICE (BC_TENANT_ID, BC_DATE, BC_START_DATE, "
                    + "BC_END_DATE, BC_BOUGHT_FORWARD, BC_CARRIED_FORWARD, BC_TOTAL_PAYMENTS, "
                    + "BC_TOTAL_COST) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the data values
            ps.setInt(1, invoice.getCustomer().getId());
            ps.setTimestamp(2, new Timestamp(invoice.getDate().getTime()));
            ps.setTimestamp(3, new Timestamp(invoice.getStartDate().getTime()));
            ps.setTimestamp(4, new Timestamp(invoice.getEndDate().getTime()));
            ps.setString(5, invoice.getBoughtForward().serializeToString());
            ps.setString(6, invoice.getCarriedForward().serializeToString());
            ps.setString(7, invoice.getTotalPayment().serializeToString());
            ps.setString(8, invoice.getTotalCost().serializeToString());

            ps.executeUpdate();
            result = ps.getGeneratedKeys();
            if (result.next()) {
                invoiceId = result.getInt(1);
                invoice.setId(invoiceId);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the invoice for customer, "
                    + invoice.getCustomer().getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoiceId;
    }

    public int addItem(Item item) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int itemId = INVALID;
        try {
            String sql = "INSERT INTO BC_ITEM (BC_NAME, BC_COST, BC_DESCRIPTION, " +
                    "BC_PARENT_ITEM_ID) " +
                    "VALUES (?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the data values
            ps.setString(1, item.getName());


            Cash cost = item.getCost();
            if (cost == null) {
                ps.setString(2, null);
            } else {
                ps.setString(2, cost.serializeToString());
            }
            ps.setString(3, item.getDescription());
            if (item.getParent() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, item.getParent().getId());
            }

            ps.executeUpdate();
            result = ps.getGeneratedKeys();
            if (result.next()) {
                itemId = result.getInt(1);
                item.setId(itemId);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the item, " +
                    item.getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return itemId;
    }

    // return the existence of the customer

    public void fillCustomerData(int customerId, Customer customer) throws BillingException {
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

    public List<Customer> getAllCustomers() throws BillingException {
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

    public List<Item> getAllItems() throws BillingException {
        Map<Integer, Item> items = new HashMap<Integer, Item>();
        Map<Item, Integer> itemParents = new HashMap<Item, Integer>();
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            String sql = "SELECT BC_ID, BC_NAME, BC_COST, BC_DESCRIPTION, BC_PARENT_ITEM_ID " +
                    " FROM BC_ITEM";
            ps = conn.prepareStatement(sql);

            result = ps.executeQuery();

            while (result.next()) {
                Item item = new Item();
                int id = result.getInt("BC_ID");
                item.setId(id);
                String costStr = result.getString("BC_COST");
                Cash cost = new Cash(costStr);
                item.setCost(cost);
                item.setDescription(result.getString("BC_DESCRIPTION"));
                int parentId = result.getInt("BC_PARENT_ITEM_ID");
                if (parentId > 0) {
                    itemParents.put(item, parentId);
                }
                items.put(id, item);
            }
        } catch (SQLException e) {
            String msg = "Failed to get all the items.";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }

        // before return resolve all the ids for items who have parents
        for (Map.Entry<Item, Integer> entry : itemParents.entrySet()) {
            Item item = entry.getKey();
            int parentId = entry.getValue();
            Item parentItem = items.get(parentId);
            item.setParent(parentItem);
        }
        List<Item> returnVal = new ArrayList<Item>();
        returnVal.addAll(items.values());
        return returnVal;
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

    public Customer getCustomer(int customerId) throws BillingException {
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

    public Item getItem(int itemId) throws BillingException {
        Connection conn = Transaction.getConnection();
        /*try {
            conn = DataSourceHolder.getDataSource().getConnection();
        } catch (SQLException e) {
            String msg = "Failed to establish data connection";
            log.error(msg, e);
        }*/
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            String sql = "SELECT BC_NAME, BC_COST, BC_DESCRIPTION, BC_PARENT_ITEM_ID " +
                    " FROM BC_ITEM WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, itemId);

            result = ps.executeQuery();

            if (result.next()) {
                Item item = new Item();
                item.setId(itemId);
                String costStr = result.getString("BC_COST");
                Cash cost = new Cash(costStr);
                item.setCost(cost);
                item.setDescription(result.getString("BC_DESCRIPTION"));
                item.setName(result.getString("BC_NAME"));
                int parentId = result.getInt("BC_PARENT_ITEM_ID");
                if (parentId > 0) {
                    Item parentItem;
                    if (itemId == parentId) {
                        parentItem = item;
                    } else {
                        parentItem = getItem(parentId);
                    }
                    item.setParent(parentItem);
                }
                return item;
            }
        } catch (SQLException e) {
            String msg = "Failed to get the item with item id: " + itemId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
            /*try{
                if(conn !=null){
                    conn.close();
                }
            }
            catch (SQLException e){
                String msg = "Error while closing connection";
                log.error(msg, e);
            }*/
        }

        return null;
    }

    public void updateSubscription(Subscription subscription) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE BC_SUBSCRIPTION SET BC_IS_ACTIVE=?, " +
                    "BC_ACTIVE_SINCE=? , BC_ACTIVE_UNTIL=?, BC_ITEM_ID=?, BC_TENANT_ID=? " +
                    "WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);

            // updating the subscription
            ps.setInt(1, subscription.isActive() ? 1 : 0);
            long activeSinceTime = 0;
            if (subscription.getActiveSince() != null) {
                activeSinceTime = subscription.getActiveSince().getTime();
            }
            ps.setTimestamp(2, new Timestamp(activeSinceTime));
            long activeUntilTime = 0;
            if (subscription.getActiveUntil() != null) {
                activeUntilTime = subscription.getActiveUntil().getTime();
            }
            ps.setTimestamp(3, new Timestamp(activeUntilTime));
            ps.setInt(4, subscription.getItem().getId());
            ps.setInt(5, subscription.getCustomer().getId());
            ps.setInt(6, subscription.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            String msg = "Error in updating the subscription: " + subscription.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            }
            catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
    }

    public int addSubscription(Subscription subscription, String filter) throws BillingException {

        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int subscriptionId = -1;
        try {
            String sql = "INSERT INTO BC_SUBSCRIPTION (BC_FILTER, BC_IS_ACTIVE, " +
                    "BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, BC_TENANT_ID) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the subscription
            ps.setString(1, filter);
            ps.setInt(2, subscription.isActive() ? 1 : 0);
            long activeSinceTime = 0;
            if (subscription.getActiveSince() != null) {
                activeSinceTime = subscription.getActiveSince().getTime();
            }else{
                activeSinceTime = System.currentTimeMillis();
                subscription.setActiveSince(new Date(activeSinceTime));
            }
            ps.setTimestamp(3, new Timestamp(activeSinceTime));
            long activeUntilTime = 0;

            if (subscription.getActiveUntil() != null) {
                activeUntilTime = subscription.getActiveUntil().getTime();
            }
            else{
                //When adding to database each user will activate until 2 years 
                //from the registration date
                Calendar activeUntilDate = Calendar.getInstance();
                activeUntilDate.setTimeInMillis(subscription.getActiveSince().getTime());
                activeUntilDate.add(Calendar.YEAR,2);
                activeUntilTime = activeUntilDate.getTimeInMillis();
            }

            ps.setTimestamp(4, new Timestamp(activeUntilTime));
            ps.setInt(5, getItemIdWithName(filter));

            //setting the customer id: we dont have the customer id in the customer object
            //which comes with the subscription object. Therefore we have to get it.
            if(subscription.getCustomer().getId()==0){
                int customerId = CustomerUtils.getCustomerId(subscription.getCustomer().getName());
                if(customerId==0){
                    throw new BillingException("No customer found with domain: " +
                            subscription.getCustomer().getName());
                }
                ps.setInt(6, customerId);
            }else{
                ps.setInt(6, subscription.getCustomer().getId());
            }

            ps.executeUpdate();
            result = ps.getGeneratedKeys();
            if (result.next()) {
                subscriptionId = result.getInt(1);
                subscription.setId(subscriptionId);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the subscription.";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscriptionId;
    }

    public void deleteBillingData(int tenantId) throws Exception {
        Connection conn = Transaction.getConnection();
        PreparedStatement deleteSubItemPs = null;
        PreparedStatement deleteInvoiceSubsPs = null;
        PreparedStatement deletePaymentSubsPs = null;
        PreparedStatement deletePaymentPs = null;
        PreparedStatement deleteInvoicePs = null;
        PreparedStatement deleteSubscriptionPs = null;
        try {
            conn.setAutoCommit(false);
            String deleteSubItemRecordsSql = "DELETE FROM BC_INVOICE_SUBSCRIPTION_ITEM WHERE BC_INVOICE_SUBSCRIPTION_ID IN " +
                                             "(SELECT BC_ID FROM BC_INVOICE_SUBSCRIPTION WHERE BC_INVOICE_ID IN " +
                                             "(SELECT BC_ID FROM BC_INVOICE WHERE BC_TENANT_ID = ?));";
            deleteSubItemPs = conn.prepareStatement(deleteSubItemRecordsSql);
            deleteSubItemPs.setInt(1, tenantId);
            deleteSubItemPs.executeUpdate();

            String deleteInvoiceSubsRecordsSql = "DELETE FROM BC_INVOICE_SUBSCRIPTION WHERE BC_INVOICE_ID IN " +
                                                 "(SELECT BC_ID FROM BC_INVOICE WHERE BC_TENANT_ID = ?) OR " +
                                                 "BC_SUBSCRIPTION_ID IN (SELECT BC_ID FROM BC_SUBSCRIPTION WHERE BC_TENANT_ID = ?)";
            deleteInvoiceSubsPs = conn.prepareStatement(deleteInvoiceSubsRecordsSql);
            deleteInvoiceSubsPs.setInt(1, tenantId);
            deleteInvoiceSubsPs.setInt(2, tenantId);
            deleteInvoiceSubsPs.executeUpdate();

            String deletePaymentSubsRecordsSql = "DELETE FROM BC_PAYMENT_SUBSCRIPTION WHERE BC_PAYMENT_ID IN " +
                                                 "(SELECT BC_ID FROM BC_PAYMENT WHERE BC_TENANT_ID = ?) OR " +
                                                 "BC_SUBSCRIPTION_ID IN (SELECT BC_ID FROM BC_SUBSCRIPTION WHERE BC_TENANT_ID = ?)";
            deletePaymentSubsPs = conn.prepareStatement(deletePaymentSubsRecordsSql);
            deletePaymentSubsPs.setInt(1, tenantId);
            deletePaymentSubsPs.setInt(2, tenantId);
            deletePaymentSubsPs.executeUpdate();

            String deletePaymentRecordsSql = "DELETE FROM BC_PAYMENT WHERE BC_INVOICE_ID IN " +
                                             "(SELECT BC_ID FROM BC_INVOICE WHERE BC_TENANT_ID = ?)";
            deletePaymentPs = conn.prepareStatement(deletePaymentRecordsSql);
            deletePaymentPs.setInt(1, tenantId);
            deletePaymentPs.executeUpdate();

            String deleteInvoiceRecordsSql = "DELETE FROM BC_INVOICE WHERE BC_TENANT_ID = ?";
            deleteInvoicePs = conn.prepareStatement(deleteInvoiceRecordsSql);
            deleteInvoicePs.setInt(1, tenantId);
            deleteInvoicePs.executeUpdate();

            String deleteSubscriptionRecordsSql = "DELETE FROM BC_SUBSCRIPTION WHERE BC_TENANT_ID = ?";
            deleteSubscriptionPs = conn.prepareStatement(deleteSubscriptionRecordsSql);
            deleteSubscriptionPs.setInt(1, tenantId);
            deleteSubscriptionPs.executeUpdate();
        } catch (SQLException e) {
            String msg = "Failed to delete billing information for tenant: " + tenantId;
            log.error(msg, e);
            throw new BillingException(msg, e);
        } finally {
            try {
                if (deleteSubItemPs != null) {
                    deleteSubItemPs.close();
                }
                if (deleteInvoiceSubsPs != null) {
                    deleteInvoiceSubsPs.close();
                }
                if (deletePaymentSubsPs != null) {
                    deletePaymentSubsPs.close();
                }
                if (deletePaymentPs != null) {
                    deletePaymentPs.close();
                }
                if (deleteInvoicePs != null) {
                    deleteInvoicePs.close();
                }
                if (deleteSubscriptionPs != null) {
                    deleteSubscriptionPs.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
    }

    /**
     * Get all the active subscriptions for a particular filter.
     * Customers and Items are just dummy objects that only have a valid id.
     *
     * @param filter the name of the filter (This is not used.Will be removed from
     *               the signature in the future trunk).
     * @return the array of the subscriptions.
     * @throws BillingException throws if there is an error.
     */
    public List<Subscription> getFilteredActiveSubscriptions(String filter) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Date lastInvoiceDate = getLastInvoiceDate();
        try {
            String sql = "";
            //if the last invoice date is null, that means there has been no bill generation yet.
            //so we have to consider all the subscriptions
            if(lastInvoiceDate==null){
                sql = "SELECT BC_ID, BC_IS_ACTIVE, BC_FILTER, BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_TENANT_ID FROM BC_SUBSCRIPTION";
                ps = conn.prepareStatement(sql);
            }else{
                //this means there has been a previous bill generation. Now we only consider
                //1. Active subscriptions
                //2. Inactive subscriptions which ended after the last invoice date
                sql = "SELECT BC_ID, BC_IS_ACTIVE, BC_FILTER, BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_TENANT_ID FROM BC_SUBSCRIPTION WHERE (BC_IS_ACTIVE=?) OR " +
                        "(BC_IS_ACTIVE=? AND BC_ACTIVE_UNTIL>=?)";
                ps = conn.prepareStatement(sql);
                ps.setInt(1,1);
                ps.setInt(2,0);
                ps.setTimestamp(3, new Timestamp(lastInvoiceDate.getTime()));
            }

            result = ps.executeQuery();

            while (result.next()) {
                Subscription subscription = new Subscription();
                subscription.setId(result.getInt("BC_ID"));
                subscription.setActive(result.getInt("BC_IS_ACTIVE")==1);
                subscription.setSubscriptionPlan(result.getString("BC_FILTER"));
                subscription.setActiveSince(new Date(
                        result.getTimestamp("BC_ACTIVE_SINCE").getTime()));
                subscription.setActiveUntil(
                        new Date(result.getTimestamp("BC_ACTIVE_UNTIL").getTime()));
                int itemId = result.getInt("BC_ITEM_ID");
                int customerId = result.getInt("BC_TENANT_ID");

                // filling with dummy item
                Item item = new Item();
                item.setId(itemId);
                subscription.setItem(item);

                // filling with dummy customer
                Customer customer = new Customer();
                customer.setId(customerId);
                subscription.setCustomer(customer);
                //subscription.setActive(true);
                subscriptions.add(subscription);
                // we will fill the payment details too
            }
        } catch (SQLException e) {
            String msg = "Failed to get the active subscriptions.";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscriptions;
    }

    public Invoice getLastInvoice(Customer customer) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        Invoice invoice = null;
        try {
            String sql = "SELECT BC_ID, BC_TENANT_ID, BC_DATE, BC_START_DATE, BC_END_DATE, "
                    + "BC_BOUGHT_FORWARD, BC_CARRIED_FORWARD, BC_TOTAL_COST, BC_TOTAL_PAYMENTS "
                    + "FROM BC_INVOICE WHERE BC_TENANT_ID=? AND BC_DATE=(SELECT MAX(BC_DATE) "
                    + "FROM BC_INVOICE WHERE BC_TENANT_ID=?) ";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customer.getId());
            ps.setInt(2, customer.getId());
            result = ps.executeQuery();

            if (result.next()) {
                invoice = new Invoice();
                invoice.setId(result.getInt("BC_ID"));
                invoice.setCustomer(customer);
                invoice.setDate(result.getTimestamp("BC_DATE"));
                invoice.setStartDate(result.getTimestamp("BC_START_DATE"));
                invoice.setEndDate(result.getTimestamp("BC_END_DATE"));
                String boughtForwardStr = result.getString("BC_BOUGHT_FORWARD");
                invoice.setBoughtForward(new Cash(boughtForwardStr));
                String carriedForwardStr = result.getString("BC_CARRIED_FORWARD");
                invoice.setCarriedForward(new Cash(carriedForwardStr));
                String totalCostStr = result.getString("BC_TOTAL_COST");
                invoice.setTotalCost(new Cash(totalCostStr));
                String totalPaymentsStr = result.getString("BC_TOTAL_PAYMENTS");
                invoice.setTotalPayment(new Cash(totalPaymentsStr));
            }
        } catch (SQLException e) {
            String msg = "Failed to get the invoice for customer: " + customer.getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoice;
    }

    public List<Invoice> getAllInvoices(Customer customer) throws BillingException {
        Connection conn = Transaction.getConnection();
        List<Invoice> invoices = new ArrayList<Invoice>();
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            String sql = "SELECT BC_ID, BC_TENANT_ID, BC_DATE, BC_START_DATE, BC_END_DATE, "
                    + "BC_BOUGHT_FORWARD, BC_CARRIED_FORWARD, BC_TOTAL_COST, BC_TOTAL_PAYMENTS "
                    + "FROM BC_INVOICE WHERE BC_TENANT_ID=? ";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customer.getId());
            result = ps.executeQuery();

            while (result.next()) {
                Invoice invoice = new Invoice();
                invoice.setId(result.getInt("BC_ID"));
                invoice.setCustomer(customer);
                invoice.setDate(result.getTimestamp("BC_DATE"));
                invoice.setStartDate(result.getTimestamp("BC_START_DATE"));
                invoice.setEndDate(result.getTimestamp("BC_END_DATE"));
                String boughtForwardStr = result.getString("BC_BOUGHT_FORWARD");
                invoice.setBoughtForward(new Cash(boughtForwardStr));
                String carriedForwardStr = result.getString("BC_CARRIED_FORWARD");
                invoice.setCarriedForward(new Cash(carriedForwardStr));
                String totalCostStr = result.getString("BC_TOTAL_COST");
                invoice.setTotalCost(new Cash(totalCostStr));
                String totalPaymentsStr = result.getString("BC_TOTAL_PAYMENTS");
                invoice.setTotalPayment(new Cash(totalPaymentsStr));

                invoices.add(invoice);
            }
        } catch (SQLException e) {
            String msg = "Failed to get invoices for customer: " + customer.getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoices;
    }

    public void fillUnbilledPayments(Subscription subscription,
                                     Map<Integer, Payment> payments,
                                     Invoice invoice) throws BillingException {

        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet results = null;
        java.sql.Timestamp startDate= new Timestamp(invoice.getStartDate().getTime());
        java.sql.Timestamp endDate= new Timestamp(invoice.getEndDate().getTime());
        try {
            /*String sql = "SELECT P.BC_ID, P.BC_DATE, P.BC_AMOUNT, P.BC_DESCRIPTION " +
                    " FROM BC_PAYMENT P, BC_PAYMENT_SUBSCRIPTION PS " +
                    "WHERE PS.BC_SUBSCRIPTION_ID=? AND PS.BC_PAYMENT_ID=P.BC_ID " +
                    "AND P.BC_INVOICE_ID IS NULL";
            */
            String sql = "SELECT P.BC_ID, P.BC_DATE, P.BC_AMOUNT, P.BC_DESCRIPTION " +
                    " FROM BC_PAYMENT P, BC_PAYMENT_SUBSCRIPTION PS " +
                    "WHERE PS.BC_SUBSCRIPTION_ID=? AND PS.BC_PAYMENT_ID=P.BC_ID " +
                    "AND P.BC_DATE>=? AND P.BC_DATE<?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, subscription.getId());
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
            ps.setTimestamp(2, startDate, cal);
            ps.setTimestamp(3, endDate, cal);
            results = ps.executeQuery();
            while (results.next()) {
                Payment payment;
                int paymentId = results.getInt("BC_ID");
                if (payments.get(paymentId) != null) {
                    payment = payments.get(paymentId);
                } else {
                    payment = new Payment();
                    payment.setId(paymentId);
                    payment.setDate(results.getTimestamp("BC_DATE"));
                    payment.setDescription(results.getString("BC_DESCRIPTION"));
                    String amount = results.getString("BC_AMOUNT");
                    if (amount == null) {
                        amount = "0";
                    }
                    Cash paymentCash = new Cash(amount);
                    payment.setAmount(paymentCash);
                    payments.put(paymentId, payment);
                }
                payment.addSubscription(subscription);
            }
        } catch (SQLException e) {
            String msg = "Failed to fill the payment for subscription: "
                    + subscription.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (results != null) {
                    results.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
    }

    public void associatePaymentWithInvoice(Payment payment,
                                            Invoice invoice) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE BC_PAYMENT SET BC_INVOICE_ID=? WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);

            // inserting the data values
            ps.setInt(1, invoice.getId());
            ps.setInt(2, payment.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            String msg = "Error in associating invoice: " + invoice.getId() +
                    " with payment: " + payment.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }
        }
    }

    public int addInvoiceSubscription(Invoice invoice,
                                      Subscription subscription) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int invoiceSubscriptionId = INVALID;
        try {
            String sql = "INSERT INTO BC_INVOICE_SUBSCRIPTION (BC_INVOICE_ID, BC_SUBSCRIPTION_ID) "
                    + "VALUES (?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the data values
            ps.setInt(1, invoice.getId());
            ps.setInt(2, subscription.getId());

            ps.executeUpdate();
            result = ps.getGeneratedKeys();
            if (result.next()) {
                invoiceSubscriptionId = result.getInt(1);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the invoice subscription, invoice id: " +
                    invoice.getId() + ", subscription id: " + subscription.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoiceSubscriptionId;
    }

    public int addInvoiceSubscriptionItem(Item item,
                                          int invoiceSubscriptionId) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int invoiceSubscriptionItemId = INVALID;
        try {
            String sql = "INSERT INTO BC_INVOICE_SUBSCRIPTION_ITEM (BC_INVOICE_SUBSCRIPTION_ID, " +
                    "BC_ITEM_ID, BC_COST, BC_DESCRIPTION) VALUES (?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the data values
            ps.setInt(1, invoiceSubscriptionId);
            ps.setInt(2, item.getId());
            if(item.getCost()!=null){
                ps.setString(3, item.getCost().serializeToString());
            }else{
                ps.setString(3, new Cash("$0").serializeToString());
            }
            ps.setString(4, item.getDescription());

            ps.executeUpdate();
            result = ps.getGeneratedKeys();
            if (result.next()) {
                invoiceSubscriptionItemId = result.getInt(1);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the invoice subscription item, item id: " +
                    item.getId() + ", invoice subscription id: " + invoiceSubscriptionId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoiceSubscriptionItemId;
    }


    public int addPayment(Payment payment) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int paymentId = INVALID;
        int invoiceId = INVALID;
        int customerId = INVALID;
        if (payment.getInvoice() != null) {
            invoiceId = payment.getInvoice().getId();
        }
        if(invoiceId!=INVALID){
            customerId = getCustomerIdFromInvoiceId(invoiceId);
        }
        try {
            String sql = "INSERT INTO BC_PAYMENT (BC_DATE, BC_AMOUNT, " +
                    "BC_DESCRIPTION, BC_INVOICE_ID, BC_TENANT_ID) " +
                    "VALUES (?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the data
            long paymentTime = System.currentTimeMillis();
            //there was a problem of time shown as 00:00:00 when getting the date sent
            //from frontend. Therefore, to get rid of it, I am creating the timestamp
            //by getting the current time. This may be a few seconds different from the
            //time sent from frontend
            /*if (payment.getDate() != null) {
                paymentTime = payment.getDate().getTime();
            }*/
            ps.setTimestamp(1, new Timestamp(paymentTime));
            ps.setString(2, payment.getAmount().serializeToString());
            ps.setString(3, payment.getDescription());
            if (invoiceId == INVALID) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, invoiceId);
            }
            if(customerId == INVALID){
                ps.setNull(5, Types.INTEGER);
            }else{
                ps.setInt(5, customerId);
            }

            ps.executeUpdate();

            result = ps.getGeneratedKeys();
            if (result.next()) {
                paymentId = result.getInt(1);
                payment.setId(paymentId);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the payment, payment id: " + paymentId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        // adn then insert the subscriptions
        addPaymentSubscriptionsEntry(payment);
        return paymentId;
    }

    public void addPaymentSubscriptionsEntry(Payment payment) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        List<Subscription> subscriptions = payment.getSubscriptions();
        if (subscriptions == null) {
            return;
        }
        try {
            for (Subscription subscription : subscriptions) {
                String sql = "INSERT INTO BC_PAYMENT_SUBSCRIPTION ( " +
                        "BC_PAYMENT_ID, BC_SUBSCRIPTION_ID) " +
                        "VALUES (?, ?)";
                ps = conn.prepareStatement(sql);

                // inserting the data
                ps.setInt(1, payment.getId());
                ps.setInt(2, subscription.getId());

                ps.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the payment subscriptions, " +
                    "payment id: " + payment.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }
        }
    }

    public int addRegistrationPayment(Payment payment, String usagePlan) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int paymentId = INVALID;
        String tenantDomain = payment.getDescription().split(" ")[0];
        try {
            int tenantId = Util.getRealmService().getTenantManager().getTenantId(tenantDomain);
            String sql = "INSERT INTO BC_REGISTRATION_PAYMENT (BC_DATE, BC_AMOUNT, " +
                         "BC_DESCRIPTION, BC_USAGE_PLAN, BC_TENANT_ID) " +
                         "VALUES (?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, new String[]{"BC_ID"});

            // inserting the data
            long paymentTime = System.currentTimeMillis();
            ps.setTimestamp(1, new Timestamp(paymentTime));
            ps.setString(2, payment.getAmount().serializeToString());
            ps.setString(3, payment.getDescription().split(" ")[1]);
            ps.setString(4, usagePlan);
            ps.setInt(5, tenantId);
            ps.executeUpdate();
            result = ps.getGeneratedKeys();
            if (result.next()) {
                paymentId = result.getInt(1);
                payment.setId(paymentId);
            }
        } catch (SQLException e) {
            String msg = "Failed to insert the registration payment record, id: " + paymentId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } catch (UserStoreException e) {
            String msg = "Failed to get tenant id of registration payment for: " + tenantDomain + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return paymentId;
    }

    public List<Subscription> getFilteredActiveSubscriptionsForCustomer(String filter, Customer customer)
            throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Date lastInvoiceDate = getLastInvoiceDate();
        try {
            String sql="";
            if(lastInvoiceDate==null){
                sql = "SELECT BC_ID, BC_IS_ACTIVE, BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_TENANT_ID FROM BC_SUBSCRIPTION WHERE " +
                    "BC_TENANT_ID=? ORDER BY BC_ACTIVE_UNTIL DESC"; //not sure whether this
                                                                                           //ORDER BY is necessary
                ps = conn.prepareStatement(sql);
                ps.setInt(1, customer.getId());
            }else{
                sql = "SELECT BC_ID, BC_IS_ACTIVE, BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_TENANT_ID FROM BC_SUBSCRIPTION WHERE BC_TENANT_ID=? AND " +
                        "((BC_IS_ACTIVE=? ) OR (BC_IS_ACTIVE=? AND BC_ACTIVE_UNTIL>=?)) ORDER BY BC_ACTIVE_UNTIL DESC";

                ps = conn.prepareStatement(sql);
                ps.setInt(1, customer.getId());
                ps.setInt(2, 1);             //active=true
                ps.setInt(3, 0);             //active=false
                ps.setTimestamp(4, new Timestamp(lastInvoiceDate.getTime()));
            }

            result = ps.executeQuery();

            while (result.next()) {
                Subscription subscription = new Subscription();
                subscription.setId(result.getInt("BC_ID"));
                subscription.setActive(result.getInt("BC_IS_ACTIVE")==1);
                subscription.setActiveSince(
                        new Date(result.getTimestamp("BC_ACTIVE_SINCE").getTime()));
                subscription.setActiveUntil(
                        new Date(result.getTimestamp("BC_ACTIVE_UNTIL").getTime()));
                int itemId = result.getInt("BC_ITEM_ID");
                // filling with dummy item
                Item item = new Item();
                item.setId(itemId);
                subscription.setItem(item);
                // filling with dummy customer
                subscription.setCustomer(customer);
                //subscription.setActive(true);
                subscriptions.add(subscription);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the active subscriptions for " +
                    "customer = " + customer.getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscriptions;
    }

    /**
     * this doesn't load the subscriptions
     *
     * @param customer
     * @return
     * @throws BillingException
     */
    public List<Invoice> getInvoices(Customer customer) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Invoice> invoices = new ArrayList<Invoice>();
        try {
            String sql = "SELECT BC_ID, BC_DATE, BC_START_DATE, BC_END_DATE, BC_BOUGHT_FORWARD, "
                    + "BC_CARRIED_FORWARD, BC_TOTAL_PAYMENTS, BC_TOTAL_COST FROM BC_INVOICE "
                    + "WHERE BC_TENANT_ID=? ORDER BY BC_DATE DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customer.getId());
            result = ps.executeQuery();

            while (result.next()) {
                Invoice invoice = new Invoice();
                invoice.setId(result.getInt("BC_ID"));
                invoice.setDate(new Date(result.getTimestamp("BC_DATE").getTime()));
                invoice.setStartDate(new Date(result.getTimestamp("BC_START_DATE").getTime()));
                invoice.setEndDate(new Date(result.getTimestamp("BC_END_DATE").getTime()));
                invoice.setCustomer(customer);

                String bfStr = result.getString("BC_BOUGHT_FORWARD");
                if (bfStr == null) {
                    bfStr = "$0";
                }
                invoice.setBoughtForward(new Cash(bfStr));

                String cfStr = result.getString("BC_CARRIED_FORWARD");
                if (cfStr == null) {
                    cfStr = "$0";
                }
                invoice.setCarriedForward(new Cash(cfStr));

                String totalPayStr = result.getString("BC_TOTAL_PAYMENTS");
                if (totalPayStr == null) {
                    totalPayStr = "$0";
                }
                invoice.setTotalPayment(new Cash(totalPayStr));

                String totalCostStr = result.getString("BC_TOTAL_COST");
                if (totalCostStr == null) {
                    totalCostStr = "$0";
                }
                invoice.setTotalCost(new Cash(totalCostStr));
                invoices.add(invoice);
            }
        } catch (SQLException e) {
            String msg =
                    "Failed to get the invoice for: " + "customer = " + customer.getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg =
                        RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR +
                                ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoices;
    }

    /**
     * this load the subscriptions and payments associated
     *
     * @param invoiceId
     * @return
     * @throws BillingException
     */
    public Invoice getInvoice(int invoiceId) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        Invoice invoice = null;
        try {
            String sql = "SELECT BC_TENANT_ID, BC_DATE, BC_START_DATE, BC_END_DATE, "
                    + "BC_BOUGHT_FORWARD, BC_CARRIED_FORWARD, BC_TOTAL_PAYMENTS, BC_TOTAL_COST "
                    + "FROM BC_INVOICE WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, invoiceId);
            result = ps.executeQuery();

            if (result.next()) {
                invoice = new Invoice();

                invoice.setEndDate(new Date(result.getTimestamp("BC_END_DATE").getTime()));
                invoice.setId(invoiceId);
                invoice.setStartDate(new Date(result.getTimestamp("BC_START_DATE").getTime()));
                invoice.setDate(new Date(result.getTimestamp("BC_DATE").getTime()));

                String bfStr = result.getString("BC_BOUGHT_FORWARD");
                if (bfStr == null) {
                    bfStr = "$0";
                }
                invoice.setBoughtForward(new Cash(bfStr));

                String cfStr = result.getString("BC_CARRIED_FORWARD");
                if (cfStr == null) {
                    cfStr = "$0";
                }
                invoice.setCarriedForward(new Cash(cfStr));

                String totalPayStr = result.getString("BC_TOTAL_PAYMENTS");
                if (totalPayStr == null) {
                    totalPayStr = "$0";
                }
                invoice.setTotalPayment(new Cash(totalPayStr));

                String totalCostStr = result.getString("BC_TOTAL_COST");
                if (totalCostStr == null) {
                    totalCostStr = "$0";
                }
                invoice.setTotalCost(new Cash(totalCostStr));

                int customerId = result.getInt("BC_TENANT_ID");
                //Customer customer = getCustomer(customerId);
                Customer customer = CustomerUtils.getCustomer(customerId);
                invoice.setCustomer(customer);

                fillInvoiceSubscriptions(invoice);
                fillInvoicePayments(invoice);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the invoice for: " + "invoice id = " + invoiceId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR +
                        ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return invoice;
    }

    private void fillInvoicePayments(Invoice invoice) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int customerId = INVALID;
        //int invoiceId = invoice.getId();
        if(invoice.getCustomer()==null){
            return;
        }else{
            customerId = invoice.getCustomer().getId();
        }
        java.sql.Timestamp startDate = new Timestamp(invoice.getStartDate().getTime());
        java.sql.Timestamp endDate = new Timestamp(invoice.getEndDate().getTime());
        List<Payment> payments = new ArrayList<Payment>();

        try {
            /*String sql = "SELECT BC_ID, BC_DATE, BC_AMOUNT, BC_DESCRIPTION "
                    + "FROM BC_PAYMENT WHERE BC_INVOICE_ID=?";
            */
            String sql = "SELECT BC_ID, BC_DATE, BC_AMOUNT, BC_DESCRIPTION "
                    + "FROM BC_PAYMENT WHERE BC_TENANT_ID=? AND BC_DATE>=? AND BC_DATE<?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customerId);
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
            ps.setTimestamp(2, startDate, cal);
            ps.setTimestamp(3, endDate, cal);
            result = ps.executeQuery();

            while (result.next()) {
                Payment payment = new Payment();
                payment.setId(result.getInt("BC_ID"));
                String cashPayment = result.getString("BC_AMOUNT");
                payment.setAmount(new Cash(cashPayment));
                payment.setDate(result.getTimestamp("BC_DATE"));
                payment.setDescription(result.getString("BC_DESCRIPTION"));

                payments.add(payment);
            }
        } catch (SQLException e) {
            String msg =
                    "Failed to get the invoice payments for: " + "invoice id = " + invoice.getId() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        invoice.setPayments(payments);
    }

    private void fillInvoiceSubscriptions(Invoice invoice) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int invoiceId = invoice.getId();
        List<Subscription> subscriptions = new ArrayList<Subscription>();

        try {
            String sql = "SELECT BC_ID, BC_SUBSCRIPTION_ID "
                    + "FROM BC_INVOICE_SUBSCRIPTION WHERE BC_INVOICE_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, invoiceId);
            result = ps.executeQuery();

            while (result.next()) {
                int invoiceSubscriptionId = result.getInt("BC_ID");
                int subscriptionId = result.getInt("BC_SUBSCRIPTION_ID");
                Subscription subscription = getSubscription(subscriptionId);
                subscription.setInvoiceSubscriptionId(invoiceSubscriptionId);
                subscriptions.add(subscription);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the invoice subscriptions for: " + "invoice id = "
                    + invoiceId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR +
                        ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        invoice.setSubscriptions(subscriptions);
    }

    public Subscription getSubscription(int subscriptionId)
            throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        Subscription subscription = null;
        try {
            String sql = "SELECT BC_ID, BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_TENANT_ID, BC_IS_ACTIVE, BC_FILTER FROM BC_SUBSCRIPTION WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, subscriptionId);
            result = ps.executeQuery();

            if (result.next()) {
                subscription = new Subscription();
                subscription.setId(subscriptionId);
                subscription.setActiveSince(
                        new Date(result.getTimestamp("BC_ACTIVE_SINCE").getTime()));
                subscription.setActiveUntil(
                        new Date(result.getTimestamp("BC_ACTIVE_UNTIL").getTime()));
                int itemId = result.getInt("BC_ITEM_ID");
                int customerId = result.getInt("BC_TENANT_ID");
                // filling with dummy item
                Item item = getItem(itemId);
                subscription.setItem(item);
                // filling with dummy customer
                //Customer customer = getCustomer(customerId);
                Customer customer = CustomerUtils.getCustomer(customerId);
                subscription.setCustomer(customer);
                int isActive = result.getInt("BC_IS_ACTIVE");
                subscription.setActive(isActive == 1);
                //subscription plan name
                subscription.setSubscriptionPlan(result.getString("BC_FILTER"));
            }
        } catch (SQLException e) {
            String msg = "Failed to get the active subscription for id: " + subscriptionId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscription;
    }

        public Subscription getActiveSubscriptionOfCustomer(int customerId)
            throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        Subscription subscription = null;
        try {
            String sql = "SELECT BC_ID, BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_TENANT_ID, BC_IS_ACTIVE, BC_FILTER FROM BC_SUBSCRIPTION WHERE BC_TENANT_ID=? AND BC_IS_ACTIVE=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customerId);
            ps.setInt(2,1);
            result = ps.executeQuery();

            if (result.next()) {
                subscription = new Subscription();
                subscription.setId(result.getInt("BC_ID"));
                subscription.setActiveSince(
                        new Date(result.getTimestamp("BC_ACTIVE_SINCE").getTime()));
                subscription.setActiveUntil(
                        new Date(result.getTimestamp("BC_ACTIVE_UNTIL").getTime()));
                int itemId = result.getInt("BC_ITEM_ID");
                // filling with dummy item
                Item item = getItem(itemId);
                subscription.setItem(item);
                // filling with dummy customer
                //Customer customer = getCustomer(customerId);
                Customer customer = CustomerUtils.getCustomer(customerId);
                subscription.setCustomer(customer);
                int isActive = result.getInt("BC_IS_ACTIVE");
                subscription.setActive(isActive == 1);
                //subscription plan name
                subscription.setSubscriptionPlan(result.getString("BC_FILTER"));
            }
        } catch (SQLException e) {
            String msg = "Failed to get the active subscription for customer id: " + customerId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscription;
    }

    public List<Item> getBilledItems(Subscription subscription) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;

        int invoiceSubscriptionId = subscription.getInvoiceSubscriptionId();
        if (invoiceSubscriptionId == INVALID) {
            String msg = "Not a invoiced subscription, subscription id: "
                    + subscription.getId() + ".";
            log.error(msg);
            throw new BillingException(msg);
        }

        List<Item> items = new ArrayList<Item>();
        try {
            String sql = "SELECT BC_ITEM_ID, BC_COST, BC_DESCRIPTION " +
                    "FROM BC_INVOICE_SUBSCRIPTION_ITEM WHERE BC_INVOICE_SUBSCRIPTION_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, invoiceSubscriptionId);
            result = ps.executeQuery();

            while (result.next()) {
                Item item = getItem(result.getInt("BC_ITEM_ID"));
                String cost = result.getString("BC_COST");
                String description = result.getString("BC_DESCRIPTION");
                if (cost != null) {
                    item.setCost(new Cash(cost));
                }
                if(description!=null && !"".equals(description)){
                    item.setDescription(description);
                }
                items.add(item);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the invoiced subscription items for " +
                    "invoice subscription id: " + invoiceSubscriptionId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return items;
    }

    public List<Subscription> getInvoiceSubscriptions(int invoiceId) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Subscription> subscriptions = new ArrayList<Subscription>();

        try {
            String sql = "SELECT S.BC_ID, S.BC_FILTER, S.BC_IS_ACTIVE, S.BC_ACTIVE_SINCE, S.BC_ACTIVE_UNTIL, S.BC_ITEM_ID, S.BC_TENANT_ID FROM " +
                        "BC_SUBSCRIPTION S, BC_INVOICE_SUBSCRIPTION BIS " +
                        "WHERE S.BC_ID=BIS.BC_SUBSCRIPTION_ID AND BIS.BC_INVOICE_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, invoiceId);
            result = ps.executeQuery();

            while (result.next()) {
                Subscription subscription = new Subscription();
                subscription.setId(result.getInt("BC_ID"));
                subscription.setSubscriptionPlan(result.getString("BC_FILTER"));
                subscription.setActiveSince(new Date(
                        result.getTimestamp("BC_ACTIVE_SINCE").getTime()));
                subscription.setActiveUntil(
                        new Date(result.getTimestamp("BC_ACTIVE_UNTIL").getTime()));
                int itemId = result.getInt("BC_ITEM_ID");
                int customerId = result.getInt("BC_TENANT_ID");
                boolean isActive = result.getBoolean("BC_IS_ACTIVE");

                // filling with dummy item
                Item item = new Item();
                item.setId(itemId);
                subscription.setItem(item);

                // filling with dummy customer
                Customer customer = new Customer();
                customer.setId(customerId);
                subscription.setCustomer(customer);
                subscription.setActive(isActive);
                subscriptions.add(subscription);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the invoice subscriptions for invoice: " + invoiceId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscriptions;
    }

    private int getCustomerIdFromInvoiceId(int invoiceId) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        int customerId = INVALID;

        try {
            String sql = "SELECT BC_TENANT_ID FROM BC_INVOICE WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, invoiceId);
            result = ps.executeQuery();

            while (result.next()) {
                customerId = result.getInt("BC_TENANT_ID");
            }
        } catch (SQLException e) {
            String msg = "Failed to get the customer Id for invoice: " + invoiceId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return customerId;
    }

    public Payment getLastPayment(Customer customer) throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        Payment payment = null;
        try {
            String sql = "SELECT BC_ID, BC_DATE, BC_AMOUNT, BC_DESCRIPTION FROM BC_PAYMENT WHERE BC_TENANT_ID=?" +
                        " AND BC_DATE=(SELECT MAX(BC_DATE) FROM BC_PAYMENT WHERE BC_TENANT_ID=?) ";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customer.getId());
            ps.setInt(2, customer.getId());
            result = ps.executeQuery();

            if (result.next()) {
                payment = new Payment();
                payment.setAmount(new Cash(result.getString("BC_AMOUNT")));
                payment.setDate(new Date(result.getTimestamp("BC_DATE").getTime()));
                payment.setDescription(result.getString("BC_DESCRIPTION"));
            }
        } catch (SQLException e) {
            String msg = "Failed to get the last payment for customer: " + customer.getName() + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return payment;
    }

    public Date getLastInvoiceDate() throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        Date maxDate = null;
        try {
            String sql = "SELECT MAX(BC_DATE) FROM BC_INVOICE";
            ps = conn.prepareStatement(sql);
            result = ps.executeQuery();

            if (result.next()) {
                Timestamp lastInvoiceTimestamp = result.getTimestamp("MAX(BC_DATE)");
                if(lastInvoiceTimestamp!=null){
                    maxDate = new Date(lastInvoiceTimestamp.getTime());
                }
            }
        } catch (SQLException e) {
            String msg = "Failed to get the last invoice date";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return maxDate;
    }

 public boolean deactivateCurrentSubscriptoin(int customerID) throws BillingException{
        /*Connection conn = null;
        try {
            conn = DataSourceHolder.getDataSource().getConnection();
        } catch (SQLException e) {
            String msg = "Failed to establish data connection";
            log.error(msg, e);
        }*/
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE BC_SUBSCRIPTION SET BC_IS_ACTIVE=? ,BC_ACTIVE_UNTIL=?, BC_ACTIVE_SINCE=?" +
                    " WHERE BC_TENANT_ID=? AND BC_IS_ACTIVE=? ";
            ps = conn.prepareStatement(sql);
            Subscription subscription = getActiveSubscriptionOfCustomer(customerID);
            ps.setInt(1, 0);
            long now = System.currentTimeMillis();
            ps.setTimestamp(2, new Timestamp(now));
            ps.setTimestamp(3, new Timestamp(subscription.getActiveSince().getTime()));
            ps.setInt(4, customerID);
            ps.setInt(5, subscription.isActive() ? 1 : 0);
            int updated = ps.executeUpdate();
            if(updated>0){
                return true;
            }else{
                return false;
            }
        } catch (SQLException e) {
            String msg = "Error in deactivating the subscription for tenant : " + customerID + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        }catch (Exception e) {
            String msg = "Error in deactivating the subscription for tenant : " + customerID + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            }
            catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
            /*try{
                if(conn !=null){
                    conn.close();
                }
            }
            catch (SQLException e){
                String msg = "Error while closing connection";
                log.error(msg, e);
            }*/
        }
    }


    public boolean changeSubscription(int customerID, String newPlanName) throws BillingException {
        boolean isChanged = false;
        try {
            Subscription subscription = getActiveSubscriptionOfCustomer(customerID);
            if (subscription != null) {
                Item item = getItemsWithName(newPlanName).get(0);
                subscription.setItem(item);
                Calendar activeSinceDate = Calendar.getInstance();
                subscription.setActiveSince(activeSinceDate.getTime());
                subscription.setActiveUntil(null);
                subscription.setActive(true);
                boolean deactivatedCurrent = deactivateCurrentSubscriptoin(customerID);
                if (deactivatedCurrent) {
                    int newSubscriptionId = addSubscription(subscription, newPlanName);                   
                    if (newSubscriptionId > 0) {
                        isChanged = true;
                    } 
                }
            } else {
                Customer currentCustomer = new Customer();
                currentCustomer.setId(customerID);
                Subscription currentSubscription = new Subscription();
                currentSubscription.setCustomer(currentCustomer);
                currentSubscription.setActive(true);
                long now = System.currentTimeMillis();
                currentSubscription.setActiveSince(new Date(now));
                addSubscription(currentSubscription, newPlanName);
                isChanged = true;
            }
        } catch (Exception e) {
            String msg = "Error in updating the subscription for tenant : " + customerID + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }
        
        
        return isChanged;
    }

    public List<Subscription> getInactiveSubscriptionsOfCustomer(int customerID)
            throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        try {
            String sql = "SELECT BC_ACTIVE_SINCE, BC_ACTIVE_UNTIL, BC_ITEM_ID, " +
                    "BC_ID, BC_IS_ACTIVE, BC_FILTER FROM BC_SUBSCRIPTION WHERE BC_TENANT_ID=? AND BC_IS_ACTIVE=? " +
                    "ORDER BY BC_ACTIVE_SINCE DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, customerID);
            ps.setInt(2, 0);
            result = ps.executeQuery();

            while (result.next()) {
                Subscription subscription = new Subscription();
                subscription.setActiveSince(
                        new Date(result.getTimestamp("BC_ACTIVE_SINCE").getTime()));
                subscription.setActiveUntil(
                        new Date(result.getTimestamp("BC_ACTIVE_UNTIL").getTime()));
                int itemId = result.getInt("BC_ITEM_ID");
                int bcID = result.getInt("BC_ID");
                subscription.setId(bcID);
                // filling with dummy item
                Item item = getItem(itemId);
                subscription.setItem(item);
                // filling with dummy customer
                //Customer customer = getCustomer(customerID);
                Customer customer = CustomerUtils.getCustomer(customerID);
                subscription.setCustomer(customer);
                int isActive = result.getInt("BC_IS_ACTIVE");
                subscription.setActive(isActive == 1);
                subscription.setSubscriptionPlan(result.getString("BC_FILTER"));
                subscriptions.add(subscription);
            }
        } catch (SQLException e) {
            String msg = "Failed to get the inactive subscriptions for customer id: " + customerID + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return subscriptions;
    }

    public boolean activateSubscription(int subscriptionId) throws BillingException{
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        int updated = 0;
        try {
            String sql = "UPDATE BC_SUBSCRIPTION SET BC_IS_ACTIVE=? ,BC_ACTIVE_UNTIL=?, BC_ACTIVE_SINCE=?" +
                    " WHERE BC_ID=?";
            ps = conn.prepareStatement(sql);
            Subscription subscription = getSubscription(subscriptionId);
            ps.setInt(1, 1);
            ps.setTimestamp(2, new Timestamp(subscription.getActiveUntil().getTime()));
            //ps.setTimestamp(3, new Timestamp(subscription.getActiveSince().getTime()));

            //setting the active since time to current time
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setInt(4, subscription.getId());

            updated = ps.executeUpdate();
        } catch (SQLException e) {
            String msg = "Error in activating the subscription id : " + subscriptionId + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);

        }finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            }
            catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }

        if(updated>0){
            return true;
        }else{
            return false;
        }
    }

    public boolean addDiscount(Discount discount) throws BillingException{
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        int added = 0;
        try {
            String sql = "INSERT INTO BC_DISCOUNT (BC_TENANT_ID, BC_PERCENTAGE, BC_AMOUNT, BC_START_DATE, BC_END_DATE, BC_PERCENTAGE_TYPE)" +
                        " VALUES (?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);

            ps.setInt(1, discount.getTenantId());
            if(discount.isPercentageType()){
                ps.setInt(6, 1);
                ps.setFloat(2, discount.getPercentage());
                ps.setNull(3, Types.FLOAT);
            }else{
                ps.setInt(6, 0);
                ps.setNull(2, Types.FLOAT);
                ps.setFloat(3, discount.getAmount());
            }
            ps.setTimestamp(4, new Timestamp(discount.getStartDate().getTime()));
            if(discount.getEndDate()!=null){
                ps.setTimestamp(5, new Timestamp(discount.getEndDate().getTime()));
            }else{
                ps.setNull(5, Types.TIMESTAMP);
            }

            added = ps.executeUpdate();
        } catch (SQLException e) {
            String msg = "Error in adding the discount for tenant: ";
            log.error(msg, e);
            throw new BillingException(msg, e);

        }finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            }
            catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }

        if(added>0){
            return true;
        }else{
            return false;
        }
    }


    public List<Discount> getAllActiveDiscounts() throws BillingException {
        Connection conn = Transaction.getConnection();
        PreparedStatement ps = null;
        ResultSet result = null;
        List<Discount> discounts = new ArrayList<Discount>();
        try {
            String sql = "SELECT BC_ID, BC_TENANT_ID, BC_PERCENTAGE, BC_AMOUNT, BC_START_DATE, BC_END_DATE, " +
                        "BC_PERCENTAGE_TYPE FROM BC_DISCOUNT";
            ps = conn.prepareStatement(sql);
            result = ps.executeQuery();

            while (result.next()) {
                Discount discount = new Discount();
                discount.setId(result.getInt("BC_ID"));
                discount.setTenantId(result.getInt("BC_TENANT_ID"));
                int isPercentageType = result.getInt("BC_PERCENTAGE_TYPE");
                if(isPercentageType==1){
                    discount.setPercentageType(true);
                    discount.setPercentage(result.getFloat("BC_PERCENTAGE"));
                }else{
                    discount.setPercentageType(false);
                    discount.setAmount(result.getFloat("BC_AMOUNT"));
                }
                discount.setStartDate(new java.util.Date(result.getTimestamp("BC_START_DATE").getTime()));

                if(result.getTimestamp("BC_END_DATE")!=null){
                    discount.setEndDate(new java.util.Date(result.getTimestamp("BC_END_DATE").getTime()));
                }
                //we are only returning non expired discounts
                if(discount.getEndDate()==null || discount.getEndDate().after(new java.util.Date())){
                    discounts.add(discount);
                }
            }
        } catch (SQLException e) {
            String msg = "Failed to get the list of discounts.";
            log.error(msg, e);
            throw new BillingException(msg, e);

        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR
                        + ex.getMessage();
                log.error(msg, ex);
                throw new BillingException(msg, ex);
            }

            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }
        return discounts;
    }

}
