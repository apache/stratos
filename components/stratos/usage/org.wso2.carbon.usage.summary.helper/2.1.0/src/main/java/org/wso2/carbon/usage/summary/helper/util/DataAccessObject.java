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

package org.wso2.carbon.usage.summary.helper.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class to retrieve relevant timestamps from relational database tables.
 */
public class DataAccessObject {

    private static Log log = LogFactory.getLog(DataAccessObject.class);

    private static DataSource dataSource = null;

    private static DataAccessObject usageDataAccessObj = null;

    private DataAccessObject() {
    }

    public static DataAccessObject getInstance() throws Exception {
        if (usageDataAccessObj == null) {
            usageDataAccessObj = new DataAccessObject();
        }

        if (usageDataAccessObj.dataSource == null) {
            if (DataHolder.getDataSource() != null) {
                try {
                    dataSource = DataHolder.getDataSource();
                    //dataSource = (DataSource) DataHolder.getDataSourceService().
                    //       getDataSource(DataHolder.BILLING_DATA_SOURCE_NAME).getDSObject();
                } catch (Exception e) {
                    log.error("Error occurred while obtaining " + DataHolder.BILLING_DATA_SOURCE_NAME +
                              " datasource from data source service.", e);
                    throw new Exception(e);
                }
            } else {
                log.error("Cannot obtain data source " + DataHolder.BILLING_DATA_SOURCE_NAME +
                          ". Datasource service is null");
                throw new Exception("Datasource service not available");
            }

        }

        return usageDataAccessObj;
    }

