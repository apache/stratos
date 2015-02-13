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

import org.apache.stratos.common.beans.ApiResponseBean;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionRefBean;
import org.apache.stratos.common.beans.partition.PartitionBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.topology.ClusterBean;
import org.apache.stratos.messaging.domain.tenant.SubscriptionDomain;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.wso2.carbon.context.CarbonContext;

import javax.ws.rs.core.Response.Status;
import java.util.*;

public class MockContext {
    private static MockContext mockContext = new MockContext(); // singleton

    private Map<String, List<SubscriptionDomain>> subscriptionAliasToDomainMap = new HashMap<String, List<SubscriptionDomain>>();
    private Map<Integer, List<String>> tenantIdToAliasesMap = new HashMap<Integer, List<String>>();
    private Map<Integer, List<CartridgeBean>> cartridgeDefinitionBeanList = new HashMap<Integer, List<CartridgeBean>>();
    private Map<Integer, Map<String,CartridgeBean>> availableSingleTenantCartridges = new HashMap<Integer, Map<String,CartridgeBean>>();
    private Map<Integer, Map<String,CartridgeBean>> availableMultiTenantCartridges = new HashMap<Integer, Map<String,CartridgeBean>>();
    private Map<Integer, Map<String,CartridgeBean>> subscribedCartridges = new HashMap<Integer, Map<String,CartridgeBean>>();
    private Map<String,TenantInfoBean> tenantMap = new HashMap<String, TenantInfoBean>();
    private Map<Integer, Map<String, UserInfoBean>> tenantUserMap= new HashMap<Integer, Map<String, UserInfoBean>>();
    private Map<String, Integer> tenantIdMap = new HashMap<String, Integer>();
    private Map<Integer, Map<String,PartitionBean>> partitionMap = new HashMap<Integer, Map<String, PartitionBean>>();
    private Map<Integer, Map<String,AutoscalePolicyBean>> autoscalePolicyMap = new HashMap<Integer, Map<String, AutoscalePolicyBean>>();
    private Map<Integer, Map<String,DeploymentPolicyBean>> deploymentPolicyMap = new HashMap<Integer, Map<String, DeploymentPolicyBean>>();
    private Map<String,ClusterBean> clusterMap = new HashMap<String, ClusterBean>();
    
    private int tenantIdCount=1;
    public static final int PUBLIC_DEFINITION = 0;

    private MockContext(){} // do not allow to initialize

    public static MockContext getInstance(){
        return mockContext;
    }

    public ApiResponseBean addCartirdgeDefinition(CartridgeBean cartridgeDefinitionBean){
    	int tenantId = getTenantId();
    	List<CartridgeBean> tenantCartridges;
    	
    	if(!cartridgeDefinitionBean.isPublic()){
    		if(this.cartridgeDefinitionBeanList.containsKey(tenantId)){
        		tenantCartridges = this.cartridgeDefinitionBeanList.get(tenantId);
        	}
        	else{
        		tenantCartridges = new LinkedList<CartridgeBean>();
        		this.cartridgeDefinitionBeanList.put(tenantId, tenantCartridges);
        	}
    	}
    	else{
    		if(this.cartridgeDefinitionBeanList.containsKey(PUBLIC_DEFINITION)){
        		tenantCartridges = this.cartridgeDefinitionBeanList.get(PUBLIC_DEFINITION);
        	}
        	else{
        		tenantCartridges = new LinkedList<CartridgeBean>();
        		this.cartridgeDefinitionBeanList.put(PUBLIC_DEFINITION, tenantCartridges);
        	}
    	}
    	
    	tenantCartridges.add(cartridgeDefinitionBean);
        
    	CartridgeBean cartridge = new CartridgeBean();
        cartridge.setType(cartridgeDefinitionBean.getType());
        cartridge.setDescription(cartridgeDefinitionBean.getDescription());
        cartridge.setDisplayName(cartridgeDefinitionBean.getDisplayName());
        cartridge.setMultiTenant(cartridgeDefinitionBean.isMultiTenant());
        cartridge.setProvider(cartridgeDefinitionBean.getProvider());
        cartridge.setVersion(cartridgeDefinitionBean.getVersion());
        //cartridge.setIsPublic(cartridgeDefinitionBean.isPublic());

        Map<String,CartridgeBean> cartridges;
        if(cartridge.isMultiTenant()){
        	if(!cartridge.isPublic()){
        		if(this.availableMultiTenantCartridges.containsKey(tenantId)){
            		cartridges = availableMultiTenantCartridges.get(tenantId);
            	}
            	else{
            		cartridges = new HashMap<String,CartridgeBean>();
            		this.availableMultiTenantCartridges.put(tenantId, cartridges);
            	}
        	}else{
        		if(this.availableMultiTenantCartridges.containsKey(PUBLIC_DEFINITION)){
            		cartridges = availableMultiTenantCartridges.get(PUBLIC_DEFINITION);
            	}
            	else{
            		cartridges = new HashMap<String,CartridgeBean>();
            		this.availableMultiTenantCartridges.put(PUBLIC_DEFINITION, cartridges);
            	}
        	}
        	
            cartridges.put(cartridge.getType(), cartridge);
            System.out.println(cartridges.size());
        }else{
        	if(!cartridge.isPublic()){
        		if(this.availableSingleTenantCartridges.containsKey(tenantId)){
            		cartridges = availableSingleTenantCartridges.get(tenantId);
            	}
            	else{
            		cartridges = new HashMap<String,CartridgeBean>();
            		this.availableSingleTenantCartridges.put(tenantId, cartridges);
            	}
        	}else{
        		if(this.availableSingleTenantCartridges.containsKey(PUBLIC_DEFINITION)){
            		cartridges = availableSingleTenantCartridges.get(PUBLIC_DEFINITION);
            	}
            	else{
            		cartridges = new HashMap<String,CartridgeBean>();
            		this.availableSingleTenantCartridges.put(PUBLIC_DEFINITION, cartridges);
            	}
        	}
        	
        	cartridges.put(cartridge.getType(), cartridge);
            System.out.println(cartridges.size());
        }

        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully deployed cartridge definition with type ");
        return stratosApiResponse;
    }

