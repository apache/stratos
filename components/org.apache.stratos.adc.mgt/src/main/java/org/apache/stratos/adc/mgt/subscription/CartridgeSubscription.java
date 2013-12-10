/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.adc.mgt.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.custom.domain.RegistryManager;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dao.Cluster;
import org.apache.stratos.adc.mgt.dns.DNSManager;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.payload.Payload;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.service.RepositoryInfoBean;
import org.apache.stratos.adc.mgt.subscriber.Subscriber;
import org.apache.stratos.adc.mgt.subscription.tenancy.SubscriptionMultiTenantBehaviour;
import org.apache.stratos.adc.mgt.subscription.tenancy.SubscriptionSingleTenantBehaviour;
import org.apache.stratos.adc.mgt.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.adc.mgt.utils.*;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class CartridgeSubscription implements Serializable {

    private static Log log = LogFactory.getLog(CartridgeSubscription.class);
    private int subscriptionId;
    private String type;
    private String alias;
    private String autoscalingPolicyName;
    private String deploymentPolicyName;
    private Subscriber subscriber;
    private Repository repository;
    private CartridgeInfo cartridgeInfo;
    private Payload payload;
    private Cluster cluster;
    private String subscriptionStatus;
    private String serviceStatus;
    private String mappedDomain;
    private List<String> connectedSubscriptionAliases;
    private String subscriptionKey;
    protected SubscriptionTenancyBehaviour subscriptionTenancyBehaviour;

    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo subscription
     */
    public CartridgeSubscription(CartridgeInfo cartridgeInfo) {

        this.setCartridgeInfo(cartridgeInfo);
        this.setType(cartridgeInfo.getType());
        this.setCluster(new Cluster());
        getCluster().setClusterDomain("");
        getCluster().setClusterSubDomain(CartridgeConstants.DEFAULT_SUBDOMAIN);
        getCluster().setMgtClusterDomain("");
        getCluster().setMgtClusterSubDomain(CartridgeConstants.DEFAULT_MGT_SUBDOMAIN);
        getCluster().setHostName(cartridgeInfo.getHostName());
        this.setSubscriptionStatus(CartridgeConstants.SUBSCRIBED);
        this.connectedSubscriptionAliases = new ArrayList<String>();
        if(getCartridgeInfo().getMultiTenant()) {
            subscriptionTenancyBehaviour = new SubscriptionMultiTenantBehaviour(this);
        }
        else {
            subscriptionTenancyBehaviour = new SubscriptionSingleTenantBehaviour(this);
        }
    }

    /**
     * Subscribes to this cartridge subscription
     *
     * @param subscriber Subscriber subscription
     * @param alias Alias of the cartridge subscription
     * @param autoscalingPolicy Auto scaling policy
     * @param deploymentPolicyName Deployment policy
     * @param repository Relevenat Repository subscription
     *
     * @throws org.apache.stratos.adc.mgt.exception.ADCException
     * @throws org.apache.stratos.adc.mgt.exception.PolicyException
     * @throws org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException
     * @throws org.apache.stratos.adc.mgt.exception.InvalidCartridgeAliasException
     * @throws org.apache.stratos.adc.mgt.exception.DuplicateCartridgeAliasException
     * @throws org.apache.stratos.adc.mgt.exception.RepositoryRequiredException
     * @throws org.apache.stratos.adc.mgt.exception.AlreadySubscribedException
     * @throws org.apache.stratos.adc.mgt.exception.RepositoryCredentialsRequiredException
     * @throws org.apache.stratos.adc.mgt.exception.InvalidRepositoryException
     * @throws org.apache.stratos.adc.mgt.exception.RepositoryTransportException
     */
    public void createSubscription (Subscriber subscriber, String alias, String autoscalingPolicy,
                                    String deploymentPolicyName, Repository repository)
            throws ADCException, PolicyException, UnregisteredCartridgeException, InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, RepositoryRequiredException, AlreadySubscribedException,
            RepositoryCredentialsRequiredException, InvalidRepositoryException, RepositoryTransportException {

        setSubscriber(subscriber);
        setAlias(alias);
        setAutoscalingPolicyName(autoscalingPolicy);
        setDeploymentPolicyName(deploymentPolicyName);
        setRepository(repository);
    }

    /**
     * Unsubscribe from this cartridge subscription
     *
     * @throws ADCException
     * @throws NotSubscribedException
     */
    public abstract void removeSubscription() throws ADCException, NotSubscribedException;

    /**
     * Registers the subscription
     *
     * @param properties Any additional properties needed
     *
     * @return CartridgeSubscriptionInfo subscription populated with relevant data
     * @throws ADCException
     * @throws UnregisteredCartridgeException
     */
    public abstract CartridgeSubscriptionInfo registerSubscription(Properties properties)
            throws ADCException, UnregisteredCartridgeException;

    /**
     * Connect cartridges
     *
     * @param connectingCartridgeAlias Alias of connecting cartridge
     */
    public void connect (String connectingCartridgeAlias) {
        connectedSubscriptionAliases.add(connectingCartridgeAlias);
    }

    /**
     * Disconnect from the cartridge subscription given by disconnectingCartridgeAlias
     *
     * @param disconnectingCartridgeAlias Alias of the cartridge subscription to disconnect
     */
    public void disconnect (String disconnectingCartridgeAlias) {
        connectedSubscriptionAliases.remove(disconnectingCartridgeAlias);
    }

    /**
     * Creates the relevant payload parameters for this cartridge subscription
     *
     * @return PayloadArg subscription
     * @throws ADCException in an errpr
     */
    public PayloadArg createPayloadParameters() throws ADCException {

        PayloadArg payloadArg = new PayloadArg();
        payloadArg.setCartridgeInfo(getCartridgeInfo());
        //payloadArg.setPolicy(getAutoscalingPolicyName());
        payloadArg.setMultitenant(getCartridgeInfo().getMultiTenant());
        payloadArg.setTenantId(getSubscriber().getTenantId());
        payloadArg.setTenantDomain(getSubscriber().getTenantDomain());
        payloadArg.setCartridgeAlias(getAlias());
        payloadArg.setServiceName(getCartridgeInfo().getType());
        payloadArg.setSubscriptionKey(subscriptionKey);
        return payloadArg;
    }

    /**
     * Manages the repository for the cartridge subscription
     *
     * @param repoURL Repository URL
     * @param repoUserName Repository Username
     * @param repoUserPassword Repository password
     * @param privateRepo public/private repository
     * @param cartridgeAlias Alias of the cartridge subscription
     * @param cartridgeInfo CartridgeInfo subscription
     * @param tenantDomain Domain of the tenant
     *
     * @return Repository populated with relevant information or null of not repository is relevant to this cartridge
     * subscription
     * @throws ADCException
     * @throws RepositoryRequiredException
     * @throws RepositoryCredentialsRequiredException
     * @throws RepositoryTransportException
     * @throws InvalidRepositoryException
     */
    public Repository manageRepository (String repoURL, String repoUserName, String repoUserPassword,
                                        boolean privateRepo, String cartridgeAlias, CartridgeInfo cartridgeInfo,
                                        String tenantDomain)

            throws ADCException, RepositoryRequiredException, RepositoryCredentialsRequiredException,
            RepositoryTransportException, InvalidRepositoryException {

        if (!new Boolean(System.getProperty(CartridgeConstants.FEATURE_INTERNAL_REPO_ENABLED))) {
            if (log.isDebugEnabled()) {
                log.debug("Internal repo feature is not enabled.");
            }
        }

        Repository repository = new Repository();
        if (repoURL != null && repoURL.trim().length() > 0) {
            log.info("External REPO URL is provided as [" + repoURL +
                    "]. Therefore not creating a new repo.");
            //repository.setRepoName(repoURL.substring(0, repoURL.length()-4)); // remove .git part
            repository.setUrl(repoURL);
            repository.setUserName(repoUserName);
            repository.setPassword(repoUserPassword);
            repository.setPrivateRepository(privateRepo);

        } else {

            log.info("External git repo url not provided for tenant "
                    + tenantDomain + ", creating an git internal repository");

            // for internal repos  internal git server username and password is used.
            repository.setUserName(System.getProperty(CartridgeConstants.INTERNAL_GIT_USERNAME));
            repository.setPassword(System.getProperty(CartridgeConstants.INTERNAL_GIT_PASSWORD));
            try {
                new RepositoryCreator(new RepositoryInfoBean(repoURL, cartridgeAlias, tenantDomain,
                        repository.getUserName(), repository.getPassword(), cartridgeInfo.getDeploymentDirs(),
                        cartridgeInfo)).createInternalRepository();

            } catch (Exception e) {
                throw new ADCException(e.getMessage(), e);
            }
            String repoName = tenantDomain + "/" + cartridgeAlias;
            repository.setUrl("https://" + System.getProperty(CartridgeConstants.GIT_HOST_NAME) + ":8443/git/" +
                    repoName);
        }

        // Validate Remote Repository.
        ApplicationManagementUtil.validateRepository(repoURL, repoUserName, repoUserPassword, privateRepo,
                new Boolean(System.getProperty(CartridgeConstants.FEATURE_EXTERNAL_REPO_VAIDATION_ENABLED)));

        return repository;
    }

    /**
     * Cleans up the subscription information after unsubscribing
     *
     * @throws ADCException
     */
    protected void cleanupSubscription () throws ADCException {

        try {
            new RepositoryFactory().destroyRepository(alias, subscriber.getTenantDomain(),
                    subscriber.getAdminUserName());
            log.info("Repo is destroyed successfully.. ");

        } catch (Exception e) {
            String errorMsg = "Error in destroying repository for tenant " + subscriber.getTenantDomain() +
                    "cartridge type " + type;
            log.error(errorMsg);
        }

        try {
            PersistenceManager.updateSubscriptionState(subscriptionId, "UNSUBSCRIBED");

        } catch (Exception e) {
            String errorMsg = "Error in unscubscribing from cartridge, alias " + alias + ", tenant " +
                    subscriber.getTenantDomain();
            throw new ADCException(errorMsg, e);
        }

        //TODO: FIXME: do we need this?
        new DNSManager().removeSubDomain(getCluster().getHostName());

        try {
            new RegistryManager().removeDomainMappingFromRegistry(getCluster().getHostName());

        } catch (Exception e) {
            String errorMsg = "Error in removing domain mapping, alias " + alias + ", tenant " +
                    subscriber.getTenantDomain();
            log.error(errorMsg, e);
        }

        TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();
        String[] ips = topologyMgtService.getActiveIPs(type, getCluster().getClusterDomain(), getCluster().getClusterSubDomain());
        try {
            PersistenceManager.updateInstanceState("INACTIVE", ips, getCluster().getClusterDomain(), getCluster().getClusterSubDomain(), type);

        } catch (Exception e) {
            String errorMsg = "Error in updating state to INACTIVE";
            log.error(errorMsg, e);
        }

        this.setSubscriptionStatus(CartridgeConstants.UNSUBSCRIBED);
    }

    public String getType() {
        return type;
    }

    public String getAlias() {
        return alias;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public Repository getRepository() {
        return repository;
    }

    public List<String> getConnectedSubscriptionAliases() {
        return connectedSubscriptionAliases;
    }

    public CartridgeInfo getCartridgeInfo() {
        return cartridgeInfo;
    }

    public String getHostName() {
        return getCluster().getHostName();
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getClusterDomain() {
        return getCluster().getClusterDomain();
    }

    public void setClusterDomain(String clusterDomain) {
        getCluster().setClusterDomain(clusterDomain);
    }

    public String getClusterSubDomain() {
        return getCluster().getClusterSubDomain();
    }

    public void setClusterSubDomain(String clusterSubDomain) {
        getCluster().setClusterSubDomain(clusterSubDomain);
    }

    public String getMgtClusterDomain() {
        return getCluster().getMgtClusterDomain();
    }

    public void setMgtClusterDomain(String mgtClusterDomain) {
        getCluster().setMgtClusterDomain(mgtClusterDomain);
    }

    public String getMgtClusterSubDomain() {
        return getCluster().getMgtClusterSubDomain();
    }

    public void setMgtClusterSubDomain(String mgtClusterSubDomain) {
        getCluster().setMgtClusterSubDomain(mgtClusterSubDomain);
    }

    public void setHostName(String hostName) {
        getCluster().setHostName(hostName);
    }

    public String getAutoscalingPolicyName() {
        return autoscalingPolicyName;
    }

    public void setAutoscalingPolicyName(String autoscalingPolicyName) {
        this.autoscalingPolicyName = autoscalingPolicyName;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCartridgeInfo(CartridgeInfo cartridgeInfo) {
        this.cartridgeInfo = cartridgeInfo;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getMappedDomain() {
        return mappedDomain;
    }

    public void setMappedDomain(String mappedDomain) {
        this.mappedDomain = mappedDomain;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public void setSubscriptionKey(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getDeploymentPolicyName() {
        return deploymentPolicyName;
    }

    public void setDeploymentPolicyName(String deploymentPolicyName) {
        this.deploymentPolicyName = deploymentPolicyName;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }
}
