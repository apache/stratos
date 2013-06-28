package org.wso2.carbon.lb.endpoint.endpoint;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.endpoints.utils.LoadbalanceAlgorithmFactory;
import org.apache.synapse.core.LoadBalanceMembershipHandler;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.DynamicLoadbalanceFaultHandler;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.dispatch.HttpSessionDispatcher;
import org.apache.synapse.endpoints.dispatch.SALSessions;
import org.apache.synapse.endpoints.dispatch.SessionInformation;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.util.HostContext;
import org.wso2.carbon.lb.common.conf.util.TenantDomainContext;
import org.wso2.carbon.lb.common.util.DomainMapping;
import org.wso2.carbon.lb.common.group.mgt.*;
import org.wso2.carbon.lb.common.cache.URLMappingCache;
import org.wso2.carbon.lb.endpoint.TenantAwareLoadBalanceEndpointException;
import org.wso2.carbon.lb.endpoint.TenantLoadBalanceMembershipHandler;
import org.wso2.carbon.lb.endpoint.internal.RegistryManager;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;

public class TenantAwareLoadBalanceEndpoint extends org.apache.synapse.endpoints.DynamicLoadbalanceEndpoint implements Serializable {

    private static final long serialVersionUID = 1577351815951789938L;
    private static final Log log = LogFactory.getLog(TenantAwareLoadBalanceEndpoint.class);
    /**
     * Axis2 based membership handler which handles members in multiple clustering domains
     */
    private TenantLoadBalanceMembershipHandler tlbMembershipHandler;

    /**
     * Key - host name
     * Value - {@link HostContext}
     */
    private Map<String, HostContext> hostContexts = new HashMap<String, HostContext>();

    private LoadBalancerConfiguration lbConfig;

    /**
     * keep the size of cache which used to keep hostNames of url mapping.
     */
    private URLMappingCache mappingCache;
    private RegistryManager registryManager;
    private int sizeOfCache;

    private boolean initialized;

    private String algorithm;
        private String configuration;
        private String failOver;

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        try {

            lbConfig = ConfigHolder.getInstance().getLbConfig();
            hostContexts = lbConfig.getHostContextMap();
            sizeOfCache = lbConfig.getLoadBalancerConfig().getSizeOfCache();
            mappingCache = URLMappingCache.getInstance(sizeOfCache);
            setSessionTimeout(lbConfig.getLoadBalancerConfig().getSessionTimeOut());
            setFailover(lbConfig.getLoadBalancerConfig().getFailOver());

        } catch (Exception e) {
            String msg = "Failed while reading Load Balancer configuration";
            log.error(msg, e);
            throw new TenantAwareLoadBalanceEndpointException(msg, e);
        }


        LoadbalanceAlgorithm algorithm = null;
        try {
            OMElement payload = AXIOMUtil.stringToOM(generatePayLoad());
            algorithm =
                    LoadbalanceAlgorithmFactory.
                            createLoadbalanceAlgorithm(payload, null);

        } catch (Exception e) {
            String msg = "Error While creating Load balance algorithm";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }

