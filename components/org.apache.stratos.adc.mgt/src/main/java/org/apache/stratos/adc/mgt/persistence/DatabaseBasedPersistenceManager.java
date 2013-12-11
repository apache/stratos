///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.apache.stratos.adc.mgt.persistence;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
//import org.apache.stratos.adc.mgt.dao.Cluster;
//import org.apache.stratos.adc.mgt.dao.DataCartridge;
//import org.apache.stratos.adc.mgt.exception.ADCException;
//import org.apache.stratos.adc.mgt.exception.PersistenceManagerException;
//import org.apache.stratos.adc.mgt.repository.Repository;
//import org.apache.stratos.adc.mgt.subscriber.Subscriber;
//import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
//import org.apache.stratos.adc.mgt.subscription.DataCartridgeSubscription;
//import org.apache.stratos.adc.mgt.subscription.factory.CartridgeSubscriptionFactory;
//import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
//import org.apache.stratos.adc.mgt.utils.RepoPasswordMgtUtil;
//import org.apache.stratos.adc.mgt.utils.StratosDBUtils;
//import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
//import org.wso2.carbon.context.CarbonContext;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
////import org.apache.stratos.adc.mgt.subscription.SingleTenantCartridgeSubscription;
//
//public class DatabaseBasedPersistenceManager extends PersistenceManager {
//
//    private static final Log log = LogFactory.getLog(DatabaseBasedPersistenceManager.class);
//
//    @Override
//    public void persistCartridgeSubscription(CartridgeSubscription cartridgeSubscription)
//            throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//             connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        int repositoryId = -1;
//        //persist Repository if available
//        if(cartridgeSubscription.getRepository() != null) {
//            repositoryId = persistRepository(connection, cartridgeSubscription.getRepository());
//        }
//
//        int dataCartridgeInfoId = -1;
//        //persist Data Cartridge Subscription specific details if available
//        if(cartridgeSubscription.getCartridgeInfo().getProvider().equals(CartridgeConstants.DATA_CARTRIDGE_PROVIDER) &&
//                cartridgeSubscription instanceof DataCartridgeSubscription) {
//            DataCartridgeSubscription dataCartridgeSubscription = (DataCartridgeSubscription) cartridgeSubscription;
//            dataCartridgeInfoId = persistDataCartridgeInformation(connection, dataCartridgeSubscription.getHost(),
//                    dataCartridgeSubscription.getUsername(), dataCartridgeSubscription.getPassword());
//        }
//
//        PreparedStatement persistSubscriptionStmt = null;
//
//        String insertSubscription = "INSERT INTO SUBSCRIPTION (CARTRIDGE_TYPE,CARTRIDGE_ALIAS,MAPPED_DOMAIN," +
//                "SUBSCRIPTION_STATUS,MULTITENANT,PROVIDER,AUTOSCALING_POLICY,HOSTNAME,DOMAIN,SUBDOMAIN,MGT_DOMAIN," +
//                "MGT_SUBDOMAIN,SERVICE_STATUS,DATA_CARTRIDGE_ID,REPOSITORY_ID)"
//                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//
//        try {
//            persistSubscriptionStmt = connection.prepareStatement(insertSubscription);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for persisting Subscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriptionStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistSubscriptionStmt.setString(1, cartridgeSubscription.getType());
//            persistSubscriptionStmt.setString(2, cartridgeSubscription.getAlias());
//            persistSubscriptionStmt.setString(3, cartridgeSubscription.getMappedDomain());
//            persistSubscriptionStmt.setString(4, cartridgeSubscription.getSubscriptionStatus());
//            persistSubscriptionStmt.setBoolean(5, cartridgeSubscription.getCartridgeInfo().getMultiTenant());
//            persistSubscriptionStmt.setString(6, cartridgeSubscription.getCartridgeInfo().getProvider());
//            persistSubscriptionStmt.setString(7, cartridgeSubscription.getAutoscalingPolicyName());
//            persistSubscriptionStmt.setString(8, cartridgeSubscription.getHostName());
//            persistSubscriptionStmt.setString(9, cartridgeSubscription.getClusterDomain());
//            persistSubscriptionStmt.setString(10, cartridgeSubscription.getClusterSubDomain());
//            persistSubscriptionStmt.setString(11, cartridgeSubscription.getMgtClusterDomain());
//            persistSubscriptionStmt.setString(12, cartridgeSubscription.getMgtClusterSubDomain());
//            persistSubscriptionStmt.setString(13, "CLUSTER_CREATED");//TODO: fix properly
//            persistSubscriptionStmt.setInt(14, dataCartridgeInfoId);
//            persistSubscriptionStmt.setInt(15, repositoryId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for persisting Subscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriptionStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistSubscriptionStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for persisting Subscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriptionStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            connection.commit();
//
//        } catch (SQLException e) {
//            try {
//                connection.rollback();
//
//            } catch (SQLException e1) {
//                log.error("Failed to rollback", e);
//            }
//            String errorMsg = "Failed to commit the changes for persisting Subscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriptionStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, persistSubscriptionStmt);
//        }
//    }
//
//    private int persistRepository (Connection connection, Repository repository) throws PersistenceManagerException{
//
//        PreparedStatement persistRepoStmt = null;
//        ResultSet resultSet = null;
//        int repoId = -1;
//
//        String insertRepo = "INSERT INTO REPOSITORY (URL,USERNAME,PASSWORD,IS_PRIVATE)"
//                + " VALUES (?,?,?,?)";
//
//        try {
//            persistRepoStmt = connection.prepareStatement(insertRepo, Statement.RETURN_GENERATED_KEYS);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for persisting Repository";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistRepoStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistRepoStmt.setString(1, repository.getUrl());
//            persistRepoStmt.setString(2, repository.getUserName());
//            persistRepoStmt.setString(3, RepoPasswordMgtUtil.encryptPassword(repository.getPassword()));
//            persistRepoStmt.setBoolean(4, repository.isPrivateRepository());
//
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for persisting Repository";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistRepoStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistRepoStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for persisting Repository";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistRepoStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//        try {
//            resultSet = persistRepoStmt.getGeneratedKeys();
//            if (resultSet.next()) {
//                repoId = resultSet.getInt(1);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to get the generated keys for the Result Set of persisting Repository";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistRepoStmt, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeStatement(persistRepoStmt);
//            StratosDBUtils.closeResultSet(resultSet);
//        }
//
//        return repoId;
//    }
//
//    /*private int persistCluster (Connection connection, Cluster cluster) throws PersistenceManagerException {
//
//        PreparedStatement persistClusterStmt = null;
//        ResultSet resultSet = null;
//        int clusterId = -1;
//
//        String insertCluster = "INSERT INTO CLUSTER (HOSTNAME,DOMAIN,SUBDOMAIN,MGT_DOMAIN,MGT_SUBDOMAIN,SERVICE_STATUS)"
//                + " VALUES (?,?,?,?,?,?,?)";
//
//        try {
//            persistClusterStmt = connection.prepareStatement(insertCluster, Statement.RETURN_GENERATED_KEYS);
//
//        } catch (SQLException e) {
//            StratosDBUtils.closeAllConnections(connection, persistClusterStmt);
//            throw new PersistenceManagerException("Failed create a Prepared Statement for persisting Cluster", e);
//        }
//
//        try {
//            persistClusterStmt.setString(1, cluster.getHostName());
//            persistClusterStmt.setString(2, cluster.getClusterDomain());
//            persistClusterStmt.setString(3, cluster.getClusterSubDomain());
//            persistClusterStmt.setString(4, cluster.getMgtClusterDomain());
//            persistClusterStmt.setString(5, cluster.getMgtClusterSubDomain());
//            persistClusterStmt.setString(6, "CREATED");//TODO:refactor
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for persisting Cluster";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistClusterStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistClusterStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for persisting Cluster";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistClusterStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//        try {
//            resultSet = persistClusterStmt.getGeneratedKeys();
//            if (resultSet.next()) {
//                clusterId = resultSet.getInt(1);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to get the generated keys for the Result Set of persisting Repository";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistClusterStmt, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeStatement(persistClusterStmt);
//            StratosDBUtils.closeResultSet(resultSet);
//        }
//
//        return clusterId;
//    } */
//
//    private int persistDataCartridgeInformation (Connection connection, String host, String adminUserName,
//                                                 String adminPassword) throws PersistenceManagerException {
//
//        PreparedStatement persistDataCartridgeInformationStmt = null;
//        ResultSet resultSet = null;
//        int dataCartridgeInfoId = -1;
//
//        String insertDataCartridgeInfo = "INSERT INTO DATA_CARTRIDGE (HOST,ADMIN_USERNAME,ADMIN_PASSWORD)"
//                + " VALUES (?,?,?)";
//
//        try {
//            persistDataCartridgeInformationStmt = connection.prepareStatement(insertDataCartridgeInfo,
//                    Statement.RETURN_GENERATED_KEYS);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for persisting Data Cartridge Information";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistDataCartridgeInformationStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistDataCartridgeInformationStmt.setString(1, host);
//            persistDataCartridgeInformationStmt.setString(2, adminUserName);
//            persistDataCartridgeInformationStmt.setString(3, RepoPasswordMgtUtil.encryptPassword(adminPassword));
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for persisting Data Cartridge Information";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistDataCartridgeInformationStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistDataCartridgeInformationStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for persisting Data Cartridge Information";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistDataCartridgeInformationStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//        try {
//            resultSet = persistDataCartridgeInformationStmt.getGeneratedKeys();
//            if (resultSet.next()) {
//                dataCartridgeInfoId = resultSet.getInt(1);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to get the generated keys for the Result Set of persisting Data Cartridge " +
//                    "Information";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistDataCartridgeInformationStmt, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeStatement(persistDataCartridgeInformationStmt);
//            StratosDBUtils.closeResultSet(resultSet);
//        }
//
//        return dataCartridgeInfoId;
//    }
//
//    /*private int persistSubscriber (Connection connection, Subscriber subscriber) throws PersistenceManagerException {
//
//        PreparedStatement persistSubscriberStmt = null;
//
//        String insertSubscriber = "INSERT INTO TENANT (TENANT_ID,USERNAME,PASSWORD) VALUES (?,?,?)";
//
//        try {
//            persistSubscriberStmt = connection.prepareStatement(insertSubscriber);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for persisting Subscriber";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriberStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistSubscriberStmt.setInt(1, subscriber.getTenantId());
//            persistSubscriberStmt.setString(2, subscriber.getAdminUserName());
//            persistSubscriberStmt.setString(3, subscriber.getTenantDomain());
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for persisting Subscriber";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriberStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            persistSubscriberStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for persisting Subscriber";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, persistSubscriberStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeStatement(persistSubscriberStmt);
//        }
//
//        return subscriber.getTenantId();
//    }*/
//
//    @Override
//    public void removeCartridgeSubscription(int tenantId, String alias) throws PersistenceManagerException {
//        //TODO
//    }
//
//    @Override
//    public CartridgeSubscription getCartridgeSubscription(int tenantId, String alias) throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.CARTRIDGE_ALIAS=? AND S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED' " +
//                "inner join REPOSITORY R on S.REPOSITORY_ID=R.REPOSITORY_ID " +
//                "inner join DATA_CARTRIDGE D on S.DATA_CARTRIDGE_ID=D.DATA_CARTRIDGE_ID ";
//
//        PreparedStatement getSubscriptionStatement = null;
//        try {
//            getSubscriptionStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getSubscriptionStatement.setString(1, alias);
//            getSubscriptionStatement.setInt(2, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getSubscriptionStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        CartridgeSubscription cartridgeSubscription = null;
//        try {
//            while (resultSet.next()) {
//                cartridgeSubscription = populateCartridgeSubscription(resultSet);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionStatement, resultSet);
//        }
//
//        return cartridgeSubscription;
//    }
//
//    private CartridgeSubscription populateCartridgeSubscription (ResultSet resultSet)
//            throws PersistenceManagerException, SQLException {
//
//        String cartridgeType = resultSet.getString("CARTRIDGE_TYPE");
//
//        CartridgeInfo cartridgeInfo = null;
//        try {
//            cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);
//
//        } catch (Exception e) {
//            //Cannot happen, but can continue
//            String message = "Error getting Cartridge Definition for " + cartridgeType;
//            log.error(message, e);
//        }
//
//        //If an error occurred while getting the CartridgeInfo instance, create an instance with a minimal data set
//        if(cartridgeInfo == null) {
//            cartridgeInfo = new CartridgeInfo();
//            cartridgeInfo.setMultiTenant(resultSet.getBoolean("MULTITENANT"));
//            cartridgeInfo.setProvider(resultSet.getString("PROVIDER"));
//        }
//
//        CartridgeSubscription cartridgeSubscription = null;
//        try {
//            cartridgeSubscription = CartridgeSubscriptionFactory.getCartridgeSubscriptionInstance(cartridgeInfo);
//
//        } catch (ADCException e) {
//            throw new PersistenceManagerException(e);
//        }
//
//        /*Policy autoScalingPolicy = PolicyHolder.getInstance().getPolicy(resultSet.getString("AUTOSCALING_POLICY"));
//        if(autoScalingPolicy == null) {
//            //get the default AutoScaling policy
//            autoScalingPolicy = PolicyHolder.getInstance().getDefaultPolicy();
//        }*/
//
//        //populate data
//        cartridgeSubscription.setSubscriptionId(resultSet.getInt("SUBSCRIPTION_ID"));
//        cartridgeSubscription.setType(cartridgeType);
//        cartridgeSubscription.setAlias(resultSet.getString("CARTRIDGE_ALIAS"));
//        cartridgeSubscription.setMappedDomain(resultSet.getString("MAPPED_DOMAIN"));
//        cartridgeSubscription.setSubscriptionStatus(resultSet.getString("SUBSCRIPTION_STATUS"));
//        cartridgeSubscription.setAutoscalingPolicyName(resultSet.getString("AUTOSCALING_POLICY"));
//
//        //Repository related data
//        if (resultSet.getInt("REPOSITORY_ID") != -1) {
//            Repository repository = new Repository();
//            repository.setId(resultSet.getInt("REPOSITORY_ID"));
//            repository.setUrl(resultSet.getString("URL"));
//            repository.setUserName(resultSet.getString("USERNAME"));
//            repository.setPassword(RepoPasswordMgtUtil.decryptPassword(resultSet.getString("PASSWORD")));
//            repository.setPrivateRepository(resultSet.getBoolean("IS_PRIVATE"));
//            cartridgeSubscription.setRepository(repository);
//        }
//
//        //Cluster related data
//        Cluster cluster = new Cluster();
//        cluster.setId(resultSet.getInt("CLUSTER_ID"));
//        cluster.setHostName(resultSet.getString("HOSTNAME"));
//        cluster.setClusterDomain(resultSet.getString("DOMAIN"));
//        cluster.setClusterSubDomain(resultSet.getString("SUBDOMAIN"));
//        cluster.setMgtClusterDomain(resultSet.getString("MGT_DOMAIN"));
//        cluster.setMgtClusterSubDomain(resultSet.getString("MGT_SUBDOMAIN"));
//        cluster.setServiceStatus(resultSet.getString("SERVICE_STATUS"));
//        cartridgeSubscription.setCluster(cluster);
//
//        //data cartridge specific information
//        //TODO: temporarily removed
////        if (resultSet.getInt("DATA_CARTRIDGE_ID") != -1 && cartridgeSubscription instanceof
////                SingleTenantCartridgeSubscription) {
////            DataCartridgeSubscription dataCartridgeSubscription = (DataCartridgeSubscription)cartridgeSubscription;
////            dataCartridgeSubscription.setHost(resultSet.getString("HOST"));
////            dataCartridgeSubscription.setUsername(resultSet.getString("ADMIN_USERNAME"));
////            dataCartridgeSubscription.setPassword(RepoPasswordMgtUtil.decryptPassword(resultSet.
////                    getString("ADMIN_PASSWORD")));
////        }
//
//        //Subscriber related data
//        CarbonContext carbonContext = CarbonContext.getThreadLocalCarbonContext();
//        Subscriber subscriber = new Subscriber(carbonContext.getUsername(), carbonContext.getTenantId(),
//                carbonContext.getTenantDomain());
//        cartridgeSubscription.setSubscriber(subscriber);
//
//        return cartridgeSubscription;
//    }
//
//    @Override
//    public List<CartridgeSubscription> getCartridgeSubscriptions(int tenantId) throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED' " +
//                "inner join REPOSITORY R on S.REPOSITORY_ID=R.REPOSITORY_ID " +
//                "inner join DATA_CARTRIDGE D on S.DATA_CARTRIDGE_ID=D.DATA_CARTRIDGE_ID ";
//
//        PreparedStatement getSubscriptionsStatement = null;
//        try {
//            getSubscriptionsStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getSubscriptionsStatement.setInt(1, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getSubscriptionsStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        List<CartridgeSubscription> cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
//        try {
//            while(resultSet.next()) {
//                CartridgeSubscription cartridgeSubscription = populateCartridgeSubscription(resultSet);
//                cartridgeSubscriptions.add(cartridgeSubscription);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement, resultSet);
//        }
//
//        return cartridgeSubscriptions;
//    }
//
//    @Override
//    public List<CartridgeSubscription> getCartridgeSubscriptions(int tenantId, String cartridgeType)
//            throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.CARTRIDGE_TYPE=? AND S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED' " +
//                "inner join REPOSITORY R on S.REPOSITORY_ID=R.REPOSITORY_ID " +
//                "inner join DATA_CARTRIDGE D on S.DATA_CARTRIDGE_ID=D.DATA_CARTRIDGE_ID ";
//
//        PreparedStatement getSubscriptionsStatement = null;
//        try {
//            getSubscriptionsStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getSubscriptionsStatement.setString(1, cartridgeType);
//            getSubscriptionsStatement.setInt(2, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getSubscriptionsStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        List<CartridgeSubscription> cartridgeSubscriptions = new ArrayList<CartridgeSubscription>();
//        try {
//            while(resultSet.next()) {
//                CartridgeSubscription cartridgeSubscription = populateCartridgeSubscription(resultSet);
//                cartridgeSubscriptions.add(cartridgeSubscription);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getSubscriptionsStatement, resultSet);
//        }
//
//        return cartridgeSubscriptions;
//    }
//
//    @Override
//    public Repository getRepository(int tenantId, String alias) throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.TENANT_ID=? AND S.CARTRIDGE_ALIAS=? AND S.STATE != 'UNSUBSCRIBED' " +
//                "inner join REPOSITORY R on S.REPOSITORY_ID=R.REPOSITORY_ID ";
//
//        PreparedStatement getRepositoryStatement = null;
//        try {
//            getRepositoryStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getRepositoryStatement.setInt(1, tenantId);
//            getRepositoryStatement.setString(2, alias);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getRepositoryStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        Repository repository = null;
//        try {
//            while(resultSet.next()) {
//                repository = populateRepository(resultSet);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement, resultSet);
//        }
//
//        return repository;
//    }
//
//    private Repository populateRepository (ResultSet resultSet) throws SQLException {
//
//        if(resultSet.getInt("REPOSITORY_ID") == -1) {
//            return null;
//        }
//
//        Repository repository = new Repository();
//        repository.setId(resultSet.getInt("REPOSITORY_ID"));
//        repository.setUrl(resultSet.getString("URL"));
//        repository.setUserName(resultSet.getString("USERNAME"));
//        repository.setPassword(RepoPasswordMgtUtil.decryptPassword(resultSet.getString("PASSWORD")));
//        repository.setPrivateRepository(resultSet.getBoolean("IS_PRIVATE"));
//
//        return repository;
//    }
//
//    @Override
//    public Repository getRepository(String clusterDomain) throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.DOMAIN=? AND S.STATE != 'UNSUBSCRIBED' " +
//                "inner join REPOSITORY R on S.REPOSITORY_ID=R.REPOSITORY_ID ";
//
//        PreparedStatement getRepositoryStatement = null;
//        try {
//            getRepositoryStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getRepositoryStatement.setString(1, clusterDomain);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getRepositoryStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        Repository repository = null;
//        try {
//            while(resultSet.next()) {
//                repository = populateRepository(resultSet);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getRepositoryStatement, resultSet);
//        }
//
//        return repository;
//    }
//
//    @Override
//    public DataCartridge getDataCartridgeSubscriptionInfo(int tenantId, String alias)
//            throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.CARTRIDGE_ALIAS=? AND S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED' " +
//                "inner join DATA_CARTRIDGE D on S.DATA_CARTRIDGE_ID=D.DATA_CARTRIDGE_ID ";
//
//        PreparedStatement getDataCartridgeSubscriptionInfoStatement = null;
//        try {
//            getDataCartridgeSubscriptionInfoStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getDataCartridgeSubscriptionInfoStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getDataCartridgeSubscriptionInfoStatement.setString(1, alias);
//            getDataCartridgeSubscriptionInfoStatement.setInt(2, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getDataCartridgeSubscriptionInfoStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getDataCartridgeSubscriptionInfoStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getDataCartridgeSubscriptionInfoStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        DataCartridge dataCartridge = null;
//        try {
//            while (resultSet.next()) {
//                dataCartridge = populateDataCartridgeInfo(resultSet);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getDataCartridgeSubscriptionInfoStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getDataCartridgeSubscriptionInfoStatement, resultSet);
//        }
//
//        return dataCartridge;
//    }
//
//    private DataCartridge populateDataCartridgeInfo (ResultSet resultSet) throws SQLException {
//
//        if(resultSet.getInt("DATA_CARTRIDGE_ID") == -1) {
//            return null;
//        }
//
//        DataCartridge dataCartridge = new DataCartridge();
//        dataCartridge.setId(resultSet.getInt("DATA_CARTRIDGE_ID"));
//        dataCartridge.setDataCartridgeType(resultSet.getString("CARTRIDGE_TYPE"));
//        dataCartridge.setHost(resultSet.getString("HOST"));
//        dataCartridge.setUserName(resultSet.getString("ADMIN_USERNAME"));
//        dataCartridge.setPassword(resultSet.getString("ADMIN_PASSWORD"));
//
//        return dataCartridge;
//    }
//
//    @Override
//    public boolean isAliasTaken(int tenantId, String alias) throws PersistenceManagerException {
//
//        boolean isAliasTaken = false;
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.CARTRIDGE_ALIAS=? AND S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED'";
//
//        PreparedStatement isAliasTakenStatement = null;
//        try {
//            isAliasTakenStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, isAliasTakenStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            isAliasTakenStatement.setString(1, alias);
//            isAliasTakenStatement.setInt(2, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, isAliasTakenStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = isAliasTakenStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, isAliasTakenStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            if(resultSet.next()) {
//                log.info("Alias " + alias + " has been already used for tenant " + tenantId);
//                isAliasTaken = true;
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, isAliasTakenStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, isAliasTakenStatement, resultSet);
//        }
//
//        return isAliasTaken;
//    }
//
//    @Override
//    public boolean hasSubscribed(int tenantId, String cartridgeType) throws PersistenceManagerException {
//
//        boolean hasSubscribed = false;
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT * FROM SUBSCRIPTION S " +
//                "WHERE S.CARTRIDGE_TYPE=? AND S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED'";
//
//        PreparedStatement hasSubscribedStatement = null;
//        try {
//            hasSubscribedStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, hasSubscribedStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            hasSubscribedStatement.setString(1, cartridgeType);
//            hasSubscribedStatement.setInt(2, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, hasSubscribedStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = hasSubscribedStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, hasSubscribedStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            if(resultSet.next()) {
//                log.info("Tenant " + tenantId + " has already subscribed for the Cartridge Type " + cartridgeType);
//                hasSubscribed = true;
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, hasSubscribedStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, hasSubscribedStatement, resultSet);
//        }
//
//        return hasSubscribed;
//    }
//
//    @Override
//    public void updateDomianMapping(int tenantId, String cartridgeAlias, String newDomain)
//            throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        PreparedStatement updateDomainMappingStmt = null;
//
//        String insertDataCartridgeInfo = "UPDATE CARTRIDGE_SUBSCRIPTION SET MAPPED_DOMAIN = ? " +
//                "WHERE TENANT_ID = ? AND ALIAS = ?";
//
//        try {
//            updateDomainMappingStmt = connection.prepareStatement(insertDataCartridgeInfo);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateDomainMappingStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            updateDomainMappingStmt.setString(1, newDomain);
//            updateDomainMappingStmt.setInt(2, tenantId);
//            updateDomainMappingStmt.setString(3, cartridgeAlias);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateDomainMappingStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            updateDomainMappingStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateDomainMappingStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, updateDomainMappingStmt);
//        }
//    }
//
//    @Override
//    public String getMappedDomain(int tenantId, String cartridgeAlias) throws PersistenceManagerException {
//
//        String mappedDomain = null;
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT MAPPED_DOMAIN FROM SUBSCRIPTION S " +
//                "WHERE S.TENANT_ID=? AND S.CARTRIDGE_ALIAS=? AND S.STATE != 'UNSUBSCRIBED'";
//
//        PreparedStatement getMappedDomainStatement = null;
//        try {
//            getMappedDomainStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getMappedDomainStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getMappedDomainStatement.setInt(1, tenantId);
//            getMappedDomainStatement.setString(2, cartridgeAlias);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getMappedDomainStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getMappedDomainStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getMappedDomainStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            if(resultSet.next()) {
//                mappedDomain = resultSet.getString("MAPPED_DOMAIN");
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getMappedDomainStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getMappedDomainStatement, resultSet);
//        }
//
//        return mappedDomain;
//    }
//
//    @Override
//    public Cluster getCluster(int tenantId, String cartridgeAlias) throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        String sqlQuery = "SELECT HOSTNAME,DOMAIN,SUBDOMAIN,MGT_DOMAIN,MGT_SUBDOMAIN,SERVICE_STATUS FROM SUBSCRIPTION S " +
//                "WHERE S.CARTRIDGE_ALIAS=? AND S.TENANT_ID=? AND S.STATE != 'UNSUBSCRIBED'";
//
//        PreparedStatement getClusterStatement = null;
//        try {
//            getClusterStatement = connection.prepareStatement(sqlQuery);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for retreiving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getClusterStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            getClusterStatement.setString(1, cartridgeAlias);
//            getClusterStatement.setInt(2, tenantId);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getClusterStatement);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        ResultSet resultSet = null;
//
//        try {
//            resultSet = getClusterStatement.executeQuery();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for retrieving CartridgeSubscription";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getClusterStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        Cluster cluster = null;
//        try {
//            if(resultSet.next()) {
//                cluster = populateCluster(resultSet);
//            }
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to populate CartridgeSubscription instance";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, getClusterStatement, resultSet);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, getClusterStatement, resultSet);
//        }
//
//        return cluster;
//    }
//
//    @Override
//    public void updateSubscriptionStatus(int tenantId, String cartridgeAlias, String newStatus)
//            throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        PreparedStatement updateSubscriptionStatusStmt = null;
//
//        String insertDataCartridgeInfo = "UPDATE CARTRIDGE_SUBSCRIPTION SET SUBSCRIPTION_STATUS = ? " +
//                "WHERE TENANT_ID = ? AND ALIAS = ?";
//
//        try {
//            updateSubscriptionStatusStmt = connection.prepareStatement(insertDataCartridgeInfo);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateSubscriptionStatusStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            updateSubscriptionStatusStmt.setString(1, newStatus);
//            updateSubscriptionStatusStmt.setInt(2, tenantId);
//            updateSubscriptionStatusStmt.setString(3, cartridgeAlias);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateSubscriptionStatusStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            updateSubscriptionStatusStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateSubscriptionStatusStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, updateSubscriptionStatusStmt);
//        }
//
//    }
//
//    @Override
//    public void updateServiceStatus(int tenantId, String cartridgeAlias, String newStatus)
//            throws PersistenceManagerException {
//
//        Connection connection = null;
//        try {
//            connection = StratosDBUtils.getConnection();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to obtain a DB connection";
//            log.error(errorMsg);
//            StratosDBUtils.closeConnection(connection);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        PreparedStatement updateServiceStatusStmt = null;
//
//        String insertDataCartridgeInfo = "UPDATE CARTRIDGE_SUBSCRIPTION SET SUBSCRIPTION_STATUS = ? " +
//                "WHERE TENANT_ID = ? AND ALIAS = ?";
//
//        try {
//            updateServiceStatusStmt = connection.prepareStatement(insertDataCartridgeInfo);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed create a Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateServiceStatusStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            updateServiceStatusStmt.setString(1, newStatus);
//            updateServiceStatusStmt.setInt(2, tenantId);
//            updateServiceStatusStmt.setString(3, cartridgeAlias);
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to add data to Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateServiceStatusStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//        }
//
//        try {
//            updateServiceStatusStmt.executeUpdate();
//
//        } catch (SQLException e) {
//            String errorMsg = "Failed to execute the Prepared Statement for updating Domain Mapping";
//            log.error(errorMsg);
//            StratosDBUtils.closeAllConnections(connection, updateServiceStatusStmt);
//            throw new PersistenceManagerException(errorMsg, e);
//
//        } finally {
//            StratosDBUtils.closeAllConnections(connection, updateServiceStatusStmt);
//        }
//    }
//
//    private Cluster populateCluster (ResultSet resultSet) throws SQLException {
//
//        Cluster cluster = new Cluster();
//        cluster.setHostName(resultSet.getString("HOSTNAME"));
//        cluster.setClusterDomain(resultSet.getString("DOMAIN"));
//        cluster.setClusterSubDomain(resultSet.getString("SUBDOMAIN"));
//        cluster.setMgtClusterDomain(resultSet.getString("MGT_DOMAIN"));
//        cluster.setMgtClusterSubDomain(resultSet.getString("MGT_SUBDOMAIN"));
//        cluster.setServiceStatus(resultSet.getString("SERVICE_STATUS"));
//
//        return cluster;
//    }
//
//    @Override
//    public void removeDomainMapping(int tenantId, String cartridgeAlias)
//            throws PersistenceManagerException {
//
//        updateDomianMapping(tenantId, cartridgeAlias, null);
//    }
//
//
//}
