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

package org.apache.stratos.manager.deploy.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.LoadbalancerConfig;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.deploy.service.multitenant.MultiTenantService;
import org.apache.stratos.manager.deploy.service.multitenant.lb.DefaultLBService;
import org.apache.stratos.manager.deploy.service.multitenant.lb.ExistingLBService;
import org.apache.stratos.manager.deploy.service.multitenant.lb.LBService;
import org.apache.stratos.manager.deploy.service.multitenant.lb.ServiceAwareLBService;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.lb.category.*;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.util.Collection;

public class ServiceDeploymentManager {

    private static Log log = LogFactory.getLog(ServiceDeploymentManager.class);
    
    public Service deployService (String type, String autoscalingPolicyName, String deploymentPolicyName, int tenantId, String tenantRange,
    		String tenantDomain, String userName, boolean isPublic)
            throws ADCException, UnregisteredCartridgeException, ServiceAlreadyDeployedException {

        //check if already we have a Multitenant service deployed for the same type
        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        Service deployedService;
        try {
            deployedService = dataInsertionAndRetrievalManager.getService(type);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in checking if Service is available is PersistenceManager";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }

        if (deployedService != null) {
            String errorMsg = "There is an already deployed Service for type " + type;
            log.error(errorMsg);
            throw new ServiceAlreadyDeployedException(errorMsg, type);
        }

        //get deployed Cartridge Definition information
        CartridgeInfo cartridgeInfo;
        try {
            cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(type);

        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String message = type + " is not a valid cartridgeSubscription type. Please try again with a valid cartridgeSubscription type.";
            log.error(message);
            throw new ADCException(message, e);

        } catch (Exception e) {
            String message = "Error getting info for " + type;
            log.error(message, e);
            throw new ADCException(message, e);
        }

        if (!cartridgeInfo.getMultiTenant()) {
            String errorMsg = "Cartridge definition with type " + type + " is not multitenant";
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }


//        // TODO - LB cartridge.... ??
//
//        List<Property> lbRefProp = new ArrayList<Property>();
//
//        // get lb config reference
//        LoadbalancerConfig lbConfig = cartridgeInfo.getLbConfig();
//
//        if (lbConfig == null || lbConfig.getProperties() == null) {
//
//            if (log.isDebugEnabled()) {
//                log.debug("This Service does not require a load balancer. " + "[Service Name] " +
//                          type);
//            }
//        } else {
//
//            Service lbService;
//
//            Properties lbReferenceProperties = lbConfig.getProperties();
//
//            Property property = new Property();
//            property.setName(org.apache.stratos.messaging.util.Constants.LOAD_BALANCER_REF);
//
//            for (org.apache.stratos.cloud.controller.pojo.Property prop : lbReferenceProperties.getProperties()) {
//
//                String name = prop.getName();
//                String value = prop.getValue();
//
//                // TODO make following a chain of responsibility pattern
//                if (Constants.NO_LOAD_BALANCER.equals(name)) {
//                    if ("true".equals(value)) {
//                        if (log.isDebugEnabled()) {
//                            log.debug("This cartridge does not require a load balancer. " +
//                                      "[Type] " + type);
//                        }
//                        property.setValue(name);
//                        lbRefProp.add(property);
//                        break;
//                    }
//                } else if (Constants.EXISTING_LOAD_BALANCERS.equals(name)) {
//                    String clusterIdsVal = value;
//                    if (log.isDebugEnabled()) {
//                        log.debug("This cartridge refers to existing load balancers. " + "[Type] " +
//                                  type + "[Referenced Cluster Ids] " + clusterIdsVal);
//                    }
//
//                    String[] clusterIds = clusterIdsVal.split(",");
//
//                    for (String clusterId : clusterIds) {
//
//                            try {
//                            	AutoscalerServiceClient.getServiceClient().checkLBExistenceAgainstPolicy(clusterId,
//                            			deploymentPolicyName);
//                            } catch (Exception ex) {
//                                // we don't need to throw the error here.
//                                log.error(ex.getMessage(), ex);
//                            }
//
//                    }
//
//                    property.setValue(name);
//                    lbRefProp.add(property);
//                    break;
//
//                } else if (Constants.DEFAULT_LOAD_BALANCER.equals(name)) {
//
//                    if ("true".equals(value)) {
//
//                        CartridgeInfo lbCartridgeInfo;
//                        String lbCartridgeType = lbConfig.getType();
//                        try {
//                            // retrieve lb Cartridge info
//                            lbCartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(lbCartridgeType);
//                        } catch (Exception e) {
//                            String msg = "Cannot get cartridge info: " + type;
//                            log.error(msg, e);
//                            throw new ADCException(msg, e);
//                        }
//
//                        property.setValue(name);
//                        if (log.isDebugEnabled()) {
//                            log.debug("This cartridge uses default load balancer. " + "[Type] " +
//                                      type);
//                        }
//
//                            try {
//                                // get the valid policies for lb cartridge
//                                DeploymentPolicy[] lbCartridgeDepPolicies =
//                                	AutoscalerServiceClient.getServiceClient().getDeploymentPolicies(lbCartridgeType);
//                                // traverse deployment policies of lb cartridge
//                                for (DeploymentPolicy policy : lbCartridgeDepPolicies) {
//                                    // check existence of the subscribed policy
//                                    if (deploymentPolicyName.equals(policy.getId())) {
//
//                                        if (!AutoscalerServiceClient.getServiceClient().checkDefaultLBExistenceAgainstPolicy(deploymentPolicyName)) {
//
//                                            // if lb cluster doesn't exist
//                                            lbService = new LBService(lbCartridgeType,
//                                                    lbCartridgeInfo.getDefaultAutoscalingPolicy(),
//                                                    deploymentPolicyName, tenantId,
//                                                    lbCartridgeInfo,
//                                                    tenantRange);
//
//                                            Properties lbDeploymentProperties = new Properties();
//
//                                            // check if there are properties in LB cartridge info
//                                            Property [] cartridgeInfoProps = lbCartridgeInfo.getProperties();
//                                            if (cartridgeInfoProps != null && cartridgeInfoProps.length > 0) {
//                                                lbDeploymentProperties.setProperties(combine(lbCartridgeInfo.getProperties(), new Property[]{property}));
//                                            } else {
//                                                lbDeploymentProperties.setProperties(new Property[]{property});
//                                            }
//
//                                            lbService.deploy(lbDeploymentProperties);
//
//                                            // persist
//                                            persist(lbService);
//                                        }
//                                    }
//                                }
//
//                            } catch (Exception ex) {
//                                // we don't need to throw the error here.
//                                log.error(ex.getMessage(), ex);
//                            }
//
//
//                        lbRefProp.add(property);
//                        break;
//                    }
//                } else if (Constants.SERVICE_AWARE_LOAD_BALANCER.equals(name)) {
//
//                    if ("true".equals(value)) {
//
//                        CartridgeInfo lbCartridgeInfo;
//                        String lbCartridgeType = lbConfig.getType();
//                        try {
//                            // retrieve lb Cartridge info
//                            lbCartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(lbCartridgeType);
//                        } catch (Exception e) {
//                            String msg = "Cannot get cartridge info: " + type;
//                            log.error(msg, e);
//                            throw new ADCException(msg, e);
//                        }
//
//                        property.setValue(name);
//                        if (log.isDebugEnabled()) {
//                            log.debug("This cartridge uses a service aware load balancer. " +
//                                    "[Type] " + type);
//                        }
//
//                        // add a property for the service type
//                        Property loadBalancedServiceTypeProperty = new Property();
//                        loadBalancedServiceTypeProperty.setName(Constants.LOAD_BALANCED_SERVICE_TYPE);
//                        loadBalancedServiceTypeProperty.setValue(type);
//
//                        try {
//
//                            // get the valid policies for lb cartridge
//                            DeploymentPolicy[] lbCartridgeDepPolicies =
//                                    AutoscalerServiceClient.getServiceClient().getDeploymentPolicies(lbCartridgeType);
//                            // traverse deployment policies of lb cartridge
//                            for (DeploymentPolicy policy : lbCartridgeDepPolicies) {
//
//                                // check existence of the subscribed policy
//                                if (deploymentPolicyName.equals(policy.getId())) {
//
//                                    if (!AutoscalerServiceClient.getServiceClient().checkServiceLBExistenceAgainstPolicy(type,
//                                            deploymentPolicyName)) {
//
//                                        lbCartridgeInfo.addProperties(property);
//                                        lbCartridgeInfo.addProperties(loadBalancedServiceTypeProperty);
//
//                                        lbService = new LBService(lbCartridgeType,
//                                                lbCartridgeInfo.getDefaultAutoscalingPolicy(),
//                                                deploymentPolicyName, tenantId,
//                                                lbCartridgeInfo,
//                                                tenantRange);
//
//                                        Properties lbDeploymentProperties = new Properties();
//
//                                        // check if there are properties in LB cartridge info
//                                        Property [] cartridgeInfoProps = lbCartridgeInfo.getProperties();
//                                        if (cartridgeInfoProps != null && cartridgeInfoProps.length > 0) {
//                                            lbDeploymentProperties.setProperties(combine(lbCartridgeInfo.getProperties(), new Property[]{property, loadBalancedServiceTypeProperty}));
//                                        } else {
//                                            lbDeploymentProperties.setProperties(new Property[]{property, loadBalancedServiceTypeProperty});
//                                        }
//
//                                        lbService.deploy(lbDeploymentProperties);
//
//                                        // persist
//                                        persist(lbService);
//                                    }
//                                }
//                            }
//
//                        } catch (Exception ex) {
//                            // we don't need to throw the error here.
//                            log.error(ex.getMessage(), ex);
//                        }
//
//
//                        lbRefProp.add(property);
//                        break;
//                    }
//                }
//            }
//        }

        LBDataContext lbDataCtxt = null;

        // get lb config reference
        LoadbalancerConfig lbConfig = cartridgeInfo.getLbConfig();
        if (lbConfig == null || lbConfig.getProperties() == null) {
            // no LB ref
            if (log.isDebugEnabled()) {
                log.debug("This Service does not require a load balancer. " + "[Service Name] " + type);
            }

        } else {
            // LB ref found, get relevant LB Context data
            lbDataCtxt = CartridgeSubscriptionUtils.getLoadBalancerDataContext(-1234, type, deploymentPolicyName, lbConfig);

            // deploy LB service cluster
            deployLBCluster(type, lbDataCtxt, tenantRange);
        }

        Service service = new MultiTenantService(type, autoscalingPolicyName, deploymentPolicyName, tenantId, cartridgeInfo, tenantRange, isPublic);

        Properties serviceClusterProperties = null;
        if (lbDataCtxt != null) {
            if (lbDataCtxt.getLoadBalancedServiceProperties() != null && !lbDataCtxt.getLoadBalancedServiceProperties().isEmpty()) {
                serviceClusterProperties = new Properties();
                serviceClusterProperties.setProperties(lbDataCtxt.getLoadBalancedServiceProperties().toArray(new Property[0]));
            }
        }

        // create
        service.create();

        //deploy the service
        service.deploy(serviceClusterProperties);

        // persist
        persist(service);

        return service;
    }

    private void deployLBCluster (String loadBalancedService, LBDataContext lbDataCtxt, String tenantRange) throws ADCException, UnregisteredCartridgeException {

        if (lbDataCtxt.getLbCategory() == null || lbDataCtxt.getLbCategory().equals(StratosConstants.NO_LOAD_BALANCER)) {
            // no load balancer required
            return;
        }

        LBService lbService = null;

        if (lbDataCtxt.getLbCategory().equals(StratosConstants.EXISTING_LOAD_BALANCERS)) {
            lbService = new ExistingLBService(lbDataCtxt.getLbCartridgeInfo().getType(), lbDataCtxt.getAutoscalePolicy(),
                    lbDataCtxt.getDeploymentPolicy(), -1234, lbDataCtxt.getLbCartridgeInfo(),
                    tenantRange, false);

        } else if (lbDataCtxt.getLbCategory().equals(StratosConstants.DEFAULT_LOAD_BALANCER)) {
            lbService = new DefaultLBService(lbDataCtxt.getLbCartridgeInfo().getType(), lbDataCtxt.getAutoscalePolicy(),
                    lbDataCtxt.getDeploymentPolicy(), -1234, lbDataCtxt.getLbCartridgeInfo(),
                    tenantRange, false);

        } else if (lbDataCtxt.getLbCategory().equals(StratosConstants.SERVICE_AWARE_LOAD_BALANCER)) {
            lbService = new ServiceAwareLBService(lbDataCtxt.getLbCartridgeInfo().getType(), lbDataCtxt.getAutoscalePolicy(),
                    lbDataCtxt.getDeploymentPolicy(), -1234, lbDataCtxt.getLbCartridgeInfo(),
                    tenantRange, false);
        }

        if (lbService == null) {
            throw new ADCException("The given Load Balancer category " + lbDataCtxt.getLbCategory() + " not found");
        }

        // Set the load balanced service type
        lbService.setLoadBalancedServiceType(loadBalancedService);

        // Set if the load balanced service is multi tenant or not
        //lbService.setLoadBalancedServiceMultiTenant(true); // TODO --- temp hack

        // set the relevant deployment policy
        //log.info(" ******* Setting Deployment Policy name : ------>  " + lbDataCtxt.getDeploymentPolicy());
        //loadBalancerCategory.setDeploymentPolicyName(lbDataCtxt.getDeploymentPolicy());

        Properties lbProperties = null;
        if (lbDataCtxt.getLbProperperties() != null && !lbDataCtxt.getLbProperperties().isEmpty())  {
            lbProperties = new Properties();
            lbProperties.setProperties(lbDataCtxt.getLbProperperties().toArray(new Property[0]));
        }

        // create service
        lbService.create();

        // add LB category to the payload
        if (lbService.getPayloadData() != null) {
            lbService.getPayloadData().add(CartridgeConstants.LB_CATEGORY, lbDataCtxt.getLbCategory());
        }

        // delpoy
        lbService.deploy(lbProperties);

        // persist
        persist(lbService);
    }

    private void persist (Service service) throws ADCException {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        try {
            dataInsertionAndRetrievalManager.persistService(service);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in persisting Service in PersistenceManager";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public Service getService (String type) throws ADCException {

        try {
            return new DataInsertionAndRetrievalManager().getService(type);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in getting Service for type " + type;
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public Collection<Service> getServices () throws ADCException {

        try {
            return new DataInsertionAndRetrievalManager().getServices();

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in getting deployed Services";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    public void undeployService (String type) throws ADCException, ServiceDoesNotExistException {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        // check if there are already created Subscriptions for this type
        Collection<CartridgeSubscription> cartridgeSubscriptions = dataInsertionAndRetrievalManager.getCartridgeSubscriptions(type);
        if (cartridgeSubscriptions != null) {
            if (!cartridgeSubscriptions.isEmpty()) {
                // can't undeploy; there are existing Subscriptions
                String errorMsg = "Cannot undeploy Service since existing Subscriptions are found";
                log.error(errorMsg);
                throw new ServiceDoesNotExistException(errorMsg, type);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("No subscriptions found for service type: " + type + " , proceeding with undeploying service");
        }

        Service service;
        try {
            service = dataInsertionAndRetrievalManager.getService(type);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in checking if Service is available is PersistenceManager";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }

        if (service == null) {
            String errorMsg = "No service found for type " + type;
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        // if service is found, undeploy
        try {
            service.undeploy();

        } catch (NotSubscribedException e) {
            String errorMsg = "Undeploying Service Cluster failed for " + type;
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }

        try {
            dataInsertionAndRetrievalManager.removeService(type);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in removing Service from PersistenceManager";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }
    }

    @SuppressWarnings("unused")
	private static Property[] combine (Property[] propertyArray1, Property[] propertyArray2) {

        int length = propertyArray1.length + propertyArray2.length;
        Property[] combinedProperties = new Property[length];
        System.arraycopy(propertyArray1, 0, combinedProperties, 0, propertyArray1.length);
        System.arraycopy(propertyArray2, 0, combinedProperties, propertyArray1.length, propertyArray2.length);

        return combinedProperties;
    }
}
