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
package org.wso2.carbon.usage.agent.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.AuthenticationException;
import org.wso2.carbon.databridge.commons.exception.NoStreamDefinitionExistException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.statistics.services.util.SystemStatistics;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.stratos.common.util.StratosConfiguration;
import org.wso2.carbon.usage.agent.beans.BandwidthUsage;
import org.wso2.carbon.usage.agent.exception.UsageException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * this class provide utility methods to publish usage statistics
 */
public class PublisherUtils {
    private static Log log = LogFactory.getLog(PublisherUtils.class);
    private static final String TRANSPORT = "https";
    private static ConfigurationContextService configurationContextService;
    private static Agent agent;
    private static DataPublisher dataPublisher;
    private static AsyncDataPublisher asyncDataPublisher;
    private static String streamId;
    private static final String usageEventStream = "org.wso2.carbon.usage.agent";
    private static final String usageEventStreamVersion = "1.0.0";
    
    private static final String reqStatEventStream="org.wso2.carbon.service.request.stats";
    private static final String reqStatEventStreamVersion="1.0.0";
    private static String reqStatEventStreamId;
    
    private static Map<Integer, String> serverUrlMap = new HashMap<Integer, String>();


    /**
     * method to update server name
     * @param tenantId tenant id
     * @return server name
     * @throws UsageException
     */

