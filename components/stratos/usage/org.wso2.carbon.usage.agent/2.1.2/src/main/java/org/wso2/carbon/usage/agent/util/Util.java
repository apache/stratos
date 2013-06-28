/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.usage.agent.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.event.core.EventBroker;
import org.wso2.carbon.event.core.exception.EventBrokerException;
import org.wso2.carbon.event.core.subscription.Subscription;
import org.wso2.carbon.event.core.util.EventBrokerConstants;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.statistics.services.SystemStatisticsUtil;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.usage.agent.beans.BandwidthUsage;
import org.wso2.carbon.usage.agent.config.UsageAgentConfiguration;
import org.wso2.carbon.usage.agent.listeners.RegistryUsageListener;
import org.wso2.carbon.usage.agent.persist.UsageDataPersistenceManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;


/**
 * this class provide utility methods to set and get RealmService, initializing listeners,
 * initializing PersistenceManager etc
 * Further it provide methods to create statistics event subscription
 */

public class Util {
    private static final Log log = LogFactory.getLog(Util.class);
    private static final String USAGE_THROTTLING_AGENT_CONFIG_FILE = "usage-throttling-agent-config.xml";
    private static RealmService realmService;
    private static ConfigurationContextService contextService;
    private static UsageDataPersistenceManager persistenceManager;
    //private static EventBrokerService eventBrokerService;
    private static EventBroker eventBrokerService;
    private static ServerConfigurationService serverConfiguration;

    private static SystemStatisticsUtil systemStatisticsUtil;

    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static void initializeAllListeners() throws Exception {
        RegistryUsageListener.registerRegistryUsagePersistingListener(
                RegistryContext.getBaseInstance());
    }

    public static void setConfigurationContextService(ConfigurationContextService contextService) {
        Util.contextService = contextService;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return Util.contextService;
    }

    /**
     * this method create a PersistenceManager instance and start a thread for persisting statistics
     */

    public static void initializePersistenceManager() {
        File usageAgentConfigFile  = new File(CarbonUtils.getCarbonConfigDirPath() + File.separator +
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator + Util.USAGE_THROTTLING_AGENT_CONFIG_FILE);
        persistenceManager = new UsageDataPersistenceManager(new UsageAgentConfiguration(usageAgentConfigFile));
        //start a thread for persisting bandwidth Usage statistics
        if("true".equals(ServerConfiguration.getInstance().getFirstProperty("EnableMetering"))){
            persistenceManager.scheduleBandwidthUsageDataRetrievalTask();
            persistenceManager.scheduleUsageDataPersistenceTask();
        }
    }

    public static void addToPersistingControllerQueue(BandwidthUsage usage) {
        persistenceManager.addToQueue(usage);
    }

    public static EventBroker getEventBrokerService() {
        return eventBrokerService;
    }

    public static void setEventBrokerService(EventBroker eventBrokerService) {
        Util.eventBrokerService = eventBrokerService;
    }

    public static ServerConfigurationService getServerConfiguration() {
        return serverConfiguration;
    }

    public static void setServerConfiguration(ServerConfigurationService serverConfiguration) {
        Util.serverConfiguration = serverConfiguration;
    }

    public static SystemStatisticsUtil getSystemStatisticsUtil() {
        return systemStatisticsUtil;
    }

    public static void setSystemStatisticsUtil(SystemStatisticsUtil systemStatisticsUtil) {
        Util.systemStatisticsUtil = systemStatisticsUtil;
    }

    /**
     * method to create static subscription to BAM
     *
     * @throws EventBrokerException, if creating the static subscription to BAM failed.
     */
    public static void createStaticEventSubscription() throws EventBrokerException {

        //Get BAM URL from carbon.xml
        ServerConfigurationService serverConfiguration = getServerConfiguration();
        if (serverConfiguration == null) {
            throw new IllegalArgumentException("Invalid server configuration");
        }
        String serverURL = serverConfiguration.getFirstProperty(UsageAgentConstants.BAM_SERVER_URL);

        if(log.isDebugEnabled()){
            log.debug("Bam url = " + serverURL);
        }

        //Add static subscription only if bam url is set
        if (serverURL != null) {
            String serviceURL = serverURL + UsageAgentConstants.BAM_SERVER_STAT_SERVICE;

            EventBroker eventBrokerService = getEventBrokerService();
            Subscription subscription = new Subscription();
            // set the subscription end point to the service url
            subscription.setEventSinkURL(serviceURL);
            subscription.setTopicName(UsageAgentConstants.BAM_SERVER_STAT_FILTER);
            subscription.setOwner(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
            subscription.setEventDispatcherName(EventBrokerConstants.WS_EVENT_DISPATCHER_NAME);

            try {
                eventBrokerService.subscribe(subscription);
            } catch (EventBrokerException e) {
                String msg = "Cannot subscribe to the event broker ";
                log.error(msg);
                throw e;
            }
        }
    }
    
    public static int getTenantId(String toAddress) throws Exception {
        int index = toAddress.indexOf("/t/");
        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        if(index >= 0){
            String tenantDomain = toAddress.substring(index+2, toAddress.indexOf("/", index+3));
            tenantId = getRealmService().getTenantManager().getTenantId(tenantDomain);
        }
        return tenantId;
    }
}
