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
import org.apache.stratos.rest.endpoint.bean.StratosAdminResponse;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
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
    private Map<String,ServiceDefinitionBean> serviceDefinitionMap = new HashMap<String, ServiceDefinitionBean>();
    private Map<String,Cluster> clusterMap = new HashMap<String, Cluster>();

    private Set<Cartridge> temp = new HashSet<Cartridge>();


    private MockContext(){} // do not allow to initialize

    public static MockContext getInstance(){
        return mockContext;
    }


    public StratosAdminResponse addCartirdgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean){
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
        System.out.println(availableMultiTenantCartridges.size());
        System.out.println(availableSingleTenantCartridges.size());
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed deployment policy definition with type ");
        return stratosAdminResponse;
    }



    public Cartridge[] getAvailableMultiTenantCartridges() throws RestAPIException{
       return availableMultiTenantCartridges.values().toArray(new Cartridge[0]);
    }


    public Cartridge[] getAvailableSingleTenantCartridges() throws RestAPIException{
        return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
    }


    public Cartridge[] getAvailableLbCartridges() throws RestAPIException{
        /*Map<String,Cartridge> availableLbCartridges = new HashMap<String,Cartridge>();
        Iterator it = availableSingleTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(cartridge.getCartridgeType().equals("lb")){
                availableLbCartridges.put(cartridge.getCartridgeType(),cartridge);
            }
            it.remove();
        }

        it = availableMultiTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(cartridge.getCartridgeType().equals("lb")){
                availableLbCartridges.put(cartridge.getCartridgeType(),cartridge);
            }
            it.remove();
        }
        return availableLbCartridges.values().toArray(new Cartridge[0]);*/
        return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
    }

    public Cartridge[] getAvailableCartridges() throws RestAPIException{
        /*Map<String, Cartridge> availableCartridges = new HashMap<String,Cartridge>();
        Iterator it = availableSingleTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(!cartridge.getCartridgeType().equals("lb")){
                availableCartridges.put(cartridge.getCartridgeType(),cartridge);
            }
            it.remove();
        }

        it = availableMultiTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(!cartridge.getCartridgeType().equals("lb")){
                availableCartridges.put(cartridge.getCartridgeType(),cartridge);
            }
            it.remove();
        }
        System.out.println(availableCartridges.size());
        return availableCartridges.values().toArray(new Cartridge[0]);*/
        return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
    }


    public Cartridge[] getSubscribedCartridges() throws RestAPIException{
        return subscribedCartridges.values().toArray(new Cartridge[0]);
    }

    public SubscriptionInfo subscribeToCartridge(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException{
         String cartridgeType = cartridgeInfoBean.getCartridgeType();
         String alias = cartridgeInfoBean.getAlias();
         Cartridge subscribedCartridge;
         // retrieve the cartridge from available ones
         if(availableSingleTenantCartridges.containsKey(cartridgeType)){
            subscribedCartridge = availableSingleTenantCartridges.get(cartridgeType);

         }else if(availableMultiTenantCartridges.containsKey(cartridgeType)){
             subscribedCartridge = availableMultiTenantCartridges.get(cartridgeType);
         }else {
             String msg = "Wrong programme sequence";
             throw new RestAPIException(msg);
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

    public StratosAdminResponse unsubscribe(String alias) throws RestAPIException{
        if(subscribedCartridges.containsKey(alias)){
            subscribedCartridges.remove(alias);
        }else{
            throw new RestAPIException("Unable to un-subscribe");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully un-subscribed");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException{
        try{
            tenantMap.put(tenantInfoBean.getTenantDomain(),tenantInfoBean);
        }catch (Exception e){
            throw new RestAPIException(e.getMessage());
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully added new Tenant");
        return stratosAdminResponse;
    }

    public TenantInfoBean getTenant(String tenantDomain) throws RestAPIException{
        if(!tenantMap.containsKey(tenantDomain)){
            throw new RestAPIException("Information for tenant: " + tenantDomain + " is not available");
        }
        return tenantMap.get(tenantDomain);
    }

    public Cartridge getCartridgeInfo(String alias) throws RestAPIException{
        if(!subscribedCartridges.containsKey(alias)){
            throw new RestAPIException("Cartridge information is not available.");
        }
        return subscribedCartridges.get(alias);
    }

    public Cartridge getAvailableSingleTenantCartridgeInfo(String cartridgeType) throws RestAPIException{
        if(!availableSingleTenantCartridges.containsKey(cartridgeType)){
            throw new RestAPIException("Cartridge is not available.");
        }
        return availableSingleTenantCartridges.get(cartridgeType);
    }

    public StratosAdminResponse deleteTenant(String tenantDomain) {
          tenantMap.remove(tenantDomain);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deleted tenant");
        return stratosAdminResponse;
    }

    public TenantInfoBean[] getTenants() throws RestAPIException{
        return tenantMap.values().toArray(new TenantInfoBean[0]);
    }

    public TenantInfoBean[] retrievePartialSearchTenants(String searchDomain) throws RestAPIException{
        List<TenantInfoBean> searchResult = new LinkedList<TenantInfoBean>();
        for(String tenantDomain : tenantMap.keySet()){
            if(tenantDomain.contains(searchDomain)){
                 searchResult.add(new TenantInfoBean(tenantMap.get(tenantDomain)));
            }
        }
        return searchResult.toArray(new TenantInfoBean[0]);
    }

    public StratosAdminResponse  activateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(true);
        } else{
            throw new RestAPIException("Invalid tenant domain");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully activated Tenant");
        return stratosAdminResponse;
    }

    public StratosAdminResponse deactivateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(false);
        } else{
            throw new RestAPIException("Invalid tenant domain");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deactivated Tenant");
        return stratosAdminResponse;
    }

    public StratosAdminResponse deleteCartridgeDefinition(String cartridgeType) throws RestAPIException{
        if(availableSingleTenantCartridges.containsKey(cartridgeType)){
            availableSingleTenantCartridges.remove(cartridgeType);
        } else if(availableMultiTenantCartridges.containsKey(cartridgeType)){
            availableMultiTenantCartridges.remove(cartridgeType);
        } else{
            throw new RestAPIException("invalid cartridge type");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully delete cartridge definition");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addPartition(Partition partition) {
            partitionMap.put(partition.id, partition);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed partition");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addAutoScalingPolicyDefinition(AutoscalePolicy autoscalePolicy) {
            autoscalePolicyMap.put(autoscalePolicy.getId(), autoscalePolicy);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed auto scaling policy definition");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy) {
           deploymentPolicyMap.put(deploymentPolicy.id,deploymentPolicy);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed deployment policy definition");
        return stratosAdminResponse;
    }

    public Partition[] getPartitions() throws RestAPIException{
        return partitionMap.values().toArray(new Partition[0]);
    }

    public Partition getPartition(String partitionId) throws RestAPIException{
        if(!partitionMap.containsKey(partitionId)){
            throw new RestAPIException("There is no partition with the id: " + partitionId);
        }
        return  partitionMap.get(partitionId);
    }


    public Partition[] getPartitionsOfPolicy(String deploymentPolicyId) throws RestAPIException{
        if(!deploymentPolicyMap.containsKey(deploymentPolicyId)){
            throw new RestAPIException("There is no deployment policy with id: " + deploymentPolicyId);
        }
        return deploymentPolicyMap.get(deploymentPolicyId).partition.toArray(new Partition[0]);
    }

    public PartitionGroup[] getPartitionGroups(String deploymentPolicyId)  throws RestAPIException{
        if(!deploymentPolicyMap.containsKey(deploymentPolicyId)){
            throw new RestAPIException("There is no policy with id: " + deploymentPolicyId);
        }
        return deploymentPolicyMap.get(deploymentPolicyId).partitionGroup.toArray(new PartitionGroup[0]);
    }

    public AutoscalePolicy[] getAutoscalePolicies()  throws RestAPIException{
         return autoscalePolicyMap.values().toArray(new AutoscalePolicy[0]);
    }

    public AutoscalePolicy getAutoscalePolicies(String autoscalePolicyId) throws  RestAPIException{
        if(!autoscalePolicyMap.containsKey(autoscalePolicyId)){
            throw new RestAPIException("There is no auto scale policy with id: " + autoscalePolicyId);
        }
        return autoscalePolicyMap.get(autoscalePolicyId);
    }

    public DeploymentPolicy[] getDeploymentPolicies() throws RestAPIException{
        return deploymentPolicyMap.values().toArray(new DeploymentPolicy[0]);
    }

    public DeploymentPolicy getDeploymentPolicies(String deploymentPolicyId) throws RestAPIException{
        if(!deploymentPolicyMap.containsKey(deploymentPolicyId)){
            throw new RestAPIException("There is no deployment policy with id: " + deploymentPolicyId);
        }
        return deploymentPolicyMap.get(deploymentPolicyId);
    }
    public StratosAdminResponse deployService(ServiceDefinitionBean serviceDefinitionBean) {
        serviceDefinitionMap.put(serviceDefinitionBean.getCartridgeType(),serviceDefinitionBean);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed service");
        return stratosAdminResponse;

    }
    public ServiceDefinitionBean[] getServices() throws RestAPIException{
        return serviceDefinitionMap.values().toArray(new ServiceDefinitionBean[0]);
    }

    public Partition[] getPartitions(String deploymentPolicyId, String partitionGroupId) throws RestAPIException{
        if(!deploymentPolicyMap.containsKey(deploymentPolicyId)){
            throw new RestAPIException("There is no deployment policy with id: " + deploymentPolicyId);
        }
        Partition[] partitions = null;
         DeploymentPolicy deploymentPolicy = deploymentPolicyMap.get(deploymentPolicyId);
         for(PartitionGroup partitionGroup : deploymentPolicy.partitionGroup){
             if(partitionGroup.id.equals(partitionGroupId)){
                 partitions =  partitionGroup.partition.toArray(new Partition[0]);
             }
         }
        if(partitions == null){
            throw new RestAPIException("Partition not found");
        }
        return partitions;
    }
    public Cluster[] getClusters() throws RestAPIException{
        return clusterMap.values().toArray(new Cluster[0]);
    }
    public DeploymentPolicy[] getDeploymentPoliciesForCartridgeType(String cartridgeType) throws RestAPIException{
        return deploymentPolicyMap.values().toArray(new DeploymentPolicy[0]);
    }

}