    public CartridgeBean[] getAvailableMultiTenantCartridges() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!availableMultiTenantCartridges.containsKey(tenantId) && !availableMultiTenantCartridges.containsKey(PUBLIC_DEFINITION)){
    		return new HashMap<String, CartridgeBean>().values().toArray(new CartridgeBean[0]);
    	}    	
    	
    	List<CartridgeBean> p = new ArrayList<CartridgeBean>();
    	
		if(availableMultiTenantCartridges.get(tenantId) != null)
    		p.addAll(availableMultiTenantCartridges.get(tenantId).values());
		
		if(availableMultiTenantCartridges.get(PUBLIC_DEFINITION) != null)
			p.addAll(availableMultiTenantCartridges.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new CartridgeBean[0]);
    }


    public CartridgeBean[] getAvailableSingleTenantCartridges() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!availableSingleTenantCartridges.containsKey(tenantId) && !availableSingleTenantCartridges.containsKey(PUBLIC_DEFINITION)){
    		return new HashMap<String, CartridgeBean>().values().toArray(new CartridgeBean[0]);
    	}    	
    	
    	List<CartridgeBean> p = new ArrayList<CartridgeBean>();
    	
		if(availableSingleTenantCartridges.get(tenantId) != null)
    		p.addAll(availableSingleTenantCartridges.get(tenantId).values());
		
		if(availableSingleTenantCartridges.get(PUBLIC_DEFINITION) != null)
			p.addAll(availableSingleTenantCartridges.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new CartridgeBean[0]);
    }

    public CartridgeBean[] getAvailableLbCartridges() throws RestAPIException{
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

    public CartridgeBean[] getAvailableCartridges() throws RestAPIException{
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


    public CartridgeBean[] getSubscribedCartridges() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!subscribedCartridges.containsKey(tenantId) && !subscribedCartridges.containsKey(PUBLIC_DEFINITION)){
    		return new HashMap<String, CartridgeBean>().values().toArray(new CartridgeBean[0]);
    	}
        List<CartridgeBean> p = new ArrayList<CartridgeBean>();
    	
		if(subscribedCartridges.get(tenantId) != null)
    		p.addAll(subscribedCartridges.get(tenantId).values());
		
		if(subscribedCartridges.get(PUBLIC_DEFINITION) != null)
			p.addAll(subscribedCartridges.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new CartridgeBean[0]);
    }

    public ApiResponseBean unsubscribe(String alias) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(subscribedCartridges.containsKey(tenantId)){
        	if((subscribedCartridges.get(tenantId)).containsKey(alias)){
            	(subscribedCartridges.get(tenantId)).remove(alias);
        	}   	
        }else{
            throw new RestAPIException(Status.NO_CONTENT,"Unable to un-subscribe");
        }
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully un-subscribed");
        return stratosApiResponse;
    }

    public CartridgeBean getCartridgeInfo(String alias) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!subscribedCartridges.containsKey(tenantId))
    		throw new RestAPIException(Status.NO_CONTENT, "No cartridges subscribed for current tenant.");
    	
    	if(!(subscribedCartridges.get(tenantId)).containsKey(alias))
    		throw new RestAPIException(Status.NO_CONTENT,"Cartridge information is not available.");
    		
        return (subscribedCartridges.get(tenantId)).get(alias);
    }

    public CartridgeBean getAvailableSingleTenantCartridgeInfo(String cartridgeType) throws RestAPIException{
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
    
    public CartridgeBean getAvailableMultiTenantCartridgeInfo(String cartridgeType) throws RestAPIException{
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
    
    public ApiResponseBean deleteCartridgeDefinition(String cartridgeType) throws RestAPIException{
    	if(!deleteFromAvailableSingleTenantCartridgeDefinitions(cartridgeType) && !deleteFromAvailableMultiTenantCartridgeDefinitions(cartridgeType)){
    		throw new RestAPIException(Status.NO_CONTENT,"No cartridges defined for tenant");
    	}
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
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

    public ApiResponseBean addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException{
    	try{
            tenantMap.put(tenantInfoBean.getTenantDomain(),tenantInfoBean);
            tenantInfoBean.setTenantId(tenantIdCount);
            tenantIdMap.put(tenantInfoBean.getAdmin(), tenantIdCount++);
        }catch (Exception e){
            throw new RestAPIException(Status.INTERNAL_SERVER_ERROR,e.getMessage());
        }
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully added new Tenant");
        return stratosApiResponse;
    }

    public TenantInfoBean getTenant(String tenantDomain) throws RestAPIException{
        if(!tenantMap.containsKey(tenantDomain)){
            throw new RestAPIException(Status.NO_CONTENT,"Information for tenant: " + tenantDomain + " is not available");
        }
        return tenantMap.get(tenantDomain);
    }
    
    public ApiResponseBean deleteTenant(String tenantDomain) {
        if(tenantMap.containsKey(tenantDomain)){
        	TenantInfoBean tenant=tenantMap.get(tenantDomain);
        	tenantMap.remove(tenantDomain);
        	tenantIdMap.remove(tenant.getTenantId());
        }
    	        
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
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

    public ApiResponseBean activateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(true);
        } else{
            throw new RestAPIException(Status.BAD_REQUEST,"Invalid tenant domain");
        }
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully activated Tenant");
        return stratosApiResponse;
    }

    public ApiResponseBean deactivateTenant(String tenantDomain) throws RestAPIException{
        if(tenantMap.containsKey(tenantDomain)){
            tenantMap.get(tenantDomain).setActive(false);
        } else{
            throw new RestAPIException(Status.BAD_REQUEST,"Invalid tenant domain");
        }
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully deactivated Tenant");
        return stratosApiResponse;
    }

    public ApiResponseBean addPartition(PartitionBean partition) {
    	int tenantId = getTenantId();
    	Map<String,PartitionBean> partitions;
    	
    	if(!partition.isPublic()){
    		if (partitionMap.containsKey(tenantId)){
        		partitions = partitionMap.get(tenantId);
        	}
        	else{
        		partitions = new HashMap<String, PartitionBean>();
        		partitionMap.put(tenantId, partitions);
        	}
    	}
    	else {
    		if (partitionMap.containsKey(PUBLIC_DEFINITION)){
    			partitions = partitionMap.get(PUBLIC_DEFINITION);
    		}
    		else{
    			partitions = new HashMap<String, PartitionBean>();
        		partitionMap.put(PUBLIC_DEFINITION, partitions);
    		}
    	} 	
    	
    	partitions.put(partition.getId(), partition);
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully deployed partition");
        return stratosApiResponse;
    }

    public ApiResponseBean addAutoScalingPolicyDefinition(AutoscalePolicyBean autoscalePolicy) {
    	int tenantId = getTenantId();
    	Map<String,AutoscalePolicyBean> policies;
    	
    	if(!autoscalePolicy.getIsPublic()){
    		if (autoscalePolicyMap.containsKey(tenantId)){
        		policies = autoscalePolicyMap.get(tenantId);
        	}
        	else{
        		policies = new HashMap<String, AutoscalePolicyBean>();
        		autoscalePolicyMap.put(tenantId, policies);
        	}
    	}
    	else{
    		if (autoscalePolicyMap.containsKey(PUBLIC_DEFINITION)){
    			policies = autoscalePolicyMap.get(PUBLIC_DEFINITION);
    		}
    		else{
    			policies = new HashMap<String, AutoscalePolicyBean>();
    			autoscalePolicyMap.put(PUBLIC_DEFINITION, policies);
    		}
    	}
    	
    	policies.put(autoscalePolicy.getId(), autoscalePolicy);
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully deployed auto scaling policy definition");
        return stratosApiResponse;
    }

    public ApiResponseBean addDeploymentPolicyDefinition(String applicationId, DeploymentPolicyBean deploymentPolicy) {
    	int tenantId = getTenantId();
    	Map<String,DeploymentPolicyBean> policies;
    	

    	if (deploymentPolicyMap.containsKey(tenantId)){
        	policies = deploymentPolicyMap.get(tenantId);
        }
        else{
        	policies = new HashMap<String, DeploymentPolicyBean>();
        	deploymentPolicyMap.put(tenantId, policies);
        }

    	
    	policies.put(applicationId + UUID.randomUUID().getLeastSignificantBits(),deploymentPolicy);
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully deployed deployment policy definition");
        return stratosApiResponse;
    }

    public PartitionBean[] getPartitions() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!partitionMap.containsKey(tenantId) && !partitionMap.containsKey(PUBLIC_DEFINITION)){
        	return new HashMap<String,PartitionBean>().values().toArray(new PartitionBean[0]);
    	}
    	
    	List<PartitionBean> p = new ArrayList<PartitionBean>();
    	
		if(partitionMap.get(tenantId) != null)
    		p.addAll(partitionMap.get(tenantId).values());
		
		if(partitionMap.get(PUBLIC_DEFINITION) != null)
			p.addAll(partitionMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new PartitionBean[0]);
    }

    public PartitionBean getPartition(String partitionId) throws RestAPIException{
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

    public PartitionBean[] getPartitionsOfPolicy(String deploymentPolicyId) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
    		if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    		}
    		else{
    			if(!(deploymentPolicyMap.get(PUBLIC_DEFINITION)).containsKey(deploymentPolicyId)){
        			throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        		}
        		return (deploymentPolicyMap.get(PUBLIC_DEFINITION)).get(deploymentPolicyId).getNetworkPartition().get(0).getPartitions().toArray(new PartitionBean[0]);
    		}
    	}
        	
    	if(!(deploymentPolicyMap.get(tenantId)).containsKey(deploymentPolicyId)){
    		throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        }
        //FIXME to parse thr all the NW partitions
    	return (deploymentPolicyMap.get(tenantId)).
                get(deploymentPolicyId).getNetworkPartition().get(0).getPartitions().toArray(new PartitionBean[0]);
    }

    public NetworkPartitionRefBean[] getPartitionGroups(String deploymentPolicyId)  throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
    		if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
    			throw new RestAPIException(Status.NO_CONTENT,"No deployment policies have been defined for tenant");
    		}
    		else{
    			if(!(deploymentPolicyMap.get(PUBLIC_DEFINITION)).containsKey(deploymentPolicyId)){
        			throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        		}
        		return (NetworkPartitionRefBean[])(deploymentPolicyMap.get(PUBLIC_DEFINITION)).get(deploymentPolicyId).getNetworkPartition().toArray();
    		}
    	}
        	
    	if(!(deploymentPolicyMap.get(tenantId)).containsKey(deploymentPolicyId)){
    		throw new RestAPIException(Status.NO_CONTENT,"There is no deployment policy with id: " + deploymentPolicyId);
        }
    	return (NetworkPartitionRefBean[])(deploymentPolicyMap.get(tenantId)).get(deploymentPolicyId).getNetworkPartition().toArray();
    }

    public AutoscalePolicyBean[] getAutoscalePolicies()  throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!autoscalePolicyMap.containsKey(tenantId) && !autoscalePolicyMap.containsKey(PUBLIC_DEFINITION)){
    			return new HashMap<String,AutoscalePolicyBean>().values().toArray(new AutoscalePolicyBean[0]);
    	}
    	
    	List<AutoscalePolicyBean> p = new ArrayList<AutoscalePolicyBean>();
    	
		if(autoscalePolicyMap.get(tenantId) != null)
    		p.addAll(autoscalePolicyMap.get(tenantId).values());
		
		if(autoscalePolicyMap.get(PUBLIC_DEFINITION) != null)
			p.addAll(autoscalePolicyMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new AutoscalePolicyBean[0]);
    }

    public AutoscalePolicyBean getAutoscalePolicies(String autoscalePolicyId) throws  RestAPIException{
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

    public DeploymentPolicyBean[] getDeploymentPolicies() throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId) && !deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
			return new HashMap<String,DeploymentPolicyBean>().values().toArray(new DeploymentPolicyBean[0]);
    	}
	
    	List<DeploymentPolicyBean> p = new ArrayList<DeploymentPolicyBean>();
    	
    	if(deploymentPolicyMap.get(tenantId) != null)
    		p.addAll(deploymentPolicyMap.get(tenantId).values());
    	
    	if(deploymentPolicyMap.get(PUBLIC_DEFINITION) != null)
    		p.addAll(deploymentPolicyMap.get(PUBLIC_DEFINITION).values());
    	
    	return p.toArray(new DeploymentPolicyBean[0]);
    }

    public DeploymentPolicyBean getDeploymentPolicies(String deploymentPolicyId) throws RestAPIException{
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
    
    public ApiResponseBean deployService(Object serviceDefinitionBean) {
//    	int tenantId = getTenantId();
//    	Map<String,ServiceDefinitionBean> serviceDefinitions;
//
//    	if(!serviceDefinitionBean.getIsPublic()){
//        	if(!serviceDefinitionMap.containsKey(tenantId)){
//        		serviceDefinitions = new HashMap<String,ServiceDefinitionBean>();
//        		serviceDefinitionMap.put(tenantId, serviceDefinitions);
//        	}
//        	else{
//        		serviceDefinitions = serviceDefinitionMap.get(tenantId);
//        	}
//    	}
//    	else{
//    		if(!serviceDefinitionMap.containsKey(PUBLIC_DEFINITION)){
//        		serviceDefinitions = new HashMap<String,ServiceDefinitionBean>();
//        		serviceDefinitionMap.put(PUBLIC_DEFINITION, serviceDefinitions);
//        	}
//        	else{
//        		serviceDefinitions = serviceDefinitionMap.get(PUBLIC_DEFINITION);
//        	}
//    	}
//
//    	serviceDefinitions.put(serviceDefinitionBean.getCartridgeType(),serviceDefinitionBean);
        ApiResponseBean stratosApiResponse = new ApiResponseBean();
        stratosApiResponse.setMessage("Successfully deployed service");
        return stratosApiResponse;

    }

    public PartitionBean[] getPartitions(String deploymentPolicyId, String partitionGroupId) throws RestAPIException{
    	int tenantId = getTenantId();
    	DeploymentPolicyBean deploymentPolicy;
    	
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
        	  	
        PartitionBean[] partitions = null;
         for(NetworkPartitionRefBean networkPartition : deploymentPolicy.getNetworkPartition()){
             if(networkPartition.getId().equals(partitionGroupId)){
                 partitions =  networkPartition.getPartitions().toArray(new PartitionBean[0]);
             }
         }
        if(partitions == null){
            throw new RestAPIException(Status.NO_CONTENT,"Partition not found");
        }
        return partitions;
    }
    
    public ClusterBean[] getClusters() throws RestAPIException{
        return clusterMap.values().toArray(new ClusterBean[0]);
    }
    
    public DeploymentPolicyBean[] getDeploymentPoliciesForCartridgeType(String cartridgeType) throws RestAPIException{
    	int tenantId = getTenantId();
    	if(!deploymentPolicyMap.containsKey(tenantId)){
        	if(!deploymentPolicyMap.containsKey(PUBLIC_DEFINITION)){
        		return new HashMap<String,DeploymentPolicyBean>().values().toArray(new DeploymentPolicyBean[0]);
        	}
        	else{
        		return (deploymentPolicyMap.get(PUBLIC_DEFINITION)).values().toArray(new DeploymentPolicyBean[0]);
        	}
        }
        else{
        	return (deploymentPolicyMap.get(tenantId)).values().toArray(new DeploymentPolicyBean[0]);
        }
    }

	public ApiResponseBean removeSubscriptionDomain(int tenantId,
			String subscriptionAlias, String domainName) {
		ApiResponseBean stratosApiResponse = new ApiResponseBean();
		
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
