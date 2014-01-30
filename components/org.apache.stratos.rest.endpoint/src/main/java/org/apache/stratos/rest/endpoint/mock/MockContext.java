package org.apache.stratos.rest.endpoint.mock;/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import java.util.*;

public class MockContext {
    private static MockContext mockContext = new MockContext(); // singleton
    private List<CartridgeDefinitionBean> cartridgeDefinitionBeanList = new LinkedList<CartridgeDefinitionBean>();
    private Map<String,Cartridge> availableSingleTenantCartridges = new HashMap<String,Cartridge>();
    private Map<String,Cartridge> availableMultiTenantCartridges = new HashMap<String,Cartridge>();
    private Map<String,Cartridge> subscribedCartridges = new HashMap<String,Cartridge>();
    private Map<String,TenantInfoBean> tenantMap = new HashMap<String, TenantInfoBean>();
    private Map<String,Partition> partitionMap = new HashMap<String, Partition>();
    private Map<String,AutoscalePolicy> autoscalePolicyMap = new HashMap<String, AutoscalePolicy>();
    private Map<String,DeploymentPolicy> deploymentPolicyMap = new HashMap<String, DeploymentPolicy>();

    private Set<Cartridge> temp = new HashSet<Cartridge>();


    private MockContext(){} // do not allow to initialize

    public static MockContext getInstance(){
        return mockContext;
    }


