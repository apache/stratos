/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.carbon.mediator.autoscale.lbautoscale.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.task.Task;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.mediator.autoscale.lbautoscale.callables.AppNodeSanityCheckCallable;
import org.wso2.carbon.mediator.autoscale.lbautoscale.callables.AutoscaleDeciderCallable;
import org.wso2.carbon.mediator.autoscale.lbautoscale.callables.InstanceCountCallable;
import org.wso2.carbon.mediator.autoscale.lbautoscale.callables.PendingInstanceCountCallable;
import org.wso2.carbon.mediator.autoscale.lbautoscale.clients.CloudControllerClient;
import org.wso2.carbon.mediator.autoscale.lbautoscale.clients.CloudControllerOsgiClient;
import org.wso2.carbon.mediator.autoscale.lbautoscale.clients.CloudControllerStubClient;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.AppDomainContext;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.LoadBalancerContext;
import org.wso2.carbon.lb.common.replication.RequestTokenReplicationCommand;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleConstants;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleUtil;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;

/**
 * Service request in flight autoscaler task for Stratos service level auto-scaling
 */
public class ServiceRequestsInFlightAutoscaler implements Task, ManagedLifecycle {

    private static final Log log = LogFactory.getLog(ServiceRequestsInFlightAutoscaler.class);

    /**
     * This instance holds the loadbalancer configuration
     */
    private LoadBalancerConfiguration loadBalancerConfig;

    /**
     * Autoscaler service client instance
     */
    private CloudControllerClient autoscalerService;

    /**
     * AppDomainContexts for each domain
     * Key - domain
     * Value - Map of key - sub domain
     * value - {@link AppDomainContext}
     */
    private Map<String, Map<String, ?>> appDomainContexts =
        new HashMap<String, Map<String, ?>>();

    /**
     * LB Context for LB cluster
     */
    private final LoadBalancerContext lbContext = new LoadBalancerContext();

    /**
     * Attribute to keep track whether this instance is the primary load balancer.
     */
    private boolean isPrimaryLoadBalancer;

    /**
     * Keeps track whether this task is still running
     */
    private boolean isTaskRunning;
    
    /**
     * Thread pool used in this task to execute parallel tasks.
     */
    private ExecutorService executor = Executors.newFixedThreadPool(100);

    /**
     * Check that all app nodes in all clusters meet the minimum configuration
     */
    private void appNodesSanityCheck() {
        
        List<Future<Boolean>> jobList = new ArrayList<Future<Boolean>>();
        
        for (String serviceDomain : loadBalancerConfig.getServiceDomains()) {
            
            String msg =
                    "Sanity check is failed to run. No Appdomain context is generated for the" +
                        " domain " + serviceDomain;
            
            // get the list of service sub_domains specified in loadbalancer config
            String[] serviceSubDomains = loadBalancerConfig.getServiceSubDomains(serviceDomain);

            for (String serviceSubDomain : serviceSubDomains) {
                log.debug("Sanity check has started for: "+AutoscaleUtil.domainSubDomainString(serviceDomain, serviceSubDomain));
                AppDomainContext appCtxt;
                if (appDomainContexts.get(serviceDomain) != null) {
                    appCtxt = (AppDomainContext) appDomainContexts.get(serviceDomain).get(serviceSubDomain);
                    
                    if (appCtxt != null) {
                        // Concurrently perform the application node sanity check.
                        Callable<Boolean> worker =
                            new AppNodeSanityCheckCallable(serviceDomain, serviceSubDomain, autoscalerService, appCtxt);
                        Future<Boolean> appNodeSanityCheck = executor.submit(worker);
                        jobList.add(appNodeSanityCheck);

                    } else{
                        log.error(msg + " and sub domain " + serviceSubDomain + " combination.");
                    }
                } else{
                    log.error(msg);
                }
            }
        }
        
        // Retrieve the results of the concurrently performed sanity checks.
        for (Future<Boolean> job : jobList) {
            try {
                job.get();
            } catch (Exception ignore) {
                log.error(ignore.getMessage(), ignore);
                // no need to throw
            } 
        }

    }

