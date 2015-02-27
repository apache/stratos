/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ClusterChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupChildContext;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.applications.pojo.CartridgeContext;
import org.apache.stratos.autoscaler.applications.pojo.ComponentContext;
import org.apache.stratos.autoscaler.applications.pojo.GroupContext;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.InvalidApplicationPolicyException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorFactory;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicyNetworkPartitionReference;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.domain.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.domain.NetworkPartitionRef;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.client.CloudControllerServiceClient;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;


/**
 * This class contains utility methods used by Autoscaler.
 */
public class AutoscalerUtil {

	private static final Log log = LogFactory.getLog(AutoscalerUtil.class);

    private AutoscalerUtil() {
    }

    public static AutoscalerUtil getInstance() {
        return Holder.INSTANCE;
    }

	private static class Holder {
        private static final AutoscalerUtil INSTANCE = new AutoscalerUtil();
    }

    public static Applications  getApplications () {

        Applications applications;
        String [] appResourcePaths = RegistryManager.getInstance().getApplicationResourcePaths();
        if (appResourcePaths != null) {
            applications = new Applications();
            for (String appResourcePath : appResourcePaths) {
                applications.addApplication(getApplicationFromPath(appResourcePath));
            }

            return applications;
        }

        return null;
    }

    public static Application getApplication (String appId) {
        return getApplicationFromPath(AutoscalerConstants.AUTOSCALER_RESOURCE +
                AutoscalerConstants.APPLICATIONS_RESOURCE + "/" + appId);
    }

    public static void persistApplication (Application application) {
        RegistryManager.getInstance().persistApplication(application);
    }

    private static Application getApplicationFromPath (String appResourcePath) {
        return RegistryManager.getInstance().getApplicationByResourcePath(appResourcePath);
    }

    public static void removeApplication (String applicationId) {
        RegistryManager.getInstance().removeApplication(applicationId);
    }

    public static String getAliasFromClusterId(String clusterId) {
        return clusterId.substring(0, clusterId.indexOf("."));
    }

    public static boolean allClustersInitialized(Application application) {
        boolean allClustersInitialized = false;
        for (ClusterDataHolder holder : application.getClusterDataRecursively()) {
            TopologyManager.acquireReadLockForCluster(holder.getServiceType(),
                    holder.getClusterId());

            try {
                Topology topology = TopologyManager.getTopology();
                if (topology != null) {
                    Service service = topology.getService(holder.getServiceType());
                    if (service != null) {
                        //If one cluster of the application presents,
                        // then we can assume that all there clusters are there
                        // as we receive ApplicationClustersCreatedEvent with all the clusters.
                        if (service.clusterExists(holder.getClusterId())) {
                            allClustersInitialized = true;
                            return allClustersInitialized;
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("[Cluster] " + holder.getClusterId() + " is not found in " +
                                        "the Topology");
                            }
                            allClustersInitialized = false;
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Service is null in the CompleteTopologyEvent");
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Topology is null in the CompleteTopologyEvent");
                    }
                }
            } finally {
                TopologyManager.releaseReadLockForCluster(holder.getServiceType(),
                        holder.getClusterId());
            }
        }
        return allClustersInitialized;
    }

    public static Properties getProperties(final OMElement elt) {

        Iterator<?> it = elt.getChildrenWithName(new QName(AutoscalerConstants.PROPERTY_ELEMENT));
        ArrayList<Property> propertyList = new ArrayList<Property>();

        while (it.hasNext()) {
            OMElement prop = (OMElement) it.next();

            if (prop.getAttribute(new QName(AutoscalerConstants.PROPERTY_NAME_ATTR)) == null ||
                prop.getAttribute(new QName(AutoscalerConstants.PROPERTY_VALUE_ATTR)) == null) {

                String msg =
                             "Property element's, name and value attributes should be specified. "
                                     + "Property: ";
                log.warn(msg + prop.toString());

            }

            String name =
                          prop.getAttribute(new QName(AutoscalerConstants.PROPERTY_NAME_ATTR))
                              .getAttributeValue();
            String value =
                           prop.getAttribute(new QName(AutoscalerConstants.PROPERTY_VALUE_ATTR))
                               .getAttributeValue();

            Property property = new Property();
            property.setName(name);
            property.setValue(value);
            propertyList.add(property);
        }

        if(propertyList.isEmpty()) {
            return null;
        }

        Property[] propertyArray = propertyList.toArray(new Property[propertyList.size()]);
        Properties properties = new Properties();
        properties.setProperties(propertyArray);
        return properties;
    }

