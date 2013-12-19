/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.adc.mgt.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dao.DataCartridge;
import org.apache.stratos.adc.mgt.dao.PortMapping;
import org.apache.stratos.adc.mgt.dao.RepositoryCredentials;
import org.apache.stratos.adc.mgt.deploy.service.Service;
import org.apache.stratos.adc.mgt.repository.Repository;

/**
 * This class is responsible for handling persistence
 * 
 */
public class PersistenceManager {

	private static final Log log = LogFactory.getLog(PersistenceManager.class);

	public static void persistCartridgeInstanceInfo(String instanceIp, String clusterDomain, String clusterSubDomain,
			String cartridgeType, String state) throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement updateStatement = null;
		ResultSet resultSet = null;

		boolean isUpdate = false;
		int instanceId = 0;
		try {
			con = StratosDBUtils.getConnection();

			// First check whether Ip exists..
			String sql = "SELECT ID FROM CARTRIDGE_INSTANCE where INSTANCE_IP=? AND CARTRIDGE_TYPE=? "
					+ " AND CLUSTER_DOMAIN=? AND CLUSTER_SUBDOMAIN=?";
			statement = con.prepareStatement(sql);
			statement.setString(1, instanceIp);
			statement.setString(2, cartridgeType);
			statement.setString(3, clusterDomain);
			statement.setString(4, clusterSubDomain);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				isUpdate = true;
				instanceId = resultSet.getInt("ID");
			}

