/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.stratos.cloud.controller.hive;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceHiveExecutionException;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResult;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResultRow;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.utils.CarbonUtils;

public class HiveQueryExecutor {
    private static final Log log = LogFactory.getLog(HiveQueryExecutor.class);
    private HiveExecutionServiceStub hiveService;
    private String payloadPrefix = CloudControllerConstants.PAYLOAD_PREFIX;
    private String hiveTable = "cloudController";
    private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
    
    public HiveQueryExecutor() {

        ServerConfiguration serverConfig =  CarbonUtils.getServerConfiguration();
        String bamServerUrl = serverConfig.getFirstProperty("BamServerURL");
        String serviceName = "HiveExecutionService";
        HttpTransportProperties.Authenticator authenticator;
        
        try {
            hiveService = new HiveExecutionServiceStub(bamServerUrl+"/services/"+serviceName);
            
            // admin service authentication
            authenticator = new HttpTransportProperties.Authenticator();
            authenticator.setUsername(dataHolder.getBamUsername());
            authenticator.setPassword(dataHolder.getBamPassword());
            authenticator.setPreemptiveAuthentication(true);
            
            ServiceClient client = hiveService._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
            option.setTimeOutInMilliSeconds(120000);

        } catch (AxisFault e) {
            String msg = "Cannot get a connection to "+serviceName;
            handleException(msg, e);
        }
    }
    
    public QueryResult[] execute(String query){
        try {
            return hiveService.executeHiveScript(query);
        } catch (RemoteException e) {
            handleException("Query : '"+query+"' - "+e.getMessage(), e);
        } catch (HiveExecutionServiceHiveExecutionException e) {
            handleException("Query : '"+query+"' - "+e.getMessage(), e);
        }
        
        return new QueryResult[0];
    }
    
    public void createHiveTable(){
        String query = 
                "CREATE EXTERNAL TABLE IF NOT EXISTS "+hiveTable+" (id STRING, " +
                payloadPrefix+CloudControllerConstants.NODE_ID_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.CARTRIDGE_TYPE_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.DOMAIN_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.HOST_NAME_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.HYPERVISOR_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.IAAS_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.IMAGE_ID_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.LOGIN_PORT_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.PRIV_IP_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.PUB_IP_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.STATUS_COL+" STRING," +
                payloadPrefix+CloudControllerConstants.SUB_DOMAIN_COL+" STRING" +
                ") STORED BY 'org.apache.hadoop.hive.cassandra.CassandraStorageHandler' " +
                "WITH SERDEPROPERTIES ( \"cassandra.host\" = \""+dataHolder.getCassandraConnUrl().split(":")[0]+"\"," +
                "\"cassandra.port\" = \""+dataHolder.getCassandraConnUrl().split(":")[1]+
                "\",\"cassandra.ks.name\" = \""+CloudControllerConstants.DEFAULT_CASSANDRA_KEY_SPACE+"\"," +
                "\"cassandra.ks.username\" = \""+dataHolder.getCassandraUser()+
                "\", \"cassandra.ks.password\" = \""+dataHolder.getCassandraPassword()+"\"," +
                "\"cassandra.cf.name\" = \""+CloudControllerConstants.CLOUD_CONTROLLER_COL_FAMILY+"\"," +
                "\"cassandra.columns.mapping\" = \"" +
                payloadPrefix+CloudControllerConstants.NODE_ID_COL+"," +
                payloadPrefix+CloudControllerConstants.CARTRIDGE_TYPE_COL+"," +
                payloadPrefix+CloudControllerConstants.DOMAIN_COL+"," +
                payloadPrefix+CloudControllerConstants.HOST_NAME_COL+"," +
                payloadPrefix+CloudControllerConstants.HYPERVISOR_COL+"," +
                payloadPrefix+CloudControllerConstants.IAAS_COL+"," +
                payloadPrefix+CloudControllerConstants.IMAGE_ID_COL+"," +
                payloadPrefix+CloudControllerConstants.LOGIN_PORT_COL+"," +
                payloadPrefix+CloudControllerConstants.PRIV_IP_COL+"," +
                payloadPrefix+CloudControllerConstants.PUB_IP_COL+"," +
                payloadPrefix+CloudControllerConstants.STATUS_COL+"," +
                payloadPrefix+CloudControllerConstants.SUB_DOMAIN_COL +
                "\");";
        
        execute(query);
    }
    
    public List<String> getRunningNodeIds() {
        List<String> nodeIds = new ArrayList<String>();
        String query =
//                       "select " + payloadPrefix + AutoscalerConstant.NODE_ID_COL + " from " +
//                               hiveTable + " where payload_status='RUNNING' OR payload_status='PENDING' ;";

//        "select id1 from (select distinct payload_nodeId from cloud1  where payload_status='RUNNING' OR payload_status='PENDING') table1
//LEFT OUTER JOIN
//(select distinct payload_nodeId as nodeId from cloud1  where payload_status='TERMINATED') table2
//ON(table2.nodeId = table1.payload_nodeId)
//where table2.nodeId is null;";
                "select table1.id1 from (select distinct "+payloadPrefix+CloudControllerConstants.NODE_ID_COL+
                " as id1 from "+ hiveTable +" where "+payloadPrefix+CloudControllerConstants.STATUS_COL+
                "='RUNNING' OR "+payloadPrefix+CloudControllerConstants.STATUS_COL+"='PENDING') table1 " +
                "LEFT OUTER JOIN " +"(select distinct "+payloadPrefix+CloudControllerConstants.NODE_ID_COL+
                " as id2 from "+hiveTable+" where "+payloadPrefix+CloudControllerConstants.STATUS_COL+"='TERMINATED') table2 " +
                "ON(table1.id1 = table2.id2) where table2.id2 is null;";
        
        QueryResult[] result = execute(query);

        for (QueryResult queryResult : result) {
            if(queryResult == null || queryResult.getResultRows() == null){
                continue;
            }
            for (QueryResultRow row : queryResult.getResultRows()) {
                if (row != null && row.getColumnValues() != null && row.getColumnValues().length != 0) {
                    nodeIds.add(row.getColumnValues()[0]);
                }
            }
        }
        
        return nodeIds;

    }
    
    private void handleException(String msg, Exception e){
        log.error(msg, e);
        throw new CloudControllerException(msg, e);
    }
    
}
