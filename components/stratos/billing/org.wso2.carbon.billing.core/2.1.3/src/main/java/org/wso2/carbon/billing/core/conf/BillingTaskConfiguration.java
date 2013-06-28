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
package org.wso2.carbon.billing.core.conf;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingConstants;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingHandler;
import org.wso2.carbon.billing.core.scheduler.ScheduleHelper;

import javax.xml.namespace.QName;
import java.util.*;

public class BillingTaskConfiguration {
    private static final Log log = LogFactory.getLog(BillingTaskConfiguration.class);

    private String id;
    private ScheduleHelper scheduleHelper = null;
    private Map<String, String> schedulerHelperArgs;
    private String schedulerServiceName;

    private List<BillingHandler> billingHandlers = new ArrayList<BillingHandler>();
    private List<HandlerConfigBean> handlerArgs = new ArrayList<HandlerConfigBean>(); //key - handler service name
    private static Map<String, ScheduleHelper> schedulerServices =
            new HashMap<String, ScheduleHelper>(); 
    private static Map<String, BillingHandler> handlerServices =
            new HashMap<String, BillingHandler>();

    private class HandlerConfigBean {
        public String name;
        public boolean isServiceType;
        public Map<String, String> constructorArgs;

        public HandlerConfigBean(String name, boolean isServiceType, Map<String, String> constructorArgs){
            this.name = name;
            this.isServiceType = isServiceType;
            this.constructorArgs = constructorArgs;
        }
    }

    public BillingTaskConfiguration(String id, OMElement billingConfigEle) throws BillingException {
        this.id = id;
        deserialize(billingConfigEle);
    }