			String persistQuery = null;
			if (isUpdate) {
				persistQuery = "UPDATE CARTRIDGE_INSTANCE SET STATE=?" + " WHERE ID=?";
				updateStatement = con.prepareStatement(persistQuery);
				updateStatement.setString(1, state);
				updateStatement.setInt(2, instanceId);
			} else {
				persistQuery = "INSERT INTO CARTRIDGE_INSTANCE (INSTANCE_IP, CARTRIDGE_TYPE, STATE, CLUSTER_DOMAIN, CLUSTER_SUBDOMAIN)"
						+ " VALUES (?, ?, ?, ?, ?)";
				updateStatement = con.prepareStatement(persistQuery);
				updateStatement.setString(1, instanceIp);
				updateStatement.setString(2, cartridgeType);
				updateStatement.setString(3, state);
				updateStatement.setString(4, clusterDomain);
				updateStatement.setString(5, clusterSubDomain);
			}
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + persistQuery);
			}
			updateStatement.executeUpdate();
			con.commit();
		} catch (Exception e) {
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException e1) {
					log.error("Failed to rollback", e);
				}
			}
			;
			log.error("Error", e);
			throw e;
		} finally {
			StratosDBUtils.closeResultSet(resultSet);
			StratosDBUtils.closeAllConnections(con, statement, updateStatement);
		}
	}

	public static boolean isAlreadySubscribed(String cartridgeType, int tenantId) throws Exception {

		Connection con = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT C.ALIAS FROM CARTRIDGE_SUBSCRIPTION C WHERE TENANT_ID = ? AND C.CARTRIDGE = ? AND C.STATE != 'UNSUBSCRIBED'";
			preparedStatement = con.prepareStatement(sql);
			preparedStatement.setInt(1, tenantId);
			preparedStatement.setString(2, cartridgeType);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				String alias = resultSet.getString("ALIAS");
				if (log.isDebugEnabled()) {
					log.debug("Already subscribed to " + cartridgeType + " with alias " + alias);
				}
				return true;
			} else {
				return false;
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, preparedStatement, resultSet);
		}
	}

	public static List<CartridgeSubscriptionInfo> retrieveSubscribedCartridges(int tenantId) throws Exception {

		List<CartridgeSubscriptionInfo> subscribedCartridgeList = new ArrayList<CartridgeSubscriptionInfo>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT C.CARTRIDGE, C.ALIAS, C.CLUSTER_DOMAIN, C.CLUSTER_SUBDOMAIN, C.POLICY, C.STATE, "
					+ "C.TENANT_ID, C.SUBSCRIPTION_ID, C.DATA_CARTRIDGE_ID, D.TYPE, D.USER_NAME, D.PASSWORD, "
					+ "C.PROVIDER, C.HOSTNAME, C.MAPPED_DOMAIN, R.REPO_NAME FROM CARTRIDGE_SUBSCRIPTION C "
					+ "LEFT JOIN DATA_CARTRIDGE D on D.DATA_CART_ID=C.DATA_CARTRIDGE_ID  "
					+ "LEFT JOIN REPOSITORY R ON C.REPO_ID=R.REPO_ID WHERE TENANT_ID=? AND C.STATE != 'UNSUBSCRIBED' "
					+ "ORDER BY C.SUBSCRIPTION_ID";
			statement = con.prepareStatement(sql);
			statement.setInt(1, tenantId);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				CartridgeSubscriptionInfo cartridge = new CartridgeSubscriptionInfo();
				cartridge.setAlias(resultSet.getString("ALIAS"));
				cartridge.setCartridge(resultSet.getString("CARTRIDGE"));
				cartridge.setState(resultSet.getString("STATE"));
				cartridge.setClusterDomain(resultSet.getString("CLUSTER_DOMAIN"));
				cartridge.setClusterSubdomain(resultSet.getString("CLUSTER_SUBDOMAIN"));
				cartridge.setProvider(resultSet.getString("PROVIDER"));
				cartridge.setPolicy(resultSet.getString("POLICY"));
				cartridge.setMappedDomain(resultSet.getString("MAPPED_DOMAIN"));
				Repository repo = new Repository();
				repo.setUrl(resultSet.getString("REPO_NAME"));
				cartridge.setRepository(repo);
				cartridge.setHostName(resultSet.getString("HOSTNAME"));
				int dataCartridgeId = resultSet.getInt("DATA_CARTRIDGE_ID");
				if (dataCartridgeId != 0) {
					DataCartridge dataCartridge = new DataCartridge();
					dataCartridge.setDataCartridgeType(resultSet.getString("TYPE"));
					dataCartridge.setPassword(resultSet.getString("PASSWORD"));
					dataCartridge.setUserName(resultSet.getString("USER_NAME"));
					cartridge.setDataCartridge(dataCartridge);
				}
				subscribedCartridgeList.add(cartridge);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return subscribedCartridgeList;
	}

	public static String getRepoURL(int tenantId, String cartridge) throws Exception {

		String repoUrl = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT REPO_NAME FROM REPOSITORY R, CARTRIDGE_SUBSCRIPTION C "
					+ "WHERE C.REPO_ID=R.REPO_ID AND C.TENANT_ID=? AND C.CARTRIDGE=? "
					+ "AND C.STATE != 'UNSUBSCRIBED' ";
			statement = con.prepareStatement(sql);
			statement.setInt(1, tenantId);
			statement.setString(2, cartridge);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				repoUrl = resultSet.getString("REPO_NAME");
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return repoUrl;
	}

	public static RepositoryCredentials getRepoCredentials(int tenantId, String cartridge, String alias)
			throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		RepositoryCredentials repoCredentials = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT REPO_NAME,REPO_USER_NAME,REPO_USER_PASSWORD FROM REPOSITORY R, CARTRIDGE_SUBSCRIPTION C "
					+ "WHERE C.REPO_ID=R.REPO_ID AND C.TENANT_ID=? AND C.CARTRIDGE=? AND C.STATE != 'UNSUBSCRIBED' ";
			if (alias != null) {
				sql = sql + " AND C.ALIAS=?";
			}
			statement = con.prepareStatement(sql);
			statement.setInt(1, tenantId);
			statement.setString(2, cartridge);
			if (alias != null) {
				statement.setString(3, alias);
			}
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				repoCredentials = new RepositoryCredentials();
				repoCredentials.setUrl(resultSet.getString("REPO_NAME"));
				repoCredentials.setUserName(resultSet.getString("REPO_USER_NAME"));
				repoCredentials.setPassword(RepoPasswordMgtUtil.decryptPassword(resultSet.getString("REPO_USER_PASSWORD"),null)); // TODO this is no longer supported
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return repoCredentials;
	}

	public static boolean isAliasAlreadyTaken(String alias, String cartridgeType) throws Exception {
		boolean aliasAlreadyTaken = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT SUBSCRIPTION_ID FROM CARTRIDGE_SUBSCRIPTION where ALIAS=? AND CARTRIDGE=? AND STATE != 'UNSUBSCRIBED'";
			statement = con.prepareStatement(sql);
			statement.setString(1, alias);
			statement.setString(2, cartridgeType);
			statement.setMaxRows(1);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				log.info("Already taken..");
				aliasAlreadyTaken = true;
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return aliasAlreadyTaken;
	}

	public static int persistSubscription(CartridgeSubscriptionInfo cartridgeSubscriptionInfo) throws Exception {

		int cartridgeSubscriptionId = 0;
		int repoId = 0;
		int dataCartridgeId = 0;
		ResultSet res = null;
		PreparedStatement insertSubscriptionStmt = null;
		PreparedStatement insertRepoStmt = null;
		PreparedStatement insertDataCartStmt = null;

		Connection con = null;

		// persist cartridge_subscription
		try {
			con = StratosDBUtils.getConnection();
			// persist repo
			if (cartridgeSubscriptionInfo.getRepository() != null) {
				String encryptedRepoUserPassword = RepoPasswordMgtUtil.encryptPassword(cartridgeSubscriptionInfo.getRepository()
						.getPassword(),cartridgeSubscriptionInfo.getSubscriptionKey());
				String insertRepo = "INSERT INTO REPOSITORY (REPO_NAME,STATE,REPO_USER_NAME,REPO_USER_PASSWORD)"
						+ " VALUES (?,?,?,?)";

				insertRepoStmt = con.prepareStatement(insertRepo, Statement.RETURN_GENERATED_KEYS);
				insertRepoStmt.setString(1, cartridgeSubscriptionInfo.getRepository().getUrl());
				insertRepoStmt.setString(2, "ACTIVE");
				insertRepoStmt.setString(3, cartridgeSubscriptionInfo.getRepository().getUserName());
				insertRepoStmt.setString(4, encryptedRepoUserPassword);
				if (log.isDebugEnabled()) {
					log.debug("Executing insert: " + insertRepo);
				}
				insertRepoStmt.executeUpdate();
				res = insertRepoStmt.getGeneratedKeys();
				if (res.next()) {
					repoId = res.getInt(1);
				}
				StratosDBUtils.closeResultSet(res);
			}

			// persist data cartridge
			if (cartridgeSubscriptionInfo.getDataCartridge() != null) {
				String insertDataCartridge = "INSERT INTO DATA_CARTRIDGE (TYPE,USER_NAME,PASSWORD,STATE)"
						+ " VALUES (?,?,?,?)";
				insertDataCartStmt = con.prepareStatement(insertDataCartridge, Statement.RETURN_GENERATED_KEYS);
				insertDataCartStmt.setString(1, cartridgeSubscriptionInfo.getDataCartridge().getDataCartridgeType());
				insertDataCartStmt.setString(2, cartridgeSubscriptionInfo.getDataCartridge().getUserName());
				insertDataCartStmt.setString(3, cartridgeSubscriptionInfo.getDataCartridge().getPassword());
				insertDataCartStmt.setString(4, "ACTIVE");
				if (log.isDebugEnabled()) {
					log.debug("Executing insert: " + insertDataCartridge);
				}
				insertDataCartStmt.executeUpdate();
				res = insertDataCartStmt.getGeneratedKeys();
				if (res.next()) {
					dataCartridgeId = res.getInt(1);
				}
				StratosDBUtils.closeResultSet(res);
			}

			// TODO - Mapped domain is not used. Is it not used anymore?
			String insertSubscription = "INSERT INTO CARTRIDGE_SUBSCRIPTION (TENANT_ID, CARTRIDGE, PROVIDER,"
					+ "HOSTNAME, POLICY, CLUSTER_DOMAIN, CLUSTER_SUBDOMAIN, MGT_DOMAIN, MGT_SUBDOMAIN, STATE, "
					+ "ALIAS, TENANT_DOMAIN, BASE_DIR, REPO_ID, DATA_CARTRIDGE_ID, SUBSCRIPTION_KEY)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			insertSubscriptionStmt = con.prepareStatement(insertSubscription, Statement.RETURN_GENERATED_KEYS);
			insertSubscriptionStmt.setInt(1, cartridgeSubscriptionInfo.getTenantId());
			insertSubscriptionStmt.setString(2, cartridgeSubscriptionInfo.getCartridge());
			insertSubscriptionStmt.setString(3, cartridgeSubscriptionInfo.getProvider());
			insertSubscriptionStmt.setString(4, cartridgeSubscriptionInfo.getHostName());
			insertSubscriptionStmt.setString(5, cartridgeSubscriptionInfo.getPolicy());
			insertSubscriptionStmt.setString(6, cartridgeSubscriptionInfo.getClusterDomain());
			insertSubscriptionStmt.setString(7, cartridgeSubscriptionInfo.getClusterSubdomain());
			insertSubscriptionStmt.setString(8, cartridgeSubscriptionInfo.getMgtClusterDomain());
			insertSubscriptionStmt.setString(9, cartridgeSubscriptionInfo.getMgtClusterSubDomain());
			insertSubscriptionStmt.setString(10, cartridgeSubscriptionInfo.getState());
			insertSubscriptionStmt.setString(11, cartridgeSubscriptionInfo.getAlias());
			insertSubscriptionStmt.setString(12, cartridgeSubscriptionInfo.getTenantDomain());
			insertSubscriptionStmt.setString(13, cartridgeSubscriptionInfo.getBaseDirectory());
			insertSubscriptionStmt.setInt(14, repoId);
			insertSubscriptionStmt.setInt(15, dataCartridgeId);
			insertSubscriptionStmt.setString(16, cartridgeSubscriptionInfo.getSubscriptionKey());
			if (log.isDebugEnabled()) {
				log.debug("Executing insert: " + insertSubscription);
			}
			insertSubscriptionStmt.executeUpdate();
			res = insertSubscriptionStmt.getGeneratedKeys();
			if (res.next()) {
				cartridgeSubscriptionId = res.getInt(1);
			}

			List<PortMapping> portMapping = cartridgeSubscriptionInfo.getPortMappings();
			// persist port map
			if (portMapping != null && !portMapping.isEmpty()) {
				for (PortMapping portMap : portMapping) {
					String insertPortMapping = "INSERT INTO PORT_MAPPING (SUBSCRIPTION_ID, TYPE, PRIMARY_PORT, PROXY_PORT, STATE)"
							+ " VALUES (?,?,?,?,?)";

					PreparedStatement insertPortsStmt = con.prepareStatement(insertPortMapping);
					insertPortsStmt.setInt(1, cartridgeSubscriptionId);
					insertPortsStmt.setString(2, portMap.getType());
					insertPortsStmt.setString(3, portMap.getPrimaryPort());
					insertPortsStmt.setString(4, portMap.getProxyPort());
					insertPortsStmt.setString(5, "ACTIVE");
					if (log.isDebugEnabled()) {
						log.debug("Executing insert: " + insertPortMapping);
					}
					insertPortsStmt.executeUpdate();
					StratosDBUtils.closeStatement(insertPortsStmt);
				}
			}
			con.commit(); // Commit manually
		} catch (Exception e) {
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException e1) {
					log.error("Failed to rollback", e);
				}
			}
			;
			log.error(e.getMessage());
			String msg = "Exception : " + e.getMessage();
			log.error(msg, e);
			throw new Exception("Subscription failed!", e);
		} finally {
			StratosDBUtils.closeResultSet(res);
			StratosDBUtils.closeAllConnections(con, insertRepoStmt, insertDataCartStmt, insertSubscriptionStmt);
		}
		return cartridgeSubscriptionId;
	}

	public static String getHostNameForCartridgeName(int tenantId, String alias) throws Exception {

		String hostName = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT HOSTNAME FROM CARTRIDGE_SUBSCRIPTION where TENANT_ID=?"
					+ " AND ALIAS=? AND STATE != 'UNSUBSCRIBED'";
			statement = con.prepareStatement(sql);
			statement.setInt(1, tenantId);
			statement.setString(2, alias);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				hostName = resultSet.getString("HOSTNAME");
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return hostName;
	}

	public static CartridgeSubscriptionInfo getSubscription(String tenantDomain, String alias) throws Exception {

		CartridgeSubscriptionInfo cartridgeSubscriptionInfo = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT * FROM CARTRIDGE_SUBSCRIPTION C left join REPOSITORY R on "
					+ "C.REPO_ID=R.REPO_ID left join DATA_CARTRIDGE D on "
					+ "D.DATA_CART_ID=C.DATA_CARTRIDGE_ID WHERE ALIAS=? AND TENANT_DOMAIN=? AND C.STATE != 'UNSUBSCRIBED'";
			statement = con.prepareStatement(sql);
			statement.setString(1, alias);
			statement.setString(2, tenantDomain);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				cartridgeSubscriptionInfo = new CartridgeSubscriptionInfo();
				populateSubscription(cartridgeSubscriptionInfo, resultSet);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}

		return cartridgeSubscriptionInfo;
	}

	private static void populateSubscription(CartridgeSubscriptionInfo cartridgeSubscriptionInfo, ResultSet resultSet)
			throws Exception {
		String repoName = resultSet.getString("REPO_NAME");
		String repoUserName = resultSet.getString("REPO_USER_NAME");
		String repoPassword = resultSet.getString("REPO_USER_PASSWORD");
		if (repoName != null) {
			Repository repo = new Repository();
			repo.setUrl(repoName);
			repo.setUserName(repoUserName);
			repo.setPassword(repoPassword);
			cartridgeSubscriptionInfo.setRepository(repo);
		}

		int dataCartridgeId = resultSet.getInt("DATA_CARTRIDGE_ID");
		if (dataCartridgeId != 0) {
			DataCartridge dataCartridge = new DataCartridge();
			dataCartridge.setDataCartridgeType(resultSet.getString("TYPE"));
			dataCartridge.setPassword(resultSet.getString("PASSWORD"));
			dataCartridge.setUserName(resultSet.getString("USER_NAME"));
			cartridgeSubscriptionInfo.setDataCartridge(dataCartridge);
		}
		cartridgeSubscriptionInfo.setPortMappings(getPortMappings(resultSet.getInt("SUBSCRIPTION_ID")));
		cartridgeSubscriptionInfo.setTenantId(resultSet.getInt("TENANT_ID"));
		cartridgeSubscriptionInfo.setState(resultSet.getString("STATE"));
		cartridgeSubscriptionInfo.setPolicy(resultSet.getString("POLICY"));
		cartridgeSubscriptionInfo.setCartridge(resultSet.getString("CARTRIDGE"));
		cartridgeSubscriptionInfo.setAlias(resultSet.getString("ALIAS"));
		cartridgeSubscriptionInfo.setClusterDomain(resultSet.getString("CLUSTER_DOMAIN"));
		cartridgeSubscriptionInfo.setClusterSubdomain(resultSet.getString("CLUSTER_SUBDOMAIN"));
		cartridgeSubscriptionInfo.setMgtClusterDomain(resultSet.getString("MGT_DOMAIN"));
		cartridgeSubscriptionInfo.setMgtClusterSubDomain(resultSet.getString("MGT_SUBDOMAIN"));
		cartridgeSubscriptionInfo.setProvider(resultSet.getString("PROVIDER"));
		cartridgeSubscriptionInfo.setHostName(resultSet.getString("HOSTNAME"));
		cartridgeSubscriptionInfo.setTenantDomain(resultSet.getString("TENANT_DOMAIN"));
		cartridgeSubscriptionInfo.setBaseDirectory(resultSet.getString("BASE_DIR"));
		cartridgeSubscriptionInfo.setSubscriptionId(resultSet.getInt("SUBSCRIPTION_ID"));
		cartridgeSubscriptionInfo.setMappedDomain(resultSet.getString("MAPPED_DOMAIN"));
		cartridgeSubscriptionInfo.setSubscriptionKey(resultSet.getString("SUBSCRIPTION_KEY"));
	}

	private static List<PortMapping> getPortMappings(int subscriptionId) throws Exception {

		List<PortMapping> portMappingList = new ArrayList<PortMapping>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT * FROM PORT_MAPPING WHERE SUBSCRIPTION_ID = ?";
			statement = con.prepareStatement(sql);
			statement.setInt(1, subscriptionId);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				PortMapping portMapping = new PortMapping();
				portMapping.setPrimaryPort(resultSet.getString("PRIMARY_PORT"));
				portMapping.setProxyPort(resultSet.getString("PROXY_PORT"));
				portMapping.setType(resultSet.getString("TYPE"));
				portMappingList.add(portMapping);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return portMappingList;
	}

	public static void updateDomainMapping(int tenantId, String cartridgeAlias, String domain) throws Exception {
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "UPDATE CARTRIDGE_SUBSCRIPTION SET MAPPED_DOMAIN = ? WHERE TENANT_ID = ? AND ALIAS = ?";
			statement = con.prepareStatement(sql);
			statement.setString(1, domain);
			statement.setInt(2, tenantId);
			statement.setString(3, cartridgeAlias);
			if (log.isDebugEnabled()) {
				log.debug("Executing update: " + sql);
			}
			statement.executeUpdate();
			con.commit();
		} catch (Exception s) {
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException e) {
					log.error("Failed to rollback", e);
				}
			}
			String msg = "Error: " + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
	}

	public static List<CartridgeSubscriptionInfo> getSubscription(String repositoryURL) throws Exception {

		List<CartridgeSubscriptionInfo> subscriptionList = new ArrayList<CartridgeSubscriptionInfo>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT * from CARTRIDGE_SUBSCRIPTION C, REPOSITORY R "
					+ "where R.REPO_NAME LIKE ? AND C.REPO_ID = R.REPO_ID AND C.STATE != 'UNSUBSCRIBED'";
			statement = con.prepareStatement(sql);
			statement.setString(1, repositoryURL + "%");
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				CartridgeSubscriptionInfo cartridgeSubscriptionInfo = new CartridgeSubscriptionInfo();
				populateSubscription(cartridgeSubscriptionInfo, resultSet);
				subscriptionList.add(cartridgeSubscriptionInfo);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return subscriptionList;
	}

    public static List<CartridgeSubscriptionInfo> getSubscriptionsForTenant (int tenantId) throws Exception {

        List<CartridgeSubscriptionInfo> cartridgeSubscriptionInfos = null;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            con = StratosDBUtils.getConnection();
            String sql = "SELECT * FROM CARTRIDGE_SUBSCRIPTION C left join REPOSITORY R on "
                    + "C.REPO_ID=R.REPO_ID left join DATA_CARTRIDGE D on "
                    + "D.DATA_CART_ID=C.DATA_CARTRIDGE_ID WHERE TENANT_ID=? AND C.STATE != 'UNSUBSCRIBED'";
            statement = con.prepareStatement(sql);
            statement.setInt(1, tenantId);
            if (log.isDebugEnabled()) {
                log.debug("Executing query: " + sql);
            }

            resultSet = statement.executeQuery();
            cartridgeSubscriptionInfos = new ArrayList<CartridgeSubscriptionInfo>();
            if (resultSet.next()) {
                CartridgeSubscriptionInfo cartridgeSubscriptionInfo = new CartridgeSubscriptionInfo();
                populateSubscription(cartridgeSubscriptionInfo, resultSet);
                cartridgeSubscriptionInfos.add(cartridgeSubscriptionInfo);
            }
        } catch (Exception s) {
            String msg = "Error while sql connection :" + s.getMessage();
            log.error(msg, s);
            throw s;

        } finally {
            StratosDBUtils.closeAllConnections(con, statement, resultSet);
        }

        return cartridgeSubscriptionInfos;
    }


    public static void updateSubscriptionState(int subscriptionId, String state) throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "UPDATE CARTRIDGE_SUBSCRIPTION SET STATE=? WHERE SUBSCRIPTION_ID=?";
			statement = con.prepareStatement(sql);
			statement.setString(1, state);
			statement.setInt(2, subscriptionId);
			if (log.isDebugEnabled()) {
				log.debug("Executing update: " + sql);
			}
			statement.executeUpdate();
			con.commit();
		} catch (Exception s) {
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException e) {
					log.error("Failed to rollback", e);
				}
			}
			;
			String msg = "Error: " + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
	}

	public static Map<String, String> getCartridgeInstanceInfo(String[] ips, String clusterDomain, String clusterSubdomain)
            throws Exception {
		Map<String, String> instanceIpToStateMap = new HashMap<String, String>();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = StratosDBUtils.getConnection();
			StringBuilder sqlBuilder = new StringBuilder(
					"SELECT INSTANCE_IP, STATE FROM CARTRIDGE_INSTANCE WHERE INSTANCE_IP IN (");
			for (int i = 0; i < ips.length; i++) {
				if (i > 0) {
					sqlBuilder.append(", ");
				}
				sqlBuilder.append("?");
			}
			sqlBuilder.append(") AND CLUSTER_DOMAIN=? AND CLUSTER_SUBDOMAIN=?");
			String sql = sqlBuilder.toString();
			
			statement = con.prepareStatement(sql);
			int i = 1;
			for (int j = 0; j < ips.length; j++, i++) {
				String ip = ips[j];
				statement.setString(i, ip);
			}
			statement.setString(i++, clusterDomain);
			statement.setString(i, clusterSubdomain);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				instanceIpToStateMap.put(resultSet.getString("INSTANCE_IP"), resultSet.getString("STATE"));
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw new Exception("Ann error occurred while listing cartridge information.");
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}
		return instanceIpToStateMap;
	}

	

	public static void updateInstanceState(String state, String[] ips, String clusterDomain, String clusterSubDomain, String cartridgeType)
			throws Exception {

		Connection con = null;
		PreparedStatement statement = null;

		if (ips != null && ips.length > 0) {
			try {
				con = StratosDBUtils.getConnection();
				StringBuilder sqlBuilder = new StringBuilder(
						"UPDATE CARTRIDGE_INSTANCE SET STATE=? WHERE INSTANCE_IP IN (");
				for (int i = 0; i < ips.length; i++) {
					if (i > 0) {
						sqlBuilder.append(", ");
					}
					sqlBuilder.append("?");
				}
				sqlBuilder.append(") AND CLUSTER_DOMAIN=? AND CLUSTER_SUBDOMAIN=? AND CARTRIDGE_TYPE=?");
				String sql = sqlBuilder.toString();
				statement = con.prepareStatement(sql);
				statement.setString(1, state);
				int i = 2;
				for (int j = 0; j < ips.length; j++, i++) {
					String ip = ips[j];
					statement.setString(i, ip);
				}
				statement.setString(i++, clusterDomain);
				statement.setString(i++, clusterSubDomain);
				statement.setString(i, cartridgeType);
				if (log.isDebugEnabled()) {
					log.debug("Executing query: " + sql);
				}
				statement.executeUpdate();
				con.commit();
			} catch (Exception s) {
				if (con != null) {
					try {
						con.rollback();
					} catch (SQLException e) {
						log.error("Failed to rollback", e);
					}
				}
				String msg = "Error: " + s.getMessage();
				log.error(msg, s);
				throw s;
			} finally {
				StratosDBUtils.closeAllConnections(con, statement);
			}
		}

	}
	
	
	public static CartridgeSubscriptionInfo getSubscriptionFromClusterId(String clusterId) throws Exception {

		CartridgeSubscriptionInfo cartridgeSubscriptionInfo = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			con = StratosDBUtils.getConnection();
			String sql = "SELECT * FROM CARTRIDGE_SUBSCRIPTION C left join REPOSITORY R on "
					+ "C.REPO_ID=R.REPO_ID left join DATA_CARTRIDGE D on "
					+ "D.DATA_CART_ID=C.DATA_CARTRIDGE_ID WHERE C.CLUSTER_DOMAIN=? AND C.STATE != 'UNSUBSCRIBED'";
			statement = con.prepareStatement(sql);
			statement.setString(1, clusterId);
			if (log.isDebugEnabled()) {
				log.debug("Executing query: " + sql);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				cartridgeSubscriptionInfo = new CartridgeSubscriptionInfo();
				populateSubscription(cartridgeSubscriptionInfo, resultSet);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}

		return cartridgeSubscriptionInfo;
	}
	
	
	public static void persistService(Service service) throws Exception {

		Connection con = null;
		PreparedStatement insertServiceStmt = null;

		String insertServiceSQL = "INSERT INTO SERVICE (TYPE, AUTOSCALING_POLICY,DEPLOYMENT_POLICY,TENANT_RANGE,"
				+ "CLUSTER_ID,HOST_NAME,SUBSCRIPTION_KEY)"
				+ " VALUES (?,?,?,?,?,?,?)";

		try {

			con = StratosDBUtils.getConnection();
			insertServiceStmt = con.prepareStatement(insertServiceSQL);
			insertServiceStmt.setString(1, service.getType());
			insertServiceStmt.setString(2, service.getAutoscalingPolicyName());
			insertServiceStmt.setString(3, service.getDeploymentPolicyName());
			insertServiceStmt.setString(4, service.getTenantRange());
			insertServiceStmt.setString(5, service.getClusterId());
			insertServiceStmt.setString(6, service.getHostName());
			insertServiceStmt.setString(7, service.getSubscriptionKey());
			insertServiceStmt.executeUpdate();
			con.commit();
			if (log.isDebugEnabled()) {
				log.debug(" Service " + service.getType() + " is inserted into DB");
			}
		} catch (Exception e) {
			String msg = "Error while sql connection :" + e.getMessage();
			log.error(msg, e);
			throw e;
		} finally {
			StratosDBUtils.closeStatement(insertServiceStmt);
		}

	} 
	
	
	public static Service getServiceFromCartridgeType(String cartridgeType) {
		return null;
	}

	

}
