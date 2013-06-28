package org.wso2.carbon.usage.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.constants.UsageConstants;
import org.wso2.carbon.usage.beans.BandwidthStatistics;
import org.wso2.carbon.usage.beans.RequestStatistics;
import org.wso2.carbon.usage.beans.TenantDataCapacity;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DataAccessObject {
    
    private static Log log = LogFactory.getLog(DataAccessObject.class);
    private DataSource dataSource;
    
    
    public DataAccessObject(){
        if(Util.getDataSourceService()!=null){
            try{
            this.dataSource = (DataSource)Util.getDataSourceService().
                                getDataSource(Util.BILLING_DATA_SOURCE_NAME).getDSObject();
            }catch(Exception e){
                log.error("Error occurred while obtaining " + Util.BILLING_DATA_SOURCE_NAME +
                        " datasource from data source service.", e);
                dataSource=null;
            }
        }else{
            log.error("Cannot obtain data source " + Util.BILLING_DATA_SOURCE_NAME + ". Datasource service is null");
            dataSource=null;
        }
    }


    public List<BandwidthStatistics> getHourlyBandwidthStats(int tenantId, Calendar startDate,
                                                        Calendar endDate) throws Exception{
        Connection connection = null;
        List<BandwidthStatistics> bwsList = new ArrayList<BandwidthStatistics>();

        try{
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM USAGE_HOURLY_ANALYTICS WHERE TENANT_ID = ? AND HOUR_FACT >= ? AND HOUR_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                String key = resultSet.getString("PAYLOAD_TYPE");
                BandwidthStatistics bws = new BandwidthStatistics(key);

                if(UsageConstants.SERVICE_INCOMING_BW.equals(key) ||
                        UsageConstants.WEBAPP_INCOMING_BW.equals(key) ||
                        UsageConstants.REGISTRY_INCOMING_BW.equals(key)){
                    bws.setIncomingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));

                }else if(UsageConstants.SERVICE_OUTGOING_BW.equals(key) ||
                        UsageConstants.WEBAPP_OUTGOING_BW.equals(key) ||
                        UsageConstants.REGISTRY_OUTGOING_BW.equals(key)){
                    bws.setOutgoingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));
                }else {
                    //Do nothing
                }

                bws.setServerUrl(resultSet.getString("SERVER_NAME"));

                bwsList.add(bws);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving hourly usage data from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return bwsList;

    }

    /**
     *
     * @param tenantId Tenant ID
     * @param startDate Start date - Stats of this date will be included
     * @param endDate End date - Stats of this date will be included
     * @return A list of BandwidthStatistics objects
     * @throws Exception
     */
    public List<BandwidthStatistics> getDailyBandwidthStats(int tenantId, Calendar startDate,
                                                        Calendar endDate) throws Exception{
        Connection connection = null;
        List<BandwidthStatistics> bwsList = new ArrayList<BandwidthStatistics>();

        try{
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM USAGE_DAILY_ANALYTICS WHERE TENANT_ID = ? AND DAY_FACT >= ? AND DAY_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                String key = resultSet.getString("PAYLOAD_TYPE");
                BandwidthStatistics bws = new BandwidthStatistics(key);
                
                if(UsageConstants.SERVICE_INCOMING_BW.equals(key) || 
                        UsageConstants.WEBAPP_INCOMING_BW.equals(key) ||
                        UsageConstants.REGISTRY_INCOMING_BW.equals(key)){
                    bws.setIncomingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));
                    
                }else if(UsageConstants.SERVICE_OUTGOING_BW.equals(key) || 
                        UsageConstants.WEBAPP_OUTGOING_BW.equals(key) ||
                        UsageConstants.REGISTRY_OUTGOING_BW.equals(key)){
                    bws.setOutgoingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));
                }else {
                    //Do nothing
                }
                
                bws.setServerUrl(resultSet.getString("SERVER_NAME"));
                
                bwsList.add(bws);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving daily usage data from the database. ", e);
            
        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return bwsList;

    }

    /**
     *
     * @param tenantId Tenant ID
     * @param month Stats of this month will be retrieved
     * @return A list of BandwidthStatistics objects
     * @throws Exception
     */
    public List<BandwidthStatistics> getMonthlyBandwidthStats(int tenantId,
                                                          Calendar month) throws Exception{
        Connection connection = null;
        List<BandwidthStatistics> bwsList = new ArrayList<BandwidthStatistics>();

        try{
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM USAGE_MONTHLY_ANALYTICS WHERE TENANT_ID = ? AND MONTH_FACT = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(month.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                String key = resultSet.getString("PAYLOAD_DATA");
                BandwidthStatistics bws = new BandwidthStatistics(key);

                if(UsageConstants.SERVICE_INCOMING_BW.equals(key) ||
                        UsageConstants.WEBAPP_INCOMING_BW.equals(key) ||
                        UsageConstants.REGISTRY_INCOMING_BW.equals(key)){
                    bws.setIncomingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));

                }else if(UsageConstants.SERVICE_OUTGOING_BW.equals(key) ||
                        UsageConstants.WEBAPP_OUTGOING_BW.equals(key) ||
                        UsageConstants.REGISTRY_OUTGOING_BW.equals(key)){
                    bws.setOutgoingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));
                }else {
                    //Do nothing
                }

                bws.setServerUrl(resultSet.getString("SERVER_NAME"));

                bwsList.add(bws);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving monthly usage data from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return bwsList;

    }

    /**
     *
     * @param tenantId Tenant ID
     * @param startDate Start date - Stats of this date will be included
     * @param endDate End date - Stats of this date will be included
     * @return A list of RequestStatistics objects
     * @throws Exception
     */
    public List<RequestStatistics> getHourlyRequestStats(int tenantId, Calendar startDate,
                                                         Calendar endDate) throws Exception{
        Connection connection = null;
        List<RequestStatistics> rsList = new ArrayList<RequestStatistics>();

        try{
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM SERVICE_STATS_HOURLY_ANALYTICS WHERE TENANT_ID = ? AND HOUR_FACT >= ? AND HOUR_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                String key = resultSet.getString("SERVER_NAME");
                RequestStatistics reqStat = new RequestStatistics(key);
                reqStat.setRequestCount(resultSet.getInt("REQUEST_COUNT"));
                reqStat.setResponseCount(resultSet.getInt("RESPONSE_COUNT"));
                reqStat.setFaultCount(resultSet.getInt("FAULT_COUNT"));

                rsList.add(reqStat);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving hourly service request stats from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return rsList;

    }


    /**
     *
     * @param tenantId Tenant ID
     * @param startDate Start date - Stats of this date will be included
     * @param endDate End date - Stats of this date will be included
     * @return A list of RequestStatistics objects
     * @throws Exception
     */
    public List<RequestStatistics> getDailyRequestStats(int tenantId, Calendar startDate,
                                                        Calendar endDate) throws Exception{
        Connection connection = null;
        List<RequestStatistics> rsList = new ArrayList<RequestStatistics>();

        try{
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM SERVICE_STATS_DAILY_ANALYTICS WHERE TENANT_ID = ? AND DAY_FACT >= ? AND DAY_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                String key = resultSet.getString("SERVER_NAME");
                RequestStatistics reqStat = new RequestStatistics(key);
                reqStat.setRequestCount(resultSet.getInt("REQUEST_COUNT"));
                reqStat.setResponseCount(resultSet.getInt("RESPONSE_COUNT"));
                reqStat.setFaultCount(resultSet.getInt("FAULT_COUNT"));

                rsList.add(reqStat);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving daily service request stats from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return rsList;

    }

    /**
     *
     * @param tenantId Tenant ID
     * @param month Month - Stats of this month will be retrieved
     * @return A list of RequestStatistics objects
     * @throws Exception
     */
    public List<RequestStatistics> getMonthlyRequestStats(int tenantId,
                                                          Calendar month) throws Exception{
        Connection connection = null;
        List<RequestStatistics> rsList = new ArrayList<RequestStatistics>();

        try{
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM SERVICE_STATS_MONTHLY_ANALYTICS WHERE TENANT_ID = ? AND MONTH_FACT = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(month.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                String key = resultSet.getString("SERVER_NAME");
                RequestStatistics reqStat = new RequestStatistics(key);
                reqStat.setRequestCount(resultSet.getInt("REQUEST_COUNT"));
                reqStat.setResponseCount(resultSet.getInt("RESPONSE_COUNT"));
                reqStat.setFaultCount(resultSet.getInt("FAULT_COUNT"));

                rsList.add(reqStat);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving monthly service request stats from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return rsList;

    }

    public TenantDataCapacity getTenantDataCapacity(int tenantId) throws Exception{
        Connection connection = null;
        TenantDataCapacity tenantDataCapacity = null;

        try{
            connection = dataSource.getConnection();
            String id = ""  + tenantId + "Final";
            String sql = "SELECT * FROM REGISTRY_USAGE_HOURLY_ANALYTICS WHERE ID = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                long currentCapacity = resultSet.getLong("CURRENT_USAGE");
                long historyCapacity = resultSet.getLong("HISTORY_USAGE");

                tenantDataCapacity = new TenantDataCapacity(currentCapacity, historyCapacity);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving registry data usage from . ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return tenantDataCapacity;
    }
}
