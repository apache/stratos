/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.throttling.manager.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskScheduler;
import org.apache.synapse.task.TaskSchedulerFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.billing.core.BillingEngine;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Item;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.rule.kernel.config.RuleEngineConfigService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.billing.mgt.api.MultitenancyBillingInfo;
import org.wso2.carbon.billing.mgt.dataobjects.MultitenancyPackage;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.throttling.agent.client.ThrottlingRuleInvoker;
import org.wso2.carbon.throttling.manager.conf.ThrottlingConfiguration;
import org.wso2.carbon.throttling.manager.dataproviders.DataProvider;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.scheduling.ThrottlingJob;
import org.wso2.carbon.throttling.manager.services.MultitenancyThrottlingService;
import org.wso2.carbon.throttling.manager.tasks.Task;
import org.wso2.carbon.usage.api.TenantUsageRetriever;

import java.io.File;
import java.lang.reflect.Constructor;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Util methods for throttling manager.
 */
public class Util {
    private static final Log log = LogFactory.getLog(Util.class);
    private static RegistryService registryService;
    private static RealmService realmService;
    private static RuleEngineConfigService ruleEngineConfigService;
    private static BillingManager billingManager;
    private static BundleContext bundleContext;
    private static TenantUsageRetriever tenantUsageRetriever;
    private static MultitenancyBillingInfo mtBillingInfo;
    private static final String THROTTLING_CONFIG = "usage-throttling-agent-config.xml";
    private static final String THROTTLING_TASK_ID = "throttling-task";
    private static ThrottlingConfiguration throttlingConfiguration;
    private static final String THROTTLING_RULE_FILE = "throttling-rules.drl";

    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    public static synchronized void setBundleContext(BundleContext context) {
        if (bundleContext == null) {
            bundleContext = context;
        }
    }

