/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.endpoint;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.TransportInDescription;
import org.apache.http.protocol.HTTP;
import org.apache.stratos.load.balancer.RequestDelegator;
import org.apache.stratos.load.balancer.algorithm.LoadBalanceAlgorithmFactory;
import org.apache.stratos.load.balancer.statistics.LoadBalancerInFlightRequestCountCollector;
import org.apache.stratos.load.balancer.util.Constants;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.DynamicLoadbalanceFaultHandler;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.dispatch.HttpSessionDispatcher;
import org.apache.synapse.endpoints.dispatch.SessionInformation;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class TenantAwareLoadBalanceEndpoint extends org.apache.synapse.endpoints.LoadbalanceEndpoint implements Serializable {
    private static final String PORT_MAPPING_PREFIX = "port.mapping.";

    /* Request delegator identifies the next member */
    private RequestDelegator requestDelegator;

    /* Load balance algorithm class name */
    private String algorithmClassName;

    /* Flag to enable session affinity based load balancing */
    private boolean sessionAffinity;

    /* Dispatcher used for session affinity */
    private HttpSessionDispatcher dispatcher;

    /* Sessions time out interval */
    private long sessionTimeout = -1;

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        super.init(synapseEnvironment);

        requestDelegator = new RequestDelegator(LoadBalanceAlgorithmFactory.createAlgorithm(algorithmClassName));
        synapseEnvironment.getSynapseConfiguration().setProperty(SynapseConstants.PROP_SAL_ENDPOINT_DEFAULT_SESSION_TIMEOUT, String.valueOf(sessionTimeout));
        setDispatcher(new HttpSessionDispatcher());
    }

    @Override
    public void send(MessageContext synCtx) {

        SessionInformation sessionInformation = null;
        org.apache.axis2.clustering.Member currentMember = null;
        if (isSessionAffinityBasedLB()) {
            // Check existing session information
            sessionInformation = (SessionInformation) synCtx.getProperty(
                    SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION);

            currentMember = (org.apache.axis2.clustering.Member) synCtx.getProperty(
                    SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER);

            if (sessionInformation == null && currentMember == null) {
                sessionInformation = dispatcher.getSession(synCtx);
                if (sessionInformation != null) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Existing session found: %s", sessionInformation.getId()));
                    }

                    currentMember = sessionInformation.getMember();
                    synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, currentMember);
                    // This is for reliably recovery any session information if while response is getting ,
                    // session information has been removed by cleaner.
                    // This will not be a cost as session information is not a heavy data structure
                    synCtx.setProperty(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, sessionInformation);
                }
            }

        }

        TenantAwareLoadBalanceFaultHandler faultHandler = new TenantAwareLoadBalanceFaultHandler();
        if (sessionInformation != null && currentMember != null) {
            // Send request to the member with the existing session
            sessionInformation.updateExpiryTime();
            sendToApplicationMember(synCtx, currentMember, faultHandler, false);
        } else {
            // No existing session found
            // Find next member
            org.apache.axis2.clustering.Member axis2Member = findNextMember(synCtx);
            if (axis2Member != null) {
                // Send request to member
                sendToApplicationMember(synCtx, axis2Member, faultHandler, true);
            } else {
                throwSynapseException(synCtx, 404, "Active application instances not found");
            }
        }
    }

    private void throwSynapseException(MessageContext synCtx, int errorCode, String errorMessage) {
        synCtx.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
        throw new SynapseException(errorMessage);
    }

    /**
     * Setup load balancer message context properties to be used by the out block of the main sequence.
     * These values will be used to update the Location value in the response header.
     *
     * @param synCtx
     */
    private void setupLoadBalancerContextProperties(MessageContext synCtx) {
        String lbHostName = extractTargetHost(synCtx);
        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        TransportInDescription httpTransportIn = axis2MsgCtx.getConfigurationContext().getAxisConfiguration().getTransportIn("http");
        TransportInDescription httpsTransportIn = axis2MsgCtx.getConfigurationContext().getAxisConfiguration().getTransportIn("https");
        String lbHttpPort = (String) httpTransportIn.getParameter("port").getValue();
        String lbHttpsPort = (String) httpsTransportIn.getParameter("port").getValue();

        synCtx.setProperty(Constants.LB_HOST_NAME, lbHostName);
        synCtx.setProperty(Constants.LB_HTTP_PORT, lbHttpPort);
        synCtx.setProperty(Constants.LB_HTTPS_PORT, lbHttpsPort);
    }


    /**
     * Adding the X-Forwarded-For/X-Originating-IP headers to the outgoing message.
     *
     * @param synCtx Current message context
     */
    protected void setupTransportHeaders(MessageContext synCtx) {
        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            String xForwardFor = (String) headersMap.get(NhttpConstants.HEADER_X_FORWARDED_FOR);
            String remoteHost = (String) axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.REMOTE_ADDR);

            if (xForwardFor != null && !"".equals(xForwardFor)) {
                StringBuilder xForwardedForString = new StringBuilder();
                xForwardedForString.append(xForwardFor);
                if (remoteHost != null && !"".equals(remoteHost)) {
                    xForwardedForString.append(",").append(remoteHost);
                }
                headersMap.put(NhttpConstants.HEADER_X_FORWARDED_FOR, xForwardedForString.toString());
            } else {
                headersMap.put(NhttpConstants.HEADER_X_FORWARDED_FOR, remoteHost);
            }

            //Extracting information of X-Originating-IP
            if (headersMap.get(NhttpConstants.HEADER_X_ORIGINATING_IP_FORM_1) != null) {
                headersMap.put(NhttpConstants.HEADER_X_ORIGINATING_IP_FORM_1, headersMap.get(NhttpConstants.HEADER_X_ORIGINATING_IP_FORM_1));
            } else if (headersMap.get(NhttpConstants.HEADER_X_ORIGINATING_IP_FORM_2) != null) {
                headersMap.put(NhttpConstants.HEADER_X_ORIGINATING_IP_FORM_2, headersMap.get(NhttpConstants.HEADER_X_ORIGINATING_IP_FORM_2));
            }

        }
    }

    private org.apache.axis2.clustering.Member findNextMember(MessageContext synCtx) {
        String targetHost = extractTargetHost(synCtx);
        if(!requestDelegator.isTargetHostValid(targetHost)) {
            throwSynapseException(synCtx, 404, String.format("Unknown host name %s", targetHost));
        }
        Member member = requestDelegator.findNextMemberFromHostName(targetHost);
        if (member == null)
            return null;

        // Create Axi2 member object
        String transport = extractTransport(synCtx);
        Port transportPort = member.getPort(transport);
        if (transportPort == null) {
            if(log.isErrorEnabled()) {
                log.error(String.format("Port not found for transport %s in member %s", transport, member.getMemberId()));
            }
            throwSynapseException(synCtx, 500, "Internal server error");
        }

        int memberPort = transportPort.getValue();
        org.apache.axis2.clustering.Member axis2Member = new org.apache.axis2.clustering.Member(member.getMemberIp(), memberPort);
        axis2Member.setDomain(member.getClusterId());
        Port httpPort = member.getPort("http");
        if (httpPort != null)
            axis2Member.setHttpPort(httpPort.getValue());
        Port httpsPort = member.getPort("https");
        if (httpsPort != null)
            axis2Member.setHttpsPort(httpsPort.getValue());
        axis2Member.setActive(member.isActive());
        return axis2Member;
    }

    private String extractTargetHost(MessageContext synCtx) {
        org.apache.axis2.context.MessageContext msgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Map headerMap = (Map) msgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String hostName = null;
        if (headerMap != null) {
            Object hostObj = headerMap.get(HTTP.TARGET_HOST);
            hostName = (String) hostObj;
            if (hostName.contains(":")) {
                hostName = hostName.substring(0, hostName.indexOf(":"));
            }
        }
        return hostName;
    }

    private int extractPort(MessageContext synCtx, String transport) {
        org.apache.axis2.context.MessageContext msgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Map headerMap = (Map) msgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        int port = -1;
        if (headerMap != null) {
            String hostHeader = (String) headerMap.get(HTTP.TARGET_HOST);
            int index = hostHeader.indexOf(':');
            if (index != -1) {
                port = Integer.parseInt(hostHeader.trim().substring(index + 1));
            } else {
                if ("http".equals(transport)) {
                    port = 80;
                } else if ("https".equals(transport)) {
                    port = 443;
                }
            }
        }
        return port;
    }

    private String extractTransport(MessageContext synCtx) {
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        return axis2MessageContext.getTransportIn().getName();
    }

    /**
     * @param to     get an endpoint to send the information
     * @param member The member to which an EP has to be created
     * @param synCtx synapse context
     * @return the created endpoint
     */
    private Endpoint getEndpoint(EndpointReference to, org.apache.axis2.clustering.Member member, MessageContext synCtx) {
        AddressEndpoint endpoint = new AddressEndpoint();
        endpoint.setEnableMBeanStats(false);
        endpoint.setName("DLB:" + member.getHostName() +
                ":" + member.getPort() + ":" + UUID.randomUUID());
        EndpointDefinition definition = new EndpointDefinition();
        definition.setSuspendMaximumDuration(10000);
        definition.setReplicationDisabled(true);
        definition.setAddress(to.getAddress());
        endpoint.setDefinition(definition);
        endpoint.init((SynapseEnvironment)
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().
                        getConfigurationContext().getAxisConfiguration().
                        getParameterValue(SynapseConstants.SYNAPSE_ENV));
        return endpoint;
    }

    private EndpointReference getEndpointReferenceAfterURLRewrite(org.apache.axis2.clustering.Member currentMember,
                                                                  String transport,
                                                                  String address,
                                                                  int incomingPort) {

        if (transport.startsWith("https")) {
            transport = "https";
        } else if (transport.startsWith("http")) {
            transport = "http";
        } else {
            String msg = "Cannot load balance for non-HTTP/S transport " + transport;
            log.error(msg);
            throw new SynapseException(msg);
        }
        // URL Rewrite
        if (transport.startsWith("http") || transport.startsWith("https")) {
            if (address.startsWith("http://") || address.startsWith("https://")) {
                try {
                    String _address = address.indexOf("?") > 0 ? address.substring(address.indexOf("?"), address.length()) : "";
                    address = new URL(address).getPath() + _address;
                } catch (MalformedURLException e) {
                    String msg = "URL " + address + " is malformed";
                    log.error(msg, e);
                    throw new SynapseException(msg, e);
                }
            }

            int port;
            Properties memberProperties = currentMember.getProperties();
            String mappedPort = memberProperties.getProperty(PORT_MAPPING_PREFIX + incomingPort);
            if (mappedPort != null) {
                port = Integer.parseInt(mappedPort);
            } else if (transport.startsWith("https")) {
                port = currentMember.getHttpsPort();
            } else {
                port = currentMember.getHttpPort();
            }

            String remoteHost = memberProperties.getProperty("remoteHost");
            String hostName = (remoteHost == null) ? currentMember.getHostName() : remoteHost;
            return new EndpointReference(transport + "://" + hostName +
                    ":" + port + address);
        } else {
            String msg = "Cannot load balance for non-HTTP/S transport " + transport;
            log.error(msg);
            throw new SynapseException(msg);
        }
    }

    /*
     * Preparing the endpoint sequence for a new session establishment request
     */
    private void prepareEndPointSequence(MessageContext synCtx, Endpoint endpoint) {

        Object o = synCtx.getProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST);
        List<Endpoint> endpointList;
        if (o instanceof List) {
            endpointList = (List<Endpoint>) o;
            endpointList.add(this);

        } else {
            // this is the first endpoint in the hierarchy. so create the queue and
            // insert this as the first element.
            endpointList = new ArrayList<Endpoint>();
            endpointList.add(this);
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpointList);
        }

        // if the next endpoint is not a session affinity one, endpoint sequence ends
        // here. but we have to add the next endpoint to the list.
        if (!(endpoint instanceof TenantAwareLoadBalanceEndpoint)) {
            endpointList.add(endpoint);
            // Clearing out if there any any session information with current message
            if (dispatcher.isServerInitiatedSession()) {
                dispatcher.removeSessionID(synCtx);
            }
        }
    }

    protected void sendToApplicationMember(MessageContext synCtx,
                                           org.apache.axis2.clustering.Member currentMember,
                                           DynamicLoadbalanceFaultHandler faultHandler,
                                           boolean newSession) {
        //Rewriting the URL
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        //Removing the REST_URL_POSTFIX - this is a hack.
        //In this load balance endpoint we create an endpoint per request by setting the complete url as the address.
        //If a REST message comes Axis2FlexibleMEPClient append the REST_URL_POSTFIX to the address. Hence endpoint fails
        //do send the request. e.g.  http://localhost:8080/example/index.html/example/index.html
        axis2MsgCtx.removeProperty(NhttpConstants.REST_URL_POSTFIX);

        String transport = axis2MsgCtx.getTransportIn().getName();
        String address = synCtx.getTo().getAddress();
        int incomingPort = extractPort(synCtx, transport);
        EndpointReference to = getEndpointReferenceAfterURLRewrite(currentMember, transport, address, incomingPort);
        synCtx.setTo(to);

        Endpoint endpoint = getEndpoint(to, currentMember, synCtx);

        if (isFailover()) {
            faultHandler.setTo(to);
            faultHandler.setCurrentMember(currentMember);
            faultHandler.setCurrentEp(endpoint);
            synCtx.pushFaultHandler(faultHandler);
            synCtx.getEnvelope().build();
        }

        if (isSessionAffinityBasedLB() && newSession) {
            prepareEndPointSequence(synCtx, endpoint);
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, currentMember);
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_DISPATCHER, dispatcher);
            // we should also indicate that this is the first message in the session. so that
            // onFault(...) method can resend only the failed attempts for the first message.
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_FIRST_MESSAGE_IN_SESSION,
                    Boolean.TRUE);
        }

        Map<String, String> memberHosts;
        if ((memberHosts = (Map<String, String>) currentMember.getProperties().get(HttpSessionDispatcher.HOSTS)) == null) {
            currentMember.getProperties().put(HttpSessionDispatcher.HOSTS,
                    memberHosts = new HashMap<String, String>());
        }
        memberHosts.put(extractTargetHost(synCtx), "true");
        setupTransportHeaders(synCtx);
        setupLoadBalancerContextProperties(synCtx);

        // Update health stats
        LoadBalancerInFlightRequestCountCollector.getInstance().incrementRequestInflightCount(currentMember.getDomain());
        // Set the cluster id in the message context
        synCtx.setProperty(Constants.CLUSTER_ID, currentMember.getDomain());

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Sending request to endpoint: %s", to.getAddress()));
            }
            endpoint.send(synCtx);
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("io reactor shutdown")) {
                log.fatal("System cannot continue normal operation. Restarting", e);
                System.exit(121); // restart
            } else {
                throw new SynapseException(e);
            }
        }
    }

    public void setDispatcher(HttpSessionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public HttpSessionDispatcher getDispatcher() {
        return dispatcher;
    }

    public String getAlgorithmClassName() {
        return algorithmClassName;
    }

    public void setAlgorithmClassName(String algorithmClassName) {
        this.algorithmClassName = algorithmClassName;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public boolean isSessionAffinityBasedLB() {
        return sessionAffinity;
    }

    public void setSessionAffinity(boolean sessionAffinity) {
        this.sessionAffinity = sessionAffinity;
    }

    private class TenantAwareLoadBalanceFaultHandler extends DynamicLoadbalanceFaultHandler {
        private org.apache.axis2.clustering.Member currentMember;
        private Endpoint currentEp;
        private EndpointReference to;
        private Map<String, Boolean> faultyMembers;

        public TenantAwareLoadBalanceFaultHandler() {
            faultyMembers = new HashMap<String, Boolean>();
        }

        @Override
        public void setCurrentMember(org.apache.axis2.clustering.Member currentMember) {
            this.currentMember = currentMember;
        }

        @Override
        public void setCurrentEp(Endpoint currentEp) {
            this.currentEp = currentEp;
        }

        @Override
        public void setTo(EndpointReference to) {
            this.to = to;
        }

        @Override
        public void onFault(MessageContext synCtx) {
            // Cleanup endpoint if exists
            if (currentEp != null) {
                currentEp.destroy();
            }
            if (currentMember == null) {
                return;
            }

            // Add current member to faulty members
            faultyMembers.put(currentMember.getHostName(), true);

            currentMember = findNextMember(synCtx);
            if (currentMember == null) {
                String msg = String.format("No application members available to serve the request %s", synCtx.getTo().getAddress());
                if(log.isErrorEnabled()) {
                    log.error(msg);
                }
                throwSynapseException(synCtx, 404, msg);
            }
            if(faultyMembers.containsKey(currentMember.getHostName())) {
                // This member has been identified as faulty previously. It implies that
                // this request could not be served by any of the members in the cluster.
                throwSynapseException(synCtx, 404, String.format("Requested resource could not be found"));
            }

            synCtx.setTo(to);
            if (isSessionAffinityBasedLB()) {
                //We are sending the this message on a new session,
                // hence we need to remove previous session information
                Set pros = synCtx.getPropertyKeySet();
                if (pros != null) {
                    pros.remove(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION);
                }
            }
            sendToApplicationMember(synCtx, currentMember, this, true);
        }
    }
}
