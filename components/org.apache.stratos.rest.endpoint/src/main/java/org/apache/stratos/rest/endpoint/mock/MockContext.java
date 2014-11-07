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
import org.apache.stratos.manager.user.mgt.beans.UserInfoBean;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.StratosApiResponse;
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
    private Map<Integer, Map<String, UserInfoBean>> tenantUserMap= new HashMap<Integer, Map<String, UserInfoBean>>();
    private Map<String, Integer> tenantIdMap = new HashMap<String, Integer>();
    private Map<Integer, Map<String,Partition>> partitionMap = new HashMap<Integer, Map<String, Partition>>();
    private Map<Integer, Map<String,AutoscalePolicy>> autoscalePolicyMap = new HashMap<Integer, Map<String, AutoscalePolicy>>();
    private Map<Integer, Map<String,DeploymentPolicy>> deploymentPolicyMap = new HashMap<Integer, Map<String, DeploymentPolicy>>();
    private Map<Integer, Map<String,ServiceDefinitionBean>> serviceDefinitionMap = new HashMap<Integer, Map<String, ServiceDefinitionBean>>();
    private Map<String,Cluster> clusterMap = new HashMap<String, Cluster>();
    
    private int tenantIdCount=1;
    public static final int PUBLIC_DEFINITION = 0;

    private MockContext(){} // do not allow to initialize

    public static MockContext getInstance(){
        return mockContext;
    }

    public StratosApiResponse addCartirdgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean){
    	int tenantId = getTenantId();
    	List<CartridgeDefinitionBean> tenantCartridges;
    	
    	if(!cartridgeDefinitionBean.isPublic){
    		if(this.cartridgeDefinitionBeanList.containsKey(tenantId)){
        		tenantCartridges = this.cartridgeDefinitionBeanList.get(tenantId);
        	}
        	else{
        		tenantCartridges = new LinkedList<CartridgeDefinitionBean>();
        		this.cartridgeDefinitionBeanList.put(tenantId, tenantCartridges);
        	}
    	}
    	else{
    		if(this.cartridgeDefinitionBeanList.containsKey(PUBLIC_DEFINITION)){
        		tenantCartridges = this.cartridgeDefinitionBeanList.get(PUBLIC_DEFINITION);
        	}
        	else{
        		tenantCartridges = new LinkedList<CartridgeDefinitionBean>();
        		this.cartridgeDefinitionBeanList.put(PUBLIC_DEFINITION, tenantCartridges);
        	}
    	}
    	
    	tenantCartridges.add(cartridgeDefinitionBean);
        
    	Cartridge cartridge = new Cartridge();
        cartridge.setCartridgeType(cartridgeDefinitionBean.type);
        cartridge.setDescription(cartridgeDefinitionBean.description);
        cartridge.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridge.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridge.setProvider(cartridgeDefinitionBean.provider);
        cartridge.setVersion(cartridgeDefinitionBean.version);
        cartridge.setIsPublic(cartridgeDefinitionBean.isPublic);

        Map<String,Cartridge> cartridges;
        if(cartridge.isMultiTenant()){
        	if(!cartridge.getIsPublic()){
        		if(this.availableMultiTenantCartridges.containsKey(tenantId)){
            		cartridges = availableMultiTenantCartridges.get(tenantId);
            	}
            	else{
            		cartridges = new HashMap<String,Cartridge>();
            		this.availableMultiTenantCartridges.put(tenantId, cartridges);
            	}
        	}else{
        		if(this.availableMultiTenantCartridges.containsKey(PUBLIC_DEFINITION)){
            		cartridges = availableMultiTenantCartridges.get(PUBLIC_DEFINITION);
            	}
            	else{
            		cartridges = new HashMap<String,Cartridge>();
            		this.availableMultiTenantCartridges.put(PUBLIC_DEFINITION, cartridges);
            	}
        	}
        	
            cartridges.put(cartridge.getCartridgeType(), cartridge);
            System.out.println(cartridges.size());
        }else{
        	if(!cartridge.getIsPublic()){
        		if(this.availableSingleTenantCartridges.containsKey(tenantId)){
            		cartridges = availableSingleTenantCartridges.get(tenantId);
            	}
            	else{
            		cartridges = new HashMap<String,Cartridge>();
            		this.availableSingleTenantCartridges.put(tenantId, cartridges);
            	}
        	}else{
        		if(this.availableSingleTenantCartridges.containsKey(PUBLIC_DEFINITION)){
            		cartridges = availableSingleTenantCartridges.get(PUBLIC_DEFINITION);
            	}
            	else{
            		cartridges = new HashMap<String,Cartridge>();
            		this.availableSingleTenantCartridges.put(PUBLIC_DEFINITION, cartridges);
            	}
        	}
        	
        	cartridges.put(cartridge.getCartridgeType(), cartridge);
            System.out.println(cartridges.size());
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed cartridge definition with type ");
        return stratosApiResponse;
    }

    public Cartridge[] getAvailableMultiTenantCartridges() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!availableMultiTenantCartridges.containsKey(tenantId) && !availableMultiTenantCartridges.containsKey(PUBLIC_DEFINITION)){
    		return new HashMap<String, Cartridge>().values().toArray(new Cartridge[0]);
    	}    	
    	
    	List<Cartridge> p = new ArrayList<Cartridge>();
    	
		if(availableMultiTenantCartridges.get(tenantId) != null)
    		p.addAll(availableMultiTenantCartridges.get(tenantId).values());
		
		if(availableMultiTenantCartridges.get(PUBLIC_DEFINITION) != null)
			p.addAll(availableMultiTenantCartridges.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new Cartridge[0]);
    }


    public Cartridge[] getAvailableSingleTenantCartridges() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!availableSingleTenantCartridges.containsKey(tenantId) && !availableSingleTenantCartridges.containsKey(PUBLIC_DEFINITION)){
    		return new HashMap<String, Cartridge>().values().toArray(new Cartridge[0]);
    	}    	
    	
    	List<Cartridge> p = new ArrayList<Cartridge>();
    	
		if(availableSingleTenantCartridges.get(tenantId) != null)
    		p.addAll(availableSingleTenantCartridges.get(tenantId).values());
		
		if(availableSingleTenantCartridges.get(PUBLIC_DEFINITION) != null)
			p.addAll(availableSingleTenantCartridges.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new Cartridge[0]);
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
    	int tenantId = getTenantId();
    	if(!subscribedCartridges.containsKey(tenantId) && !subscribedCartridges.containsKey(PUBLIC_DEFINITION)){
    		return new HashMap<String, Cartridge>().values().toArray(new Cartridge[0]);
    	}
        List<Cartridge> p = new ArrayList<Cartridge>();
    	
		if(subscribedCartridges.get(tenantId) != null)
    		p.addAll(subscribedCartridges.get(tenantId).values());
		
		if(subscribedCartridges.get(PUBLIC_DEFINITION) != null)
			p.addAll(subscribedCartridges.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new Cartridge[0]);
    }

    public SubscriptionInfo subscribeToCartridge(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException{
        int tenantId = this.getTenantId();
        String cartridgeType = cartridgeInfoBean.getCartridgeType();
        String alias = cartridgeInfoBean.getAlias();
        Cartridge subscribedCartridge=null;
        
        // retrieve the cartridge from single tenant cartridges
        try{
        	subscribedCartridge=getAvailableSingleTenantCartridgeInfo(cartridgeType);
        }
        catch(RestAPIException e){
        	//ignore once
        }
        
        // retrieve the cartridge from multitenant cartridges if not found
        if(subscribedCartridge==null){
        	try{
            	subscribedCartridge=getAvailableMultiTenantCartridgeInfo(cartridgeType);
            }
            catch(RestAPIException e){
            	throw new RestAPIException(Status.NO_CONTENT,"Cartridge not defined");
            }
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

    public StratosApiResponse unsubscribe(String alias) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(subscribedCartridges.containsKey(tenantId)){
        	if((subscribedCartridges.get(tenantId)).containsKey(alias)){
            	(subscribedCartridges.get(tenantId)).remove(alias);
        	}   	
        }else{
            throw new RestAPIException(Status.NO_CONTENT,"Unable to un-subscribe");
        }
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully un-subscribed");
        return stratosApiResponse;
    }

    public Cartridge getCartridgeInfo(String alias) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!subscribedCartridges.containsKey(tenantId))
    		throw new RestAPIException(Status.NO_CONTENT, "No cartridges subscribed for current tenant.");
    	
    	if(!(subscribedCartridges.get(tenantId)).containsKey(alias))
    		throw new RestAPIException(Status.NO_CONTENT,"Cartridge information is not available.");
    		
        return (subscribedCartridges.get(tenantId)).get(alias);
    }

    public Cartridge getAvailableSingleTenantCartridgeInfo(String cartridgeType) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!availableSingleTenantCartridges.containsKey(tenantId)){
        	if(!availableSingleTenantCartridges.containsKey(PUBLIC_DEFINITION)){
        		throw new RestAPIException(Status.NO_CONTENT,"No cartridges defined for current tenant");
        	}
        	if(!(availableSingleTenantCartridges.get(PUBLIC_DEFINITION)).containsKey(cartridgeType))
                throw new RestAPIException(Status.NO_CONTENT,"Cartridge is not available.");
            
            return (availableSingleTenantCartridges.get(PUBLIC_DEFINITION)).get(cartridgeType);
        }  	
    	if(!(availableSingleTenantCartridges.get(tenantId)).containsKey(cartridgeType))
            throw new RestAPIException(Status.NO_CONTENT,"Cartridge is not available.");
        
        return (availableSingleTenantCartridges.get(tenantId)).get(cartridgeType);
    }
    
    public Cartridge getAvailableMultiTenantCartridgeInfo(String cartridgeType) throws RestAPIException{
    	int tenantId = getTenantId();
        if(!availableMultiTenantCartridges.containsKey(tenantId)){
        	if(!availableMultiTenantCartridges.containsKey(PUBLIC_DEFINITION)){
        		throw new RestAPIException(Status.NO_CONTENT,"No cartridges defined for current tenant");
        	}
        	if(!(availableMultiTenantCartridges.get(PUBLIC_DEFINITION)).containsKey(cartridgeType))
                throw new RestAPIException(Status.NO_CONTENT,"Cartridge is not available.");
            
            return (availableMultiTenantCartridges.get(PUBLIC_DEFINITION)).get(cartridgeType);
        }  	
    	if(!(availableMultiTenantCartridges.get(tenantId)).containsKey(cartridgeType))
            throw new RestAPIException(Status.NO_CONTENT,"Cartridge is not available.");
        
        return (availableMultiTenantCartridges.get(tenantId)).get(cartridgeType);
    }
    
    public StratosApiResponse deleteCartridgeDefinition(String cartridgeType) throws RestAPIException{
    	if(!deleteFromAvailableSingleTenantCartridgeDefinitions(cartridgeType) && !deleteFromAvailableMultiTenantCartridgeDefinitions(cartridgeType)){
    		throw new RestAPIException(Status.NO_CONTENT,"No cartridges defined for tenant");
    	}
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully delete cartridge definition");
        return stratosApiResponse;
    }
    
    private boolean deleteFromAvailableSingleTenantCartridgeDefinitions(String cartridgeType){
    	int tenantId = getTenantId();
    	if(!availableSingleTenantCartridges.containsKey(tenantId)){
        	if(!availableSingleTenantCartridges.containsKey(PUBLIC_DEFINITION)){
        		return false;
        	}
        	if(!(availableSingleTenantCartridges.get(PUBLIC_DEFINITION)).containsKey(cartridgeType))
                return false;
            
            (availableSingleTenantCartridges.get(PUBLIC_DEFINITION)).remove(cartridgeType);
            return true;
        }  	
    	if(!(availableSingleTenantCartridges.get(tenantId)).containsKey(cartridgeType))
            return false;
        
        (availableSingleTenantCartridges.get(tenantId)).remove(cartridgeType);
        return true;
    }
    
    private boolean deleteFromAvailableMultiTenantCartridgeDefinitions(String cartridgeType){
    	int tenantId = getTenantId();
    	if(!availableMultiTenantCartridges.containsKey(tenantId)){
        	if(!availableMultiTenantCartridges.containsKey(PUBLIC_DEFINITION)){
        		return false;
        	}
        	if(!(availableMultiTenantCartridges.get(PUBLIC_DEFINITION)).containsKey(cartridgeType))
                return false;
            
            (availableMultiTenantCartridges.get(PUBLIC_DEFINITION)).remove(cartridgeType);
            return true;
        }  	
    	if(!(availableMultiTenantCartridges.get(tenantId)).containsKey(cartridgeType))
            return false;
        
        (availableMultiTenantCartridges.get(tenantId)).remove(cartridgeType);
        return true;
    }

    public StratosApiResponse addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException{
    	try{
            tenantMap.put(tenantInfoBean.getTenantDomain(),tenantInfoBean);
            tenantInfoBean.setTenantId(tenantIdCount);
            tenantIdMap.put(tenantInfoBean.getAdmin(), tenantIdCount++);
        }catch (Exception e){
            throw new RestAPIException(Status.INTERNAL_SERVER_ERROR,e.getMessage());
        }
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully added new Tenant");
        return stratosApiResponse;
    }

    public TenantInfoBean getTenant(String tenantDomain) throws RestAPIException{
        if(!tenantMap.containsKey(tenantDomain)){
            throw new RestAPIException(Status.NO_CONTENT,"Information for tenant: " + tenantDomain + " is not available");
        }
        return tenantMap.get(tenantDomain);
    }
    
    public StratosApiResponse deleteTenant(String tenantDomain) {
        if(tenantMap.containsKey(tenantDomain)){
        	TenantInfoBean tenant=tenantMap.get(tenantDomain);
        	tenantMap.remove(tenantDomain);
        	tenantIdMap.remove(tenant.getTenantId());
        }
    	        
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deleted tenant");
        return stratosApiResponse;
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

    public StratosApiResponse activateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(true);
        } else{
            throw new RestAPIException(Status.BAD_REQUEST,"Invalid tenant domain");
        }
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully activated Tenant");
        return stratosApiResponse;
    }

    public StratosApiResponse deactivateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(false);
        } else{
            throw new RestAPIException(Status.BAD_REQUEST,"Invalid tenant domain");
        }
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deactivated Tenant");
        return stratosApiResponse;
    }

    public StratosApiResponse addPartition(Partition partition) {
    	int tenantId = getTenantId();
    	Map<String,Partition> partitions;
    	
    	if(!partition.isPublic){
    		if (partitionMap.containsKey(tenantId)){
        		partitions = partitionMap.get(tenantId);
        	}
        	else{
        		partitions = new HashMap<String, Partition>();
        		partitionMap.put(tenantId, partitions);
        	}
    	}
    	else {
    		if (partitionMap.containsKey(PUBLIC_DEFINITION)){
    			partitions = partitionMap.get(PUBLIC_DEFINITION);
    		}
    		else{
    			partitions = new HashMap<String, Partition>();
        		partitionMap.put(PUBLIC_DEFINITION, partitions);
    		}
    	} 	
    	
    	partitions.put(partition.id, partition);
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed partition");
        return stratosApiResponse;
    }

    public StratosApiResponse addAutoScalingPolicyDefinition(AutoscalePolicy autoscalePolicy) {
    	int tenantId = getTenantId();
    	Map<String,AutoscalePolicy> policies;
    	
    	if(!autoscalePolicy.getIsPublic()){
    		if (autoscalePolicyMap.containsKey(tenantId)){
        		policies = autoscalePolicyMap.get(tenantId);
        	}
        	else{
        		policies = new HashMap<String, AutoscalePolicy>();
        		autoscalePolicyMap.put(tenantId, policies);
        	}
    	}
    	else{
    		if (autoscalePolicyMap.containsKey(PUBLIC_DEFINITION)){
    			policies = autoscalePolicyMap.get(PUBLIC_DEFINITION);
    		}
    		else{
    			policies = new HashMap<String, AutoscalePolicy>();
    			autoscalePolicyMap.put(PUBLIC_DEFINITION, policies);
    		}
    	}
    	
    	policies.put(autoscalePolicy.getId(), autoscalePolicy);
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed auto scaling policy definition");
        return stratosApiResponse;
    }

    public StratosApiResponse addDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy) {
    	int tenantId = getTenantId();
    	Map<String,DeploymentPolicy> policies;
    	
    	if(!deploymentPolicy.isPublic()){
    		if (deploymentPolicyMap.containsKey(tenantId)){
        		policies = deploymentPolicyMap.get(tenantId);
        	}
        	else{
        		policies = new HashMap<String, DeploymentPolicy>();
        		deploymentPolicyMap.put(tenantId, policies);
        	}
    	}
    	else{
    		if (deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			policies = deploymentPolicyMap.get(PUBLIC_DEFINITION);
    		}
    		else{
    			policies = new HashMap<String, DeploymentPolicy>();
    			deploymentPolicyMap.put(PUBLIC_DEFINITION, policies);
    		}
    	}
    	
    	policies.put(deploymentPolicy.getId(),deploymentPolicy);
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed deployment policy definition");
        return stratosApiResponse;
    }

    public Partition[] getPartitions() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!partitionMap.containsKey(tenantId) && !partitionMap.containsKey(PUBLIC_DEFINITION)){
        	return new HashMap<String,Partition>().values().toArray(new Partition[0]);
    	}
    	
    	List<Partition> p = new ArrayList<Partition>();
    	
		if(partitionMap.get(tenantId) != null)
    		p.addAll(partitionMap.get(tenantId).values());
		
		if(partitionMap.get(PUBLIC_DEFINITION) != null)
			p.addAll(partitionMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new Partition[0]);
    }

    public Partition getPartition(String partitionId) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!partitionMap.containsKey(tenantId)){
        	if(!partitionMap.containsKey(PUBLIC_DEFINITION)){
        		throw new RestAPIException(Status.NO_CONTENT,"No partitions have been defined for the tenant");
        	}
        	if(!(partitionMap.get(PUBLIC_DEFINITION)).containsKey(partitionId)){
        		throw new RestAPIException("There is no partition with the id: " + partitionId);
        	}
        	return  (partitionMap.get(PUBLIC_DEFINITION)).get(partitionId);
        }
        else{
        	if(!(partitionMap.get(tenantId)).containsKey(partitionId)){
        		throw new RestAPIException("There is no partition with the id: " + partitionId);
        	}
        	return  (partitionMap.get(tenantId)).get(partitionId);
        }
    }

    public Partition[] getPartitionsOfPolicy(String deploymentPolicyId) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
    		if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    		}
    		else{
    			if(!(deploymentPolicyMap.get(PUBLIC_DEFINITION)).containsKey(deploymentPolicyId)){
        			throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        		}
        		return (deploymentPolicyMap.get(PUBLIC_DEFINITION)).get(deploymentPolicyId).getPartition().toArray(new Partition[0]);
    		}
    	}
        	
    	if(!(deploymentPolicyMap.get(tenantId)).containsKey(deploymentPolicyId)){
    		throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        }
    	return (deploymentPolicyMap.get(tenantId)).get(deploymentPolicyId).getPartition().toArray(new Partition[0]);
    }

    public PartitionGroup[] getPartitionGroups(String deploymentPolicyId)  throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
    		if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    		}
    		else{
    			if(!(deploymentPolicyMap.get(PUBLIC_DEFINITION)).containsKey(deploymentPolicyId)){
        			throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        		}
        		return (deploymentPolicyMap.get(PUBLIC_DEFINITION)).get(deploymentPolicyId).getPartitionGroup().toArray(new PartitionGroup[0]);
    		}
    	}
        	
    	if(!(deploymentPolicyMap.get(tenantId)).containsKey(deploymentPolicyId)){
    		throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        }
    	return (deploymentPolicyMap.get(tenantId)).get(deploymentPolicyId).getPartitionGroup().toArray(new PartitionGroup[0]);
    }

    public AutoscalePolicy[] getAutoscalePolicies()  throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!autoscalePolicyMap.containsKey(tenantId) && !autoscalePolicyMap.containsKey(PUBLIC_DEFINITION)){
    			return new HashMap<String,AutoscalePolicy>().values().toArray(new AutoscalePolicy[0]);
    	}
    	
    	List<AutoscalePolicy> p = new ArrayList<AutoscalePolicy>();
    	
		if(autoscalePolicyMap.get(tenantId) != null)
    		p.addAll(autoscalePolicyMap.get(tenantId).values());
		
		if(autoscalePolicyMap.get(PUBLIC_DEFINITION) != null)
			p.addAll(autoscalePolicyMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new AutoscalePolicy[0]);
    }

    public AutoscalePolicy getAutoscalePolicies(String autoscalePolicyId) throws  RestAPIException{
    	int tenantId = getTenantId();
    	if(!autoscalePolicyMap.containsKey(tenantId)){
    		if(!autoscalePolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException(Status.NO_CONTENT,"No autoscaling policies have been defined for tenant");
    		}
    		if(!(autoscalePolicyMap.get(PUBLIC_DEFINITION)).containsKey(autoscalePolicyId)){
        		throw new RestAPIException("There is no auto scale policy with id: " + autoscalePolicyId);
            }
            return (autoscalePolicyMap.get(PUBLIC_DEFINITION)).get(autoscalePolicyId);
    	}
    	else{
        	if(!(autoscalePolicyMap.get(tenantId)).containsKey(autoscalePolicyId)){
        		throw new RestAPIException("There is no auto scale policy with id: " + autoscalePolicyId);
            }
            return (autoscalePolicyMap.get(tenantId)).get(autoscalePolicyId);
    	}
    }

    public DeploymentPolicy[] getDeploymentPolicies() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId) && !deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
			return new HashMap<String,DeploymentPolicy>().values().toArray(new DeploymentPolicy[0]);
    	}
	
    	List<DeploymentPolicy> p = new ArrayList<DeploymentPolicy>();
    	
    	if(deploymentPolicyMap.get(tenantId) != null)
    		p.addAll(deploymentPolicyMap.get(tenantId).values());
    	
    	if(deploymentPolicyMap.get(PUBLIC_DEFINITION) != null)
    		p.addAll(deploymentPolicyMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new DeploymentPolicy[0]);
    }

    public DeploymentPolicy getDeploymentPolicies(String deploymentPolicyId) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
    		if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException("No deployment policies have been defined for tenant");
    		}
    		else{
    			if(!(deploymentPolicyMap.get(PUBLIC_DEFINITION)).containsKey(deploymentPolicyId)){
    	            throw new RestAPIException("There is no deployment policy with id: " + deploymentPolicyId);
    	        }
    			return (deploymentPolicyMap.get(PUBLIC_DEFINITION)).get(deploymentPolicyId);
    		}
    	}
    	else{
    		if(!(deploymentPolicyMap.get(tenantId)).containsKey(deploymentPolicyId)){
                throw new RestAPIException("There is no deployment policy with id: " + deploymentPolicyId);
            }
            return (deploymentPolicyMap.get(tenantId)).get(deploymentPolicyId);
    	}
    }
    
    public StratosApiResponse deployService(ServiceDefinitionBean serviceDefinitionBean) {
    	int tenantId = getTenantId();
    	Map<String,ServiceDefinitionBean> serviceDefinitions;
    	
    	if(!serviceDefinitionBean.getIsPublic()){
        	if(!serviceDefinitionMap.containsKey(tenantId)){
        		serviceDefinitions = new HashMap<String,ServiceDefinitionBean>();
        		serviceDefinitionMap.put(tenantId, serviceDefinitions);
        	}
        	else{
        		serviceDefinitions = serviceDefinitionMap.get(tenantId);
        	}
    	}
    	else{
    		if(!serviceDefinitionMap.containsKey(PUBLIC_DEFINITION)){
        		serviceDefinitions = new HashMap<String,ServiceDefinitionBean>();
        		serviceDefinitionMap.put(PUBLIC_DEFINITION, serviceDefinitions);
        	}
        	else{
        		serviceDefinitions = serviceDefinitionMap.get(PUBLIC_DEFINITION);
        	}
    	}
    	
    	serviceDefinitions.put(serviceDefinitionBean.getCartridgeType(),serviceDefinitionBean);
        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed service");
        return stratosApiResponse;

    }
       
    public ServiceDefinitionBean[] getServices() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!serviceDefinitionMap.containsKey(tenantId) && !serviceDefinitionMap.containsKey(PUBLIC_DEFINITION)){
    			return new HashMap<String,ServiceDefinitionBean>().values().toArray(new ServiceDefinitionBean[0]);
    	}
    	   	
    	List<ServiceDefinitionBean> p = new ArrayList<ServiceDefinitionBean>();
    	
    	if(serviceDefinitionMap.get(tenantId) != null)
    		p.addAll(serviceDefinitionMap.get(tenantId).values());
    	
    	if(serviceDefinitionMap.get(PUBLIC_DEFINITION) != null)
    		p.addAll(serviceDefinitionMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new ServiceDefinitionBean[0]);
    }

    public ServiceDefinitionBean getServiceType(String serviceType) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!serviceDefinitionMap.containsKey(tenantId)){
        	if(!serviceDefinitionMap.containsKey(PUBLIC_DEFINITION)){
        		throw new RestAPIException(Status.NO_CONTENT,"No services have been defined for tenant");
        	}
        	return (serviceDefinitionMap.get(PUBLIC_DEFINITION)).get(serviceType);
        }
        return (serviceDefinitionMap.get(tenantId)).get(serviceType);
    }

    public Partition[] getPartitions(String deploymentPolicyId, String partitionGroupId) throws RestAPIException{
    	int tenantId = getTenantId();
    	DeploymentPolicy deploymentPolicy;
    	
    	if(!deploymentPolicyMap.containsKey(tenantId)){
    		if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    		}
    		else{
    			if(!(deploymentPolicyMap.get(PUBLIC_DEFINITION)).containsKey(deploymentPolicyId)){
                    throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
                }
        		else{
        			deploymentPolicy = (deploymentPolicyMap.get(PUBLIC_DEFINITION)).get(deploymentPolicyId);
        		}
    		}
    	}
    	else{
    		if(!(deploymentPolicyMap.get(tenantId)).containsKey(deploymentPolicyId)){
                throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
            }
    		else{
    			deploymentPolicy = (deploymentPolicyMap.get(tenantId)).get(deploymentPolicyId);
    		}
    	}
        	  	
        Partition[] partitions = null;
         for(PartitionGroup partitionGroup : deploymentPolicy.getPartitionGroup()){
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
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
        	if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
        		return new HashMap<String,DeploymentPolicy>().values().toArray(new DeploymentPolicy[0]);
        	}
        	else{
        		return (deploymentPolicyMap.get(PUBLIC_DEFINITION)).values().toArray(new DeploymentPolicy[0]);
        	}
        }
        else{
        	return (deploymentPolicyMap.get(tenantId)).values().toArray(new DeploymentPolicy[0]);
        }
    }

	public StratosApiResponse addSubscriptionDomains(int tenantId, String alias, SubscriptionDomainRequest request) {
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
		
		StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully added subscription domain/s.");
        return stratosApiResponse;
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

	public StratosApiResponse removeSubscriptionDomain(int tenantId,
			String subscriptionAlias, String domainName) {
		StratosApiResponse stratosApiResponse = new StratosApiResponse();
		
		List<String> tenantAliases = tenantIdToAliasesMap.get(tenantId);
		if(tenantAliases != null && tenantAliases.contains(subscriptionAlias)) {
			for (Iterator<SubscriptionDomain> iterator = subscriptionAliasToDomainMap.get(subscriptionAlias).iterator(); iterator
					.hasNext();) {
				SubscriptionDomain subscriptionDomain = (SubscriptionDomain) iterator.next();
				if (subscriptionDomain.getDomainName().equals(domainName)) {
					iterator.remove();
					stratosApiResponse.setMessage("Successfully removed the subscription domain: "+domainName);
				}
			}
		} else {		
			stratosApiResponse.setMessage("Failed to remove the subscription domain: "+domainName);
		}
		
        return stratosApiResponse;
	}
	
	public void addUser(UserInfoBean user) {
		int tenantId = getTenantId();
		Map<String, UserInfoBean> users;
		
		if(tenantUserMap.containsKey(tenantId)) {
			users = tenantUserMap.get(tenantId);
		}
		else {
			users = new HashMap<String, UserInfoBean>();
			tenantUserMap.put(tenantId, users);
		}
		
		users.put(user.getUserName(), user);
	}
	
	public void deleteUser(String userName) {
		int tenantId = getTenantId();
		Map<String, UserInfoBean> users;
		
		if(!tenantUserMap.containsKey(tenantId)) {
			return;
		}
		
		users = tenantUserMap.get(tenantId);
		users.remove(userName);
	}
	
	public void updateUser(UserInfoBean user) {
		int tenantId = getTenantId();
		Map<String, UserInfoBean> users;
		
		if(!tenantUserMap.containsKey(tenantId)) {
			return;
		}
		
		users = tenantUserMap.get(tenantId);
		if(users.containsKey(user.getUserName())) {
			users.put(user.getUserName(), user);
		}
	}
	
	public List<UserInfoBean> getAllUsers() {
		int tenantId = getTenantId();
        List<UserInfoBean> userList = new ArrayList<UserInfoBean>();
        
        if(tenantUserMap.containsKey(tenantId)) {
        	userList.addAll(tenantUserMap.get(tenantId).values());
		}
        return userList;
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
