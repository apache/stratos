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

package org.apache.stratos.status.monitor.core.jdbc;

import org.apache.stratos.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.apache.stratos.status.monitor.core.constants.StatusMonitorConstants;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

/**
 * The class connecting with the mysql database
 */
public class MySQLConnectionInitializer {
    private static Connection conn = null;
    private static BasicDataSource dataSource;
    private static final Log log = LogFactory.getLog(StatusMonitorConfigurationBuilder.class);

    private static List<String> serviceList = new ArrayList<String>();
    private static List<String> statusList = new ArrayList<String>();

    /**
     * gets a copy of services list
     *
     * @return an unmodifiable list.
     */
    public static List<String> getServiceList() {
        return Collections.unmodifiableList(serviceList);
    }

    /**
     * gets a copy of statuses list
     *
     * @return an unmodifiable list.
     */
    public static List<String> getStatusList() {
        return Collections.unmodifiableList(statusList);
    }

    /**
     * gets the sql connection and initializes the MySQLConnectionInitializer.
     *
     * @return Static Connection
     * @throws Exception, throws exception       MySQLConnectionInitializer
     */
    public static Connection initialize() throws Exception {
        //gets the data source from the configuration builder.
        dataSource = StatusMonitorConfigurationBuilder.getDataSource();

        //gets the sql connection.
        getConnection();

        //initializes the service and state lists.
        serviceList = getServiceNamesList();
        statusList = getStateNameList();

        if (log.isDebugEnabled()) {
            log.debug("Successfully initialized the mysql connection");
        }
        return conn;
    }

    /**
     * Gets the SQL Connection to the status monitor database
     *
     * @return a Connection object
     * @throws Exception, if getting the connection failed.
     */
    private static Connection getConnection() throws Exception {
        try {
            String userName = dataSource.getUsername(); //monitor
            String password = dataSource.getPassword();   //monitor
            String url = dataSource.getUrl(); //jdbc:mysql://localhost:3306/stratos_status
            String driverName = dataSource.getDriverClassName();

            Class.forName(driverName).newInstance();
            conn = DriverManager.getConnection(url, userName, password);

            if (conn != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Connection Successful");
                }
            }
        } catch (SQLException e) {
            String msg = "SQL connection to the health monitor database instance failed";
            log.error(msg, e);
            throw new Exception(msg, e);
        } catch (Exception e) {
            String msg = "Connection to the health monitor database instance failed";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        return conn;
    }

    /**
     * Gets the list of available services
     *
     * @return List of services
     * @throws SQLException, if getting the service name failed.
     */
    private static List<String> getServiceNamesList() throws SQLException {
        List<String> serviceList = new ArrayList<String>();
        ResultSet rs;
        Statement stmtCon = conn.createStatement();
        String sql = StatusMonitorConstants.GET_SERVICE_NAME_SQL;

        stmtCon.executeQuery(sql);
        rs = stmtCon.getResultSet();
        String serviceName;
        try {
            while (rs.next()) {
                serviceName = rs.getString(StatusMonitorConstants.NAME);
                serviceList.add(serviceName);
            }
        } catch (SQLException e) {
            String msg = "Getting the service name list failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            rs.close();
            stmtCon.close();
        }
        return serviceList;
    }

    /**
     * Gets state name with the given state id
     *
     * @return state name
     *         {Up & Running, Broken, Down, Fixed. }
     * @throws java.sql.SQLException, if the retrieval of the list of states failed.
     */
    private static List<String> getStateNameList() throws SQLException {
        List<String> stateList = new ArrayList<String>();
        ResultSet rs;
        Statement stmtCon = conn.createStatement();
        String sql = StatusMonitorConstants.GET_STATE_NAME_SQL;

        stmtCon.executeQuery(sql);
        rs = stmtCon.getResultSet();
        String stateName;
        try {
            while (rs.next()) {
                stateName = rs.getString(StatusMonitorConstants.NAME);
                stateList.add(stateName);
            }
        } catch (SQLException e) {
            String msg = "Getting the serviceID failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            rs.close();
            stmtCon.close();
        }
        return stateList;
    }

    /**
     * Gets the ServiceID, for the given product.
     *
     * @param product, name of the service/product
     * @return serviceID: int
     * @throws SQLException, if retrieving the service id failed.
     */
    public static int getServiceID(String product) throws SQLException {
        Statement stmtCon = conn.createStatement();
        String sql = StatusMonitorConstants.GET_SERVICE_ID_SQL_WSL_NAME_LIKE_SQL + "\"" + product + "\"";
        int serviceId = 0;
        ResultSet rs = stmtCon.getResultSet();

        try {
            stmtCon.executeQuery(sql);
            while (rs.next()) {
                serviceId = rs.getInt(StatusMonitorConstants.ID);
            }
        } catch (SQLException e) {
            String msg = "Getting the serviceID failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            rs.close();
            stmtCon.close();
        }
        return serviceId;
    }

    /**
     * Gets the ServiceStateID, for the given product/service.
     *
     * @param serviceID: int; ID of the service/product
     * @return serviceStateID: int
     * @throws SQLException, if retrieving the service id failed.
     */
    public static int getServiceStateID(int serviceID) throws SQLException {
        ResultSet rs;
        Statement stmtCon = conn.createStatement();
        String sql = StatusMonitorConstants.GET_SERVICE_STATE_ID_SQL + serviceID +
                StatusMonitorConstants.ORDER_BY_TIMESTAMP_SQL;

        stmtCon.executeQuery(sql);
        rs = stmtCon.getResultSet();
        int stateID = 0;
        try {
            while (rs.next()) {
                stateID = rs.getInt(StatusMonitorConstants.ID);
            }
        } catch (SQLException e) {
            String msg = "Getting the service state ID failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            rs.close();
            stmtCon.close();
        }
        return stateID;
    }
}