    /*
     *  Deserialize following and creates tasks
        <task id="multitenancyScheduledTask">
            <subscriptionFilter>multitenancy</subscriptionFilter>
            <schedule scheduleHelperClass="package.ClassName">
                ...
            </schedule>
            <handlers>
                ...
            </handlers>
        </task>
     */
    private void deserialize(OMElement taskConfigEle) throws BillingException {
        Iterator billingConfigChildIt = taskConfigEle.getChildElements();
        
        while (billingConfigChildIt.hasNext()) {
            OMElement childEle = (OMElement) billingConfigChildIt.next();
            if (new QName(BillingConstants.CONFIG_NS, BillingConstants.SCHEDULE_CONF_KEY,
                    BillingConstants.NS_PREFIX).equals(childEle.getQName())) {
                deserializeSchedule(childEle);
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.HANDLERS,
                    BillingConstants.NS_PREFIX).equals(childEle.getQName())) {
                deserializeHandlers(childEle);
            } else {
                String msg = "Unknown element in task configuration for task " + id +
                                ": " + childEle.getQName().getLocalPart();
                log.error(msg);
                throw new BillingException(msg);
            }
        }
    }

    /*
        <handlers>
            <handler service="serviceName">
            </handler>
            <handler class="org.wso2.carbon.billing.core.handlers.EmailSendingHandler">
                <parameter name="file">email-billing-notifications.xml</parameter>
            </handler>
        </handlers>
     */
    private void deserializeHandlers(OMElement handlersEle) throws BillingException {
        // iterate through each billingHandlers
        Iterator handlersChildIt = handlersEle.getChildElements();
        
        while (handlersChildIt.hasNext()) {
            OMElement handlerEle = (OMElement) handlersChildIt.next();
            if (!(new QName(BillingConstants.CONFIG_NS, BillingConstants.HANDLER,
                    BillingConstants.NS_PREFIX).equals(handlerEle.getQName()))) {
                String msg = "Unknown element in handler configuration for task " + id +
                                ": " + handlerEle.getQName().getLocalPart();
                log.error(msg);
                throw new BillingException(msg);
            }

            // get the parameters for handler
            Iterator handlerParametersIt = handlerEle.getChildElements();
            Map<String, String> constructorArgs = extractConstructorArgs(handlerParametersIt);
            String handlerClassName =
                    handlerEle.getAttributeValue(new QName(BillingConstants.HANDLER_CLASS_ATTR));
            if (handlerClassName == null) {
                // className is not given. So, it uses a handlerService
                String handlerServiceName = handlerEle.getAttributeValue(
                        new QName(BillingConstants.HANDLER_SERVICE_ATTR));
                HandlerConfigBean bean = new HandlerConfigBean(handlerServiceName, true, constructorArgs);
                handlerArgs.add(bean);
            } else {
                HandlerConfigBean bean = new HandlerConfigBean(handlerClassName, false, constructorArgs);
                handlerArgs.add(bean);
            }
        }
    }

    /* 
     * Deserialize following and creates scheduleHelper
        <schedule scheduleHelperClass="package.ClassName">
            <parameter name="dayToTriggerOn">1</parameter>
            <parameter name="hourToTriggerOn">0</parameter>
            <parameter name="timeZone">GMT-8:00</parameter>
        </schedule>
     */
    private void deserializeSchedule(OMElement scheduleEle) throws BillingException {
        Iterator scheduleHelperConfigChildIt = scheduleEle.getChildElements();
        Map<String, String> constructorArgs = extractConstructorArgs(scheduleHelperConfigChildIt);
        
        // get the scheduleHelper class name
        String className = scheduleEle.getAttributeValue(
                new QName(BillingConstants.TRIGGER_CALCULATOR_CLASS_ATTR));
        
        if (className == null) {
            //className is not given; it is using scheduler service
            schedulerServiceName = scheduleEle.getAttributeValue(
                    new QName(BillingConstants.TRIGGER_CALCULATOR_SERVICE_ATTR));
            schedulerHelperArgs = constructorArgs;
        } else {
            //className is given; Construct the object
            scheduleHelper = (ScheduleHelper) constructObject(className);
            scheduleHelper.init(constructorArgs);
        }
    }

    public ScheduleHelper getScheduleHelper() throws BillingException {
        if (scheduleHelper == null && schedulerServiceName != null) {
            scheduleHelper = schedulerServices.get(schedulerServiceName);
            if (scheduleHelper == null) {
                String msg = "The scheduler helper service: " + schedulerServiceName +
                                " is not loaded.";
                log.error(msg);
                throw new BillingException(msg);
            }
            scheduleHelper.init(schedulerHelperArgs);
        }
        return scheduleHelper;
    }

    public List<BillingHandler> getBillingHandlers() throws BillingException {
        // We have to combine the handlers and handlerServices as a single list and return. When
        // creating and initializing handerServices, remove them from the handelerArgs so that they
        // will be included only once
        if(!handlerArgs.isEmpty()){
            for(HandlerConfigBean bean : handlerArgs){
                if(bean.isServiceType){
                    BillingHandler handlerService = handlerServices.get(bean.name);
                    if (handlerService == null) {
                        billingHandlers = null;
                        String msg = "The handler service: " + bean.name + " is not loaded.";
                        log.error(msg);
                        throw new BillingException(msg);
                    }
                    handlerService.init(bean.constructorArgs);
                    billingHandlers.add(handlerService);

                } else {
                    BillingHandler handler = (BillingHandler) constructObject(bean.name);
                    handler.init(bean.constructorArgs);
                    billingHandlers.add(handler);
                }
            }

            //all the billing handler services are initialized properly, can clear handlerArgs
            handlerArgs.clear();
        }
        return billingHandlers;
    }

    private static Object constructObject(String className) throws BillingException {
        try {
            return Class.forName(className).newInstance();
        } catch (ClassNotFoundException e) {
            String msg = "The class: " + className + " is not in the classpath.";
            log.error(msg, e);
            throw new BillingException(msg, e);
        } catch (Exception e) {
            String msg = "Error in initializing the object for " + className + ".";
            log.error(msg);
            throw new BillingException(msg, e);
        }
    }

    private static Map<String, String> extractConstructorArgs(Iterator parameterIt) {
        Map<String, String> constructorArgs = new HashMap<String, String>();
        
        while (parameterIt.hasNext()) {
            OMElement paramEle = (OMElement) parameterIt.next();
            if (!new QName(BillingConstants.CONFIG_NS, BillingConstants.SCHEDULE_CONF_PARAM_KEY,
                    BillingConstants.NS_PREFIX).equals(paramEle.getQName())) {
                continue;
            }
            
            String paramName = paramEle.getAttributeValue(
                    new QName(BillingConstants.SCHEDULE_CONF_PARAM_NAME_KEY));
            String paramValue = paramEle.getText();
            constructorArgs.put(paramName, paramValue);
        }
        return constructorArgs;
    }

    public String getId() {
        return id;
    }

    // the following two methods will be called when the services are available for the schedule
    // helper and the billing handlers
    public static void addScheduleHelper(ScheduleHelper scheduleHelper) {
        schedulerServices.put(scheduleHelper.getClass().getName(), scheduleHelper);
    }

    public static void addBillingHandler(BillingHandler billingHandler) {
        handlerServices.put(billingHandler.getClass().getName(), billingHandler);
    }
}
