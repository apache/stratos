package org.wso2.carbon.usage.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.constants.UsageConstants;
import org.wso2.carbon.usage.beans.*;

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


    /**
     *
     * @param tenantId Tenant ID
     * @param startDate Start date - Stats of this date will be included
     * @param endDate End date - Stats of this date will be included
     * @return A list of CartridgeStatistics objects
     * @throws Exception
     */
    public List<CartridgeStatistics> getHourlyCartridgeStats(int tenantId, Calendar startDate,
                                                         Calendar endDate) throws Exception{
        Connection connection = null;
        List<CartridgeStatistics> csList = new ArrayList<CartridgeStatistics>();

        try{
            connection = dataSource.getConnection();

            String sql = "SELECT * FROM CARTRIDGE_STATS_HOURLY_ANALYTICS WHERE TENANT_ID = ? AND HOUR_FACT >= ? AND HOUR_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, String.valueOf(tenantId));
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                CartridgeStatistics cs = new CartridgeStatistics();
                cs.setInstanceId(resultSet.getString("IMAGE_ID"));
                cs.setCartridgeHours(resultSet.getInt("DURATION_HOURS"));
                cs.setKey(resultSet.getString("CARTRIDGE_TYPE") + " - " + resultSet.getString("NODE_ID"));

                csList.add(cs);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving hourly cartridge stats from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return csList;

    }


    /**
     *
     * @param tenantId Tenant ID
     * @param startDate Start date - Stats of this date will be included
     * @param endDate End date - Stats of this date will be included
     * @return A list of RequestStatistics objects
     * @throws Exception
     */
    public List<CartridgeStatistics> getDailyCartridgeStats(int tenantId, Calendar startDate,
                                                        Calendar endDate) throws Exception{
        Connection connection = null;
        List<CartridgeStatistics> csList = new ArrayList<CartridgeStatistics>();

        try{
            connection = dataSource.getConnection();
            //TODO: Implement the SQL logic
            String sql = "SELECT * FROM CARTRIDGE_STATS_DAILY_ANALYTICS WHERE TENANT_ID = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, String.valueOf(tenantId));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                CartridgeStatistics cs = new CartridgeStatistics();
                cs.setInstanceId(resultSet.getString("IMAGE_ID"));
                cs.setCartridgeHours(resultSet.getInt("DURATION_HOURS"));
                cs.setKey(resultSet.getString("CARTRIDGE_TYPE") + " - " + resultSet.getString("NODE_ID"));

                csList.add(cs);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving daily cartridge stats from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return csList;

    }

    /**
     *
     * @param tenantId Tenant ID
     * @param month Month - Stats of this month will be retrieved
     * @return A list of RequestStatistics objects
     * @throws Exception
     */
    public List<CartridgeStatistics> getMonthlyCartridgeStats(int tenantId,
                                                          Calendar month) throws Exception{
        Connection connection = null;
        List<CartridgeStatistics> csList = new ArrayList<CartridgeStatistics>();

        try{
            connection = dataSource.getConnection();
            //TODO: Implement SQL logic
            String sql = "SELECT * FROM CARTRIDGE_STATS_MONTHLY_ANALYTICS WHERE TENANT_ID = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, String.valueOf(tenantId));
            ResultSet resultSet = ps.executeQuery();

            while(resultSet.next()){
                CartridgeStatistics cs = new CartridgeStatistics();
                cs.setInstanceId(resultSet.getString("NODE_ID"));
                cs.setCartridgeHours(resultSet.getInt("DURATION_HOURS"));
                cs.setKey(resultSet.getString("IMAGE_ID"));

                csList.add(cs);
            }
        }catch(SQLException e){
            log.error("Error occurred while retrieving monthly cartridge stats from the database. ", e);

        }finally {
            if(connection!=null){
                connection.close();
            }
        }

        return csList;

    }

    /**
     * @param tenantId  Tenant Id of associated tenant
     * @param startDate Start Date start time stamp of hour
     * @param endDate   End date end time stamp of hour
     * @return APIManagerUsageStats objects
     * @throws Exception
     */
    public List<APIManagerUsageStats> getHourlyAPIManagerUsageStats(int tenantId, Calendar startDate,
                                                                    Calendar endDate) throws Exception {
        Connection connection = null;
        List<APIManagerUsageStats> bwsList = new ArrayList<APIManagerUsageStats>();

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM USAGE_HOURLY_ANALYTICS WHERE TENANT_ID = ? AND HOUR_FACT >= ? AND HOUR_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                String key = resultSet.getString("PAYLOAD_TYPE");
                APIManagerUsageStats stats = new APIManagerUsageStats(key);

                if (UsageConstants.API_CALL_COUNT.equals(key)) {
                    stats.setIncomingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));

                } else {
                    //Do nothing
                }

                stats.setServerUrl(resultSet.getString("SERVER_NAME"));
                bwsList.add(stats);
            }
        } catch (SQLException e) {
            log.error("Error occurred while retrieving hourly usage data from the database. ", e);

        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return bwsList;

    }

    /**
     * @param tenantId  Tenant ID
     * @param startDate Start date - Stats of this date will be included
     * @param endDate   End date - Stats of this date will be included
     * @return A list of APIManagerUsageStats objects
     * @throws Exception
     */
    public List<APIManagerUsageStats> getDailyAPIManagerUsageStats(int tenantId, Calendar startDate,
                                                                   Calendar endDate) throws Exception {
        Connection connection = null;
        List<APIManagerUsageStats> bwsList = new ArrayList<APIManagerUsageStats>();

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM USAGE_DAILY_ANALYTICS WHERE TENANT_ID = ? AND DAY_FACT >= ? AND DAY_FACT <= ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(startDate.getTimeInMillis()));
            ps.setTimestamp(3, new Timestamp(endDate.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                String key = resultSet.getString("PAYLOAD_TYPE");
                APIManagerUsageStats stats = new APIManagerUsageStats(key);

                if ("API-Call".equals(key)) {
                    stats.setRequestCount(resultSet.getLong("PAYLOAD_VALUE"));
                } else {
                    //Do nothing
                }

                stats.setServerUrl(resultSet.getString("SERVER_NAME"));
                bwsList.add(stats);
            }
        } catch (SQLException e) {
            log.error("Error occurred while retrieving daily usage data from the database. ", e);

        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        return bwsList;
    }

    /**
     * @param tenantId Tenant ID
     * @param month    Stats of this month will be retrieved
     * @return A list of APIManagerUsageStats objects
     * @throws Exception
     */
    public List<APIManagerUsageStats> getMonthlyAPIManagerUsageStats(int tenantId,
                                                                     Calendar month) throws Exception {
        Connection connection = null;
        List<APIManagerUsageStats> bwsList = new ArrayList<APIManagerUsageStats>();

        try {
            connection = dataSource.getConnection();
            String sql = "SELECT * FROM USAGE_MONTHLY_ANALYTICS WHERE TENANT_ID = ? AND MONTH_FACT = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, tenantId);
            ps.setTimestamp(2, new Timestamp(month.getTimeInMillis()));
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                String key = resultSet.getString("PAYLOAD_DATA");
                APIManagerUsageStats stats = new APIManagerUsageStats(key);

                if (UsageConstants.API_CALL_COUNT.equals(key)) {
                    stats.setIncomingBandwidth(resultSet.getLong("PAYLOAD_VALUE"));

                } else {
                    //Do nothing
                }

                stats.setServerUrl(resultSet.getString("SERVER_NAME"));
                bwsList.add(stats);
            }
        } catch (SQLException e) {
            log.error("Error occurred while retrieving monthly usage data from the database. ", e);

        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return bwsList;

    }
}
