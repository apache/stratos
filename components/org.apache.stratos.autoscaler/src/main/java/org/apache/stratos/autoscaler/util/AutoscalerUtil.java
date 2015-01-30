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

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.MonitorFactory;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


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
		    autoscalerContext.addPendingMonitor(applicationId);
		    ServiceReferenceHolder.getInstance().getExecutorService().submit(new ApplicationMonitorAdder(applicationId));
	    } else {
		    if (log.isDebugEnabled()) {
			    log.debug(String.format("Application monitor thread already exists: " +
                        "[application] %s ", applicationId));
		    }
	    }
    }

    private class ApplicationMonitorAdder implements Runnable {
        private String appId;

        public ApplicationMonitorAdder(String appId) {
            this.appId = appId;
        }

        public void run() {
            ApplicationMonitor applicationMonitor = null;
            int retries = 5;
            boolean success = false;
            while (!success && retries != 0) {
                /*try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }*/
                try {
                    long start = System.currentTimeMillis();
                    log.info("Application monitor is going to be started for [application] " + appId);
                    try {
                        applicationMonitor = MonitorFactory.getApplicationMonitor(appId);
                    } catch (PolicyValidationException e) {
                        String msg = "Application monitor creation failed for Application: ";
                        log.warn(msg, e);
                        retries--;
                    }
                    long end = System.currentTimeMillis();
                    log.info("Time taken to start app monitor: " + (end - start) / 1000);
                    success = true;
                } catch (DependencyBuilderException e) {
                    String msg = "Application monitor creation failed for Application: ";
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Application monitor creation failed for Application: ";
                    log.warn(msg, e);
                    retries--;
                }
            }

            if (applicationMonitor == null) {
                String msg = "Application monitor creation failed, even after retrying for 5 times, "
                        + "for Application: " + appId;
                log.error(msg);
                throw new RuntimeException(msg);
            }
            AutoscalerContext autoscalerContext = AutoscalerContext.getInstance();

            autoscalerContext.removeAppMonitor(appId);
            autoscalerContext.addAppMonitor(applicationMonitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Application monitor has been added successfully: " +
                        "[application] %s", applicationMonitor.getId()));
            }
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


//    public static LbClusterMonitor getLbClusterMonitor(Cluster cluster) throws PolicyValidationException, PartitionValidationException {
//        if (null == cluster) {
//               return null;
//           }
//
//           String autoscalePolicyName = cluster.getAutoscalePolicyName();
//           String deploymentPolicyName = cluster.getDeploymentPolicyName();
//
//           if (log.isDebugEnabled()) {
//               log.debug("Deployment policy name: " + deploymentPolicyName);
//               log.debug("Autoscaler policy name: " + autoscalePolicyName);
//           }
//
//           AutoscalePolicy policy =
//                                    PolicyManager.getInstance()
//                                                 .getAutoscalePolicy(autoscalePolicyName);
//           DeploymentPolicy deploymentPolicy =
//                                               PolicyManager.getInstance()
//                                                            .getDeploymentPolicy(deploymentPolicyName);
//
//           if (deploymentPolicy == null) {
//               String msg = "Deployment Policy is null. Policy name: " + deploymentPolicyName;
//               log.error(msg);
//               throw new PolicyValidationException(msg);
//           }
//
//           Partition[] allPartitions = deploymentPolicy.getAllPartitions();
//           if (allPartitions == null) {
//               String msg =
//                            "Deployment Policy's Partitions are null. Policy name: " +
//                                    deploymentPolicyName;
//               log.error(msg);
//               throw new PolicyValidationException(msg);
//           }
//
//           try {
//               validateExistenceOfPartions(allPartitions);
//           } catch (InvalidPartitionException e) {
//               String msg = "Deployment Policy is invalid. Policy name: " + deploymentPolicyName;
//               log.error(msg, e);
//               throw new PolicyValidationException(msg, e);
//           }
//
//           CloudControllerClient.getInstance()
//                                .validateDeploymentPolicy(cluster.getServiceName(),
//                                                            allPartitions);
//
//           LbClusterMonitor clusterMonitor =
//                                           new LbClusterMonitor(cluster.getClusterId(),
//                                                              cluster.getServiceName(),
//                                                              deploymentPolicy, policy);
//           fNetworkPartitionroup partitionGroup: deploymentPoliNetworkPartitionnGroups()){
//
//               NetworkPartitionContext networkPartitionContext
//                       = PartitionManager.getInstance().getNetworkPartitionLbHolder(partitionGroup.getNetworkPartitionId());
//               clusterMonitor.addNetworkPartitionCtxt(networkPartitionContext);
//           }
//        return null;
//    }

}
