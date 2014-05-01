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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.HTTP;
import org.apache.stratos.load.balancer.RequestDelegator;
import org.apache.stratos.load.balancer.algorithm.LoadBalanceAlgorithmFactory;
import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;
import org.apache.stratos.load.balancer.conf.domain.MemberIpType;
import org.apache.stratos.load.balancer.conf.domain.TenantIdentifier;
import org.apache.stratos.load.balancer.statistics.InFlightRequestDecrementCallable;
import org.apache.stratos.load.balancer.statistics.InFlightRequestIncrementCallable;
import org.apache.stratos.load.balancer.statistics.LoadBalancerStatisticsExecutor;
import org.apache.stratos.load.balancer.util.Constants;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
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
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TenantAwareLoadBalanceEndpoint extends org.apache.synapse.endpoints.LoadbalanceEndpoint implements Serializable {
    private static final long serialVersionUID = -6612900240087164008L;

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
     * @param currentMember
     */
    private void setupLoadBalancerContextProperties(MessageContext synCtx, org.apache.axis2.clustering.Member currentMember) {
        String lbHostName = extractTargetHost(synCtx);
        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        TransportInDescription httpTransportIn = axis2MsgCtx.getConfigurationContext().getAxisConfiguration().getTransportIn("http");
        TransportInDescription httpsTransportIn = axis2MsgCtx.getConfigurationContext().getAxisConfiguration().getTransportIn("https");
        String lbHttpPort = (String) httpTransportIn.getParameter("port").getValue();
        String lbHttpsPort = (String) httpsTransportIn.getParameter("port").getValue();
        String clusterId = currentMember.getProperties().getProperty(Constants.CLUSTER_ID);

        synCtx.setProperty(Constants.LB_HOST_NAME, lbHostName);
        synCtx.setProperty(Constants.LB_HTTP_PORT, lbHttpPort);
        synCtx.setProperty(Constants.LB_HTTPS_PORT, lbHttpsPort);
        synCtx.setProperty(Constants.CLUSTER_ID, clusterId);
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
        try {
            String targetHost = extractTargetHost(synCtx);
            if (!requestDelegator.isTargetHostValid(targetHost)) {
                throwSynapseException(synCtx, 404, String.format("Unknown host name %s", targetHost));
            }

            Member member = null;
            if (LoadBalancerConfiguration.getInstance().isMultiTenancyEnabled()) {
                // Try to find next member from multi-tenant cluster map
                if (log.isDebugEnabled()) {
                    log.debug("Multi-tenancy enabled, scanning URL for tenant...");
                }
                String url = extractUrl(synCtx);
                int tenantId = scanUrlForTenantId(url);
                if (tenantExists(tenantId)) {
                    // Tenant found, find member from hostname and tenant id
                    member = requestDelegator.findNextMemberFromTenantId(targetHost, tenantId);
                } else {
                    // Tenant id not found in URL, find member from host name
                    member = requestDelegator.findNextMemberFromHostName(targetHost);
                }
            } else {
                // Find next member from host name
                member = requestDelegator.findNextMemberFromHostName(targetHost);
            }

            if (member == null)
                return null;

            // Find mapping outgoing port for incoming port
            int incomingPort = findIncomingPort(synCtx);
            Port outgoingPort = findOutgoingPort(member, incomingPort);
            if (outgoingPort == null) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Could not find port for proxy port %d in member %s", incomingPort,
                            member.getMemberId()));
                }
                throwSynapseException(synCtx, 500, "Internal server error");
            }

            // Create Axi2 member object
            org.apache.axis2.clustering.Member axis2Member = new org.apache.axis2.clustering.Member(
                    getMemberIp(synCtx, member), outgoingPort.getValue());
            axis2Member.setDomain(member.getClusterId());
            axis2Member.setActive(member.isActive());

            // Set cluster id and partition id in message context
            axis2Member.getProperties().setProperty(Constants.CLUSTER_ID, member.getClusterId());
            return axis2Member;
        }
        catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Could not find a member to serve the request");
            }
            throwSynapseException(synCtx, 500, "Internal server error");
        }
        return null;
    }

    /***
     * Find incoming port from request URL.
     * @param synCtx
     * @return
     * @throws MalformedURLException
     */
    private int findIncomingPort(MessageContext synCtx) throws MalformedURLException {
        try {
            URL url = new URL(extractUrl(synCtx));
            if(log.isDebugEnabled()) {
                log.debug("Incoming request port found: " + url.getPort());
            }
            return url.getPort();
        }
        catch (MalformedURLException e) {
            if(log.isErrorEnabled()) {
                log.error("Could not extract port from incoming request", e);
            }
            throw e;
        }
    }

    /***
     * Find mapping outgoing port for incoming port.
     * @param member
     * @param incomingPort
     * @return
     * @throws MalformedURLException
     */
    private Port findOutgoingPort(Member member, int incomingPort) throws MalformedURLException {
        if((member != null) && (member.getPorts() != null)) {
            Port outgoingPort = member.getPort(incomingPort);
            if(log.isDebugEnabled()) {
                log.debug("Outgoing request port found: " + outgoingPort.getValue());
            }
            return outgoingPort;
        }
        return null;
    }

    /***
     * Get members private or public ip according to load balancer configuration.
     * @param synCtx
     * @param member
     * @return
     */
    private String getMemberIp(MessageContext synCtx, Member member) {
        if(LoadBalancerConfiguration.getInstance().isTopologyEventListenerEnabled()) {
            if(LoadBalancerConfiguration.getInstance().getTopologyMemberIpType() == MemberIpType.Public) {
                // Return member's public IP address
                if(StringUtils.isBlank(member.getMemberPublicIp())) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Member public IP address not found: [member] %s", member.getMemberId()));
                    }
                    throwSynapseException(synCtx, 500, "Internal server error");
                }
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Using member public IP address: [member] %s [ip] %s", member.getMemberId(), member.getMemberPublicIp()));
                }
                return member.getMemberPublicIp();
            }
        }
        // Return member's private IP address
        if(StringUtils.isBlank(member.getMemberIp())) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Member IP address not found: [member] %s", member.getMemberId()));
            }
            throwSynapseException(synCtx, 500, "Internal server error");
        }
        if(log.isDebugEnabled()) {
            log.debug(String.format("Using member IP address: [member] %s [ip] %s", member.getMemberId(), member.getMemberIp()));
        }
        return member.getMemberIp();
    }

    private String extractUrl(MessageContext synCtx) {
        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        return (String) axis2MessageCtx.getProperty(Constants.AXIS2_MSG_CTX_TRANSPORT_IN_URL);
    }

    private int scanUrlForTenantId(String url) {
        int tenantId = -1;
        String regex = LoadBalancerConfiguration.getInstance().getTenantIdentifierRegex();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Request URL: %s ", url));
            log.debug(String.format("Tenant identifier regex: %s ", regex));
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            if (LoadBalancerConfiguration.getInstance().getTenantIdentifier() == TenantIdentifier.TenantId) {
                if (log.isDebugEnabled()) {
                    log.debug("Identifying tenant using tenant id...");
                }
                tenantId = Integer.parseInt(matcher.group(1));
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Tenant identifier found: [tenant-id] %d", tenantId));
                }
            } else if (LoadBalancerConfiguration.getInstance().getTenantIdentifier() == TenantIdentifier.TenantDomain) {
                if (log.isDebugEnabled()) {
                    log.debug("Identifying tenant using tenant domain...");
                }
                String tenantDomain = matcher.group(1);
                tenantId = findTenantIdFromTenantDomain(tenantDomain);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Tenant identifier found: [tenant-domain] %s [tenant-id] %d", tenantDomain, tenantId));
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Tenant identifier not found in URL");
            }
        }
        return tenantId;
    }

    private boolean tenantExists(int tenantId) {
        try {
            TenantManager.acquireReadLock();
            return TenantManager.getInstance().tenantExists(tenantId);
        } finally {
            TenantManager.releaseReadLock();
        }
    }

    private int findTenantIdFromTenantDomain(String tenantDomain) {
        try {
            TenantManager.acquireReadLock();
            Tenant tenant = TenantManager.getInstance().getTenant(tenantDomain);
            if (tenant != null) {
                return tenant.getTenantId();
            }
            return -1;
        } finally {
            TenantManager.releaseReadLock();
        }
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
        definition.setTimeoutAction(SynapseConstants.DISCARD_AND_FAULT);
        definition.setTimeoutDuration(LoadBalancerConfiguration.getInstance().getEndpointTimeout());
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
                                                                  String address) {

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
                    String msg = String.format("URL is malformed: %s", address);
                    log.error(msg, e);
                    throw new SynapseException(msg, e);
                }
            }

            String hostName = currentMember.getHostName();
            int port = currentMember.getPort();
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
        EndpointReference to = getEndpointReferenceAfterURLRewrite(currentMember, transport, address);
        synCtx.setTo(to);

        Endpoint endpoint = getEndpoint(to, currentMember, synCtx);

        // Push fault handler to manage statistics and fail-over logic
        faultHandler.setTo(to);
        faultHandler.setCurrentMember(currentMember);
        faultHandler.setCurrentEp(endpoint);
        synCtx.pushFaultHandler(faultHandler);
        synCtx.getEnvelope().build();

        if (isSessionAffinityBasedLB()) {
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_DEFAULT_SESSION_TIMEOUT, getSessionTimeout());
            synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_DISPATCHER, dispatcher);

            if (newSession) {
                prepareEndPointSequence(synCtx, endpoint);
                synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, currentMember);
                // we should also indicate that this is the first message in the session. so that
                // onFault(...) method can resend only the failed attempts for the first message.
                synCtx.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_FIRST_MESSAGE_IN_SESSION,
                        Boolean.TRUE);
            }
        }

        Map<String, String> memberHosts;
        if ((memberHosts = (Map<String, String>) currentMember.getProperties().get(HttpSessionDispatcher.HOSTS)) == null) {
            currentMember.getProperties().put(HttpSessionDispatcher.HOSTS,
                    memberHosts = new HashMap<String, String>());
        }
        memberHosts.put(extractTargetHost(synCtx), "true");
        setupTransportHeaders(synCtx);
        setupLoadBalancerContextProperties(synCtx, currentMember);

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Sending request to endpoint: %s", to.getAddress()));
            }
            endpoint.send(synCtx);

            // Increment in-flight request count
            incrementInFlightRequestCount(synCtx);
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("io reactor shutdown")) {
                log.fatal("System cannot continue normal operation. Restarting", e);
                System.exit(121); // restart
            } else {
                throw new SynapseException(e);
            }
        }
    }

    private void incrementInFlightRequestCount(MessageContext messageContext) {
        try {
            String clusterId = (String) messageContext.getProperty(Constants.CLUSTER_ID);
            if(StringUtils.isBlank(clusterId)) {
                throw new RuntimeException("Cluster id not found in message context");
            }
            FutureTask<Object> task = new FutureTask<Object>(new InFlightRequestIncrementCallable(clusterId));
            LoadBalancerStatisticsExecutor.getInstance().getService().submit(task);
        }
        catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Could not increment in-flight request count", e);
            }
        }
    }

    private void decrementInFlightRequestCount(MessageContext messageContext) {
        try {
            String clusterId = (String) messageContext.getProperty(Constants.CLUSTER_ID);
            if(StringUtils.isBlank(clusterId)) {
                throw new RuntimeException("Cluster id not found in message context");
            }
            FutureTask<Object> task = new FutureTask<Object>(new InFlightRequestDecrementCallable(clusterId));
            LoadBalancerStatisticsExecutor.getInstance().getService().submit(task);
        }
        catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Could not decrement in-flight request count", e);
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
            if (log.isWarnEnabled()) {
                log.warn(String.format("A fault detected in message sent to: %s ", (to != null) ? to.getAddress() : "address not found"));
            }

            // Decrement in-flight request count
            decrementInFlightRequestCount(synCtx);

            if (isFailover()) {
                if(log.isDebugEnabled()) {
                    log.debug("Fail-over enabled, trying to send the message to the next available member");
                }

                // Cleanup endpoint if exists
                if (currentEp != null) {
                    currentEp.destroy();
                }
                if (currentMember == null) {
                    if(log.isErrorEnabled()) {
                        log.error("Current member is null, could not fail-over");
                    }
                    return;
                }

                // Add current member to faulty members
                faultyMembers.put(currentMember.getHostName(), true);

                currentMember = findNextMember(synCtx);
                if (currentMember == null) {
                    String msg = String.format("No members available to serve the request %s", (to != null) ? to.getAddress() : "address not found");
                    if (log.isErrorEnabled()) {
                        log.error(msg);
                    }
                    throwSynapseException(synCtx, 404, msg);
                }
                if (faultyMembers.containsKey(currentMember.getHostName())) {
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
}
