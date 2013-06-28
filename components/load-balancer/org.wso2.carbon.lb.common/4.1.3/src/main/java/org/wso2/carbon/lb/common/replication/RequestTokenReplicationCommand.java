/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.lb.common.replication;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * This is the notification message a primary load balancer will send to all other load balancers
 * in the cluster, to replicate its state. When the other load balancers received this message, 
 * they will set their states to the state of primary load balancer.
 */
public class RequestTokenReplicationCommand extends ClusteringMessage {

    private static final long serialVersionUID = -7897961078018830555L;
    private static final Log log = LogFactory.getLog(RequestTokenReplicationCommand.class);
    private Map<String, Map<String, ?>> appDomainContexts;

    public Map<String, Map<String, ?>> getAppDomainContexts() {
        return appDomainContexts;
    }

    public void setAppDomainContexts(Map<String, Map<String, ?>> appDomainContexts) {
        this.appDomainContexts = appDomainContexts;
    }

    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
        // set the appDomainContexts map
        configurationContext.setNonReplicableProperty("autoscale.app.domain.contexts",
                                          getAppDomainContexts());
        
        log.info("Request Tokens Replicated! ");
    }

    public String toString() {
        return "Replication message sent!";
    }

    @Override
    public ClusteringCommand getResponse() {
        return new ClusteringCommand() {
            
            private static final long serialVersionUID = -8271265673996681347L;

            @Override
            public void execute(ConfigurationContext arg0) throws ClusteringFault {
                // do nothing
            }
        };
    }
}
