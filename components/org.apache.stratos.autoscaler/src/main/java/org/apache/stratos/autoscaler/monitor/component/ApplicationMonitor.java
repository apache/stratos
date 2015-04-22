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
import org.apache.stratos.autoscaler.algorithms.NetworkPartitionAlgorithm;
import org.apache.stratos.autoscaler.algorithms.networkpartition.AllAtOnceAlgorithm;
import org.apache.stratos.autoscaler.algorithms.networkpartition.NetworkPartitionAlgorithmContext;
import org.apache.stratos.autoscaler.algorithms.networkpartition.OneAfterAnotherAlgorithm;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.application.ApplicationInstanceContext;
import org.apache.stratos.autoscaler.context.partition.network.ApplicationLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.NetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.*;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.GroupStatus;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * ApplicationMonitor is to control the child monitors
 */
public class ApplicationMonitor extends ParentComponentMonitor {

    private static final Log log = LogFactory.getLog(ApplicationMonitor.class);

    private final ExecutorService executorService;

    //Flag to set whether application is terminating
    private boolean isTerminating;

    // Flag to set if forcefull undeployment is invoked for the application.
    private boolean force;

    public ApplicationMonitor(Application application) throws DependencyBuilderException,
            TopologyInConsistentException {
        super(application);

        int threadPoolSize = Integer.getInteger(AutoscalerConstants.APPLICATION_MONITOR_THREAD_POOL_SIZE, 20);
        this.executorService = StratosThreadPool.getExecutorService(
                AutoscalerConstants.APPLICATION_MONITOR_THREAD_POOL_ID, threadPoolSize);

        //setting the appId for the application
        this.appId = application.getUniqueIdentifier();
    }

    @Override
    public MonitorType getMonitorType() {
        return MonitorType.Application;
    }

    @Override
    public void run() {
        try {
            monitor();
        } catch (Exception e) {
            log.error("Application monitor failed : " + this.toString(), e);
        }
    }

