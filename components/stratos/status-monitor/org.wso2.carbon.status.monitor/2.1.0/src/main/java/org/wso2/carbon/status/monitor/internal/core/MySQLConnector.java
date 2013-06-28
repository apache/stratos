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

package org.wso2.carbon.status.monitor.internal.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.status.monitor.beans.ServiceStateDetailInfoBean;
import org.wso2.carbon.status.monitor.beans.ServiceStateInfoBean;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * The class connecting with the mysql database for the Status Monitor Back end
 */
public class MySQLConnector {
    private static Connection conn;
    private static final Log log = LogFactory.getLog(MySQLConnector.class);

    private static List<String> serviceList = new ArrayList<String>();
    private static List<String> statusList = new ArrayList<String>();

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
     * Gets the service state ID from the service ID
     *
     * @param serviceID: int
     * @return service state ID
     * @throws java.sql.SQLException, if the retrieval of the service state failed.
     */
    public static ServiceStateInfoBean getServiceState(int serviceID) throws SQLException {
        ResultSet rs;
        Statement stmtCon = conn.createStatement();
        String sql = StatusMonitorConstants.GET_SERVICE_STATE_SQL + serviceID +
                StatusMonitorConstants.ORDER_BY_TIMESTAMP_SQL;

        stmtCon.executeQuery(sql);
        rs = stmtCon.getResultSet();
        int stateID;
        Timestamp date;
        ServiceStateInfoBean serviceStateInfoBean = new ServiceStateInfoBean();

        try {
            while (rs.next()) {
                stateID = rs.getInt(StatusMonitorConstants.STATE_ID);
                date = rs.getTimestamp(StatusMonitorConstants.TIMESTAMP);
                serviceStateInfoBean.setDate(date.getTime());
                serviceStateInfoBean.setService(serviceList.get(serviceID - 1));
                serviceStateInfoBean.setServiceID(serviceID);
                serviceStateInfoBean.setServiceState(statusList.get(stateID - 1));
            }
        } catch (SQLException e) {
            String msg = "Getting the service state failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            rs.close();
            stmtCon.close();
        }
        return serviceStateInfoBean;
    }

    /**
     * Gets the list of all the service state details.
     *
     * @return the list of the service state details.
     * @throws Exception, if the retrieval of the service state details failed.
     */
    public static List<ServiceStateDetailInfoBean> getAllServiceStateDetail() throws Exception {
        List<ServiceStateDetailInfoBean> stateDetailList = new ArrayList<ServiceStateDetailInfoBean>();

        ResultSet rs;
        Statement stmtCon = conn.createStatement();
        String sql = StatusMonitorConstants.GET_ALL_STATE_DETAIL_SQL;
        stmtCon.executeQuery(sql);
        rs = stmtCon.getResultSet();
        String service;
        String serviceStateDetail;
        Timestamp stateLoggedTime;
        Timestamp detailLoggedTime;

        ServiceStateDetailInfoBean serviceStateDetailInfoBean;

        try {
            while (rs.next()) {
                serviceStateDetailInfoBean = new ServiceStateDetailInfoBean();

                service = rs.getString(StatusMonitorConstants.SERVICE_WSL_NAME);
                stateLoggedTime = rs.getTimestamp(StatusMonitorConstants.SERVICE_STATE_WSL_TIMESTAMP);
                detailLoggedTime =
                        rs.getTimestamp(StatusMonitorConstants.SERVICE_STATE_DETAIL_WSL_TIMESTAMP);
                serviceStateDetail = rs.getString(StatusMonitorConstants.SERVICE_STATE_DETAIL);

                serviceStateDetailInfoBean.setService(service);
                serviceStateDetailInfoBean.setStateLoggedTime(stateLoggedTime.getTime());
                serviceStateDetailInfoBean.setServiceStateDetail(serviceStateDetail);
                serviceStateDetailInfoBean.setDetailLoggedTime(detailLoggedTime.getTime());

                stateDetailList.add(serviceStateDetailInfoBean);
            }
        } catch (SQLException e) {
            String msg = "Getting the serviceID failed";
            log.error(msg, e);
            throw new SQLException(msg, e);
        } finally {
            rs.close();
            stmtCon.close();
        }
        return stateDetailList;
    }

    /**
     * Gets the list of all the service state.
     *
     * @return list of ServiceStateInfoBean.
     * @throws Exception, if the retrieval of the list of service state infobean failed.
     */
    public static List<ServiceStateInfoBean> getAllServiceState() throws Exception {
        List<ServiceStateInfoBean> serviceStateInfoBeanList = new ArrayList<ServiceStateInfoBean>();
        for (int serviceID = 1; serviceID <= serviceList.size(); serviceID++) {
            serviceStateInfoBeanList.add(getServiceState(serviceID));
        }
        return serviceStateInfoBeanList;
    }
}