    public static void setTenantUsageRetriever(TenantUsageRetriever tenantUsageRetriever) {
        Util.tenantUsageRetriever = tenantUsageRetriever;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static UserRegistry getSuperTenantGovernanceSystemRegistry() throws RegistryException {
        return registryService.getGovernanceSystemRegistry();
    }

    public static TenantUsageRetriever getTenantUsageRetriever() {
        return tenantUsageRetriever;
    }

    public static void setRuleEngineConfigService(
            RuleEngineConfigService ruleEngineConfigService) {
        Util.ruleEngineConfigService = ruleEngineConfigService;
    }

    public static RuleEngineConfigService getRuleEngineConfigService() {
        return Util.ruleEngineConfigService;
    }

    public static BillingManager getBillingManager() {
        return billingManager;
    }

    public static void setBillingManager(BillingManager billingManager) {
        Util.billingManager = billingManager;
    }

    /**
     * get current billing customer.
     *
     * @param tenantId, tenant id.
     * @return Customer
     * @throws RegistryException, if getting the current billing customer failed.
     */
    public static Customer getCurrentBillingCustomer(int tenantId) throws RegistryException {
        // get the host name of the current domain
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            return null;
        }
        Tenant tenant;
        try {
            tenant = (Tenant) realmService.getTenantManager().getTenant(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in getting the realm Information.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        if (tenant == null) {
            return null;
        }
        String customerName = tenant.getDomain();
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_VIEWING_TASK_ID);
        Customer customer;
        try {
            if (billingEngine != null) {
                List<Customer> customers = billingEngine.getCustomersWithName(customerName);
                if (customers == null || customers.size() == 0) {
                    customer = null;
                } else {
                    customer = customers.get(0);
                }
            } else {
                customer=null;
                String msg = "Error in getting billing Engine";
                log.error(msg);
            }
        } catch (BillingException e) {
            String msg = "Error in getting the current customer";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        return customer;
    }

    /**
     * get current subscription type for the tenant.
     *
     * @param tenantId, tenant id
     * @throws RegistryException, if getting the current subscription type failed.
     * @return, Subscripiton
     */
    public static Subscription getCurrentSubscription(int tenantId) throws RegistryException {
        BillingEngine billingEngine =
                billingManager.getBillingEngine(StratosConstants.MULTITENANCY_SCHEDULED_TASK_ID);

        Customer customer = getCurrentBillingCustomer(tenantId);
        if (customer == null) {
            return null;
        }
        List<Subscription> subscriptions;
        try {
            subscriptions = billingEngine.getActiveSubscriptions(customer);
        } catch (BillingException e) {
            String msg = "Error in getting the current subscription.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        if (subscriptions == null || subscriptions.size() == 0) {
            return null;
        }
        Subscription subscription = subscriptions.get(0);
        if (subscription.getActiveUntil().getTime() <= System.currentTimeMillis()) {
            return null;
        }
        int itemId = subscription.getItem().getId();
        // fill with a correct item
        Item item;
        try {
            item = billingEngine.getItem(itemId);
        } catch (BillingException e) {
            String msg = "Error in getting the item for item id: " + itemId + ".";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
        subscription.setItem(item);
        return subscription;
    }

    /**
     * get current billing package.
     *
     * @param tenantId, tenant id.
     * @return MultitenancyPackage
     * @throws RegistryException, if getting the current billing package failed.
     */
    public static MultitenancyPackage getCurrentBillingPackage(int tenantId)
            throws RegistryException {
        if (mtBillingInfo == null) {
            String msg =
                    "Error in retrieving the current billing package. The package info is null.";
            log.error(msg);
            throw new RegistryException(msg);
        }
        List<MultitenancyPackage> multitenancyPackages = mtBillingInfo.getMultitenancyPackages();
        Subscription subscription = getCurrentSubscription(tenantId);
        Item currentPackage;
        if (subscription == null) {
            currentPackage = null;
        } else {
            currentPackage = subscription.getItem();
        }
        MultitenancyPackage currentMultitenancyPackage = null;
        for (MultitenancyPackage multitenancyPackage : multitenancyPackages) {
            if (multitenancyPackage.getName().toLowerCase().contains("free") &&
                    currentPackage == null) {
                currentMultitenancyPackage = multitenancyPackage;
                break;
            } else if (currentPackage != null &&
                    multitenancyPackage.getName().equals(currentPackage.getName())) {
                currentMultitenancyPackage = multitenancyPackage;
                break;
            }
        }
        return currentMultitenancyPackage;
    }

    /**
     * get maximum users allowed for a tenant.
     *
     * @param tenantId, tenant id
     * @throws RegistryException, if getting the maximum number of users failed.
     * @return, maximum number of users allowed.
     */
    public static int getMaximumUsersAllow(int tenantId) throws RegistryException {
        MultitenancyPackage multitenancyPackage = getCurrentBillingPackage(tenantId);
        if (multitenancyPackage == null) {
            String msg = "The multitenancy package is null.";
            log.error(msg);
            throw new RegistryException(msg);
        }
        return multitenancyPackage.getUsersLimit();
    }

    /**
     * returns the maximum resource volume in bytes
     *
     * @param tenantId, tenant id
     * @return Resource volume limit.
     * @throws RegistryException, if getting the maximum resource limit failed.
     */
    public static long getMaximumResourceVolume(int tenantId) throws RegistryException {
        MultitenancyPackage multitenancyPackage = getCurrentBillingPackage(tenantId);
        if (multitenancyPackage == null) {
            String msg = "The multitenancy package is null.";
            log.error(msg);
            throw new RegistryException(msg);
        }
        // converting the mb to bytes
        return ((long) multitenancyPackage.getResourceVolumeLimit()) * 1024 * 1024;
    }

    /**
     * get the current month
     *
     * @param calendar, Calendar
     * @return, year-month
     */
    public static String getCurrentMonthString(Calendar calendar) {
        int currentMonth = calendar.get(Calendar.MONTH);

        String[] monthArr = new DateFormatSymbols().getMonths();
        String month = monthArr[currentMonth];
        return calendar.get(Calendar.YEAR) + "-" + month;
    }

    public static void registerTaskOSGIService(Task task) {
        if (bundleContext != null) {
            bundleContext.registerService(Task.class.getName(), task, null);
        }
    }

    public static void registerHandlerOSGIService(DataProvider handler) {
        if (bundleContext != null) {
            bundleContext.registerService(DataProvider.class.getName(), handler, null);
        }
    }

    public static void setMultitenancyBillingInfo(MultitenancyBillingInfo mtBillingInfo) {
        Util.mtBillingInfo = mtBillingInfo;
    }

    /**
     * Construct the object for the given class
     *
     * @param className - name of the class
     * @return the constructed object.
     * @throws ThrottlingException, if constructing the object failed.
     */
    public static Object constructObject(String className) throws ThrottlingException {
        Class cl;
        Constructor co;
        Object obj;
        try {
            cl = Class.forName(className);
        } catch (ClassNotFoundException e) {
            String msg = "The class: " + className + " is not in the classpath.";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }

        try {
            co = cl.getConstructor();
        } catch (NoSuchMethodException e) {
            String msg = "The default constructor for the  is not available for " + className + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
        try {
            obj = co.newInstance();
        } catch (Exception e) {
            String msg = "Error in initializing the object for " + className + ".";
            log.error(msg);
            throw new ThrottlingException(msg, e);
        }
        return obj;
    }

    public static void registerThrottlingRuleInvoker() {
        // construct an instance of throttling service as the rule invoker.
        bundleContext.registerService(ThrottlingRuleInvoker.class.getName(),
                new MultitenancyThrottlingService(), null);
    }

    /**
     * Initialize throttling
     *
     * @throws ThrottlingException, if initializing failed.
     */
    public static void initializeThrottling() throws ThrottlingException {

        // load the configuration and initialize the billing engine + do the
        // necessary scheduling.
        String configFile = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator + THROTTLING_CONFIG;
        // the configuration init will initialize task objects.
        throttlingConfiguration = new ThrottlingConfiguration(configFile);
        List<Task> throttlingTasks = throttlingConfiguration.getThrottlingTasks();

        // now initialize the scheduling per each task
        for (Task throttlingTask : throttlingTasks) {
            initializeScheduling(throttlingTask);
        }
    }

    private static void initializeScheduling(Task throttlingTask) {
        // generate tasks
        if (throttlingTask.getTriggerInterval() < 0) {
            log.info("Throttling manager Validation info service Disabled");
        } else {
            String taskName = UUIDGenerator.generateUUID();
            String groupId = UUIDGenerator.generateUUID();

            TaskDescription taskDescription = new TaskDescription();
            taskDescription.setName(taskName);
            taskDescription.setGroup(groupId);
            // we are triggering only at the period

            taskDescription.setInterval(throttlingTask.getTriggerInterval());

            //Delay first run by given minutes
            Calendar startTime = Calendar.getInstance();
            startTime.add(Calendar.MILLISECOND, throttlingTask.getStartDelayInterval());
            taskDescription.setStartTime(startTime.getTime());

            Map<String, Object> resources = new HashMap<String, Object>();
            resources.put(ThrottlingJob.THROTTLING_TASK_CONTEXT_KEY, throttlingTask);

            TaskScheduler taskScheduler = TaskSchedulerFactory.getTaskScheduler(THROTTLING_TASK_ID);
            if (!taskScheduler.isInitialized()) {
                Properties properties = new Properties();
                taskScheduler.init(properties);
            }
            taskScheduler.scheduleTask(taskDescription, resources, ThrottlingJob.class);
        }
    }

    /**
     * get all the tenants
     *
     * @return Tenant[]
     * @throws UserStoreException, if getting the tenants failed.
     */
    public static Tenant[] getAllTenants() throws UserStoreException {
        TenantManager tenantManager = realmService.getTenantManager();
        try {
            return (Tenant[]) tenantManager.getAllTenants();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }
    }

    public static List<Task> getTasks() {
        return throttlingConfiguration.getThrottlingTasks();
    }

    /**
     * Load throttling rules
     *
     * @throws Exception, if loading the throttling rules failed.
     */
    public static void loadThrottlingRules() throws Exception {
        UserRegistry systemRegistry = getSuperTenantGovernanceSystemRegistry();
        if (systemRegistry.resourceExists(StratosConstants.THROTTLING_RULES_PATH)) {
            return;
        }
        String throttlingRuleFile = CarbonUtils.getCarbonConfigDirPath() +
                File.separator + THROTTLING_RULE_FILE;
        byte[] content = CarbonUtils.getBytesFromFile(new File(throttlingRuleFile));
        Resource ruleResource = systemRegistry.newResource();
        ruleResource.setContent(content);
        systemRegistry.put(StratosConstants.THROTTLING_RULES_PATH, ruleResource);
    }
}
