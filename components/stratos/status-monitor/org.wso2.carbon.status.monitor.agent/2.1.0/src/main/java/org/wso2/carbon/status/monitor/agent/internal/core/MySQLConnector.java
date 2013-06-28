/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.status.monitor.agent.internal.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The class connecting with the mysql database for the Status Monitor Agent
 */
public class MySQLConnector {
    private static Connection conn;
    private static final Log log = LogFactory.getLog(MySQLConnector.class);

    private static List<String> serviceList = new ArrayList<String>();
    private static List<String> statusList = new ArrayList<String>();

    private static int resolvedNotFixed = 0;
    private static int resolvedWSLID;

    /**
     * gets the sql connection and initializes the MySQLConnectionInitializer.
     *
     * @return Static Connection
     * @throws Exception, throws exception
     */
    public static Connection initialize() throws Exception {
        //gets the sql connection.
        conn = MySQLConnectionInitializer.initialize();

        //initializes the service and state lists.
        serviceList = MySQLConnectionInitializer.getServiceList();
        statusList = MySQLConnectionInitializer.getStatusList();

        if (log.isDebugEnabled()) {
            log.debug("Connection to the status database is initialized from status.monitor");
        }

        return conn;
    }

    /**
     * Inserts into the heartbeats table
     *
     * @param serviceID serviceId
     * @param status    - status of the service
     * @throws SQLException, if inserting the stats failed
     */
    public static void insertStats(int serviceID, Boolean status) throws SQLException {
        String sql = StatusMonitorConstants.INSERT_STAT_SQL;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        try {
            pstmt.setString(1, null);
            pstmt.setInt(2, serviceID);
            pstmt.setBoolean(3, status);
            pstmt.setString(4, null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String msg = "Inserting stats failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            pstmt.close();
        }
    }

    /**
     * Inserting state into the state table.
     *
     * @param serviceID, service id
     * @param status,    status of the service {"Up & Running", "Broken", "Down", and "Fixed"}
     * @param details,   the service state details.
     * @throws SQLException, if writing to the database failed.
     */
    public static void insertState(int serviceID, Boolean status, String details) throws SQLException {

        int stateID = MySQLConnectionInitializer.getServiceStateID(serviceID);
        if (!status) {
            insertStateDetails(stateID, status, details);
        }

        // boolean insertStatus = getInsertStatus(serviceID);
        if (/*insertStatus & */(resolvedNotFixed == 0 || resolvedNotFixed == 1)) {
            if (log.isDebugEnabled()) {
                log.debug("Inserting data into the state database");
            }
            String sql = StatusMonitorConstants.INSERT_STATE_SQL;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            try {

                pstmt.setString(1, null);
                pstmt.setInt(2, serviceID);
                if (status) {
                    pstmt.setInt(3, 1);
                } else {
                    pstmt.setInt(3, 2);
                }
                pstmt.setString(4, null);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "Inserting state failed";
                log.error(msg, e);
                throw new SQLException(msg, e);
            } finally {
                resolvedWSLID = 0;
                resolvedNotFixed = 0;
                pstmt.close();
            }
        }

        if (/*insertStatus & */resolvedNotFixed == 2) {
            String sql = StatusMonitorConstants.UPDATE_STATE_SQL;
            PreparedStatement pstmtUpdate = conn.prepareStatement(sql);
            try {
                if (status) {
                    pstmtUpdate.setInt(1, 1);
                } else {
                    pstmtUpdate.setInt(1, 2);
                }
                pstmtUpdate.setInt(2, resolvedWSLID);
                pstmtUpdate.executeUpdate();
            } catch (SQLException e) {
                String msg = "Inserting state failed";
                log.error(msg, e);
                throw new SQLException(msg, e);
            } finally {
                resolvedNotFixed = 0;
                resolvedWSLID = 0;
                pstmtUpdate.close();
            }
        }
    }

    /**
     * Inserts the state details into the
     *
     * @param serviceStateID, service state ID
     * @param status,         boolean: status of the service
     * @param detail,         service state detail
     * @throws SQLException, if writing to the database failed.
     */
    public static void insertStateDetails(int serviceStateID, boolean status, String detail) throws SQLException {

        String sql = StatusMonitorConstants.INSERT_STATE_DETAIL_SQL;
        PreparedStatement pstmt = conn.prepareStatement(sql);

        try {
            pstmt.setString(1, null);
            pstmt.setInt(2, serviceStateID);
            if (!status) {
                pstmt.setString(3, detail);
            }
            pstmt.setString(4, null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String msg = "Inserting state details failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            pstmt.close();
        }
    }

    /**
     * Gets the insert status
     *
     * @param ServiceID, id of the service
     * @return true, if insert status was successful
     * @throws SQLException, if writing to the database failed.
     */
    public static boolean getInsertStatus(int ServiceID) throws SQLException {

        ResultSet rs = null;
        Statement stmtCon = null;
        boolean currentStatus = false;
        String sqlGetStateID = StatusMonitorConstants.SELECT_ALL_FROM_WSL_SERVICE_STATE_SQL + ServiceID +
                StatusMonitorConstants.ORDER_BY_TIMESTAMP_SQL_DESC_LIMIT_01_SQL;

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date SystemDate = new Date();
        dateFormat.format(SystemDate);

        int state_id;
        Date date;
        try {
            stmtCon = conn.createStatement();
            stmtCon.executeQuery(sqlGetStateID);
            rs = stmtCon.getResultSet();
            if (rs != null) {

                while (rs.next()) {
                    state_id = rs.getInt(StatusMonitorConstants.STATE_ID);

                    if (state_id == 1) {
                        if (log.isDebugEnabled()) {
                            log.debug("Up and Running :" + state_id);
                        }
                        currentStatus = true;
                    }

                    if (state_id == 2) {
                        if (log.isDebugEnabled()) {
                            log.debug("Broken :" + state_id);
                        }
                        currentStatus = true;
                    }

                    if (state_id == 4) {
                        currentStatus = true;
                        date = rs.getTimestamp(StatusMonitorConstants.TIMESTAMP);
                        resolvedWSLID = rs.getInt(StatusMonitorConstants.ID);

                        long currentTimeMs = SystemDate.getTime();
                        long resolvedTimeMs = date.getTime();

                        double time_diff = ((currentTimeMs - resolvedTimeMs) / (double) StatusMonitorConstants.HOUR_IN_MILLIS);
                        if (log.isDebugEnabled()) {
                            log.debug("State ID: " + state_id);
                        }
                        if (time_diff >= 1.0) {
                            resolvedNotFixed = 1;
                        } else {
                            resolvedNotFixed = 2;
                        }
                    }
                }

            } else {
                currentStatus = true;
            }
        } catch (SQLException e) {
            String msg = "Getting Insert state failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmtCon != null) {
                stmtCon.close();
            }
        }
        return currentStatus;
    }
}


