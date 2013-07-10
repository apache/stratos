/*
 * Copyright 2013, WSO2, Inc. http://wso2.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.apache.stratos.adc.mgt.utils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscription;
import org.apache.stratos.adc.mgt.dao.DataCartridge;
import org.apache.stratos.adc.mgt.dao.PortMapping;
import org.apache.stratos.adc.mgt.dao.Repository;
import org.apache.stratos.adc.mgt.dao.RepositoryCredentials;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

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

	public static List<CartridgeSubscription> retrieveSubscribedCartridges(int tenantId) throws Exception {

		List<CartridgeSubscription> subscribedCartridgeList = new ArrayList<CartridgeSubscription>();
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
				CartridgeSubscription cartridge = new CartridgeSubscription();
				cartridge.setAlias(resultSet.getString("ALIAS"));
				cartridge.setCartridge(resultSet.getString("CARTRIDGE"));
				cartridge.setState(resultSet.getString("STATE"));
				cartridge.setClusterDomain(resultSet.getString("CLUSTER_DOMAIN"));
				cartridge.setClusterSubdomain(resultSet.getString("CLUSTER_SUBDOMAIN"));
				cartridge.setProvider(resultSet.getString("PROVIDER"));
				cartridge.setPolicy(resultSet.getString("POLICY"));
				cartridge.setMappedDomain(resultSet.getString("MAPPED_DOMAIN"));
				Repository repo = new Repository();
				repo.setRepoName(resultSet.getString("REPO_NAME"));
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
				repoCredentials.setPassword(decryptPassword(resultSet.getString("REPO_USER_PASSWORD")));
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

	public static int persistSubscription(CartridgeSubscription cartridgeSubscription) throws Exception {

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
			if (cartridgeSubscription.getRepository() != null) {
				String encryptedRepoUserPassword = encryptPassword(cartridgeSubscription.getRepository()
						.getRepoUserPassword());
				String insertRepo = "INSERT INTO REPOSITORY (REPO_NAME,STATE,REPO_USER_NAME,REPO_USER_PASSWORD)"
						+ " VALUES (?,?,?,?)";

				insertRepoStmt = con.prepareStatement(insertRepo, Statement.RETURN_GENERATED_KEYS);
				insertRepoStmt.setString(1, cartridgeSubscription.getRepository().getRepoName());
				insertRepoStmt.setString(2, "ACTIVE");
				insertRepoStmt.setString(3, cartridgeSubscription.getRepository().getRepoUserName());
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
			if (cartridgeSubscription.getDataCartridge() != null) {
				String insertDataCartridge = "INSERT INTO DATA_CARTRIDGE (TYPE,USER_NAME,PASSWORD,STATE)"
						+ " VALUES (?,?,?,?)";
				insertDataCartStmt = con.prepareStatement(insertDataCartridge, Statement.RETURN_GENERATED_KEYS);
				insertDataCartStmt.setString(1, cartridgeSubscription.getDataCartridge().getDataCartridgeType());
				insertDataCartStmt.setString(2, cartridgeSubscription.getDataCartridge().getUserName());
				insertDataCartStmt.setString(3, cartridgeSubscription.getDataCartridge().getPassword());
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

			String insertSubscription = "INSERT INTO CARTRIDGE_SUBSCRIPTION (TENANT_ID, CARTRIDGE, PROVIDER,"
					+ "HOSTNAME, POLICY, CLUSTER_DOMAIN, " + "CLUSTER_SUBDOMAIN, MGT_DOMAIN, MGT_SUBDOMAIN, STATE, "
					+ "ALIAS, TENANT_DOMAIN, BASE_DIR, REPO_ID, DATA_CARTRIDGE_ID)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			insertSubscriptionStmt = con.prepareStatement(insertSubscription, Statement.RETURN_GENERATED_KEYS);
			insertSubscriptionStmt.setInt(1, cartridgeSubscription.getTenantId());
			insertSubscriptionStmt.setString(2, cartridgeSubscription.getCartridge());
			insertSubscriptionStmt.setString(3, cartridgeSubscription.getProvider());
			insertSubscriptionStmt.setString(4, cartridgeSubscription.getHostName());
			insertSubscriptionStmt.setString(5, cartridgeSubscription.getPolicy());
			insertSubscriptionStmt.setString(6, cartridgeSubscription.getClusterDomain());
			insertSubscriptionStmt.setString(7, cartridgeSubscription.getClusterSubdomain());
			insertSubscriptionStmt.setString(8, cartridgeSubscription.getMgtClusterDomain());
			insertSubscriptionStmt.setString(9, cartridgeSubscription.getMgtClusterSubDomain());
			insertSubscriptionStmt.setString(10, cartridgeSubscription.getState());
			insertSubscriptionStmt.setString(11, cartridgeSubscription.getAlias());
			insertSubscriptionStmt.setString(12, cartridgeSubscription.getTenantDomain());
			insertSubscriptionStmt.setString(13, cartridgeSubscription.getBaseDirectory());
			insertSubscriptionStmt.setInt(14, repoId);
			insertSubscriptionStmt.setInt(15, dataCartridgeId);
			if (log.isDebugEnabled()) {
				log.debug("Executing insert: " + insertSubscription);
			}
			insertSubscriptionStmt.executeUpdate();
			res = insertSubscriptionStmt.getGeneratedKeys();
			if (res.next()) {
				cartridgeSubscriptionId = res.getInt(1);
			}

			List<PortMapping> portMapping = cartridgeSubscription.getPortMappings();
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

	public static CartridgeSubscription getSubscription(String tenantDomain, String alias) throws Exception {

		CartridgeSubscription cartridgeSubscription = null;
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
				cartridgeSubscription = new CartridgeSubscription();
				populateSubscription(cartridgeSubscription, resultSet);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw s;
		} finally {
			StratosDBUtils.closeAllConnections(con, statement, resultSet);
		}

		return cartridgeSubscription;
	}

	private static void populateSubscription(CartridgeSubscription cartridgeSubscription, ResultSet resultSet)
			throws Exception {
		String repoName = resultSet.getString("REPO_NAME");
		if (repoName != null) {
			Repository repo = new Repository();
			repo.setRepoName(repoName);
			cartridgeSubscription.setRepository(repo);
		}

		int dataCartridgeId = resultSet.getInt("DATA_CARTRIDGE_ID");
		if (dataCartridgeId != 0) {
			DataCartridge dataCartridge = new DataCartridge();
			dataCartridge.setDataCartridgeType(resultSet.getString("TYPE"));
			dataCartridge.setPassword(resultSet.getString("PASSWORD"));
			dataCartridge.setUserName(resultSet.getString("USER_NAME"));
			cartridgeSubscription.setDataCartridge(dataCartridge);
		}
		cartridgeSubscription.setPortMappings(getPortMappings(resultSet.getInt("SUBSCRIPTION_ID")));
		cartridgeSubscription.setTenantId(resultSet.getInt("TENANT_ID"));
		cartridgeSubscription.setState(resultSet.getString("STATE"));
		cartridgeSubscription.setPolicy(resultSet.getString("POLICY"));
		cartridgeSubscription.setCartridge(resultSet.getString("CARTRIDGE"));
		cartridgeSubscription.setAlias(resultSet.getString("ALIAS"));
		cartridgeSubscription.setClusterDomain(resultSet.getString("CLUSTER_DOMAIN"));
		cartridgeSubscription.setClusterSubdomain(resultSet.getString("CLUSTER_SUBDOMAIN"));
		cartridgeSubscription.setMgtClusterDomain(resultSet.getString("MGT_DOMAIN"));
		cartridgeSubscription.setMgtClusterSubDomain(resultSet.getString("MGT_SUBDOMAIN"));
		cartridgeSubscription.setProvider(resultSet.getString("PROVIDER"));
		cartridgeSubscription.setHostName(resultSet.getString("HOSTNAME"));
		cartridgeSubscription.setTenantDomain(resultSet.getString("TENANT_DOMAIN"));
		cartridgeSubscription.setBaseDirectory(resultSet.getString("BASE_DIR"));
		cartridgeSubscription.setSubscriptionId(resultSet.getInt("SUBSCRIPTION_ID"));
		cartridgeSubscription.setMappedDomain(resultSet.getString("MAPPED_DOMAIN"));
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

	public static List<CartridgeSubscription> getSubscription(String repositoryURL) throws Exception {

		List<CartridgeSubscription> subscriptionList = new ArrayList<CartridgeSubscription>();
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
				CartridgeSubscription cartridgeSubscription = new CartridgeSubscription();
				populateSubscription(cartridgeSubscription, resultSet);
				subscriptionList.add(cartridgeSubscription);
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

	public static String getSecurityKey() {
		String securityKey = CartridgeConstants.DEFAULT_SECURITY_KEY;
		OMElement documentElement = null;
		File xmlFile = new File(CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator + "conf"
				+ File.separator + CartridgeConstants.SECURITY_KEY_FILE);

		if (xmlFile.exists()) {
			try {
				documentElement = new StAXOMBuilder(xmlFile.getPath()).getDocumentElement();
			} catch (Exception ex) {
				String msg = "Error occurred when parsing the " + xmlFile.getPath() + ".";
				log.error(msg, ex);
				ex.printStackTrace();
			}
			if (documentElement != null) {
				Iterator<?> it = documentElement.getChildrenWithName(new QName(CartridgeConstants.SECURITY_KEY));
				if (it.hasNext()) {
					OMElement securityKeyElement = (OMElement) it.next();
					SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);
					String alias = securityKeyElement.getAttributeValue(new QName(CartridgeConstants.ALIAS_NAMESPACE,
							CartridgeConstants.ALIAS_LOCALPART, CartridgeConstants.ALIAS_PREFIX));

					if (secretResolver != null && secretResolver.isInitialized()
							&& secretResolver.isTokenProtected(alias)) {
						securityKey = "";
						securityKey = secretResolver.resolve(alias);
						// TODO : a proper testing on the secure vault protected
						// user defined encryption key
					}
				}
			}
		} else {
			System.out.println("No such file ezoxists");
		}
		return securityKey;
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

	private static String encryptPassword(String repoUserPassword) {
		String encryptPassword = "";
		String secret = getSecurityKey(); // secret key length must be 16
		SecretKey key;
		Cipher cipher;
		Base64 coder;
		key = new SecretKeySpec(secret.getBytes(), "AES");
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
			coder = new Base64();
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] cipherText = cipher.doFinal(repoUserPassword.getBytes());
			encryptPassword = new String(coder.encode(cipherText));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encryptPassword;
	}

	private static String decryptPassword(String repoUserPassword) {
		String decryptPassword = "";
		String secret = getSecurityKey(); // secret key length must be 16
		SecretKey key;
		Cipher cipher;
		Base64 coder;
		key = new SecretKeySpec(secret.getBytes(), "AES");
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
			coder = new Base64();
			byte[] encrypted = coder.decode(repoUserPassword.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = cipher.doFinal(encrypted);
			decryptPassword = new String(decrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decryptPassword;
	}

}