    /**
     * Autoscale the entire system, analyzing the requests in flight of each domain - sub domain
     */
    private void autoscale() {
        List<Future<Boolean>> jobList = new ArrayList<Future<Boolean>>();

        for (String serviceDomain : loadBalancerConfig.getServiceDomains()) {

            String msg =
                "Autoscaler check is failed to run. No Appdomain context is generated for the" +
                    " domain " + serviceDomain;

            // get the list of service sub_domains specified in loadbalancer config
            String[] serviceSubDomains = loadBalancerConfig.getServiceSubDomains(serviceDomain);

            for (String serviceSubDomain : serviceSubDomains) {

                log.debug("Autoscaling analysis is starting to run for domain: " + serviceDomain +
                    " and sub domain: " + serviceSubDomain);

                AppDomainContext appCtxt;
                if (appDomainContexts.get(serviceDomain) != null) {
                    appCtxt = (AppDomainContext) appDomainContexts.get(serviceDomain).get(serviceSubDomain);

                    if (appCtxt != null) {

                        // Concurrently make the auto-scaling decisions
                        Callable<Boolean> worker =
                            new AutoscaleDeciderCallable(serviceDomain, serviceSubDomain, autoscalerService, appCtxt);
                        Future<Boolean> autoscalerDeciderCheck = executor.submit(worker);
                        jobList.add(autoscalerDeciderCheck);

                    } else {
                        log.error(msg + " and sub domain " + serviceSubDomain + " combination.");
                    }
                } else {
                    log.error(msg);
                }
            }
        }

        // Retrieve the results of the concurrently performed sanity checks.
        for (Future<Boolean> job : jobList) {
            try {
                job.get();
            } catch (Exception ignore) {
                log.error(ignore.getMessage(), ignore);
                // no need to throw
            }
        }
    }

    /**
     * We compute the number of running instances of a particular domain using clustering agent.
     */
    private void computeRunningAndPendingInstances() {

        int runningInstances = 0, pendingInstances = 0;

        List<Future<Boolean>> jobList = new ArrayList<Future<Boolean>>();

        for (String serviceDomain : loadBalancerConfig.getServiceDomains()) {

            // get the list of service sub_domains specified in loadbalancer config
            String[] serviceSubDomains = loadBalancerConfig.getServiceSubDomains(serviceDomain);

            for (String serviceSubDomain : serviceSubDomains) {

                AppDomainContext appCtxt;
                if (appDomainContexts.get(serviceDomain) != null) {
                    appCtxt = (AppDomainContext) appDomainContexts.get(serviceDomain).get(serviceSubDomain);
                    
                    log.debug("Values in App domain context: " +
                        appCtxt.getPendingInstanceCount() +
                            " - " +
                            appCtxt.getRunningInstanceCount() +
                            " - Ctxt: " +
                            appCtxt.hashCode());

                    if (appCtxt != null) {
                        Callable<Boolean> worker =
                            new InstanceCountCallable(serviceDomain, serviceSubDomain, autoscalerService, appCtxt);
                        Future<Boolean> countInstancesJob = executor.submit(worker);
                        jobList.add(countInstancesJob);
                    }
                }
            }
        }

        for (Future<Boolean> job : jobList) {
            try {
                job.get();
            } catch (Exception ignore) {
                log.error(ignore.getMessage(), ignore);
                // no need to throw
            }
        }

        /* Calculate running load balancer instances */

        // count this LB instance in.
        runningInstances = 1;

        runningInstances += AutoscalerTaskDSHolder.getInstance().getAgent().getAliveMemberCount();

        lbContext.setRunningInstanceCount(runningInstances);

        if (AutoscalerTaskDSHolder.getInstance().getAgent().getParameter("domain") == null) {
            String msg = "Clustering Agent's domain parameter is null. Please specify a domain" +
                " name in axis2.xml of Elastic Load Balancer.";
            log.error(msg);
            throw new RuntimeException(msg);
        }

        String lbDomain = AutoscalerTaskDSHolder.getInstance().getAgent().getParameter("domain").getValue().toString();

        String lbSubDomain = null;

        if (AutoscalerTaskDSHolder.getInstance().getAgent().getParameter("subDomain") != null) {
            lbSubDomain =
                AutoscalerTaskDSHolder.getInstance().getAgent().getParameter("subDomain").getValue().toString();
        }

        // reset
        pendingInstances = 0;
        try {
            pendingInstances = lbContext.getPendingInstanceCount();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // no need to throw
        }

        lbContext.setPendingInstanceCount(pendingInstances);

        log.debug("Load Balancer members of domain: " +
            lbDomain +
            " and sub domain: " +
            lbSubDomain +
            " running instances (including this): " +
            runningInstances +
            " - pending instances: "
            +
            pendingInstances);

    }