    public synchronized void monitor() {
        final Collection<NetworkPartitionContext> networkPartitionContexts =
                this.getNetworkPartitionCtxts().values();

        Runnable monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (log.isDebugEnabled()) {
                    log.debug("Application monitor is running for [application] " + appId);
                }
                for (NetworkPartitionContext networkPartitionContext : networkPartitionContexts) {

                    for (InstanceContext instanceContext : networkPartitionContext.
                            getInstanceIdToInstanceContextMap().values()) {
                        ApplicationInstance instance = (ApplicationInstance) instanceIdToInstanceMap.
                                get(instanceContext.getId());
                        //stopping the monitoring when the group is inactive/Terminating/Terminated
                        if (instance.getStatus().getCode() <= GroupStatus.Active.getCode()) {
                            //Gives priority to scaling max out rather than dependency scaling
                            if (!instanceContext.getIdToScalingOverMaxEvent().isEmpty()) {
                                //handling the scaling max out of the children
                                handleScalingMaxOut(instanceContext, networkPartitionContext);

                            } else if (!instanceContext.getIdToScalingEvent().isEmpty()) {
                                //handling the dependent scaling for application
                                handleDependentScaling(instanceContext, networkPartitionContext);

                            } else if (!instanceContext.getIdToScalingDownBeyondMinEvent().isEmpty()) {
                                //handling the scale down of the application
                                handleScalingDownBeyondMin(instanceContext, networkPartitionContext);
                            }
                        }
                    }
                }
            }
        };
        executorService.execute(monitoringRunnable);
    }

    private void handleScalingMaxOut(InstanceContext instanceContext,
                                     NetworkPartitionContext networkPartitionContext) {
        if (networkPartitionContext.getPendingInstancesCount() == 0) {
            //handling the application bursting only when there are no pending instances found
            try {
                if (log.isInfoEnabled()) {
                    log.info("Handling application busting, " +
                            "since resources are exhausted in " +
                            "this application instance ");
                }
                handleApplicationBursting();
            } catch (TopologyInConsistentException e) {
                log.error("Error while bursting the application", e);
            } catch (PolicyValidationException e) {
                log.error("Error while bursting the application", e);
            } catch (MonitorNotFoundException e) {
                log.error("Error while bursting the application", e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Pending Application instance found. " +
                        "Hence waiting for it to become active");
            }
        }
        //Resetting the values
        instanceContext.setIdToScalingOverMaxEvent(
                new ConcurrentHashMap<String, ScalingUpBeyondMaxEvent>());

    }

    private void handleScalingDownBeyondMin(InstanceContext instanceContext,
                                            NetworkPartitionContext nwPartitionContext) {
        //Traverse through all the children to see whether all have sent the scale down
        boolean allChildrenScaleDown = false;
        for (Monitor monitor : this.aliasToActiveMonitorsMap.values()) {
            if (instanceContext.getScalingDownBeyondMinEvent(monitor.getId()) == null) {
                allChildrenScaleDown = false;
                break;
            } else {
                allChildrenScaleDown = true;
            }
        }

        //all the children sent the scale down only, it will try to scale down
        if (allChildrenScaleDown) {
            //Check whether this app monitor has bursted application
            ApplicationBuilder.handleApplicationInstanceTerminatingEvent(this.appId,
                    instanceContext.getId());
        }

        //Resetting the events
        instanceContext.setIdToScalingDownBeyondMinEvent(
                new ConcurrentHashMap<String, ScalingDownBeyondMinEvent>());

    }


    /**
     * Find the group monitor by traversing recursively in the hierarchical monitors.
     *
     * @param groupId the unique alias of the Group
     * @return the found GroupMonitor
     */
    public Monitor findGroupMonitorWithId(String groupId) {
        //searching within active monitors
        return findGroupMonitor(groupId, aliasToActiveMonitorsMap);
    }


    /**
     * Utility method to find the group monitor recursively within app monitor
     *
     * @param id       the unique alias of the Group
     * @param monitors the group monitors found in the app monitor
     * @return the found GroupMonitor
     */
    private Monitor findGroupMonitor(String id, Map<String, Monitor> monitors) {
        if (monitors.containsKey(id)) {
            return monitors.get(id);
        }

        for (Monitor monitor : monitors.values()) {
            if (monitor instanceof ParentComponentMonitor) {
                Monitor monitor1 = findGroupMonitor(id, ((ParentComponentMonitor) monitor).
                        getAliasToActiveMonitorsMap());
                if (monitor1 != null) {
                    return monitor1;
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

        if (applicationInstance == null) {
            log.warn("The required application [instance] " + instanceId + " not found in the AppMonitor");
        } else {
            if (applicationInstance.getStatus() != status) {
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
        String childId = statusEvent.getId();
        String instanceId = statusEvent.getInstanceId();
        LifeCycleState status1 = statusEvent.getStatus();
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination
        if (status1 == ClusterStatus.Active || status1 == GroupStatus.Active) {
            onChildActivatedEvent(childId, instanceId);

        } else if (status1 == ClusterStatus.Inactive || status1 == GroupStatus.Inactive) {
            this.markInstanceAsInactive(childId, instanceId);
            onChildInactiveEvent(childId, instanceId);

        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inActive in the map
            markInstanceAsTerminating(childId, instanceId);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Check whether all dependent goes Terminated and then start them in parallel.
            removeInstanceFromFromInactiveMap(childId, instanceId);
            removeInstanceFromFromTerminatingMap(childId, instanceId);

            ApplicationInstance instance = (ApplicationInstance) instanceIdToInstanceMap.get(instanceId);
            if (instance != null) {
                if (this.isTerminating()) {
                    ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().process(this.id,
                            appId, instanceId);
                } else {
                    onChildTerminatedEvent(childId, instanceId);
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
    public void onParentScalingEvent(ScalingEvent scalingEvent) {

    }

    public boolean startMinimumDependencies(Application application)
            throws TopologyInConsistentException, PolicyValidationException {

        return createInstanceAndStartDependency(application);
    }

    private boolean createInstanceAndStartDependency(Application application)
            throws TopologyInConsistentException, PolicyValidationException {
        boolean initialStartup = true;
        List<String> instanceIds = new ArrayList<String>();
        String instanceId;

        ApplicationPolicy applicationPolicy = PolicyManager.getInstance().getApplicationPolicy(application.getApplicationPolicyId());
        if (applicationPolicy == null) {
            String msg = String.format("Application policy not found in registry or in-memory [application-id] %s", appId);
            log.error(msg);
            throw new RuntimeException(msg);
        }

        NetworkPartitionAlgorithmContext algorithmContext = AutoscalerContext.getInstance().getNetworkPartitionAlgorithmContext(appId);
        if (algorithmContext == null) {
            String msg = String.format("Network partition algorithm context not found in registry or in-memory [application-id] %s", appId);
            log.error(msg);
            throw new RuntimeException(msg);
        }

        String networkPartitionAlgorithmName = applicationPolicy.getAlgorithm();
        if (log.isDebugEnabled()) {
            String msg = String.format("Network partition algorithm is %s [application-id] %s", networkPartitionAlgorithmName, appId);
            log.debug(msg);
        }

        NetworkPartitionAlgorithm algorithm = getNetworkPartitionAlgorithm(networkPartitionAlgorithmName);
        if (algorithm == null) {
            String msg = String.format("Coudln't create network partition algorithm [application-id] %s", appId);
            log.error(msg);
            throw new RuntimeException(msg);
        }

        List<String> nextNetworkPartitions = algorithm.getNextNetworkPartitions(algorithmContext);
        if (nextNetworkPartitions == null || nextNetworkPartitions.isEmpty()) {
            String msg = String.format("No network partitions available for application bursting [application-id] %s", appId);
            log.warn(msg);
            return false;
        }

        for (String networkPartitionIds : nextNetworkPartitions) {
            ApplicationLevelNetworkPartitionContext context =
                    new ApplicationLevelNetworkPartitionContext(networkPartitionIds);
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
                    networkPartitionIds + " [appInstanceId] " + instanceId);
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
        context.addPendingInstance(instanceContext);

        //adding to instance map
        this.instanceIdToInstanceMap.put(instanceId, instance);
        //adding ApplicationLevelNetworkPartitionContext to networkPartitionContexts map
        this.getNetworkPartitionCtxts().put(context.getId(), context);

        return instanceId;
    }

    public void handleApplicationBursting() throws TopologyInConsistentException,
            PolicyValidationException,
            MonitorNotFoundException {

        Application application = ApplicationHolder.getApplications().getApplication(appId);
        if (application == null) {
            String msg = "Application cannot be found in the Topology.";
            throw new TopologyInConsistentException(msg);
        }

        boolean burstNPFound = false;
        String instanceId = null;

        ApplicationPolicy applicationPolicy = PolicyManager.getInstance().getApplicationPolicy(application.getApplicationPolicyId());
        if (applicationPolicy == null) {
            String msg = String.format("Application policy not found in registry or in-memory [application-id] %s", appId);
            log.error(msg);
            throw new RuntimeException(msg);
        }

        NetworkPartitionAlgorithmContext algorithmContext = AutoscalerContext.getInstance().getNetworkPartitionAlgorithmContext(appId);
        if (algorithmContext == null) {
            String msg = String.format("Network partition algorithm context not found in registry or in-memory [application-id] %s", appId);
            log.error(msg);
            throw new RuntimeException(msg);
        }

        String networkPartitionAlgorithmName = applicationPolicy.getAlgorithm();
        if (log.isDebugEnabled()) {
            String msg = String.format("Network partition algorithm is %s [application-id] %s", networkPartitionAlgorithmName, appId);
            log.debug(msg);
        }

        NetworkPartitionAlgorithm algorithm = getNetworkPartitionAlgorithm(networkPartitionAlgorithmName);
        if (algorithm == null) {
            String msg = String.format("Coudln't create network partition algorithm [application-id] %s", appId);
            log.error(msg);
            throw new RuntimeException(msg);
        }

        List<String> nextNetworkPartitions = algorithm.getNextNetworkPartitions(algorithmContext);
        if (nextNetworkPartitions == null || nextNetworkPartitions.isEmpty()) {
            String msg = String.format("No network partitions available for application bursting [application-id] %s", appId);
            log.warn(msg);
            return;
        }

        for (String networkPartitionId : nextNetworkPartitions) {
            if (!this.getNetworkPartitionCtxts().containsKey(networkPartitionId)) {

                ApplicationLevelNetworkPartitionContext context = new ApplicationLevelNetworkPartitionContext(networkPartitionId);

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

        if (!burstNPFound) {
            log.warn("[Application] " + appId + " cannot be burst as no available resources found");
        } else {
            startDependency(application, instanceId);
        }
    }

    private ApplicationInstance createApplicationInstance(Application application, String networkPartitionId) {
        //String instanceId = this.generateInstanceId(application);
        ApplicationInstance instance = ApplicationBuilder.handleApplicationInstanceCreatedEvent(
                appId, networkPartitionId);
        return instance;
    }

    public boolean isTerminating() {
        return isTerminating;
    }

    public void setTerminating(boolean isTerminating) {
        this.isTerminating = isTerminating;
    }

    @Override
    public void destroy() {
        stopScheduler();
    }

    @Override
    public boolean createInstanceOnDemand(String instanceId) {
        return false;

    }

    private NetworkPartitionAlgorithm getNetworkPartitionAlgorithm(String algorithmName) {

        if (algorithmName == null || algorithmName.isEmpty()) {
            return null;
        }

        if (algorithmName.equals(StratosConstants.NETWORK_PARTITION_ONE_AFTER_ANOTHER_ALGORITHM_ID)) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Network partition algorithm is set to %s in applicatioin policy",
                        StratosConstants.NETWORK_PARTITION_ONE_AFTER_ANOTHER_ALGORITHM_ID);
                log.debug(msg);
            }
            return new OneAfterAnotherAlgorithm();
        } else if (algorithmName.equals(StratosConstants.NETWORK_PARTITION_ALL_AT_ONCE_ALGORITHM_ID)) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Network partition algorithm is set to %s in applicatioin policy",
                        StratosConstants.NETWORK_PARTITION_ALL_AT_ONCE_ALGORITHM_ID);
                log.debug(msg);
            }
            return new AllAtOnceAlgorithm();
        }

        if (log.isDebugEnabled()) {
            String msg = String.format("Invalid network partition algorithm %s found in applicatioin policy",
                    StratosConstants.NETWORK_PARTITION_ALL_AT_ONCE_ALGORITHM_ID);
            log.debug(msg);
        }

        return null;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
