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
package org.wso2.carbon.billing.core;

public class BillingConstants {
    public static final String GROUP_ID = "BillingTasks";
    public static final String TASK_NAME_KEY = "taskName";
    public static final String SCHEDULER_KEY = "scheduler";
    public static final String SCHEDULER_CONTEXT = "schedulerContext";
    public static final String BILLING_ENGINE_KEY = "billingEngine";
    public static final String BILLING_CONFIG = "billing-config.xml";
    public static final String CONFIG_NS = "http://wso2.com/carbon/multitenancy/billing/config";
    public static final String TRIGGER_CALCULATOR_CLASS_ATTR = "scheduleHelperClass";
    public static final String TRIGGER_CALCULATOR_SERVICE_ATTR = "scheduleHelperService";
    public static final String SCHEDULE_CONF_KEY = "schedule";
    public static final String SCHEDULE_CONF_PARAM_KEY = "parameter";
    public static final String SCHEDULE_CONF_PARAM_NAME_KEY = "name";
    public static final String HANDLER = "handler";
    public static final String HANDLERS = "handlers";
    public static final String HANDLER_CLASS_ATTR = "class";
    public static final String HANDLER_SERVICE_ATTR = "service";
    public static final String SUBSCRIPTION_FILTER_KEY = "subscriptionFilter";
    public static final String DB_CONFIG = "dbConfig";
    public static final String TASKS = "tasks";
    public static final String ATTR_ID = "id";
    public static final String NS_PREFIX = "";
    public static final String DBCONFIG_VALIDATION_QUERY = "validationQuery";
    public static final String DBCONFIG_MAX_WAIT = "maxWait";
    public static final String DBCONFIG_MIN_IDLE = "minIdle";
    public static final String DBCONFIG_MAX_ACTIVE = "maxActive";
    public static final String DBCONFIG_DRIVER_NAME = "driverName";
    public static final String DBCONFIG_PASSWORD = "password";
    public static final String DBCONFIG_USER_NAME = "userName";
    public static final String DBCONFIG_URL = "url";
    public static final String SUBSCRIPTION_SUBITEM = "subscription";
    public static final String BANDWIDTH_SUBITEM = "bwOveruse";
    public static final String STORAGE_SUBITEM = "storageOveruse";
    public static final String CARTRIDGE_SUBITEM = "cartridgeOveruse";
    public static final String PAYMENT_RECEIVED_EMAIL_CUSTOMER_FILE = "email-payment-received-customer.xml";
    public static final String REGISTRATION_PAYMENT_RECEIVED_EMAIL_CUSTOMER_FILE = "email-registration-payment-received-customer.xml";
    public static final String PAYMENT_RECEIVED_EMAIL_WSO2_FILE = "email-payment-received-wso2.xml";
    public static final String WSO2_BILLING_DS = "WSO2BillingDS";
}