    public static org.apache.stratos.cloud.controller.stub.Properties toStubProperties(
            org.apache.stratos.common.Properties properties) {
        org.apache.stratos.cloud.controller.stub.Properties stubProps = new org.apache.stratos.cloud.controller.stub.Properties();

        if (properties != null && properties.getProperties() != null) {
            for (Property property : properties.getProperties()) {
                if ((property != null) && (property.getValue() != null)) {
                    org.apache.stratos.cloud.controller.stub.Property newProperty = new org.apache.stratos.cloud.controller.stub.Property();
                    newProperty.setName(property.getName());
                    newProperty.setValue(property.getValue());
                    stubProps.addProperties(newProperty);
                }
            }

        }
        return stubProps;
    }

    public static org.apache.stratos.cloud.controller.stub.Properties toStubProperties(
            java.util.Properties properties) {
        org.apache.stratos.cloud.controller.stub.Properties stubProperties = new org.apache.stratos.cloud.controller.stub.Properties();
        if(properties != null) {
            for(Map.Entry<Object, Object> entry : properties.entrySet()) {
                org.apache.stratos.cloud.controller.stub.Property newProperty = new org.apache.stratos.cloud.controller.stub.Property();
                newProperty.setName(entry.getKey().toString());
                newProperty.setValue(entry.getValue().toString());
                stubProperties.addProperties(newProperty);
            }
        }
        return stubProperties;
    }

    public static org.apache.stratos.common.Properties toCommonProperties(
            org.apache.stratos.cloud.controller.stub.Properties properties) {
        org.apache.stratos.common.Properties commonProps = new org.apache.stratos.common.Properties();

        if (properties != null && properties.getProperties() != null) {

            for (org.apache.stratos.cloud.controller.stub.Property property : properties.getProperties()) {
                if ((property != null) && (property.getValue() != null)) {
                    Property newProperty = new Property();
                    newProperty.setName(property.getName());
                    newProperty.setValue(property.getValue());
                    commonProps.addProperty(newProperty);
                }
            }

        }

        return commonProps;
    }

    public static org.apache.stratos.common.Properties toCommonProperties(
            org.apache.stratos.cloud.controller.stub.Property[] propertyArray) {

        org.apache.stratos.cloud.controller.stub.Properties properties = new org.apache.stratos.cloud.controller.stub.Properties();
        properties.setProperties(propertyArray);
        return toCommonProperties(properties);
    }

    public synchronized void startApplicationMonitor(String applicationId) {

	    AutoscalerContext autoscalerContext = AutoscalerContext.getInstance();
	    if (autoscalerContext.getAppMonitor(applicationId) == null) {
		    autoscalerContext.addApplicationPendingMonitor(applicationId);
		    ServiceReferenceHolder.getInstance().getExecutorService().submit(new ApplicationMonitorAdder(applicationId));

            log.info(String.format("Monitor scheduled: [application] %s ", applicationId));
	    } else {
		    if (log.isDebugEnabled()) {
			    log.debug(String.format("Application monitor thread already exists: " +
                        "[application] %s ", applicationId));
		    }
	    }
    }

    private class ApplicationMonitorAdder implements Runnable {
        private String applicationId;

        public ApplicationMonitorAdder(String applicationId) {
            this.applicationId = applicationId;
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            long endTime = startTime;
            int retries = 5;
            boolean success = false;
            ApplicationMonitor applicationMonitor = null;
            while (!success && retries != 0) {

                try {
                    startTime = System.currentTimeMillis();
                    log.info("Starting monitor: [application] " + applicationId);
                    try {
                        applicationMonitor = MonitorFactory.getApplicationMonitor(applicationId);
                    } catch (PolicyValidationException e) {
                        String msg = "Monitor creation failed: [application] " + applicationId;
                        log.warn(msg, e);
                        retries--;
                    }
                    success = true;
                    endTime = System.currentTimeMillis();
                } catch (DependencyBuilderException e) {
                    String msg = "Monitor creation failed: [application] " + applicationId;
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Monitor creation failed: [application] " + applicationId;
                    log.warn(msg, e);
                    retries--;
                }
            }

            if (applicationMonitor == null) {
                String msg = "Monitor creation failed, even after retrying for 5 times: "
                        + "[application] " + applicationId;
                log.error(msg);
                throw new RuntimeException(msg);
            }
            AutoscalerContext autoscalerContext = AutoscalerContext.getInstance();
            autoscalerContext.removeAppMonitor(applicationId);
            autoscalerContext.addAppMonitor(applicationMonitor);

            long startupTime = ((endTime - startTime) / 1000);
            if (log.isInfoEnabled()) {
                log.info(String.format("Monitor started successfully: [application] %s [dependents] %s " +
                                "[startup-time] %d seconds", applicationMonitor.getId(),
                        applicationMonitor.getStartupDependencyTree(), startupTime));
            }
        }
    }

