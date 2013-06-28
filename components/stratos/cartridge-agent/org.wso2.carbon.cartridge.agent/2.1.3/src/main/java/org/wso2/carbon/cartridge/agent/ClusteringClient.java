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

import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.*;
import org.apache.axis2.clustering.tribes.TribesClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.exception.CartridgeAgentException;
import org.wso2.carbon.cartridge.agent.registrant.PortMapping;
import org.wso2.carbon.cartridge.agent.registrant.Registrant;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantDatabase;
import org.wso2.carbon.cartridge.agent.registrant.RegistrantUtil;
import org.wso2.carbon.cartridge.messages.CreateClusterDomainMessage;
import org.wso2.carbon.cartridge.messages.CreateRemoveClusterDomainMessage;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * This class is used for all Axis2 clustering related activities such as joining the load balancer
 * & joining registrants to clustering groups.
 */
public class ClusteringClient {
    private static final Log log = LogFactory.getLog(ClusteringClient.class);
    public static final Random RANDOM = new Random();
    private Properties conf;
    private RegistrantDatabase registrantDatabase;
    private ClusteringAgent loadBalancerAgent;

    public ClusteringClient(RegistrantDatabase registrantDatabase) {
        this.registrantDatabase = registrantDatabase;
    }

    public void init(Properties conf,
                     ConfigurationContext configurationContext,
                     MembershipListener membershipListener) throws CartridgeAgentException {
        try {
            this.conf = conf;
            joinLoadBalancer(configurationContext, membershipListener);
        } catch (Exception e) {
            String msg = "Cannot initialize ClusteringClient";
            log.error(msg, e);
            throw new CartridgeAgentException(msg, e);
        }
    }

    private void joinLoadBalancer(ConfigurationContext configurationContext,
                                  MembershipListener membershipListener)
            throws CartridgeAgentException {

        try {
            loadBalancerAgent = createClusteringAgent(configurationContext,
                                                      conf.getProperty("loadBalancerDomain"));
            List<MembershipListener> membershipListeners = new ArrayList<MembershipListener>();
            membershipListeners.add(membershipListener);
            ((TribesClusteringAgent) loadBalancerAgent).setMembershipListeners(membershipListeners);
            loadBalancerAgent.init();
        } catch (Exception e) {
            String msg = "Cannot join LB group";
            log.error(msg, e);
            throw new CartridgeAgentException(msg, e);
        }
    }

    /**
     * Join a cluster group of the Elastic Load Balancer
     *
     * @param registrant           Registrant
     * @param configurationContext ConfigurationContext
     * @throws CartridgeAgentException If an error occurs while joining a cluster group
     */
    public void joinGroup(Registrant registrant,
                          ConfigurationContext configurationContext) throws CartridgeAgentException {
        if (registrantDatabase.containsActive(registrant)) {
            throw new CartridgeAgentException("Active registrant with key " +
                                              registrant.getKey() + " already exists");
        }
        registrantDatabase.add(registrant);

        if (!RegistrantUtil.isHealthy(registrant)) {
            String msg = "Couldn't add registrant " + registrant + " due to a health check failure";
            log.error(msg);
            return;
            //throw new CartridgeAgentException(msg);
        }
        try {
            List<ClusteringCommand> clusteringCommands =
                    loadBalancerAgent.sendMessage(new CreateClusterDomainMessage(registrant.getService(),
                                                                                 registrant.retrieveClusterDomain(),
                                                                                 registrant.getHostName(),
                                                                                 registrant.getTenantRange(),
                                                                                 registrant.getMinInstanceCount(),
                                                                                 registrant.getMaxInstanceCount(),
                                                                                 registrant.getMaxRequestsPerSecond(),
                                                                                 registrant.getRoundsToAverage(),
                                                                                 registrant.getAlarmingUpperRate(),
                                                                                 registrant.getAlarmingLowerRate(),
                                                                                 registrant.getScaleDownFactor()),
                                                  true);
            if (clusteringCommands != null && !clusteringCommands.isEmpty()) {
                for (ClusteringCommand clusteringCommand : clusteringCommands) {
                    clusteringCommand.execute(configurationContext);
                }
            } else {
                return;
            }

        } catch (ClusteringFault e) {
            handleException("Cannot send CreateClusterDomainMessage to ELB", e);
        }
        ClusteringAgent agent;
        try {
            agent = createClusteringAgent(configurationContext,
                                          registrant.retrieveClusterDomain());
            
        } catch (ClusteringFault e) {
            handleException("Cannot create ClusteringAgent for registrant", e);
            return;
        }

        //Add port.mapping.<port> entries to member
        /*
       <parameter name="properties">
           <property name="port.mapping.8281" value="9768"/>
           <property name="port.mapping.8244" value="9448"/>
       </parameter>
        */
        Parameter propsParam = new Parameter();
        propsParam.setName("properties");

        StringBuilder propertiesPayload = new StringBuilder("<parameter name=\"properties\">");
        int httpPort = -1;
        int httpsPort = -1;
        for (PortMapping portMapping : registrant.getPortMappings()) {
            propertiesPayload.append("<property name=\"portMapping.mapping.").append(portMapping.getProxyPort()).
                    append("\" value=\"").append(portMapping.getPrimaryPort()).append("\" />");
            if (portMapping.getType().equals(PortMapping.PORT_TYPE_HTTP)) {
                httpPort = portMapping.getPrimaryPort();
            } else if (portMapping.getType().equals(PortMapping.PORT_TYPE_HTTPS)) {
                httpsPort = portMapping.getPrimaryPort();
            }
        }

        String remoteHost = registrant.getRemoteHost();
        propertiesPayload.append("<property name=\"httpPort\" value=\"").append(httpPort).append("\" />");
        propertiesPayload.append("<property name=\"httpsPort\" value=\"").append(httpsPort).append("\" />");
        propertiesPayload.append("<property name=\"remoteHost\" value=\"").append(remoteHost).append("\" />");
        propertiesPayload.append("<property name=\"subDomain\" value=\"__$default\" />");
        propertiesPayload.append("</parameter>");

        try {
            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(propertiesPayload.toString().getBytes()));
            propsParam.setParameterElement(builder.getDocumentElement());
            agent.addParameter(propsParam);
        } catch (XMLStreamException e) {
            handleException("Cannot create properties ClusteringAgent parameter", e);
        } catch (AxisFault ignored) { // will not occur
        }

