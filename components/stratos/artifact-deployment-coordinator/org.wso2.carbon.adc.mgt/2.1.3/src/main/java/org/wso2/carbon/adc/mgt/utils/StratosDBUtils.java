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
package org.wso2.carbon.adc.mgt.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

public final class StratosDBUtils {

	private static final Log log = LogFactory.getLog(StratosDBUtils.class);

	private static volatile javax.sql.DataSource dataSource = null;

	/**
	 * Initializes the data source
	 * 
	 * @throws RuntimeException
	 *             if an error occurs while loading DB configuration
	 */
	public static void initialize() throws Exception {
		if (dataSource != null) {
			return;
		}

		synchronized (StratosDBUtils.class) {
			if (dataSource == null) {

				String datasourceName = System.getProperty(CartridgeConstants.DB_DATASOURCE);

				if (datasourceName != null && datasourceName.trim().length() > 0) {
					if (log.isInfoEnabled()) {
						log.info("Initializing data source: " + datasourceName);
					}
					try {
						Context ctx = new InitialContext();
						dataSource = (DataSource) ctx.lookup(datasourceName);
						if (dataSource != null && log.isInfoEnabled()) {
							log.info("Found data source: " + datasourceName + ", " + dataSource.getClass().getName());
						}
					} catch (NamingException e) {
						throw new RuntimeException("Error while looking up the data source: " + datasourceName, e);
					}
				} else {
					// FIXME Should we use system properties to get database
					// details?
					String dbUrl = System.getProperty(CartridgeConstants.DB_URL);
					String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
					String username = System.getProperty(CartridgeConstants.DB_USERNAME);
					String password = System.getProperty(CartridgeConstants.DB_PASSWORD);

					if (dbUrl == null || driver == null || username == null || password == null) {
						String msg = "Required DB configuration parameters are not specified.";
						log.warn(msg);
						throw new RuntimeException(msg);
					}
					
					if (log.isInfoEnabled()) {
						log.info("Initializing data source for JDBC URL: " + dbUrl);
					}

					PoolProperties p = new PoolProperties();
					p.setUrl(dbUrl);
					p.setDriverClassName(driver);
					p.setUsername(username);
					p.setPassword(password);
					p.setJmxEnabled(true);
					p.setTestWhileIdle(false);
					p.setTestOnBorrow(true);
					p.setValidationQuery("SELECT 1");
					p.setTestOnReturn(false);
					p.setValidationInterval(30000);
					p.setTimeBetweenEvictionRunsMillis(30000);
					p.setMaxActive(100);
					p.setInitialSize(10);
					p.setMaxWait(10000);
					p.setRemoveAbandonedTimeout(60);
					p.setMinEvictableIdleTimeMillis(30000);
					p.setMinIdle(10);
					p.setLogAbandoned(true);
					p.setRemoveAbandoned(true);
					p.setDefaultAutoCommit(false);
					p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
							+ "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
					DataSource tomcatDatasource = new DataSource();
					tomcatDatasource.setPoolProperties(p);

					dataSource = tomcatDatasource;
				}

			}
		}
	}

	/**
	 * Utility method to get a new database connection
	 * 
	 * @return Connection
	 * @throws java.sql.SQLException
	 *             if failed to get Connection
	 */
	public static Connection getConnection() throws SQLException {
		if (dataSource != null) {
			return dataSource.getConnection();
		}
		throw new SQLException("Datasource is not configured properly.");
	}

	/**
	 * Utility method to close the connection streams.
	 * 
	 * @param connection
	 *            Connection
	 * @param preparedStatement
	 *            PreparedStatement
	 * @param resultSet
	 *            ResultSet
	 */
	public static void closeAllConnections(Connection connection, PreparedStatement preparedStatement,
			ResultSet resultSet) {
		closeResultSet(resultSet);
		closeStatement(preparedStatement);
		closeConnection(connection);
	}

	public static void closeAllConnections(Connection connection, PreparedStatement... preparedStatements) {
		for (PreparedStatement preparedStatement : preparedStatements) {
			closeStatement(preparedStatement);
		}
		closeConnection(connection);
	}

	/**
	 * Close Connection
	 * 
	 * @param dbConnection
	 *            Connection
	 */
	public static void closeConnection(Connection dbConnection) {
		if (dbConnection != null) {
			try {
				dbConnection.close();
			} catch (SQLException e) {
				log.warn(
						"Database error. Could not close database connection. Continuing with " + "others. - "
								+ e.getMessage(), e);
			}
		}
	}

	/**
	 * Close ResultSet
	 * 
	 * @param resultSet
	 *            ResultSet
	 */
	public static void closeResultSet(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				log.warn("Database error. Could not close ResultSet  - " + e.getMessage(), e);
			}
		}

	}

	/**
	 * Close PreparedStatement
	 * 
	 * @param preparedStatement
	 *            PreparedStatement
	 */
	public static void closeStatement(PreparedStatement preparedStatement) {
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (SQLException e) {
				log.warn(
						"Database error. Could not close PreparedStatement. Continuing with" + " others. - "
								+ e.getMessage(), e);
			}
		}

	}

}