    public static String updateServerName(int tenantId) throws UsageException {

        String serverName;
        String hostName;

        try {
            hostName = NetworkUtils.getLocalHostname();
        } catch (SocketException e) {
            throw new UsageException("Error getting host name for the registry usage event payload",
                    e);
        }

        ConfigurationContextService configurationContextService = PublisherUtils.
                getConfigurationContextService();
        ConfigurationContext configurationContext;
        if (configurationContextService != null) {
            configurationContext = configurationContextService.getServerConfigContext();
        } else {
            throw new UsageException("ConfigurationContext is null");
        }
//        int port = CarbonUtils.getTransportPort(configurationContext, "https");

        String carbonHttpsPort = System.getProperty("carbon." + TRANSPORT + ".port");
        if (carbonHttpsPort == null) {
            carbonHttpsPort = Integer.toString(
                    CarbonUtils.getTransportPort(configurationContext, TRANSPORT));
        }
        String baseServerUrl = TRANSPORT + "://" + hostName + ":" + carbonHttpsPort;
        String context = configurationContext.getContextRoot();

        String tenantDomain = null;
        try {
            Tenant tenant = Util.getRealmService().getTenantManager().getTenant(tenantId);
            if(tenant!=null){
                tenantDomain = tenant.getDomain();
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UsageException("Failed to get tenant domain", e);
        }

        if ((tenantDomain != null) &&
                !(tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
            serverName = baseServerUrl + context + "t/" + tenantDomain;

        } else if (context.equals("/")) {

            serverName = baseServerUrl + "";
        } else {
            serverName = baseServerUrl + context;

        }

        return serverName;
    }
    
    public static String getServerUrl(int tenantId){

        String serverUrl = serverUrlMap.get(tenantId);
        if(serverUrl!=null){
            return serverUrl;
        }

        if(serverUrl==null){
            try{
                serverUrl = updateServerName(tenantId);
            }catch (UsageException e) {
                log.error("Could not create the server url for tenant id: " + tenantId, e);
            }
        }

        if(serverUrl!=null && !"".equals(serverUrl)){
            serverUrlMap.put(tenantId, serverUrl);
        }
        return serverUrl;
    }

    public static void defineUsageEventStream() throws Exception {

        createDataPublisher();

        if(dataPublisher == null){
            return;
        }

        try {

            streamId = dataPublisher.findStream(usageEventStream, usageEventStreamVersion);
            log.info("Event stream with stream ID: " + streamId + " found.");

        } catch (NoStreamDefinitionExistException e) {

            log.info("Defining the event stream because it was not found in BAM");
            try {
                defineStream();    
            } catch (Exception ex) {
                String msg = "An error occurred while defining the even stream for Usage agent. " + e.getMessage();
                log.warn(msg);
            }

        }

    }
    
    private static void defineStream() throws Exception {
        streamId = dataPublisher.
                defineStream("{" +
                        "  'name':'" + usageEventStream +"'," +
                        "  'version':'" + usageEventStreamVersion +"'," +
                        "  'nickName': 'usage.agent'," +
                        "  'description': 'Tenant usage data'," +
                        "  'metaData':[" +
                        "          {'name':'clientType','type':'STRING'}" +
                        "  ]," +
                        "  'payloadData':[" +
                        "          {'name':'ServerName','type':'STRING'}," +
                        "          {'name':'TenantID','type':'STRING'}," +
                        "          {'name':'Type','type':'STRING'}," +
                        "          {'name':'Value','type':'LONG'}" +
                        "  ]" +
                        "}");
        
    }
    
    private static void defineRequestStatEventStream() throws Exception{
        reqStatEventStreamId = dataPublisher.
                defineStream("{" +
                        "  'name':'" + reqStatEventStream +"'," +
                        "  'version':'" + reqStatEventStreamVersion +"'," +
                        "  'nickName': 'service.request.stats'," +
                        "  'description': 'Tenants service request statistics'," +
                        "  'metaData':[" +
                        "          {'name':'clientType','type':'STRING'}" +
                        "  ]," +
                        "  'payloadData':[" +
                        "          {'name':'ServerName','type':'STRING'}," +
                        "          {'name':'TenantID','type':'STRING'}," +
                        "          {'name':'RequestCount','type':'INT'}," +
                        "          {'name':'ResponseCount','type':'INT'}," +
                        "          {'name':'FaultCount','type':'INT'}," +
                        "          {'name':'ResponseTime','type':'LONG'}" +
                        "  ]" +
                        "}");
    }

    public static void createDataPublisher(){

        ServerConfiguration serverConfig =  CarbonUtils.getServerConfiguration();
        String trustStorePath = serverConfig.getFirstProperty("Security.TrustStore.Location");
        String trustStorePassword = serverConfig.getFirstProperty("Security.TrustStore.Password");
        String bamServerUrl = serverConfig.getFirstProperty("BamServerURL");
        String adminUsername = CommonUtil.getStratosConfig().getAdminUserName();
        String adminPassword = CommonUtil.getStratosConfig().getAdminPassword();

        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

        try {
            dataPublisher = new DataPublisher(bamServerUrl, adminUsername, adminPassword);
        } catch (Exception e) {
            log.warn("Unable to create a data publisher to " + bamServerUrl +
                    ". Usage Agent will not function properly. " + e.getMessage());
        }

    }

    /**
     * Creates an async data publisher using the existing data publisher object
     */
    public static void createAsynDataPublisher(){
        if(dataPublisher==null){
            createDataPublisher();
        }

        if(dataPublisher==null){
            log.warn("Cannot create the async data publisher because the data publisher is null");
            return;
        }

        try {
            asyncDataPublisher = new AsyncDataPublisher(dataPublisher);
        } catch (Exception e) {
            log.error("Could not create an async data publisher using the data publisher", e);
        }
    }


    /**
     * this method get the event payload, construct the SOAP envelop and call the publish method in
     * EventBrokerService.
     *
     * @param usage BandwidthUsage
     * @throws UsageException
     */
    public static void publish(BandwidthUsage usage) throws UsageException {

        if(dataPublisher==null){
            log.info("Creating data publisher for usage data publishing");
            createDataPublisher();

            //If we cannot create a data publisher we should give up
            //this means data will not be published
            if(dataPublisher == null){
                return;
            }
        }

        if(streamId == null){
            try{
                streamId = dataPublisher.findStream(usageEventStream, usageEventStreamVersion);
            }catch (NoStreamDefinitionExistException e){
                log.info("Defining the event stream because it was not found in BAM");
                try{
                    defineStream();
                } catch(Exception ex){
                    String msg = "Error occurred while defining the event stream for publishing usage data. " + ex.getMessage();
                    log.error(msg);
                    //We do not want to proceed without an event stream. Therefore we return.
                    return;
                }
            }catch (Exception exc){
                log.error("Error occurred while searching for stream id. " + exc.getMessage());
                //We do not want to proceed without an event stream. Therefore we return.
                return;
            }
        }

        try {

            Event usageEvent = new Event(streamId, System.currentTimeMillis(), new Object[]{"external"}, null,
                                        new Object[]{getServerUrl(usage.getTenantId()),
                                                    Integer.toString(usage.getTenantId()),
                                                    usage.getMeasurement(),
                                                    usage.getValue()});

            dataPublisher.publish(usageEvent);

        } catch (Exception e) {
            log.error("Error occurred while publishing usage event to BAM. " + e.getMessage(), e);
            throw new UsageException(e.getMessage(), e);
        }

    }
    
    public static void publish(SystemStatistics statistics, int tenantId) throws Exception {

        if(dataPublisher==null){
            log.info("Creating data publisher for service-stats publishing");
            createDataPublisher();

            //If we cannot create a data publisher we should give up
            //this means data will not be published
            if(dataPublisher == null){
                return;
            }
        }

        if(reqStatEventStreamId == null){
            try{
                reqStatEventStreamId = dataPublisher.findStream(reqStatEventStream, reqStatEventStreamVersion);
            }catch (NoStreamDefinitionExistException e){
                log.info("Defining the event stream because it was not found in BAM");
                try{
                    defineRequestStatEventStream();
                } catch(Exception ex){
                    String msg = "Error occurred while defining the event stream for publishing usage data. " + ex.getMessage();
                    log.error(msg);
                    //We do not want to proceed without an event stream. Therefore we return.
                    return;
                }
            }catch (Exception exc){
                log.error("Error occurred while searching for stream id. " + exc.getMessage());
                //We do not want to proceed without an event stream. Therefore we return.
                return;
            }
        }

        try {

            Event usageEvent = new Event(reqStatEventStreamId, System.currentTimeMillis(), new Object[]{"external"}, null,
                    new Object[]{getServerUrl(tenantId),
                            Integer.toString(tenantId),
                            statistics.getCurrentInvocationRequestCount(),
                            statistics.getCurrentInvocationResponseCount(),
                            statistics.getCurrentInvocationFaultCount(),
                            statistics.getCurrentInvocationResponseTime()});

            dataPublisher.publish(usageEvent);

        } catch (Exception e) {
            log.error("Error occurred while publishing usage event to BAM. " + e.getMessage(), e);
            throw new UsageException(e.getMessage(), e);
        }

    }

    /**
     * method to get configurationContextService
     * @return configurationContextService
     */

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    /**
     * method to setConfigurationContextService
     * @param configurationContextService
     */
    public static void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        PublisherUtils.configurationContextService = configurationContextService;
    }

}
