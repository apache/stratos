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
package org.apache.stratos.rest.endpoint.mock;

import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.subscription.SubscriptionDomain;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.StratosAdminResponse;
import org.apache.stratos.rest.endpoint.bean.SubscriptionDomainRequest;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.bean.util.converter.PojoConverter;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.wso2.carbon.context.CarbonContext;
import java.util.*;

import javax.ws.rs.core.Response.Status;

public class MockContext {
    private static MockContext mockContext = new MockContext(); // singleton

    private Map<String, List<SubscriptionDomain>> subscriptionAliasToDomainMap = new HashMap<String, List<SubscriptionDomain>>();
    private Map<Integer, List<String>> tenantIdToAliasesMap = new HashMap<Integer, List<String>>();
    private Map<Integer, List<CartridgeDefinitionBean>> cartridgeDefinitionBeanList = new HashMap<Integer, List<CartridgeDefinitionBean>>();
    private Map<Integer, Map<String,Cartridge>> availableSingleTenantCartridges = new HashMap<Integer, Map<String,Cartridge>>();
    private Map<Integer, Map<String,Cartridge>> availableMultiTenantCartridges = new HashMap<Integer, Map<String,Cartridge>>();
    private Map<Integer, Map<String,Cartridge>> subscribedCartridges = new HashMap<Integer, Map<String,Cartridge>>();
    private Map<String,TenantInfoBean> tenantMap = new HashMap<String, TenantInfoBean>();
    private Map<String, Integer> tenantIdMap = new HashMap<String, Integer>();
    private Map<Integer, Map<String,Partition>> partitionMap = new HashMap<Integer, Map<String, Partition>>();
    private Map<Integer, Map<String,AutoscalePolicy>> autoscalePolicyMap = new HashMap<Integer, Map<String, AutoscalePolicy>>();
    private Map<Integer, Map<String,DeploymentPolicy>> deploymentPolicyMap = new HashMap<Integer, Map<String, DeploymentPolicy>>();
    private Map<Integer, Map<String,ServiceDefinitionBean>> serviceDefinitionMap = new HashMap<Integer, Map<String, ServiceDefinitionBean>>();
    private Map<String,Cluster> clusterMap = new HashMap<String, Cluster>();
    
    private int tenantIdCount=1;


    private MockContext(){} // do not allow to initialize

    public static MockContext getInstance(){
        return mockContext;
    }