        int newMemberPort = Integer.parseInt(conf.getProperty("clustering.localMemberPort")) +
                            RANDOM.nextInt(5000) + 27;
        addParameter(agent, "localMemberPort", newMemberPort + "");
        try {
            synchronized (registrant) {
                if(!registrant.running()) {
                    registrant.start(agent);
                }
            }
        } catch (ClusteringFault e) {
            handleException("Cannot start registrant", e);
        }
        
        // Update instance state in stratos database, with active
        new Thread(new InstanceStateNotificationClientThread(registrant, "ACTIVE")).start();
    }
    
    
    public void removeClusterDomain(String domain,
                                    String subDomain,
                                    String hostName,
                                    ConfigurationContext configurationContext) throws CartridgeAgentException {
    	try {
            List<ClusteringCommand> clusteringCommands =
                    loadBalancerAgent.sendMessage(new CreateRemoveClusterDomainMessage(domain, subDomain, hostName),
                                                  true);
            if (clusteringCommands != null && !clusteringCommands.isEmpty()) {
                for (ClusteringCommand clusteringCommand : clusteringCommands) {
                    clusteringCommand.execute(configurationContext);
                }
            } else {
                return;
            }
        } catch (ClusteringFault e) {
            handleException("Cannot send CreateClusterDomainMessage to ELB", e);
        }
    }

    private void handleException(String msg, Exception e) throws CartridgeAgentException {
        log.error(msg, e);
        throw new CartridgeAgentException(msg, e);
    }

    private ClusteringAgent createClusteringAgent(ConfigurationContext configurationContext,
                                                  String clusterDomain) throws ClusteringFault {
        TribesClusteringAgent agent = new TribesClusteringAgent();
        addParameter(agent, "AvoidInitiation", "true");
        for (String key : conf.stringPropertyNames()) {
            if (key.startsWith("clustering.")) {
                addParameter(agent,
                             key.substring(key.indexOf(".") + 1),
                             conf.getProperty(key));
            }
        }

        List<Member> members = new ArrayList<Member>();
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String host = conf.getProperty("members." + i + ".host");
            String port = conf.getProperty("members." + i + ".port");
            if (host == null || port == null) {
                break;
            }
            members.add(new Member(host, Integer.parseInt(port)));
        }
        agent.setMembers(members);

        addParameter(agent, "domain", clusterDomain);
        agent.setConfigurationContext(configurationContext);

        List<MembershipListener> membershipListeners = new ArrayList<MembershipListener>();
        membershipListeners.add(new RegistrantMembershipListener(this, configurationContext));
        agent.setMembershipListeners(membershipListeners);
        return agent;
    }

    private static void addParameter(ClusteringAgent agent,
                                     String paramName, String paramValue) {
        Parameter parameter = new Parameter(paramName, paramValue);
        try {
            agent.removeParameter(parameter);
            agent.addParameter(parameter);
        } catch (AxisFault ignored) {
        }
    }
}