    public String getAndUpdateLastUsageHourlyTimestamp() throws SQLException {

        Timestamp lastSummaryTs = null;
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT TIMESTMP FROM USAGE_LAST_HOURLY_TS WHERE ID='LatestTS'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                lastSummaryTs = resultSet.getTimestamp("TIMESTMP");
            } else {
                lastSummaryTs = new Timestamp(0);
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
            Timestamp currentTs = Timestamp.valueOf(formatter.format(new Date()));

            String currentSql = "INSERT INTO USAGE_LAST_HOURLY_TS (ID, TIMESTMP) VALUES('LatestTS',?) ON DUPLICATE KEY UPDATE TIMESTMP=?";
            PreparedStatement ps1 = connection.prepareStatement(currentSql);
            ps1.setTimestamp(1, currentTs);
            ps1.setTimestamp(2, currentTs);
            ps1.execute();

        } catch (SQLException e) {
            log.error("Error occurred while trying to get and update the last hourly timestamp. ", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return lastSummaryTs.toString();
    }

    public String getAndUpdateLastUsageDailyTimestamp() throws SQLException {

        Timestamp lastSummaryTs = null;
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT TIMESTMP FROM USAGE_LAST_DAILY_TS WHERE ID='LatestTS'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                lastSummaryTs = resultSet.getTimestamp("TIMESTMP");
            } else {
                lastSummaryTs = new Timestamp(0);
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
            Timestamp currentTs = Timestamp.valueOf(formatter.format(new Date()));

            String currentSql = "INSERT INTO USAGE_LAST_DAILY_TS (ID, TIMESTMP) VALUES('LatestTS',?) ON DUPLICATE KEY UPDATE TIMESTMP=?";
            PreparedStatement ps1 = connection.prepareStatement(currentSql);
            ps1.setTimestamp(1, currentTs);
            ps1.setTimestamp(2, currentTs);
            ps1.execute();

        } catch (SQLException e) {
            log.error("Error occurred while trying to get and update the last daily timestamp. ", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return lastSummaryTs.toString();
    }

    public String getAndUpdateLastUsageMonthlyTimestamp() throws SQLException {

        Timestamp lastSummaryTs = null;
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT TIMESTMP FROM USAGE_LAST_MONTHLY_TS WHERE ID='LatestTS'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                lastSummaryTs = resultSet.getTimestamp("TIMESTMP");
            } else {
                lastSummaryTs = new Timestamp(0);
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
            Timestamp currentTs = Timestamp.valueOf(formatter.format(new Date()));

            String currentSql = "INSERT INTO USAGE_LAST_MONTHLY_TS (ID, TIMESTMP) VALUES('LatestTS',?) ON DUPLICATE KEY UPDATE TIMESTMP=?";
            PreparedStatement ps1 = connection.prepareStatement(currentSql);
            ps1.setTimestamp(1, currentTs);
            ps1.setTimestamp(2, currentTs);
            ps1.execute();

        } catch (SQLException e) {
            log.error("Error occurred while trying to get and update the last monthly timestamp. ", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return lastSummaryTs.toString();
    }

    public String getAndUpdateLastServiceStatsHourlyTimestamp() throws SQLException {

        Timestamp lastSummaryTs = null;
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT TIMESTMP FROM SERVICE_STATS_LAST_HOURLY_TS WHERE ID='LatestTS'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                lastSummaryTs = resultSet.getTimestamp("TIMESTMP");
            } else {
                lastSummaryTs = new Timestamp(0);
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
            Timestamp currentTs = Timestamp.valueOf(formatter.format(new Date()));

            String currentSql = "INSERT INTO SERVICE_STATS_LAST_HOURLY_TS (ID, TIMESTMP) VALUES('LatestTS',?) ON DUPLICATE KEY UPDATE TIMESTMP=?";
            PreparedStatement ps1 = connection.prepareStatement(currentSql);
            ps1.setTimestamp(1, currentTs);
            ps1.setTimestamp(2, currentTs);
            ps1.execute();

        } catch (SQLException e) {
            log.error("Error occurred while trying to get and update the last hourly timestamp. ", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return lastSummaryTs.toString();
    }

    public String getAndUpdateLastServiceStatsDailyTimestamp() throws SQLException {

        Timestamp lastSummaryTs = null;
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT TIMESTMP FROM SERVICE_STATS_LAST_DAILY_TS WHERE ID='LatestTS'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                lastSummaryTs = resultSet.getTimestamp("TIMESTMP");
            } else {
                lastSummaryTs = new Timestamp(0);
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
            Timestamp currentTs = Timestamp.valueOf(formatter.format(new Date()));

            String currentSql = "INSERT INTO SERVICE_STATS_LAST_DAILY_TS (ID, TIMESTMP) VALUES('LatestTS',?) ON DUPLICATE KEY UPDATE TIMESTMP=?";
            PreparedStatement ps1 = connection.prepareStatement(currentSql);
            ps1.setTimestamp(1, currentTs);
            ps1.setTimestamp(2, currentTs);
            ps1.execute();

        } catch (SQLException e) {
            log.error("Error occurred while trying to get and update the last daily timestamp. ", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return lastSummaryTs.toString();
    }

    public String getAndUpdateLastServiceStatsMonthlyTimestamp() throws SQLException {

        Timestamp lastSummaryTs = null;
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT TIMESTMP FROM SERVICE_STATS_LAST_MONTHLY_TS WHERE ID='LatestTS'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                lastSummaryTs = resultSet.getTimestamp("TIMESTMP");
            } else {
                lastSummaryTs = new Timestamp(0);
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
            Timestamp currentTs = Timestamp.valueOf(formatter.format(new Date()));

            String currentSql = "INSERT INTO SERVICE_STATS_LAST_MONTHLY_TS (ID, TIMESTMP) VALUES('LatestTS',?) ON DUPLICATE KEY UPDATE TIMESTMP=?";
            PreparedStatement ps1 = connection.prepareStatement(currentSql);
            ps1.setTimestamp(1, currentTs);
            ps1.setTimestamp(2, currentTs);
            ps1.execute();

        } catch (SQLException e) {
            log.error("Error occurred while trying to get and update the last monthly timestamp. ", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return lastSummaryTs.toString();
    }

}
