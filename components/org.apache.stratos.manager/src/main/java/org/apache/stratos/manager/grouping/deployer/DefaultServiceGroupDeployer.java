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

package org.apache.stratos.manager.grouping.deployer;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.InvalidServiceGroupException;
import org.apache.stratos.manager.exception.ServiceGroupDefinitioException;
import org.apache.stratos.manager.grouping.definitions.StaticProperty;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.grouping.definitions.DependencyDefinitions;
import org.apache.stratos.manager.grouping.definitions.StartupOrderDefinition;
import org.apache.stratos.cloud.controller.stub.pojo.ServiceGroup;
import org.apache.stratos.cloud.controller.stub.pojo.Dependencies;
import org.apache.stratos.cloud.controller.stub.pojo.StartupOrder;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidServiceGroupExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultServiceGroupDeployer implements ServiceGroupDeployer {

    private static Log log = LogFactory.getLog(DefaultServiceGroupDeployer.class);


    public DefaultServiceGroupDeployer () {
    }

    public void deployServiceGroupDefinition (Object serviceGroupDefinitionObj) throws InvalidServiceGroupException, ServiceGroupDefinitioException, ADCException {

        ServiceGroupDefinition serviceGroupDefinition = null; 
        ServiceGroup serviceGroup = null;
        
        if (serviceGroupDefinitionObj == null) {
        	if (log.isDebugEnabled()) {
            	log.debug("deploying service group is null ");
            }
            throw new InvalidServiceGroupException("Service Group definition not found");
        }

        if (serviceGroupDefinitionObj instanceof ServiceGroupDefinition) {
            serviceGroupDefinition = (ServiceGroupDefinition) serviceGroupDefinitionObj;
            
            if (log.isDebugEnabled()) {
            	log.debug("deploying service group with name " + serviceGroupDefinition.getName());
            }
            
            // convert serviceGroupDefinition to serviceGroup
            serviceGroup = this.populateServiceGroupPojo(serviceGroupDefinition);
        } else {
            log.error("trying to deploy invalid service group ");
            throw new InvalidServiceGroupException("Invalid Service Group definition");
        }
        

        // if any cartridges are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getCartridges() != null) {
        	
        	if (log.isDebugEnabled()) {
            	log.debug("checking cartridges in service group " + serviceGroupDefinition.getName());
            }
        	
            List<String> cartridgeTypes = serviceGroupDefinition.getCartridges();
            
            Set<String> duplicates = this.findDuplicates(cartridgeTypes);
            
            if (duplicates.size() > 0) {
            	
            	StringBuffer buf = new StringBuffer();
            	for (String dup : duplicates) {
            		buf. append(dup).append(" ");
            	}
            	if (log.isDebugEnabled()) {
                	log.debug("duplicate cartridges defined: " + buf.toString());
                }
            	throw new InvalidServiceGroupException("Invalid Service Group definition, duplicate cartridges defined:" + buf.toString());
            }

            CloudControllerServiceClient ccServiceClient = null;

            try {
                ccServiceClient = CloudControllerServiceClient.getServiceClient();

            } catch (AxisFault axisFault) {
                throw new ADCException(axisFault);
            }

            for (String cartridgeType : cartridgeTypes) {
                try {
                    if(ccServiceClient.getCartridgeInfo(cartridgeType) == null) {
                        // cartridge is not deployed, can't continue
                    	log.error("invalid cartridge found in service group " + cartridgeType);
                        throw new InvalidServiceGroupException("No Cartridge Definition found with type " + cartridgeType);
                    }
                } catch (RemoteException e) {
                    throw new ADCException(e);
                } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
                	throw new ADCException(e);
                }
            }
        }

        // if any sub groups are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getSubGroups() != null) {
        	
        	if (log.isDebugEnabled()) {
            	log.debug("checking subGroups in service group " + serviceGroupDefinition.getName());
            }
        	
            List<String> subGroupNames = serviceGroupDefinition.getSubGroups();
            
        	Set<String> duplicates = this.findDuplicates(subGroupNames);
            
            if (duplicates.size() > 0) {
            	
            	StringBuffer buf = new StringBuffer();
            	for (String dup : duplicates) {
            		buf. append(dup).append(" ");
            	}
            	if (log.isDebugEnabled()) {
                	log.debug("duplicate subGroups defined: " + buf.toString());
                }
            	throw new InvalidServiceGroupException("Invalid Service Group definition, duplicate subGroups defined:" + buf.toString());
            }

            for (String subGroupName : subGroupNames) {
                if (getServiceGroupDefinition(subGroupName) == null) {
                    // sub group not deployed, can't continue
                	if (log.isDebugEnabled()) {
                		log.debug("invalid sub group found in service group " + subGroupName);
                	}
                    throw new InvalidServiceGroupException("No Service Group Definition found with name " + subGroupName);
                }
            }
        }
        
        CloudControllerServiceClient ccServiceClient = null;

        try {
            ccServiceClient = CloudControllerServiceClient.getServiceClient();
            
            if (log.isDebugEnabled()) {
            	log.debug("deplying to cloud controller service group " + serviceGroupDefinition.getName());
            }
            
            ccServiceClient.deployServiceGroup(serviceGroup);

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        }catch (RemoteException e) {
            throw new ADCException(e);
        } catch (CloudControllerServiceInvalidServiceGroupExceptionException e) {
        	throw new ADCException(e);
        }
    }

    public ServiceGroupDefinition getServiceGroupDefinition (String serviceGroupDefinitionName) throws ADCException, ServiceGroupDefinitioException {

    	if (log.isDebugEnabled()) {
        	log.debug("getting service group from cloud controller " + serviceGroupDefinitionName);
        }
    	
    	CloudControllerServiceClient ccServiceClient = null;

        try {
            ccServiceClient = CloudControllerServiceClient.getServiceClient();
            
            if (log.isDebugEnabled()) {
            	log.debug("deploying to cloud controller service group " + serviceGroupDefinitionName);
            }
            
            ServiceGroup serviceGroup = ccServiceClient.getServiceGroup(serviceGroupDefinitionName);
            ServiceGroupDefinition serviceGroupDef = populateServiceGroupDefinitionPojo(serviceGroup);
            
            return serviceGroupDef;

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        } catch (RemoteException e) {
        	throw new ADCException(e);
		} catch (CloudControllerServiceInvalidServiceGroupExceptionException e) {
			throw new ADCException(e);
		}
    	
    }


    public void undeployServiceGroupDefinition (String name) throws ADCException, ServiceGroupDefinitioException {

    	//throw new ServiceGroupDefinitioException("method not supported");
    	
    	CloudControllerServiceClient ccServiceClient = null;
    	
    	try {
            ccServiceClient = CloudControllerServiceClient.getServiceClient();
            
            if (log.isDebugEnabled()) {
            	log.debug("undeploying service group from cloud controller " + name);
            }
            
            ccServiceClient.undeployServiceGroup(name);

        } catch (AxisFault axisFault) {
            throw new ADCException(axisFault);
        } catch (RemoteException e) {
        	throw new ADCException(e);
		} catch (CloudControllerServiceInvalidServiceGroupExceptionException e) {
			throw new ADCException(e);
		}
    }
    
    
    private ServiceGroup populateServiceGroupPojo (ServiceGroupDefinition serviceGroupDefinition ) {
    	ServiceGroup servicegroup = new ServiceGroup();
    	
    	// implement conversion (mostly List -> Array)
    	servicegroup.setName(serviceGroupDefinition.getName());
    	List<String> subGroupsDef = serviceGroupDefinition.getSubGroups();
    	List<String> cartridgesDef = serviceGroupDefinition.getCartridges();
    	List<String> dynamicPropertiesDef = serviceGroupDefinition.getDynamicProperties();
    	List<StaticProperty> staticPropertiesDef = serviceGroupDefinition.getStaticProperties();
    	
    	if (subGroupsDef == null) {
    		subGroupsDef = new ArrayList<String>(0);
    	}
    	
    	if (cartridgesDef == null) {
    		cartridgesDef = new ArrayList<String>(0);
    	}

    	String [] subGroups = new String[subGroupsDef.size()];
    	String [] cartridges = new String[cartridgesDef.size()];
    	String [] dynamicProperties = new String[dynamicPropertiesDef.size()];
    	
    	subGroups = subGroupsDef.toArray(subGroups);
    	cartridges = cartridgesDef.toArray(cartridges);
    	dynamicProperties = dynamicPropertiesDef.toArray(dynamicProperties);
    	
    	servicegroup.setSubGroups(subGroups);
    	servicegroup.setCartridges(cartridges);
    	servicegroup.setDynamicProperties(dynamicProperties);
    	
    	DependencyDefinitions depDefs = serviceGroupDefinition.getDependencies();
        if (depDefs != null) {
            List<StartupOrderDefinition> startDefs = depDefs.getStartupOrder();

            Dependencies deps = new Dependencies();
            if (startDefs != null) {
                StartupOrder [] startups = new StartupOrder [startDefs.size()];
                for (int i = 0; i < startDefs.size(); i++) {
                    StartupOrderDefinition stDef = startDefs.get(i);
                    StartupOrder st = new StartupOrder();
                    st.setStart(stDef.getStart());
                    st.setAfter(stDef.getAfter());
                    startups[i] = st;
                }
                deps.setStartupOrder(startups);
            }
            deps.setKillBehaviour(depDefs.getKillBehaviour());
            servicegroup.setDependencies(deps);
        }
        
        List<StaticProperty> propDefs = serviceGroupDefinition.getStaticProperties();
        org.apache.stratos.cloud.controller.stub.pojo.Property[] props = 
        		                   new  org.apache.stratos.cloud.controller.stub.pojo.Property [propDefs.size()];
        
        int i = 0;
        for (StaticProperty propDef : propDefs) {
        	org.apache.stratos.cloud.controller.stub.pojo.Property prop = 
        			new org.apache.stratos.cloud.controller.stub.pojo.Property();
        	prop.setName(propDef.getName());
        	prop.setValue(propDef.getValue());
        	props[i] = prop;
        }
        servicegroup.setStaticProperties(props);
    	
    	return servicegroup;
    }
    
    private ServiceGroupDefinition populateServiceGroupDefinitionPojo (ServiceGroup serviceGroup ) {
    	ServiceGroupDefinition servicegroupDef = new ServiceGroupDefinition();
    	
    	String [] cartridges = serviceGroup.getCartridges();
    	String [] subGroups = serviceGroup.getSubGroups();
    	Dependencies deps = serviceGroup.getDependencies();
    	org.apache.stratos.cloud.controller.stub.pojo.Property [] props = serviceGroup.getStaticProperties();

        if (deps != null) {
            DependencyDefinitions depsDef = new DependencyDefinitions();
            StartupOrder [] startupOrders = deps.getStartupOrder();
            if (startupOrders != null && startupOrders.length > 0) {
                List<StartupOrderDefinition> startupsDef = new ArrayList<StartupOrderDefinition>();
                for (StartupOrder startupOrder :  startupOrders) {
                    if (startupOrder != null) {
                        StartupOrderDefinition astartupDef = new StartupOrderDefinition();
                        astartupDef.setAfter(startupOrder.getAfter());
                        astartupDef.setStart(startupOrder.getStart());
                        startupsDef.add(astartupDef);
                    }
                }

                depsDef.setStartupOrder(startupsDef);
            }

            depsDef.setKillBehaviour(deps.getKillBehaviour());
            servicegroupDef.setDependencies(depsDef);
        }
        
        if (props != null) {
        	List<StaticProperty> propDefs = new ArrayList<StaticProperty>();
        	for (org.apache.stratos.cloud.controller.stub.pojo.Property prop : props) {
        		StaticProperty propDef = new StaticProperty();
        		propDef.setName(prop.getName());
        		propDef.setValue(prop.getValue());
        	}
        	servicegroupDef.setStaticProperties(propDefs);
        }

        List<String> cartridgesDef = new ArrayList<String>(Arrays.asList(cartridges));
        List<String> subGroupsDef = new ArrayList<String>(Arrays.asList(subGroups));

    	servicegroupDef.setCartridges(cartridgesDef);
    	servicegroupDef.setSubGroups(subGroupsDef);
   
    	return servicegroupDef;
    }
    
    
    /**
     * returns any duplicates in a List
     * @param checkedList
     * @return
     */
    private Set<String> findDuplicates(List<String> checkedList)
    { 
      final Set<String> retVals = new HashSet<String>(); 
      final Set<String> set1 = new HashSet<String>();

      for (String val : checkedList) {
    	  
    	  if (!set1.add(val)) {
	        retVals.add(val);
    	  }
      }
      return retVals;
    }
    
 }
