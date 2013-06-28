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
package org.wso2.carbon.cartridge.agent;

import org.apache.axis2.clustering.Member;
import org.apache.axis2.clustering.MembershipListener;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.exception.CartridgeAgentException;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantDatabase;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantHealthChecker;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantUtil;

/**
 * This membership listener will get notified when the Elastic Load Balancer (ELB) joins or
 * leaves the cluster
 *
 * When the ELB leaves the cluster, we have to disconnect all Registrants, and when the ELB rejoins,
 * we have to reconnect all the Registrants to the ELB
 */
public class ELBMembershipListener implements MembershipListener {
    private static final Log log = LogFactory.getLog(ELBMembershipListener.class);

    private ClusteringClient clusteringClient;
    private ConfigurationContext configurationContext;
    private RegistrantDatabase registrantDatabase;
    private RegistrantHealthChecker healthChecker;

    public ELBMembershipListener(ClusteringClient clusteringClient,
                                 ConfigurationContext configurationContext,
                                 RegistrantDatabase registrantDatabase,
                                 RegistrantHealthChecker healthChecker) {
        this.clusteringClient = clusteringClient;
        this.configurationContext = configurationContext;
        this.registrantDatabase = registrantDatabase;
        this.healthChecker = healthChecker;
    }

    public void memberAdded(Member member, boolean b) {
        log.info("ELB Member [" + member + "] joined cluster");
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                    }
                    RegistrantUtil.reloadRegistrants(clusteringClient,
                                                     configurationContext,
                                                     registrantDatabase);
                    healthChecker.setELBRunning(true);
                } catch (CartridgeAgentException e) {
                    log.error("Could not reload registrants", e);
                }
            }
        };
        new Thread(runnable).start();
    }

    public void memberDisappeared(Member member, boolean b) {
        log.info("ELB Member [" + member + "] left cluster");
        healthChecker.setELBRunning(false);
        registrantDatabase.stopAll();
    }
}