    @Override
    public void destroy() {
        appDomainContexts.clear();
        log.debug("Cleared AppDomainContext Map.");
    }

    /**
     * This is method that gets called periodically when the task runs.
     * <p/>
     * The exact sequence of execution is shown in this method.
     */
    @Override
    public void execute() {

        appDomainContexts =
            AutoscaleUtil.getAppDomainContexts(
                AutoscalerTaskDSHolder.getInstance().getConfigCtxt(),
                loadBalancerConfig);

        if (isTaskRunning) {
            log.debug("Task is already running!");
            return;
        }
        try {
            isTaskRunning = true;
            setIsPrimaryLB();
            if (!isPrimaryLoadBalancer) {
                log.debug("This is not the primary load balancer, hence will not " +
                        "perform any sanity check.");
                return;
            }
            sanityCheck();
            autoscale();
        } finally {
            // if there are any changes in the request length
            if (Boolean.parseBoolean(System.getProperty(AutoscaleConstants.IS_TOUCHED))) {
                // primary LB will send out replication message to all load balancers
                sendReplicationMessage();
            }
            isTaskRunning = false;
            log.debug("Task finished a cycle.");
        }
    }

    @Override
    public void init(final SynapseEnvironment synEnv) {

        String msg = "Autoscaler Service initialization failed and cannot proceed.";

        loadBalancerConfig = AutoscalerTaskDSHolder.getInstance().getWholeLoadBalancerConfig();

        if (loadBalancerConfig == null) {
            log.error(msg + "Reason: Load balancer configuration is null.");
            throw new RuntimeException(msg);
        }

        ConfigurationContext configCtx = ((Axis2SynapseEnvironment) synEnv).getAxis2ConfigurationContext();
        AutoscalerTaskDSHolder.getInstance().setConfigCtxt(configCtx);

        appDomainContexts = AutoscaleUtil.getAppDomainContexts(configCtx, loadBalancerConfig);
        
        AutoscalerTaskDSHolder.getInstance().setAgent(configCtx.getAxisConfiguration().getClusteringAgent());

        boolean useEmbeddedAutoscaler = loadBalancerConfig.getLoadBalancerConfig().useEmbeddedAutoscaler();
        
        try {

            if(useEmbeddedAutoscaler){
                autoscalerService = new CloudControllerOsgiClient();
            } else{
                autoscalerService = new CloudControllerStubClient();
            }
            // let's initialize the auto-scaler service
            autoscalerService.init();

        }catch (Exception e) {
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        if (log.isDebugEnabled()) {

            log.debug("Autoscaler task is initialized.");

        }
    }

    /**
     * Sanity check to see whether the number of LBs is the number specified in the LB config
     */
    private void loadBalancerSanityCheck() {

        log.debug("Load balancer sanity check has started.");

        // get current LB instance count
        int currentLBInstances = lbContext.getInstances();

        LoadBalancerConfiguration.LBConfiguration lbConfig =
            loadBalancerConfig.getLoadBalancerConfig();

        // get minimum requirement of LB instances
        int requiredInstances = lbConfig.getInstances();

        if (currentLBInstances < requiredInstances) {
            log.debug("LB Sanity check failed. Running/Pending LB instances: " + currentLBInstances +
                ". Required LB instances: " + requiredInstances);
            int diff = requiredInstances - currentLBInstances;

            // gets the domain of the LB
            String lbDomain =
                AutoscalerTaskDSHolder
                    .getInstance()
                    .getAgent()
                    .getParameter("domain")
                    .getValue()
                    .toString();
            String lbSubDomain =
                AutoscalerTaskDSHolder
                    .getInstance()
                    .getAgent()
                    .getParameter("subDomain")
                    .getValue()
                    .toString();

            // Launch diff number of LB instances
            log.debug("Launching " + diff + " LB instances.");

            runInstances(lbContext, lbDomain, lbSubDomain, diff);
        }
    }

    private int runInstances(final LoadBalancerContext context, final String domain,
        final String subDomain,
        int diff) {

        int successfullyStartedInstanceCount = diff;

        while (diff > 0) {
            // call autoscaler service and ask to spawn an instance
            // and increment pending instance count only if autoscaler service returns
            // true.
            try {
                String ip = autoscalerService.startInstance(domain, subDomain);

                if (ip == null || ip.isEmpty()) {
                    log.debug("Instance start up failed. domain: " +
                        domain +
                            ", sub domain: " +
                            subDomain);
                    successfullyStartedInstanceCount--;
                } else {
                    log.debug("An instance of domain: " +
                        domain +
                            " and sub domain: " +
                            subDomain +
                            " is started up.");
                    if (context != null) {
                        context.incrementPendingInstances(1);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to start an instance of sub domain: " + subDomain +
                    " of domain : " + domain + ".\n", e);
                successfullyStartedInstanceCount--;
            }

            diff--;
        }

        return successfullyStartedInstanceCount;
    }

    /**
     * This method makes sure that the minimum configuration of the clusters in the system is
     * maintained
     */
    private void sanityCheck() {

        if (!isPrimaryLoadBalancer) {
            log.debug("This is not the primary load balancer, hence will not " +
                "perform any sanity check.");
            return;
        }

        log.debug("This is the primary load balancer, starting to perform sanity checks.");

        computeRunningAndPendingInstances();
        loadBalancerSanityCheck();
        appNodesSanityCheck();
    }

    /**
     * Replicate information needed to take autoscaling decision for other ELBs
     * in the cluster.
     */
    private void sendReplicationMessage() {

        ClusteringAgent clusteringAgent = AutoscalerTaskDSHolder.getInstance().getAgent();
        if (clusteringAgent != null) {
            RequestTokenReplicationCommand msg = new RequestTokenReplicationCommand();
            msg.setAppDomainContexts(appDomainContexts);
            try {
                clusteringAgent.sendMessage(msg, true);
                System.setProperty(AutoscaleConstants.IS_TOUCHED, "false");
                log.debug("Request token replication messages sent out successfully!!");

            } catch (ClusteringFault e) {
                log.error("Failed to send the request token replication message.", e);
            }
        }
        else {
            log
                .debug("Clustering Agent is null. Hence, unable to send out the replication message.");
        }
    }

    /**
     * This method will check whether this LB is the primary LB or not and set
     * attribute accordingly.
     */
    private void setIsPrimaryLB() {

        ClusteringAgent clusteringAgent = AutoscalerTaskDSHolder.getInstance().getAgent();
        if (clusteringAgent != null) {

            isPrimaryLoadBalancer = clusteringAgent.isCoordinator();

        }

    }
}