    public void addCartirdgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean){
        this.cartridgeDefinitionBeanList.add(cartridgeDefinitionBean);
        Cartridge cartridge = new Cartridge();
        cartridge.setCartridgeType(cartridgeDefinitionBean.type);
        cartridge.setDescription(cartridgeDefinitionBean.description);
        cartridge.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridge.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridge.setProvider(cartridgeDefinitionBean.provider);
        cartridge.setVersion(cartridgeDefinitionBean.version);

        if(cartridge.isMultiTenant()){
            availableMultiTenantCartridges.put(cartridge.getCartridgeType(), cartridge);
        }else{
            availableSingleTenantCartridges.put(cartridge.getCartridgeType(), cartridge);
        }
    }



    public Cartridge[] getAvailableMultiTenantCartridges(){
       return availableMultiTenantCartridges.values().toArray(new Cartridge[0]);
    }


    public Cartridge[] getAvailableSingleTenantCartridges(){
        return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
    }

    public Cartridge[] getSubscribedCartridges(){
        return subscribedCartridges.values().toArray(new Cartridge[0]);
    }

    public SubscriptionInfo subscribeToCartridge(CartridgeInfoBean cartridgeInfoBean){
         String cartridgeType = cartridgeInfoBean.getCartridgeType();
         String alias = cartridgeInfoBean.getAlias();
         Cartridge subscribedCartridge;
         // retrieve the cartridge from available ones
         if(availableSingleTenantCartridges.containsKey(cartridgeType)){
            subscribedCartridge = availableSingleTenantCartridges.get(cartridgeType);

         }else if(availableMultiTenantCartridges.containsKey(cartridgeType)){
             subscribedCartridge = availableMultiTenantCartridges.get(cartridgeType);
         }else {
             throw new RuntimeException("Wrong programme sequence"); // TODO; handle properly
         }
        //Proper way is copy construrctor
        Cartridge copy = new Cartridge();
        copy.setCartridgeType(subscribedCartridge.getCartridgeType());
        copy.setDescription(subscribedCartridge.getDescription());
        copy.setDisplayName(subscribedCartridge.getDisplayName());
        copy.setMultiTenant(subscribedCartridge.isMultiTenant());
        copy.setProvider(subscribedCartridge.getProvider());
        copy.setVersion(subscribedCartridge.getVersion());
        copy.setCartridgeAlias(alias);
        copy.setHostName("dummy.stratos.com");
        copy.setRepoURL("http://dummy.stratos.com/myrepo.git");

        subscribedCartridges.put(alias,copy);

        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setHostname(copy.getHostName());
        subscriptionInfo.setRepositoryURL(copy.getRepoURL());

        return subscriptionInfo;
    }

    public void unsubscribe(String alias) {
        if(subscribedCartridges.containsKey(alias)){
            subscribedCartridges.remove(alias);
        }
    }

    public void addTenant(TenantInfoBean tenantInfoBean){
          tenantMap.put(tenantInfoBean.getTenantDomain(),tenantInfoBean);
    }

    public TenantInfoBean getTenant(String tenantDomain){
          return tenantMap.get(tenantDomain);
    }

    public void deleteTenant(String tenantDomain) {
          tenantMap.remove(tenantDomain);
    }

    public TenantInfoBean[] getTenants() {
        return tenantMap.values().toArray(new TenantInfoBean[0]);
    }

    public TenantInfoBean[] retrievePartialSearchTenants(String searchDomain) {
        List<TenantInfoBean> searchResult = new LinkedList<TenantInfoBean>();
        for(String tenantDomain : tenantMap.keySet()){
            if(tenantDomain.contains(searchDomain)){
                 searchResult.add(new TenantInfoBean(tenantMap.get(tenantDomain)));
            }
        }
        return searchResult.toArray(new TenantInfoBean[0]);
    }

    public void activateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(true);
        } else{
            throw new RestAPIException("Invalid tenant domain");
        }
    }

    public void deactivateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(false);
        } else{
            throw new RestAPIException("Invalid tenant domain");
        }
    }

    public void deleteCartridgeDefinition(String cartridgeType) throws RestAPIException{
        if(availableSingleTenantCartridges.containsKey(cartridgeType)){
            availableSingleTenantCartridges.remove(cartridgeType);
        } else if(availableMultiTenantCartridges.containsKey(cartridgeType)){
            availableMultiTenantCartridges.remove(cartridgeType);
        } else{
            throw new RestAPIException("invalid cartridge type");
        }
    }

    public boolean addPartition(Partition partition) {
            partitionMap.put(partition.id, partition);
        return true;
    }

    public boolean addAutoScalingPolicyDefinition(AutoscalePolicy autoscalePolicy) {
            autoscalePolicyMap.put(autoscalePolicy.getId(), autoscalePolicy);
        return true;
    }

    public boolean addDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy) {
           deploymentPolicyMap.put(deploymentPolicy.id,deploymentPolicy);
        return true;

    }

    public Partition[] getPartitions() {
        return partitionMap.values().toArray(new Partition[0]);
    }

    public Partition getPartition(String partitionId) {
        return  partitionMap.get(partitionId);
    }


    public Partition[] getPartitionsOfPolicy(String deploymentPolicyId) {
        return deploymentPolicyMap.get(deploymentPolicyId).partition.toArray(new Partition[0]);
    }

    public PartitionGroup[] getPartitionGroups(String deploymentPolicyId) {
        return deploymentPolicyMap.get(deploymentPolicyId).partitionGroup.toArray(new PartitionGroup[0]);
    }

    public AutoscalePolicy[] getAutoscalePolicies() {
         return autoscalePolicyMap.values().toArray(new AutoscalePolicy[0]);
    }

    public AutoscalePolicy getAutoscalePolicies(String autoscalePolicyId) {
        return autoscalePolicyMap.get(autoscalePolicyId);
    }

    public DeploymentPolicy[] getDeploymentPolicies() {
        return deploymentPolicyMap.values().toArray(new DeploymentPolicy[0]);
    }

    public DeploymentPolicy getDeploymentPolicies(String deploymentPolicyId) {
        return deploymentPolicyMap.get(deploymentPolicyId);
    }

    public Partition[] getPartitions(String deploymentPolicyId, String partitionGroupId) {
         DeploymentPolicy deploymentPolicy = deploymentPolicyMap.get(deploymentPolicyId);
         for(PartitionGroup partitionGroup : deploymentPolicy.partitionGroup){
             if(partitionGroup.id.equals(partitionGroupId)){
                return partitionGroup.partition.toArray(new Partition[0]);
             }
         }
         return new Partition[0];
    }

    /*public DeploymentPolicy[] getValidDeploymentPolicies(String cartridgeType) {
        for(DeploymentPolicy deploymentPolicy : deploymentPolicyMap.values()){
            deploymentPolicy.
        }
    }*/
}