    public static Monitor.MonitorType findMonitorType(ApplicationChildContext context) {
        if(context instanceof GroupChildContext) {
            return Monitor.MonitorType.Group;
        } else if(context instanceof ClusterChildContext) {
            return Monitor.MonitorType.Cluster;
        } else {
            throw new RuntimeException("Unknown child context type: " + context.getClass().getName());
        }
    }

    public static String findTenantRange(int tenantId, String tenantPartitions) {
        if(StringUtils.isNotBlank(tenantPartitions)) {
            String[] tenantRanges = tenantPartitions.trim().split(",");
            if(tenantRanges != null) {
                for(String tenantRange : tenantRanges) {
                    if((tenantRange != null) && (tenantRange.contains("-"))) {
                        String[] tenantValues = tenantRange.trim().split("-");
                        if((tenantValues != null) && (tenantValues.length == 2)) {
                            if((!tenantValues[0].equals("*")) && (!tenantValues[1].equals("*"))) {
                                int startValue = Integer.parseInt(tenantValues[0]);
                                int endValue = Integer.parseInt(tenantValues[1]);
                                if ((tenantId >= startValue) && (tenantId <= endValue)) {
                                    return tenantRange;
                                }
                            } else if((!tenantValues[0].equals("*")) && (tenantValues[1].equals("*"))) {
                                int startValue = Integer.parseInt(tenantValues[0]);
                                if(tenantId >= startValue) {
                                    return tenantRange;
                                }
                            }
                        }
                    }
                }
            }
        }
        return "*";
    }
    
    /**
     * Get network partition ids referred in an application. Network partition ids are not referred directly.
     * Cartridge or cartridge group can refer deployment policy which has network partition references.
     * @param applicationId the application id
     * @return list of network partition ids
     */
    public static List<String> getNetworkPartitionIdsReferedInApplication(String applicationId) {
    	
    	List<String> deploymentPolicyIdsReferedInApplication = getDeploymentPolicyIdsReferedInApplication(applicationId);
    	if (deploymentPolicyIdsReferedInApplication == null) {
			return null;
		}
    	
    	List<String> networkPartitionIds = new ArrayList<String>();
    	for (String deploymentPolicyId : deploymentPolicyIdsReferedInApplication) {
			try {
				DeploymentPolicy deploymentPolicy = CloudControllerServiceClient.getInstance().getDeploymentPolicy(deploymentPolicyId);
				if (deploymentPolicy != null) {
						for (NetworkPartitionRef networkPartitionRef : deploymentPolicy.getNetworkPartitionsRef()) {
							if (networkPartitionRef !=  null) {
								if (!networkPartitionIds.contains(networkPartitionRef.getId())) {
									networkPartitionIds.add(networkPartitionRef.getId());
								}
							}
						}
					}
				}
				catch (Exception e) {
					String msg = String.format("Error while getting deployment policy from cloud controller [deployment-policy-id] %s ", deploymentPolicyId);
					log.error(msg, e);
					throw new AutoScalerException(msg, e);
				} 
		}
    	return networkPartitionIds;
	}    
    
    /**
     * Get deployment policy ids referred in an application.
     * @param applicationId the application id
     * @return list of deployment policy ids
     */
    private static List<String> getDeploymentPolicyIdsReferedInApplication(String applicationId) {
    	
    	Map<String, String> aliasToDeploymentPolicyIdMap = getAliasToDeploymentPolicyIdMapOfApplication(applicationId);
    	if (aliasToDeploymentPolicyIdMap == null) {
			return null;
		}
    	
    	List<String> deploymentPolicyIds = new ArrayList<String>();

		for (Map.Entry<String, String> entry : aliasToDeploymentPolicyIdMap.entrySet()) {
			System.out.println(entry.getKey() + "/" + entry.getValue());
			if (!deploymentPolicyIds.contains(entry.getValue())) {
				deploymentPolicyIds.add(entry.getValue());
			}
		}

		return deploymentPolicyIds;
    }
    
