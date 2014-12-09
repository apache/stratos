/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
package org.apache.stratos.autoscaler.monitor.component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.application.ApplicationInstanceContext;
import org.apache.stratos.autoscaler.context.partition.network.ApplicationLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.ApplicationStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ApplicationLevelNetworkPartition;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.*;

/**
 * ApplicationMonitor is to control the child monitors
 */
public class ApplicationMonitor extends ParentComponentMonitor {
    private static final Log log = LogFactory.getLog(ApplicationMonitor.class);

    //network partition contexts
    private Map<String, ApplicationLevelNetworkPartitionContext> networkPartitionCtxts;
    //Flag to set whether application is terminating
    private boolean isTerminating;


    public ApplicationMonitor(Application application) throws DependencyBuilderException,
            TopologyInConsistentException {
        super(application);
        //setting the appId for the application
        this.appId = application.getUniqueIdentifier();
        networkPartitionCtxts = new HashMap<String, ApplicationLevelNetworkPartitionContext>();
    }

    /**
     * Find the group monitor by traversing recursively in the hierarchical monitors.
     *
     * @param groupId the unique alias of the Group
     * @return the found GroupMonitor
     */
    public Monitor findGroupMonitorWithId(String groupId) {
        //searching within active monitors
        return findGroupMonitor(groupId, aliasToActiveMonitorsMap.values());
    }


    /**
     * Utility method to find the group monitor recursively within app monitor
     *
     * @param id       the unique alias of the Group
     * @param monitors the group monitors found in the app monitor
     * @return the found GroupMonitor
     */
    private Monitor findGroupMonitor(String id, Collection<Monitor> monitors) {
        for (Monitor monitor : monitors) {
            // check if alias is equal, if so, return
            if (monitor.getId().equals(id)) {
                return monitor;
            } else {
                // check if this Group has nested sub Groups. If so, traverse them as well
                if (monitor instanceof ParentComponentMonitor) {
                    return findGroupMonitor(id, ((ParentComponentMonitor) monitor).
                            getAliasToActiveMonitorsMap().values());
                }
            }
        }
        return null;
    }

    /**
     * To set the status of the application monitor
     *
     * @param status the status
     */
    public void setStatus(ApplicationStatus status, String instanceId) {
        ApplicationInstance applicationInstance = (ApplicationInstance) this.instanceIdToInstanceMap.
                get(instanceId);

        if(applicationInstance == null) {
            log.warn("The required application [instance] " + instanceId + " not found in the AppMonitor");
        } else {
            if(applicationInstance.getStatus() != status) {
                applicationInstance.setStatus(status);
            }
        }

        //notify the children about the state change
        try {
            MonitorStatusEventBuilder.notifyChildren(this, new ApplicationStatusEvent(status, appId, instanceId));
        } catch (MonitorNotFoundException e) {
            log.error("Error while notifying the children from [application] " + appId, e);
            //TODO revert siblings
        }
    }

    @Override
    public void onChildStatusEvent(MonitorStatusEvent statusEvent) {
        String id = statusEvent.getId();
        String instanceId = statusEvent.getInstanceId();
        LifeCycleState status1 = statusEvent.getStatus();
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination
        if (status1 == ClusterStatus.Active || status1 == GroupStatus.Active) {
            onChildActivatedEvent(id, instanceId);

        } else if (status1 == ClusterStatus.Inactive || status1 == GroupStatus.Inactive) {
            this.markMonitorAsInactive(id);
            onChildInactiveEvent(id, instanceId);

        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inActive in the map
            this.markMonitorAsTerminating(id);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Check whether all dependent goes Terminated and then start them in parallel.
            if (this.terminatingMonitorsList.contains(id)) {
                this.terminatingMonitorsList.remove(id);
                this.aliasToActiveMonitorsMap.remove(id);
            } else {
                log.warn("[monitor] " + id + " cannot be found in the inActive monitors list");
            }
            ApplicationInstance instance = (ApplicationInstance) instanceIdToInstanceMap.get(instanceId);
            if (instance != null) {
                if (instance.getStatus() == ApplicationStatus.Terminating) {
                    ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().process(this.id,
                            appId, instanceId);
                } else {
                    onChildTerminatedEvent(id, instanceId);
                }
            } else {
                log.warn("The required instance cannot be found in the the [GroupMonitor] " +
                        this.id);
            }
        }
    }

    @Override
    public void onParentStatusEvent(MonitorStatusEvent statusEvent) {
        // nothing to do
    }

    @Override
    public void onChildScalingEvent(MonitorScalingEvent scalingEvent) {

    }

    @Override
    public void onParentScalingEvent(MonitorScalingEvent scalingEvent) {

    }

    @Override
    public void onEvent(MonitorScalingEvent scalingEvent) {

    }

    public boolean startMinimumDependencies(Application application)
            throws TopologyInConsistentException, PolicyValidationException {

        return createInstanceAndStartDependency(application);
    }