        if (!initialized) {
            super.init(synapseEnvironment);
            ConfigurationContext cfgCtx =
                                          ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext();
            ClusteringAgent clusteringAgent = cfgCtx.getAxisConfiguration().getClusteringAgent();
            if (clusteringAgent == null) {
                throw new SynapseException("Axis2 ClusteringAgent not defined in axis2.xml");
            }

            // Add the Axis2 GroupManagement agents
            if (hostContexts != null) {
                // iterate through each host context
                for (HostContext hostCtxt : hostContexts.values()) {
                    // each host can has multiple Tenant Contexts, iterate through them
                    for (TenantDomainContext tenantCtxt : hostCtxt.getTenantDomainContexts()) {

                        String domain = tenantCtxt.getDomain();
                        String subDomain = tenantCtxt.getSubDomain();

                        if (clusteringAgent.getGroupManagementAgent(domain, subDomain) == null) {
                            String gmAgentClass = lbConfig.getLoadBalancerConfig().getGroupManagementAgentClass();
                            GroupManagementAgent groupManagementAgent;
                            if (gmAgentClass != null) {
                                try {
                                    groupManagementAgent = (GroupManagementAgent) Class.forName(gmAgentClass).newInstance();
                                } catch (Exception e) {
                                    String msg = "Cannot instantiate GroupManagementAgent. Class: " + gmAgentClass;
                                    log.error(msg, e);
                                    throw new TenantAwareLoadBalanceEndpointException(msg, e);
                                }
                            } else {
                                groupManagementAgent = new SubDomainAwareGroupManagementAgent(subDomain);
                            }
                            clusteringAgent.addGroupManagementAgent(groupManagementAgent,
                                                                    domain, subDomain,-1);
                            if (log.isDebugEnabled()) {
                                log.debug("Group management agent added to cluster domain: " +
                                          domain + " and sub domain: " + subDomain);
                            }
                        }
                    }
                }

                tlbMembershipHandler =
                                       new TenantLoadBalanceMembershipHandler(hostContexts,
                                                                              algorithm, cfgCtx,
                                                                              isClusteringEnabled,
                                                                              getName());

                // set TenantLoadBalanceMembershipHandler for future reference
                ConfigHolder.getInstance().setTenantLoadBalanceMembershipHandler(tlbMembershipHandler);
            }

            // Initialize the SAL Sessions if already has not been initialized.
            SALSessions salSessions = SALSessions.getInstance();
            if (!salSessions.isInitialized()) {
                salSessions.initialize(isClusteringEnabled, cfgCtx);
            }
            setSessionAffinity(true);
            setDispatcher(new HttpSessionDispatcher());
            initialized = true;
            log.info("Tenant Aware Load Balance Endpoint is initialized.");
        }

    }

    	public void setConfiguration(String paramEle) {
    	        configuration = paramEle;
    	}

    	    public void setAlgorithm(String paramEle) {
    	        this.algorithm = paramEle;
    	    }

    	    public void setFailOver(String paramEle) {
    	        this.failOver = paramEle;
    	    }


    public String getName() {
		return "tlbEndpoint";
	}

    //TODO remove following hard coded element
    private String generatePayLoad() {
        return " <serviceDynamicLoadbalance failover=\"true\"\n" +
                "                                           algorithm=\"org.apache.synapse.endpoints.algorithms.RoundRobin\"" +
                //"                                           configuration=\"$system:loadbalancer.xml\"" +
                "/>";
    }

    public LoadBalanceMembershipHandler getLbMembershipHandler() {
        return tlbMembershipHandler;
    }


    public void send(MessageContext synCtx) {
        /*   setCookieHeader(synCtx);     */
        Member currentMember = null;
        SessionInformation sessionInformation = null;
        String actualHost = null;

        //Gathering required information for domain mapping
        org.apache.axis2.context.MessageContext axis2MessageContext =
                                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        Map<String, String> transportHeaders = (Map<String, String>) axis2MessageContext.
                getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String targetHost = transportHeaders.get(HTTP.TARGET_HOST);

        String port = "";
        boolean containsPort = false;
        if (targetHost.contains(":")) {
            containsPort = true;
            port = targetHost.substring(targetHost.indexOf(':') + 1, targetHost.length());
            targetHost = targetHost.substring(0, targetHost.indexOf(':'));
        }
        //Gathering required information for domain mapping done

        boolean isValidHost = tlbMembershipHandler.isAValidHostName(targetHost);
        DomainMapping domainMapping = null;
        if(!isValidHost){
            //check if the host is valid, if not valid, execute following code to check whether it is a mapped domain
            domainMapping = mappingCache.getMapping(targetHost);
            if(domainMapping == null){
                registryManager = new RegistryManager();
                domainMapping = registryManager.getMapping(targetHost);
                mappingCache.addValidMapping(targetHost, domainMapping);
            }
            if (domainMapping != null) {
                actualHost = domainMapping.getActualHost();

                if(containsPort){
                    transportHeaders.put(HTTP.TARGET_HOST, actualHost + ":" + port);
                } else {
                    transportHeaders.put(HTTP.TARGET_HOST, actualHost);
                }
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty("TRANSPORT_HEADERS" , transportHeaders);

            } else {
                String msg = "Invalid host name : " + targetHost;
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        if (isSessionAffinityBasedLB()) {
            // first check if this session is associated with a session. if so, get the endpoint
            // associated for that session.
            sessionInformation =
                    (SessionInformation) synCtx.getProperty(
                            SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION);

            currentMember = (Member) synCtx.getProperty(
                    SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER);

            if (sessionInformation == null && currentMember == null) {
                sessionInformation = dispatcher.getSession(synCtx);
                if (sessionInformation != null) {

                    if (log.isDebugEnabled()) {
                        log.debug("Current session id : " + sessionInformation.getId());
                    }

                    currentMember = sessionInformation.getMember();
                    synCtx.setProperty(
                            SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_MEMBER, currentMember);
                    // This is for reliably recovery any session information if while response is getting ,
                    // session information has been removed by cleaner.
                    // This will not be a cost as  session information a not heavy data structure
                    synCtx.setProperty(
                            SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, sessionInformation);
                }
            }

        }

        // Dispatch request the relevant member
//        String targetHost = getTargetHost(synCtx);
        ConfigurationContext configCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getConfigurationContext();

        if (tlbMembershipHandler.getConfigurationContext() == null) {
            tlbMembershipHandler.setConfigurationContext(configCtx);
        }

        if(tlbMembershipHandler.getClusteringAgent() == null) {
            tlbMembershipHandler.setConfigurationContext(configCtx);
        }

        TenantDynamicLoadBalanceFaultHandlerImpl faultHandler = new TenantDynamicLoadBalanceFaultHandlerImpl();
        log.debug("************* Actual Host: "+actualHost +" ****** Target Host: "+targetHost);
        faultHandler.setHost(actualHost != null ? actualHost : targetHost);

        if (sessionInformation != null && currentMember != null) {
            //send message on current session
            sessionInformation.updateExpiryTime();
            sendToApplicationMember(synCtx, currentMember, faultHandler, false);
        } else {
//            prepare for a new session
            int tenantId = getTenantId(synCtx);
            //check if this is a valid host name registered in ELB
            if(tlbMembershipHandler.isAValidHostName(targetHost)){
                currentMember = tlbMembershipHandler.getNextApplicationMember(targetHost, tenantId);
                if (currentMember == null) {
                    String msg = "No application members available";
                    log.error(msg);
                    throw new SynapseException(msg);
                }
                sendToApplicationMember(synCtx, currentMember, faultHandler, true);
            } else {
                if(domainMapping == null){
                    registryManager = new RegistryManager();
                    domainMapping = registryManager.getMapping(targetHost);
                    mappingCache.addValidMapping(targetHost, domainMapping);
                }
                if(domainMapping != null) {

                    actualHost = domainMapping.getActualHost();
                    
                    log.debug("************* Actual Host: "+actualHost +" ****** Target Host: "+targetHost);
                    faultHandler.setHost(actualHost != null ? actualHost : targetHost);

                    if(containsPort){
                        transportHeaders.put(HTTP.TARGET_HOST, actualHost + ":" + port);
                    } else {
                        transportHeaders.put(HTTP.TARGET_HOST, actualHost);
                    }
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty("TRANSPORT_HEADERS" , transportHeaders);

                    currentMember = tlbMembershipHandler.getNextApplicationMember(actualHost,tenantId);
                    sendToApplicationMember(synCtx,currentMember,faultHandler,true);
                }else {
                    String msg = "Invalid host name : " + targetHost;
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            }
        }
    }


//    public List<HostContext> getHostContexts() {
//        return Collections.unmodifiableList(hostContexts);
//    }

    /**
     * This FaultHandler will try to resend the message to another member if an error occurs
     * while sending to some member. This is a failover mechanism
     */

    /**
     * @param url to url for target
     * @return tenantID if tenant id available else 0
     */
    private int getTenantId(String url) {
        String servicesPrefix = "/t/";
        if (url != null && url.contains(servicesPrefix)) {
            int domainNameStartIndex =
                    url.indexOf(servicesPrefix) + servicesPrefix.length();
            int domainNameEndIndex = url.indexOf('/', domainNameStartIndex);
            String domainName = url.substring(domainNameStartIndex,
                    domainNameEndIndex == -1 ? url.length() : domainNameEndIndex);

            // return tenant id if domain name is not null
            if (domainName != null) {
                try {
                    return ConfigHolder.getInstance().getRealmService().getTenantManager().getTenantId(domainName);
                } catch (org.wso2.carbon.user.api.UserStoreException e) {
                    log.error("An error occurred while obtaining the tenant id.", e);
                }
            }
        }
        // return 0 if the domain name is null
        return 0;
    }

    private int getTenantId(MessageContext synCtx){
    	String url = synCtx.getTo().toString();
    	int tenantId = getTenantId(url);
    	// tenantId = 0 because domain name was null. May be this is the SSO response
    	if(tenantId == 0 && url.contains(MultitenantConstants.TENANT_DOMAIN+"=")){
    		// OK,this is the SAML SSO response from the IS
    		// e.g url = https://localhost:9444/acs?teantDomain=domain
    		String domainName = url.split("=").clone()[1];
    		// return tenant id if domain name is not null
            if (domainName != null) {
                try {
                    return ConfigHolder.getInstance().getRealmService().getTenantManager().getTenantId(domainName);
                } catch (org.wso2.carbon.user.api.UserStoreException e) {
                    log.error("An error occurred while obtaining the tenant id.", e);
                }
            }
    	}
    	return tenantId;
    }


    /**
     * This FaultHandler will try to resend the message to another member if an error occurs
     * while sending to some member. This is a failover mechanism
     */
    private class TenantDynamicLoadBalanceFaultHandlerImpl extends DynamicLoadbalanceFaultHandler {

        private EndpointReference to;
        private Member currentMember;
        private Endpoint currentEp;
        private String host;

        private static final int MAX_RETRY_COUNT = 5;

        // ThreadLocal variable to keep track of how many times this fault handler has been
        // called
        private ThreadLocal<Integer> callCount = new ThreadLocal<Integer>() {
            protected Integer initialValue() {
                return 0;
            }
        };

        public void setHost(String host) {
            log.debug("Setting host name: "+host);
            this.host = host;
        }

        public void setCurrentMember(Member currentMember) {
            this.currentMember = currentMember;
        }

        public void setTo(EndpointReference to) {
            this.to = to;
        }

        private TenantDynamicLoadBalanceFaultHandlerImpl() {
        }

        public void onFault(MessageContext synCtx) {
            if (currentMember == null || to == null) {
                return;
            }

            // Prevent infinite retrying to failed members
            callCount.set(callCount.get() + 1);
            if (callCount.get() >= MAX_RETRY_COUNT) {
                log.debug("Retrying to a failed member has stopped.");
                return;
            }

            //cleanup endpoint if exists
            if (currentEp != null) {
                currentEp.destroy();
            }
            Integer errorCode = (Integer) synCtx.getProperty(SynapseConstants.ERROR_CODE);
            if (errorCode != null) {
                if (errorCode.equals(NhttpConstants.CONNECTION_FAILED)) {
                    currentMember.suspend(10000);     // TODO: Make this configurable.
                    log.info("Suspended member " + currentMember + " for 10s due to connection failure to that member");
                }
                if (errorCode.equals(NhttpConstants.CONNECTION_FAILED) ||
                        errorCode.equals(NhttpConstants.CONNECT_CANCEL) ||
                        errorCode.equals(NhttpConstants.CONNECT_TIMEOUT)) {
                    
                    if (!synCtx.getFaultStack().isEmpty()) {
                        synCtx.getFaultStack().pop();
                    }
                    // Try to resend to another member
                    Member newMember = tlbMembershipHandler.getNextApplicationMember(host, getTenantId(synCtx.toString()));
                    if (newMember == null || newMember.isSuspended()) {
                        String msg = "No application members available having host name : "+host+
                                " and tenant id : "+getTenantId(synCtx.toString()+" and which is not suspended.");
                        log.error(msg);
                        throw new SynapseException(msg);
                    }
                    synCtx.setTo(to);
                    if (isSessionAffinityBasedLB()) {
                        // We are sending this message on a new session,
                        // hence we need to remove previous session information
                        Set pros = synCtx.getPropertyKeySet();
                        if (pros != null) {
                            pros.remove(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION);
                        }
                    }
                    try {
                        Thread.sleep(1000);  // Sleep for sometime before retrying
                    } catch (InterruptedException ignored) {
                    }
                    
                    if(synCtx == null || to == null) {
                        return;
                    }
                    log.info("Failed over to " + newMember);
                    sendToApplicationMember(synCtx, newMember, this, true);
                } else if (errorCode.equals(NhttpConstants.SND_IO_ERROR_SENDING) ||
                        errorCode.equals(NhttpConstants.CONNECTION_CLOSED)) {
                    // TODO: Envelope is consumed
                    String msg = "Error sending request! Connection to host "+host+
                            " might be closed. Error code: "+errorCode;
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            }
            // We cannot failover since we are using binary relay
        }

        public void setCurrentEp(Endpoint currentEp) {
            this.currentEp = currentEp;
        }
    }
}