    /**
     * Get deployment policy id of an alias in the given application
     * @param applicationId the application id
     * @param alias the cartridge or cartridge-group alias
     * @return the deployment policy id if found, null otherwise
     */
    public static String getDeploymentPolicyIdByAlias(String applicationId, String alias) {
    	
    	if (alias == null || alias.isEmpty()) {
			return null;
		}
    	
    	Map<String, String> aliasToDeploymentPolicyIdMap = getAliasToDeploymentPolicyIdMapOfApplication(applicationId);
    	
    	if (aliasToDeploymentPolicyIdMap == null) {
			return null;
		}
    	
    	return aliasToDeploymentPolicyIdMap.get(alias);
    }
    
    /**
     * Get alias to deployment policy id map in the given application.
     * @param applicationId the application id
     * @return alias to deployment policy map
     */
    private static Map<String, String> getAliasToDeploymentPolicyIdMapOfApplication(String applicationId) {
    	
    	Map<String, String> aliasToDeploymentPolicyIdMap = new HashMap<String, String>();
    	
    	ApplicationContext applicationContext = RegistryManager.getInstance().getApplicationContext(applicationId);
    	if (applicationContext == null) {
			return null;
		}
    	
    	ComponentContext componentContext = applicationContext.getComponents();
    	if (componentContext == null) {
			return null;
		}
    	
    	CartridgeContext[] cartridgeContexts = componentContext.getCartridgeContexts();
    	if (cartridgeContexts != null && cartridgeContexts.length != 0) {
    		getAliasToDeployloymentPolicyIdMapFromChildCartridgeContexts(aliasToDeploymentPolicyIdMap, cartridgeContexts);
		}
    	
    	GroupContext[] groupContexts = componentContext.getGroupContexts();
    	if (groupContexts != null && groupContexts.length != 0) {
    		getAliasToDeployloymentPolicyIdMapFromChildGroupContexts(aliasToDeploymentPolicyIdMap, groupContexts);
		}
    	
    	return aliasToDeploymentPolicyIdMap;
    }
    
    private static void getAliasToDeployloymentPolicyIdMapFromChildCartridgeContexts(
    		Map<String, String> aliasToDeploymentPolicyIdMap, CartridgeContext[] cartridgeContexts) {
    	
    	if (cartridgeContexts != null && cartridgeContexts.length != 0) {
			for (CartridgeContext cartridgeContext : cartridgeContexts) {
				if (cartridgeContext != null) {
					aliasToDeploymentPolicyIdMap.put(
							cartridgeContext.getSubscribableInfoContext().getAlias(), 
							cartridgeContext.getSubscribableInfoContext().getDeploymentPolicy());
				}
			}
		}
    }
    
    private static void getAliasToDeployloymentPolicyIdMapFromChildGroupContexts(
    		Map<String, String> aliasToDeploymentPolicyIdMap, GroupContext[] groupContexts) {
    	
    	if (groupContexts != null && groupContexts.length != 0) {
			for (GroupContext groupContext : groupContexts) {
				if (groupContext != null) {
					if (groupContext.getDeploymentPolicy() == null || groupContext.getDeploymentPolicy().isEmpty()) {
						// if group does not have a deployment policy, children should have
						getAliasToDeployloymentPolicyIdMapFromChildCartridgeContexts(aliasToDeploymentPolicyIdMap, groupContext.getCartridgeContexts());
						getAliasToDeployloymentPolicyIdMapFromChildGroupContexts(aliasToDeploymentPolicyIdMap, groupContext.getGroupContexts());
					} else {
						// if group have a deployment policy, it is the same for all the children
						String deploymentPolicyId = groupContext.getDeploymentPolicy();
						aliasToDeploymentPolicyIdMap.put(groupContext.getAlias(), deploymentPolicyId);
						if (groupContext.getCartridgeContexts() != null && groupContext.getCartridgeContexts().length != 0) {
							setDeploymentPolicyIdToChildCartridgeContexts(aliasToDeploymentPolicyIdMap, deploymentPolicyId, groupContext.getCartridgeContexts());
						}
						if (groupContext.getGroupContexts() != null && groupContext.getGroupContexts().length != 0) {
							setDeploymentPolicyIdToChildGroupContexts(aliasToDeploymentPolicyIdMap, deploymentPolicyId, groupContext.getGroupContexts());
						}
						
					}
				}
			}
		}
    }
    