    private boolean createInstanceAndStartDependency(Application application)
            throws TopologyInConsistentException, PolicyValidationException {
        boolean initialStartup = true;
        List<String> instanceIds = new ArrayList<String>();
        DeploymentPolicy deploymentPolicy = getDeploymentPolicy(application);
        String instanceId;

        if (deploymentPolicy == null) {
            //FIXME for docker with deployment policy
            ApplicationInstance appInstance = createApplicationInstance(application, null);
            instanceIds.add(appInstance.getInstanceId());
        } else {
            for (ApplicationLevelNetworkPartition networkPartition :
                    deploymentPolicy.getApplicationLevelNetworkPartitions()) {
                if (networkPartition.isActiveByDefault()) {
                    ApplicationLevelNetworkPartitionContext context =
                            new ApplicationLevelNetworkPartitionContext(networkPartition.getId());
                    //If application instances found in the ApplicationsTopology,
                    // then have to add them first before creating new one
                    ApplicationInstance appInstance = (ApplicationInstance) application.
                            getInstanceByNetworkPartitionId(context.getId());
                    if (appInstance != null) {
                        //use the existing instance in the Topology to create the data
                        instanceId = handleApplicationInstanceCreation(application, context, appInstance);
                        initialStartup = false;
                    } else {
                        //create new app instance as it doesn't exist in the Topology
                        instanceId = handleApplicationInstanceCreation(application, context, null);

                    }
                    instanceIds.add(instanceId);
                    log.info("Application instance has been added for the [network partition] " +
                            networkPartition.getId() + " [appInstanceId] " + instanceId);

                }
            }
        }
        startDependency(application, instanceIds);
        return initialStartup;
    }

    private String handleApplicationInstanceCreation(Application application,
                                                     ApplicationLevelNetworkPartitionContext context,
                                                     ApplicationInstance instanceExist) {
        ApplicationInstance instance;
        ApplicationInstanceContext instanceContext;
        if (instanceExist != null) {
            //using the existing instance
            instance = instanceExist;
        } else {
            //creating a new applicationInstance
            instance = createApplicationInstance(application, context.getId());

        }
        String instanceId = instance.getInstanceId();

        //Creating appInstanceContext
        instanceContext = new ApplicationInstanceContext(instanceId);
        //adding the created App InstanceContext to ApplicationLevelNetworkPartitionContext
        context.addInstanceContext(instanceContext);

        //adding to instance map
        this.instanceIdToInstanceMap.put(instanceId, instance);
        //adding ApplicationLevelNetworkPartitionContext to networkPartitionContexts map
        this.networkPartitionCtxts.put(context.getId(), context);

        return instanceId;
    }

    public void createInstanceOnBurstingForApplication() throws TopologyInConsistentException,
            PolicyValidationException,
            MonitorNotFoundException {
        Application application = ApplicationHolder.getApplications().getApplication(appId);
        if (application == null) {
            String msg = "Application cannot be found in the Topology.";
            throw new TopologyInConsistentException(msg);
        }
        boolean burstNPFound = false;
        DeploymentPolicy deploymentPolicy = getDeploymentPolicy(application);
        String instanceId = null;
        //Find out the inActive network partition
        if (deploymentPolicy == null) {
            //FIXME for docker with deployment policy
            ApplicationInstance appInstance = createApplicationInstance(application, null);
            instanceId = appInstance.getInstanceId();

        } else {
            for (ApplicationLevelNetworkPartition networkPartition : deploymentPolicy.
                    getApplicationLevelNetworkPartitions()) {
                //Checking whether any not active NP found
                if (!networkPartition.isActiveByDefault()) {

                    if (!this.networkPartitionCtxts.containsKey(networkPartition.getId())) {

                        ApplicationLevelNetworkPartitionContext context =
                                new ApplicationLevelNetworkPartitionContext(networkPartition.getId());

                        //Setting flags saying that it has been created by burst
                        context.setCreatedOnBurst(true);
                        ApplicationInstance appInstance = (ApplicationInstance) application.
                                getInstanceByNetworkPartitionId(context.getId());

                        if (appInstance == null) {
                            instanceId = handleApplicationInstanceCreation(application, context, null);
                        } else {
                            log.warn("The Network partition is already associated with an " +
                                    "[ApplicationInstance] " + appInstance.getInstanceId() +
                                    "in the ApplicationsTopology. Hence not creating new AppInstance.");
                            instanceId = handleApplicationInstanceCreation(application, context, appInstance);
                        }
                        burstNPFound = true;
                    }
                }
            }
        }
        if (!burstNPFound) {
            log.warn("[Application] " + appId + " cannot be burst as no available resources found");
        } else {
            startDependency(application, instanceId);
        }
    }

    private DeploymentPolicy getDeploymentPolicy(Application application) throws PolicyValidationException {
        String deploymentPolicyName = application.getDeploymentPolicy();
        DeploymentPolicy deploymentPolicy = PolicyManager.getInstance().
                getDeploymentPolicyByApplication(application.getUniqueIdentifier());
        if (deploymentPolicyName != null) {
            deploymentPolicy = PolicyManager.getInstance()
                    .getDeploymentPolicy(deploymentPolicyName);
            if (deploymentPolicy == null) {
                String msg = "Deployment policy is null: [policy-name] " + deploymentPolicyName;
                log.error(msg);
                throw new PolicyValidationException(msg);
            }
        }

        return deploymentPolicy;
    }

    private ApplicationInstance createApplicationInstance(Application application, String networkPartitionId) {
        String instanceId = this.generateInstanceId(application);
        ApplicationInstance instance = ApplicationBuilder.handleApplicationInstanceCreatedEvent(
                appId, instanceId, networkPartitionId);
        return instance;
    }

    public Map<String, ApplicationLevelNetworkPartitionContext> getApplicationLevelNetworkPartitionCtxts() {
        return networkPartitionCtxts;
    }

    public void setApplicationLevelNetworkPartitionCtxts(Map<String, ApplicationLevelNetworkPartitionContext> networkPartitionCtxts) {
        this.networkPartitionCtxts = networkPartitionCtxts;
    }

    public void addApplicationLevelNetworkPartitionContext(ApplicationLevelNetworkPartitionContext applicationLevelNetworkPartitionContext) {
        this.networkPartitionCtxts.put(applicationLevelNetworkPartitionContext.getId(), applicationLevelNetworkPartitionContext);
    }

    public boolean isTerminating() {
        return isTerminating;
    }

    public void setTerminating(boolean isTerminating) {
        this.isTerminating = isTerminating;
    }
}