    public StratosAdminResponse addCartirdgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean){
    	int tenantId = getTenantId();
    	List<CartridgeDefinitionBean> tenantCartridges;
    	if(this.cartridgeDefinitionBeanList.containsKey(tenantId)){
    		tenantCartridges = this.cartridgeDefinitionBeanList.get(tenantId);
    	}
    	else{
    		tenantCartridges = new LinkedList<CartridgeDefinitionBean>();
    		this.cartridgeDefinitionBeanList.put(tenantId, tenantCartridges);
    	}
    	tenantCartridges.add(cartridgeDefinitionBean);
        
    	Cartridge cartridge = new Cartridge();
        cartridge.setCartridgeType(cartridgeDefinitionBean.type);
        cartridge.setDescription(cartridgeDefinitionBean.description);
        cartridge.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridge.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridge.setProvider(cartridgeDefinitionBean.provider);
        cartridge.setVersion(cartridgeDefinitionBean.version);

        Map<String,Cartridge> cartridges;
        if(cartridge.isMultiTenant()){
        	if(this.availableMultiTenantCartridges.containsKey(tenantId)){
        		cartridges = availableMultiTenantCartridges.get(tenantId);
        	}
        	else{
        		cartridges = new HashMap<String,Cartridge>();
        		this.availableMultiTenantCartridges.put(tenantId, cartridges);
        	}
            cartridges.put(cartridge.getCartridgeType(), cartridge);
            System.out.println(cartridges.size());
        }else{
        	if(this.availableSingleTenantCartridges.containsKey(tenantId)){
        		cartridges = availableMultiTenantCartridges.get(tenantId);
        	}
        	else{
        		cartridges = new HashMap<String,Cartridge>();
        		this.availableSingleTenantCartridges.put(tenantId, cartridges);
        	}
        	cartridges.put(cartridge.getCartridgeType(), cartridge);
            System.out.println(cartridges.size());
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed cartridge definition with type ");
        return stratosAdminResponse;
    }



    public Cartridge[] getAvailableMultiTenantCartridges() throws RestAPIException{
    	if(!this.availableMultiTenantCartridges.containsKey(getTenantId())){
    		return new HashMap<String, Cartridge>().values().toArray(new Cartridge[0]);
    	}    	
    	return this.availableMultiTenantCartridges.get(getTenantId()).values().toArray(new Cartridge[0]);
    }


    public Cartridge[] getAvailableSingleTenantCartridges() throws RestAPIException{
    	if(!this.availableSingleTenantCartridges.containsKey(getTenantId())){
    		return new HashMap<String, Cartridge>().values().toArray(new Cartridge[0]);
    	}
    	return this.availableSingleTenantCartridges.get(getTenantId()).values().toArray(new Cartridge[0]);
    }

    public Cartridge[] getAvailableLbCartridges() throws RestAPIException{
        /*Map<String,Cartridge> availableLbCartridges = new HashMap<String,Cartridge>();
        Iterator it = availableSingleTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(cartridge.getSubscriptionDomain().equals("lb")){
                availableLbCartridges.put(cartridge.getSubscriptionDomain(),cartridge);
            }
            it.remove();
        }

        it = availableMultiTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(cartridge.getSubscriptionDomain().equals("lb")){
                availableLbCartridges.put(cartridge.getSubscriptionDomain(),cartridge);
            }
            it.remove();
        }
        return availableLbCartridges.values().toArray(new Cartridge[0]);*/
        //return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
        return getAvailableSingleTenantCartridges();
    }

    public Cartridge[] getAvailableCartridges() throws RestAPIException{
        /*Map<String, Cartridge> availableCartridges = new HashMap<String,Cartridge>();
        Iterator it = availableSingleTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(!cartridge.getSubscriptionDomain().equals("lb")){
                availableCartridges.put(cartridge.getSubscriptionDomain(),cartridge);
            }
            it.remove();
        }

        it = availableMultiTenantCartridges.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();
            Cartridge cartridge = (Cartridge)pairs.getValue();
            if(!cartridge.getSubscriptionDomain().equals("lb")){
                availableCartridges.put(cartridge.getSubscriptionDomain(),cartridge);
            }
            it.remove();
        }
        System.out.println(availableCartridges.size());
        return availableCartridges.values().toArray(new Cartridge[0]);*/
        //return availableSingleTenantCartridges.values().toArray(new Cartridge[0]);
    	return getAvailableSingleTenantCartridges();
    }


    public Cartridge[] getSubscribedCartridges() throws RestAPIException{
    	if(!this.subscribedCartridges.containsKey(getTenantId())){
    		return new HashMap<String, Cartridge>().values().toArray(new Cartridge[0]);
    	}
        return (subscribedCartridges.get(getTenantId())).values().toArray(new Cartridge[0]);
    }

    public SubscriptionInfo subscribeToCartridge(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException{
        int tenantId = this.getTenantId();
        String cartridgeType = cartridgeInfoBean.getCartridgeType();
        String alias = cartridgeInfoBean.getAlias();
        Cartridge subscribedCartridge=null;
        // retrieve the cartridge from available ones for specific tenant
        if(availableMultiTenantCartridges.containsKey(tenantId)){
        	if((availableMultiTenantCartridges.get(tenantId)).containsKey(cartridgeType)){
                subscribedCartridge = (availableMultiTenantCartridges.get(tenantId)).get(cartridgeType);
        	}
        }
        else if(availableSingleTenantCartridges.containsKey(tenantId)){
        	if((availableSingleTenantCartridges.get(tenantId)).containsKey(cartridgeType)){
                 subscribedCartridge = (availableSingleTenantCartridges.get(tenantId)).get(cartridgeType);
        	}
        }else{
             throw new RestAPIException(Status.NO_CONTENT,"Cartridge not defined");
        }
         
        if(subscribedCartridge!=null){
            //Proper way is copy constructor
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

            Map<String,Cartridge> subscriptions;
            if(subscribedCartridges.containsKey(tenantId)){
            	(subscribedCartridges.get(tenantId)).put(alias,copy);
            }
            else{
            	subscriptions = new HashMap<String,Cartridge>();
            	subscriptions.put(alias, copy);
            	subscribedCartridges.put(tenantId, subscriptions);
            }
            
            SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
            subscriptionInfo.setHostname(copy.getHostName());
            subscriptionInfo.setRepositoryURL(copy.getRepoURL());

            return subscriptionInfo;
        }
        
        return new SubscriptionInfo();

    }

    public StratosAdminResponse unsubscribe(String alias) throws RestAPIException{
        if(subscribedCartridges.containsKey(getTenantId())){
        	if((subscribedCartridges.get(getTenantId())).containsKey(alias)){
            	(subscribedCartridges.get(getTenantId())).remove(alias);
        	}   	
        }else{
            throw new RestAPIException(Status.NO_CONTENT,"Unable to un-subscribe");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully un-subscribed");
        return stratosAdminResponse;
    }

    public Cartridge getCartridgeInfo(String alias) throws RestAPIException{
    	if(!subscribedCartridges.containsKey(getTenantId()))
    		throw new RestAPIException(Status.NO_CONTENT, "No cartridges subscribed for current tenant.");
    	
    	if(!(subscribedCartridges.get(getTenantId())).containsKey(alias))
    		throw new RestAPIException(Status.NO_CONTENT,"Cartridge information is not available.");
    		
        return (subscribedCartridges.get(getTenantId())).get(alias);
    }

    public Cartridge getAvailableSingleTenantCartridgeInfo(String cartridgeType) throws RestAPIException{
        if(!availableSingleTenantCartridges.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No cartridges defined for current tenant");
    	
    	if(!(availableSingleTenantCartridges.get(getTenantId())).containsKey(cartridgeType))
            throw new RestAPIException(Status.NO_CONTENT,"Cartridge is not available.");
        
        return (availableSingleTenantCartridges.get(getTenantId())).get(cartridgeType);
    }
    
    public StratosAdminResponse deleteCartridgeDefinition(String cartridgeType) throws RestAPIException{
        if((!availableSingleTenantCartridges.containsKey(getTenantId())) && (!availableMultiTenantCartridges.containsKey(getTenantId())))
        	throw new RestAPIException(Status.NO_CONTENT,"No cartridges defined for tenant");
        
    	if(availableSingleTenantCartridges.containsKey(getTenantId())){
    		if((availableSingleTenantCartridges.get(getTenantId())).containsKey(cartridgeType)){
            	(availableSingleTenantCartridges.get(getTenantId())).remove(cartridgeType);
    		}
    	} else if(availableMultiTenantCartridges.containsKey(getTenantId())){
    		if((availableMultiTenantCartridges.get(getTenantId())).containsKey(cartridgeType)){
            	(availableMultiTenantCartridges.get(getTenantId())).remove(cartridgeType);
    		}
        } else{
            throw new RestAPIException(Status.BAD_REQUEST,"invalid cartridge type");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully delete cartridge definition");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException{
        try{
            tenantMap.put(tenantInfoBean.getTenantDomain(),tenantInfoBean);
            tenantInfoBean.setTenantId(tenantIdCount);
            tenantIdMap.put(tenantInfoBean.getAdmin(), tenantIdCount++);
        }catch (Exception e){
            throw new RestAPIException(Status.INTERNAL_SERVER_ERROR,e.getMessage());
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully added new Tenant");
        return stratosAdminResponse;
    }

    public TenantInfoBean getTenant(String tenantDomain) throws RestAPIException{
        if(!tenantMap.containsKey(tenantDomain)){
            throw new RestAPIException(Status.NO_CONTENT,"Information for tenant: " + tenantDomain + " is not available");
        }
        return tenantMap.get(tenantDomain);
    }
    
    public StratosAdminResponse deleteTenant(String tenantDomain) {
        if(tenantMap.containsKey(tenantDomain)){
        	TenantInfoBean tenant=tenantMap.get(tenantDomain);
        	tenantMap.remove(tenantDomain);
        	tenantIdMap.remove(tenant.getTenantId());
        }
    	        
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
            throw new RestAPIException(Status.BAD_REQUEST,"Invalid tenant domain");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully activated Tenant");
        return stratosAdminResponse;
    }

    public StratosAdminResponse deactivateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(false);
        } else{
            throw new RestAPIException(Status.BAD_REQUEST,"Invalid tenant domain");
        }
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deactivated Tenant");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addPartition(Partition partition) {
    	Map<String,Partition> partitions;
    	if (partitionMap.containsKey(getTenantId())){
    		partitions = partitionMap.get(getTenantId());
    	}
    	else{
    		partitions = new HashMap<String, Partition>();
    		partitionMap.put(getTenantId(), partitions);
    	}
    	partitions.put(partition.id, partition);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed partition");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addAutoScalingPolicyDefinition(AutoscalePolicy autoscalePolicy) {
    	Map<String,AutoscalePolicy> policies;
    	if (autoscalePolicyMap.containsKey(getTenantId())){
    		policies = autoscalePolicyMap.get(getTenantId());
    	}
    	else{
    		policies = new HashMap<String, AutoscalePolicy>();
    		autoscalePolicyMap.put(getTenantId(), policies);
    	}
    	policies.put(autoscalePolicy.getId(), autoscalePolicy);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed auto scaling policy definition");
        return stratosAdminResponse;
    }

    public StratosAdminResponse addDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy) {
    	Map<String,DeploymentPolicy> policies;
    	if (deploymentPolicyMap.containsKey(getTenantId())){
    		policies = deploymentPolicyMap.get(getTenantId());
    	}
    	else{
    		policies = new HashMap<String, DeploymentPolicy>();
    		deploymentPolicyMap.put(getTenantId(), policies);
    	}
    	policies.put(deploymentPolicy.id,deploymentPolicy);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed deployment policy definition");
        return stratosAdminResponse;
    }

    public Partition[] getPartitions() throws RestAPIException{
    	if(!partitionMap.containsKey(getTenantId())){
        	return new HashMap<String,Partition>().values().toArray(new Partition[0]);
    	}
    	return (partitionMap.get(getTenantId())).values().toArray(new Partition[0]);
    }

    public Partition getPartition(String partitionId) throws RestAPIException{
        if(!partitionMap.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No partitions have been defined for the tenant");
        
    	if(!(partitionMap.get(getTenantId())).containsKey(partitionId)){
            throw new RestAPIException("There is no partition with the id: " + partitionId);
        }
        return  (partitionMap.get(getTenantId())).get(partitionId);
    }


    public Partition[] getPartitionsOfPolicy(String deploymentPolicyId) throws RestAPIException{
    	if(!deploymentPolicyMap.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    	
    	if(!(deploymentPolicyMap.get(getTenantId())).containsKey(deploymentPolicyId)){
            throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        }
        return (deploymentPolicyMap.get(getTenantId())).get(deploymentPolicyId).partition.toArray(new Partition[0]);
    }

    public PartitionGroup[] getPartitionGroups(String deploymentPolicyId)  throws RestAPIException{
    	if(!deploymentPolicyMap.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    	
    	if(!(deploymentPolicyMap.get(getTenantId())).containsKey(deploymentPolicyId)){
            throw new RestAPIException(Status.NO_CONTENT,"There is no policy with id: " + deploymentPolicyId);
        }
        return (deploymentPolicyMap.get(getTenantId())).get(deploymentPolicyId).partitionGroup.toArray(new PartitionGroup[0]);
    }

    public AutoscalePolicy[] getAutoscalePolicies()  throws RestAPIException{
    	if(!autoscalePolicyMap.containsKey(getTenantId())){
        	return new HashMap<String,AutoscalePolicy>().values().toArray(new AutoscalePolicy[0]);
    	}
    	return (autoscalePolicyMap.get(getTenantId())).values().toArray(new AutoscalePolicy[0]);
    }

    public AutoscalePolicy getAutoscalePolicies(String autoscalePolicyId) throws  RestAPIException{
    	if(!autoscalePolicyMap.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No autoscaling policies have been defined for tenant");
    	
    	if(!(autoscalePolicyMap.get(getTenantId())).containsKey(autoscalePolicyId)){
            throw new RestAPIException("There is no auto scale policy with id: " + autoscalePolicyId);
        }
        return (autoscalePolicyMap.get(getTenantId())).get(autoscalePolicyId);
    }

    public DeploymentPolicy[] getDeploymentPolicies() throws RestAPIException{
    	if(!deploymentPolicyMap.containsKey(getTenantId())){
    		return new HashMap<String,DeploymentPolicy>().values().toArray(new DeploymentPolicy[0]);
    	}
    	
    	return (deploymentPolicyMap.get(getTenantId())).values().toArray(new DeploymentPolicy[0]);
    }

    public DeploymentPolicy getDeploymentPolicies(String deploymentPolicyId) throws RestAPIException{
    	if(!deploymentPolicyMap.containsKey(getTenantId()))
        	throw new RestAPIException("No deployment policies have been defined for tenant");
    	
    	if(!(deploymentPolicyMap.get(getTenantId())).containsKey(deploymentPolicyId)){
            throw new RestAPIException("There is no deployment policy with id: " + deploymentPolicyId);
        }
        return (deploymentPolicyMap.get(getTenantId())).get(deploymentPolicyId);
    }
    
    public StratosAdminResponse deployService(ServiceDefinitionBean serviceDefinitionBean) {
    	Map<String,ServiceDefinitionBean> serviceDefinitions;
    	
    	if(!serviceDefinitionMap.containsKey(getTenantId())){
    		serviceDefinitions = new HashMap<String,ServiceDefinitionBean>();
    		serviceDefinitionMap.put(getTenantId(), serviceDefinitions);
    	}
    	else{
    		serviceDefinitions = serviceDefinitionMap.get(getTenantId());
    	}
    	
    	serviceDefinitions.put(serviceDefinitionBean.getCartridgeType(),serviceDefinitionBean);
        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed service");
        return stratosAdminResponse;

    }
       
    public ServiceDefinitionBean[] getServices() throws RestAPIException{
    	if(!serviceDefinitionMap.containsKey(getTenantId())){
    		return new HashMap<String,ServiceDefinitionBean>().values().toArray(new ServiceDefinitionBean[0]);
    	}
    	return (serviceDefinitionMap.get(getTenantId())).values().toArray(new ServiceDefinitionBean[0]);
    }

    public ServiceDefinitionBean getServiceType(String serviceType) throws RestAPIException{
        if(!serviceDefinitionMap.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No services have been defined for tenant");
        
    	return (serviceDefinitionMap.get(getTenantId())).get(serviceType);
    }

    public Partition[] getPartitions(String deploymentPolicyId, String partitionGroupId) throws RestAPIException{
    	if(!deploymentPolicyMap.containsKey(getTenantId()))
        	throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    	
    	if(!(deploymentPolicyMap.get(getTenantId())).containsKey(deploymentPolicyId)){
            throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        }
    	
        Partition[] partitions = null;
         DeploymentPolicy deploymentPolicy = (deploymentPolicyMap.get(getTenantId())).get(deploymentPolicyId);
         for(PartitionGroup partitionGroup : deploymentPolicy.partitionGroup){
             if(partitionGroup.id.equals(partitionGroupId)){
                 partitions =  partitionGroup.partition.toArray(new Partition[0]);
             }
         }
        if(partitions == null){
            throw new RestAPIException(Status.NO_CONTENT,"Partition not found");
        }
        return partitions;
    }
    
    public Cluster[] getClusters() throws RestAPIException{
        return clusterMap.values().toArray(new Cluster[0]);
    }
    
    public DeploymentPolicy[] getDeploymentPoliciesForCartridgeType(String cartridgeType) throws RestAPIException{
        if(!deploymentPolicyMap.containsKey(getTenantId())){
        	return new HashMap<String,DeploymentPolicy>().values().toArray(new DeploymentPolicy[0]);
        }
    	return (deploymentPolicyMap.get(getTenantId())).values().toArray(new DeploymentPolicy[0]);
    }

	public StratosAdminResponse addSubscriptionDomains(int tenantId, String alias, SubscriptionDomainRequest request) {
		// populate new alias
		List<String> aliasList;
		if(tenantIdToAliasesMap.containsKey(tenantId)) {
			aliasList = tenantIdToAliasesMap.get(tenantId);
		} else {
			aliasList = new ArrayList<String>();
		}
		aliasList.add(alias);
		tenantIdToAliasesMap.put(tenantId, aliasList);
		
		// populate domains
		List<SubscriptionDomain> list;
		if(subscriptionAliasToDomainMap.containsKey(alias)) {
			list = subscriptionAliasToDomainMap.get(alias);
		} else {
			list = new ArrayList<SubscriptionDomain>();
		}
		for (org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean bean : request.domains) {
			
			SubscriptionDomain subscriptionDomain = new SubscriptionDomain(bean.domainName, bean.applicationContext);
			list.add(subscriptionDomain);
		}
		
		subscriptionAliasToDomainMap.put(alias, list);
		
		StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully added subscription domain/s.");
        return stratosAdminResponse;
	}

	public List<SubscriptionDomainBean> getSubscriptionDomains(int tenantId, String alias) {
		List<String> tenantAliases = tenantIdToAliasesMap.get(tenantId);
		if(tenantAliases != null && tenantAliases.contains(alias)) {
			
			return PojoConverter.populateSubscriptionDomainPojos(subscriptionAliasToDomainMap.get(alias));
		}
        return new ArrayList<SubscriptionDomainBean>();
	}

	public SubscriptionDomainBean getSubscriptionDomain(int tenantId, String cartridgeType,
			String subscriptionAlias, String domainName) throws RestAPIException {
		List<String> tenantAliases = tenantIdToAliasesMap.get(tenantId);
		if(tenantAliases != null && tenantAliases.contains(subscriptionAlias)) {
			for (SubscriptionDomain subscriptionDomain : subscriptionAliasToDomainMap.get(subscriptionAlias)) {
				if(subscriptionDomain.getDomainName().equals(domainName)) {
					
					return PojoConverter.populateSubscriptionDomainPojo(subscriptionDomain);
				}
			}
		}

		String message = "Could not find a subscription [domain] " + domainName
				+ " for Cartridge [type] " + cartridgeType + " and [alias] "
				+ subscriptionAlias;
		throw new RestAPIException(Status.NOT_FOUND, message);
	}

	public StratosAdminResponse removeSubscriptionDomain(int tenantId,
			String subscriptionAlias, String domainName) {
		StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
		
		List<String> tenantAliases = tenantIdToAliasesMap.get(tenantId);
		if(tenantAliases != null && tenantAliases.contains(subscriptionAlias)) {
			for (Iterator<SubscriptionDomain> iterator = subscriptionAliasToDomainMap.get(subscriptionAlias).iterator(); iterator
					.hasNext();) {
				SubscriptionDomain subscriptionDomain = (SubscriptionDomain) iterator.next();
				if (subscriptionDomain.getDomainName().equals(domainName)) {
					iterator.remove();
					stratosAdminResponse.setMessage("Successfully removed the subscription domain: "+domainName);
				}
			}
		} else {		
			stratosAdminResponse.setMessage("Failed to remove the subscription domain: "+domainName);
		}
		
        return stratosAdminResponse;
	}
	
    private int getTenantId() {
    	String userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
    	if (tenantIdMap.containsKey(userName)){
    		return tenantIdMap.get(userName);
    	}
    	else {
    		return -1;
    	}
    }

}