    private static void setDeploymentPolicyIdToChildCartridgeContexts(
    		Map<String, String> aliasToDeploymentPolicyIdMap, String deploymentPolicyId, CartridgeContext[] cartridgeContexts) {
    	
    	if (cartridgeContexts != null && cartridgeContexts.length != 0) {
    		for (CartridgeContext cartridgeContext : cartridgeContexts) {
				if (cartridgeContext != null) {
					aliasToDeploymentPolicyIdMap.put(cartridgeContext.getSubscribableInfoContext().getAlias(), deploymentPolicyId);
				}
			}
		}
    }

    private static void setDeploymentPolicyIdToChildGroupContexts(
    		Map<String, String> aliasToDeploymentPolicyIdMap, String deploymentPolicyId, GroupContext[] groupContexts) {
    	
    	if (groupContexts != null && groupContexts.length != 0) {
    		for (GroupContext groupContext : groupContexts) {
				if (groupContext != null) {
					if (groupContext.getCartridgeContexts() != null && groupContext.getCartridgeContexts().length != 0) {
						setDeploymentPolicyIdToChildCartridgeContexts(aliasToDeploymentPolicyIdMap, deploymentPolicyId, groupContext.getCartridgeContexts());
					}
					if (groupContext.getGroupContexts() != null && groupContext.getGroupContexts().length != 0) {
						setDeploymentPolicyIdToChildGroupContexts(aliasToDeploymentPolicyIdMap, deploymentPolicyId, groupContext.getGroupContexts());
					}
				}
			}
		}
    }
    
    /**
     * Validates Application Policy against the given application.
     * @param applicationId the application id against which the application policy needs to be validated
     * @param applicationPolicy the application policy to be validated
     * @throws InvalidApplicationPolicyException if application policy is not valid
     * @throws RemoteException is anything went wrong while communicating with CC to validate network partitions
     */
	public static void validateApplicationPolicy(String applicationId, ApplicationPolicy applicationPolicy) 
    		throws InvalidApplicationPolicyException, RemoteException {
    	
    	// application policy can't be null
    	if (null == applicationPolicy) {
			String msg = "Invalid Application Policy. Cause -> Application Policy is null";
			log.error(msg);
			throw new InvalidApplicationPolicyException(msg);
		}
    	
    	// application policy should contain at least one network partition reference
    	ApplicationPolicyNetworkPartitionReference[] networkPartitionReferences = 
    			applicationPolicy.getNetworkPartitionReferences();
		if (null == networkPartitionReferences || networkPartitionReferences.length == 0) {
			String msg = "Invalid Application Policy. "
					+ "Cause -> Application Policy is not containing any network partition reference";
			log.error(msg);
			throw new InvalidApplicationPolicyException(msg);
		}
    	
    	// to count the number of network partitions which are active by default
    	// if the count is 0, we should raise the error
    	int activeByDefaultNetworkPartitionsCount = 0;
    	
    	// validating all network partition references
    	for (ApplicationPolicyNetworkPartitionReference applicationPolicyNetworkPartitionReference : networkPartitionReferences) {
			
    		// network-partition-id can't be null or empty
    		String networkPartitionId = applicationPolicyNetworkPartitionReference.getNetworkPartitionId();
			if (null == networkPartitionId || networkPartitionId.isEmpty()) {
				String msg = String.format("Invalid Application Policy. "
						+ "Cause -> Invalid network-partition-id : %s", networkPartitionId);
				log.error(msg);
				throw new InvalidApplicationPolicyException(msg);
			}
			
			// network partitions should be added already
			if (null == CloudControllerServiceClient.getInstance().getNetworkPartition(networkPartitionId)) {
				String msg = String.format("Invalid Application Policy. "
						+ "Cause -> Network partition not found for network-partition-id : %s", networkPartitionId);
				log.error(msg);
				throw new InvalidApplicationPolicyException(msg);
			}
			
			//TODO validate application policy against the given application
			
			// counting number of network partitions which are active by default
			if (true == applicationPolicyNetworkPartitionReference.isActiveByDefault()) {
				activeByDefaultNetworkPartitionsCount++;
			}
		}
    	
    	// there should be at least one network partition reference which is active by default
    	if (activeByDefaultNetworkPartitionsCount == 0) {
			String msg = "Invalid Application Policy. Cause -> No active by default network partitions found";
			log.error(msg);
			throw new InvalidApplicationPolicyException(msg);
		}
    }
}
