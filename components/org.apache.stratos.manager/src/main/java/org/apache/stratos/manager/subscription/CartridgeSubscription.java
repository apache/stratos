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

package org.apache.stratos.manager.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.io.Serializable;
import java.util.Map;

public abstract class CartridgeSubscription implements Serializable {


    private static final long serialVersionUID = -5197430500059231924L;
    private static Log log = LogFactory.getLog(CartridgeSubscription.class);
    private int subscriptionId;
    private String type;
    private String alias;
    private String autoscalingPolicyName;
    private String deploymentPolicyName;
    private Subscriber subscriber;
    private Repository repository;
    private CartridgeInfo cartridgeInfo;
    private PayloadData payloadData;
    private Cluster cluster;
    private String lbClusterId;
    private String subscriptionStatus;
    //private String serviceStatus;
    private String mappedDomain;
    //private List<String> connectedSubscriptionAliases;
    private String subscriptionKey;
    private SubscriptionTenancyBehaviour subscriptionTenancyBehaviour;

    
    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo instance
     * @param subscriptionTenancyBehaviour SubscriptionTenancyBehaviour instance
     */
    public CartridgeSubscription(CartridgeInfo cartridgeInfo, SubscriptionTenancyBehaviour subscriptionTenancyBehaviour) {

        this.setCartridgeInfo(cartridgeInfo);
        this.setType(cartridgeInfo.getType());
        this.setCluster(new Cluster());
        getCluster().setClusterDomain("");
        getCluster().setClusterSubDomain(CartridgeConstants.DEFAULT_SUBDOMAIN);
        getCluster().setMgtClusterDomain("");
        getCluster().setMgtClusterSubDomain(CartridgeConstants.DEFAULT_MGT_SUBDOMAIN);
        getCluster().setHostName(cartridgeInfo.getHostName());
        //this.setSubscriptionStatus(CartridgeConstants.SUBSCRIBED);
        //this.connectedSubscriptionAliases = new ArrayList<String>();
        this.setSubscriptionTenancyBehaviour(subscriptionTenancyBehaviour);
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
     * @throws org.apache.stratos.manager.exception.ADCException
     * @throws org.apache.stratos.manager.exception.PolicyException
     * @throws org.apache.stratos.manager.exception.UnregisteredCartridgeException
     * @throws org.apache.stratos.manager.exception.InvalidCartridgeAliasException
     * @throws org.apache.stratos.manager.exception.DuplicateCartridgeAliasException
     * @throws org.apache.stratos.manager.exception.RepositoryRequiredException
     * @throws org.apache.stratos.manager.exception.AlreadySubscribedException
     * @throws org.apache.stratos.manager.exception.RepositoryCredentialsRequiredException
     * @throws org.apache.stratos.manager.exception.InvalidRepositoryException
     * @throws org.apache.stratos.manager.exception.RepositoryTransportException
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

        setPayloadData(getSubscriptionTenancyBehaviour().create(getAlias(), getCluster(), getSubscriber(), getRepository(), getCartridgeInfo(),
                getSubscriptionKey(), getCustomPayloadEntries()));
    }

    /**
     * Unsubscribe from this cartridge subscription
     *
     * @throws ADCException
     * @throws NotSubscribedException
     */
    public void removeSubscription() throws ADCException, NotSubscribedException {

        getSubscriptionTenancyBehaviour().remove(getCluster().getClusterDomain(), getAlias());
        cleanupSubscription();
    }

    /**
     * Registers the subscription
     *
     * @param properties Any additional properties needed
     *
     * @return CartridgeSubscriptionInfo subscription populated with relevant data
     * @throws ADCException
     * @throws UnregisteredCartridgeException
     */
    public CartridgeSubscriptionInfo registerSubscription(Properties properties)
            throws ADCException, UnregisteredCartridgeException {

        // Properties props = new Properties();
        //props.setProperties(getCartridgeInfo().getProperties());

        getSubscriptionTenancyBehaviour().register (getCartridgeInfo(), getCluster(), getPayloadData(), getAutoscalingPolicyName(),
                getDeploymentPolicyName(), properties);

        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicyName(),
                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), null, "PENDING", getSubscriptionKey());
    }

    /**
     * Connect cartridges
     *
     * @param connectingCartridgeAlias Alias of connecting cartridge
     */
    public void connect (String connectingCartridgeAlias) {
        //connectedSubscriptionAliases.add(connectingCartridgeAlias);
    }

    /**
     * Disconnect from the cartridge subscription given by disconnectingCartridgeAlias
     *
     * @param disconnectingCartridgeAlias Alias of the cartridge subscription to disconnect
     */
    public void disconnect (String disconnectingCartridgeAlias) {
        //connectedSubscriptionAliases.remove(disconnectingCartridgeAlias);
    }

    /**
     * Manages the repository for the cartridge subscription
     *
     * @param repoURL Repository URL
     * @param repoUserName Repository Username
     * @param repoUserPassword Repository password
     * @param privateRepo public/private repository
     *
     * @return Repository populated with relevant information or null of not repository is relevant to this cartridge
     * subscription
     * @throws ADCException
     * @throws RepositoryRequiredException
     * @throws RepositoryCredentialsRequiredException
     * @throws RepositoryTransportException
     * @throws InvalidRepositoryException
     */
    public abstract  Repository manageRepository (String repoURL, String repoUserName, String repoUserPassword,
                                        boolean privateRepo) throws ADCException, RepositoryRequiredException, RepositoryCredentialsRequiredException,
                                                                    RepositoryTransportException, InvalidRepositoryException;

    /**
     * Cleans up the subscription information after unsubscribing
     *
     * @throws ADCException
     */
    protected void cleanupSubscription () throws ADCException {

        
    }

    public Map<String, String> getCustomPayloadEntries () {

        //no custom payload entries by default
        return null;
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

    /*public List<String> getConnectedSubscriptionAliases() {
        return connectedSubscriptionAliases;
    }*/

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

    public PayloadData getPayloadData() {
        return payloadData;
    }

    public void setPayloadData(PayloadData payloadData) {
        this.payloadData = payloadData;
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

    public SubscriptionTenancyBehaviour getSubscriptionTenancyBehaviour() {
        return subscriptionTenancyBehaviour;
    }

    public void setSubscriptionTenancyBehaviour(SubscriptionTenancyBehaviour subscriptionTenancyBehaviour) {
        this.subscriptionTenancyBehaviour = subscriptionTenancyBehaviour;
    }

    public boolean equals(Object other) {

        if(this == other) {
            return true;
        }
        if(!(other instanceof CartridgeSubscription)) {
            return false;
        }

        CartridgeSubscription that = (CartridgeSubscription)other;
        return ((this.type.equals(that.type)) && (this.alias.equals(that.alias)));
    }

    public int hashCode () {

        return type.hashCode() + alias.hashCode();
    }

    @Override
    public String toString() {
        return "CartridgeSubscription [subscriptionId=" + subscriptionId + ", type=" + type +
               ", alias=" + alias + ", autoscalingPolicyName=" + autoscalingPolicyName +
               ", deploymentPolicyName=" + deploymentPolicyName + ", subscriber=" + subscriber +
               ", repository=" + repository + ", cartridgeInfo=" + cartridgeInfo + ", payload=" +
               payloadData + ", cluster=" + cluster + "]";
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }
}
