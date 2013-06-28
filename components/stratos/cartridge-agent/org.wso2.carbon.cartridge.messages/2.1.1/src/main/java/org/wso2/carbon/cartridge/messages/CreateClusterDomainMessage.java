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
package org.wso2.carbon.cartridge.messages;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO: class description
 */
public class CreateClusterDomainMessage extends ClusteringMessage {
    private static final Log log = LogFactory.getLog(CreateClusterDomainMessage.class);
    public static final String CLUSTER_DOMAIN_MANAGER = "cluster.domain.manager";
    private String service;
    private String clusterDomain;
    private String hostName;
    //private int tenantId;
    private String tenantRange;
    private int minInstances;
    private int maxInstances;
	private int maxRequestsPerSecond;
	private int roundsToAverage;
	private double alarmingUpperRate;
	private double alarmingLowerRate;
	private double scaleDownFactor;
    

    public CreateClusterDomainMessage(String service, String clusterDomain,
                                      String hostName, String tenantRange,
                                      int minInstances, int maxInstances,
                                      int maxRequestsPerSecond, int roundsToAverage,
                                      double alarmingUpperRate, double alarmingLowerRate,
                                      double scaleDownFactor) {
        this.service = service;
        this.clusterDomain = clusterDomain;
        this.hostName = hostName;
        this.tenantRange = tenantRange;
        this.minInstances = minInstances;
        this.maxInstances = maxInstances;
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.roundsToAverage = roundsToAverage;
        this.alarmingUpperRate = alarmingUpperRate;
        this.alarmingLowerRate = alarmingLowerRate;
        this.scaleDownFactor = scaleDownFactor;
    }

    @Override
    public ClusteringCommand getResponse() {
        return new ClusteringCommand() {
            @Override
            public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
                log.info("Received response to CreateClusterDomainMessage");
            }
        };
    }

    @Override
    public void execute(final ConfigurationContext configurationContext) throws ClusteringFault {
        log.info("Received ***" + this);
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                ClusterDomainManager clusterDomainManager =
                        (ClusterDomainManager) configurationContext.getProperty(CLUSTER_DOMAIN_MANAGER);
                if (clusterDomainManager != null) {
                    clusterDomainManager.addClusterDomain(createClusterDomain());
                } else {
                    log.warn(CLUSTER_DOMAIN_MANAGER + " has not been defined in ConfigurationContext");
                }
            }

			private ClusterDomain createClusterDomain() {
				
				ClusterDomain clusterDomainObj = new ClusterDomain();
				clusterDomainObj.setDomain(clusterDomain);
				clusterDomainObj.setHostName(hostName);
				clusterDomainObj.setMaxInstances(maxInstances);
				clusterDomainObj.setMinInstances(minInstances);
				clusterDomainObj.setSubDomain(null); // TODO subdomain
				clusterDomainObj.setTenantRange(tenantRange);
				clusterDomainObj.setServiceName(service);
				clusterDomainObj.setMaxRequestsPerSecond(maxRequestsPerSecond);
				clusterDomainObj.setRoundsToAverage(roundsToAverage);
				clusterDomainObj.setAlarmingUpperRate(alarmingUpperRate);
				clusterDomainObj.setAlarmingLowerRate(alarmingLowerRate);
				clusterDomainObj.setScaleDownFactor(scaleDownFactor);
				
				if(log.isDebugEnabled())
					log.debug("Cluster Domain is created with minInstances:" + minInstances + ", maxInstances: " + maxInstances);
				
				return clusterDomainObj;
			}
        };
        new Thread(runnable).start();
    }

    @Override
    public String toString() {
        return "CreateClusterDomainMessage{" +
               "service='" + service + '\'' +
               ", clusterDomain='" + clusterDomain + '\'' +
               ", hostName='" + hostName + '\'' +
               ", tenantId=" + tenantRange +
               '}';
    }
}
