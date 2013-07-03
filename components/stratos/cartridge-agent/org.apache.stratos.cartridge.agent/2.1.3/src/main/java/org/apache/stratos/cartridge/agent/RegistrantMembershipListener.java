/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.cartridge.agent;

import org.apache.axis2.clustering.Member;
import org.apache.axis2.clustering.MembershipListener;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.registrant.Registrant;

/**
 * @author wso2
 *
 */
public class RegistrantMembershipListener implements MembershipListener{
	
	private ClusteringClient clusteringClient;
	private ConfigurationContext configurationContext;

	private static final Log log = LogFactory.getLog(RegistrantMembershipListener.class);
	
	public RegistrantMembershipListener(ClusteringClient clusteringClient,
	                                    ConfigurationContext configurationContext) {
		this.clusteringClient = clusteringClient;
		this.configurationContext = configurationContext;
    }

	public void memberAdded(Member arg0, boolean arg1) {
	    log.info(" ********* Member is added to the group ... " + arg0);
    }

	public void memberDisappeared(Member member, boolean arg1) {
		 log.info(" **********  Member is removed from group ... " + member);
		 Registrant registrant = new Registrant();
		 
		 //HostName : cartridgeName + "." + cartridgeInfo.getHostName()
		 // sajithphpautoscale.php.slive.com.php.domain
		 //Domain : HostName.php.domain
		 // TODO
    		/* String hostName = getHostNameFromDomain(member.getDomain());
    		 log.info("Host name is returned as: " + hostName);
    		 try {
    	        clusteringClient.removeClusterDomain(member.getDomain(), "__$default", hostName, configurationContext);
            } catch (CartridgeAgentException e) {
    	        e.printStackTrace();
            }*/
    }

	private String getHostNameFromDomain(String domain) {
	    return domain.substring(0, domain.length()-11);
    }

}
