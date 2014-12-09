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

package org.apache.stratos.autoscaler.applications.parser;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationUtils;
import org.apache.stratos.autoscaler.applications.ClusterInformation;
import org.apache.stratos.autoscaler.applications.MTClusterInformation;
import org.apache.stratos.autoscaler.applications.STClusterInformation;
import org.apache.stratos.autoscaler.applications.payload.PayloadData;
import org.apache.stratos.autoscaler.applications.pojo.*;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.cartridge.CartridgeInformationException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeInfo;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.applications.DependencyOrder;
import org.apache.stratos.messaging.domain.applications.Group;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/**
 * Default implementation of the Application Parser. One Application should be processed by one
 * instance of the DefaultApplicationParser.
 */
public class DefaultApplicationParser implements ApplicationParser {

    private static Log log = LogFactory.getLog(DefaultApplicationParser.class);

    private Set<ApplicationClusterContext> applicationClusterContexts;
    private Map<String, Properties> aliasToProperties;
 	private Map<String, SubscribableInfoContext> subscribableInformation = new HashMap<String, SubscribableInfoContext>();

    public DefaultApplicationParser () {
        this.applicationClusterContexts = new HashSet<ApplicationClusterContext>();
        this.setAliasToProperties(new HashMap<String, Properties>());

    }

    @Override
    public Application parse(Object obj) throws ApplicationDefinitionException {

        ApplicationContext applicationCtxt = (ApplicationContext) obj;

        if (applicationCtxt == null) {
            handleError("Invalid Composite Application Definition");
        }

        assert applicationCtxt != null;
        if (applicationCtxt.getAlias() == null || applicationCtxt.getAlias().isEmpty()) {
            handleError("Invalid alias specified");
        }

        if (applicationCtxt.getApplicationId() == null || applicationCtxt.getApplicationId().isEmpty()) {
            handleError("Invalid Composite App id specified");
        }

        // get the Subscribables Information
        Map<String, SubscribableInfoContext> subscribablesInfo = getSubscribableInformation(applicationCtxt);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, SubscribableInfoContext>> subscribableInfoCtxtEntries = subscribablesInfo.entrySet();
            log.debug("Defined Subscribable Information: [ ");
            for (Map.Entry<String, SubscribableInfoContext> subscribableInfoCtxtEntry : subscribableInfoCtxtEntries) {
                log.debug("Subscribable Information alias: " + subscribableInfoCtxtEntry.getKey());
            }
            log.debug(" ]");
        }

        if (subscribablesInfo == null) {
            handleError("Invalid Composite Application Definition, no Subscribable Information specified");
        }

        return buildCompositeAppStructure (applicationCtxt, subscribablesInfo);
    }

    @Override
    public Set<ApplicationClusterContext> getApplicationClusterContexts() throws ApplicationDefinitionException {
        return applicationClusterContexts;
    }

   
	private Map<String, SubscribableInfoContext> getSubscribableInfo(GroupContext[] groupContexts) throws
    		ApplicationDefinitionException {
		if(groupContexts != null) {
		for (GroupContext groupContext : groupContexts) {
			if (groupContext.getGroupContexts() != null) {
				getSubscribableInfo(groupContext.getGroupContexts());
			} else {
				CartridgeContext[] cartridgeContexts = groupContext.getCartridgeContexts();
				for (CartridgeContext cartridgeContext : cartridgeContexts) {
					
					 if (StringUtils.isEmpty(cartridgeContext.getSubscribableInfoContext().getAlias()) ||
		                        !ApplicationUtils.isAliasValid(cartridgeContext.getSubscribableInfoContext().getAlias())) {
		                    handleError("Invalid alias specified for Subscribable Information Obj: [ " + 
		                        cartridgeContext.getSubscribableInfoContext().getAlias() + " ]");
		                }
	                
					// check if a group is already defined under the same alias
	                if(subscribableInformation.get(cartridgeContext.getSubscribableInfoContext().getAlias()) != null) {
	                    // a group with same alias already exists, can't continue
	                    handleError("A Subscribable Info obj with alias " + cartridgeContext.getSubscribableInfoContext().getAlias() + " already exists");
	                }
					subscribableInformation.put(cartridgeContext.getSubscribableInfoContext().getAlias(), 
							cartridgeContext.getSubscribableInfoContext());
					if (log.isDebugEnabled()) {
	                    log.debug("Added Subcribables Info obj [ " +
	                    		cartridgeContext.getSubscribableInfoContext().getAlias() + " ] to map [cartridge alias -> Subscribable Information]");
	                }
                }
			}
		  }
		}
		return subscribableInformation;
	}
    
    /**
     * Extract Subscription Information from the Application Definition
     *
     * @param appCtxt ApplicationContext object with Application information
     * @return Map [cartridge alias -> Group]
     *
     * @throws ApplicationDefinitionException if the Subscription information is invalid
     */
    private Map<String, SubscribableInfoContext> getSubscribableInformation (ApplicationContext appCtxt) throws
            ApplicationDefinitionException {

        return getSubscribableInfo(appCtxt.getComponents().getGroupContexts());
    }


    /**
     * Builds the Application structure
     *
     * @param appCtxt ApplicationContext object with Application information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @return Application Application object denoting the Application structure
     *
     * @throws ApplicationDefinitionException If an error occurs in building the Application structure
     */
    private Application buildCompositeAppStructure (ApplicationContext appCtxt,
                                                    Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Application application = new Application(appCtxt.getApplicationId());

        // set tenant related information
        application.setTenantId(appCtxt.getTenantId());
        application.setTenantDomain(appCtxt.getTenantDomain());
        application.setTenantAdminUserName(appCtxt.getTeantAdminUsername());

        // following keeps track of all Clusters created for this application
        Map<String, ClusterDataHolder> clusterDataMap;

        if (appCtxt.getComponents() != null) {
            // get top level Subscribables
            if (appCtxt.getComponents().getCartridgeContexts() != null) {                
                clusterDataMap = parseLeafLevelSubscriptions(appCtxt.getApplicationId(), appCtxt.getTenantId(),
                        application.getKey(), null, Arrays.asList(appCtxt.getComponents().getCartridgeContexts()));                
                application.setClusterData(clusterDataMap);
            }

            // get Groups
            if (appCtxt.getComponents().getGroupContexts() != null) {
                application.setGroups(parseGroups(appCtxt.getApplicationId(), appCtxt.getTenantId(),
                        application.getKey(), Arrays.asList(appCtxt.getComponents().getGroupContexts()),
                        subscribableInfoCtxts));
            }

            // get top level Dependency definitions
            if (appCtxt.getComponents().getDependencyContext() != null) {
                DependencyOrder appDependencyOrder = new DependencyOrder();
                String [] startupOrders = appCtxt.getComponents().getDependencyContext().getStartupOrdersContexts();
                if (startupOrders != null) {
                	if (log.isDebugEnabled()) {
                    	log.debug("parsing application ... buildCompositeAppStructure: startupOrders != null for app alias: " +
                    				appCtxt.getAlias() + " #: " + startupOrders.length);
                    }
                    appDependencyOrder.setStartupOrders(ParserUtils.convert(startupOrders));
                } else {
                	if (log.isDebugEnabled()) {
                    	log.debug("parsing application ... buildCompositeAppStructure: startupOrders == null for app alias: " + appCtxt.getAlias());
                    }
                }
                String terminationBehavior = appCtxt.getComponents().getDependencyContext().getTerminationBehaviour();
                validateTerminationBehavior(terminationBehavior);
                appDependencyOrder.setTerminationBehaviour(terminationBehavior);

                application.setDependencyOrder(appDependencyOrder);
            }
        }

        String alias;
        Properties properties = new Properties();
        for (SubscribableInfoContext value : subscribableInfoCtxts.values()) {
            alias = value.getAlias();
            String username = value.getRepoUsername();
            String password = value.getRepoPassword();
            String repoUrl = value.getRepoUrl();
            List<Property> propertyList = new ArrayList<Property>();
            if (StringUtils.isNotEmpty(username)) {
                Property property = new Property();
                property.setName("REPO_USERNAME");
                property.setValue(username);
                propertyList.add(property);
            }

            if (StringUtils.isNotEmpty(password)) {
                String encryptedPassword = encryptPassword(password, application.getKey());
                Property property = new Property();
                property.setName("REPO_PASSWORD");
                property.setValue(encryptedPassword);
                //properties.addProperties(property);
                propertyList.add(property);

            }

            if (StringUtils.isNotEmpty(repoUrl)) {
                Property property = new Property();
                property.setName("REPO_URL");
                property.setValue(repoUrl);
                //properties.addProperties(property);
                propertyList.add(property);
            }
            if(propertyList.size() > 0 ) {
                Property[] properties1 = new Property[propertyList.size()];
                properties.setProperties(propertyList.toArray(properties1));
                this.addProperties(alias, properties);
            }

        }


        log.info("Application with id " + appCtxt.getApplicationId() + " parsed successfully");

        return application;
    }

    
    /**
     * 
     * Parse Subscription Information
     * 
     * @param appId Application id
     * @param tenantId Tenant id of tenant which deployed the Application
     * @param key Generated key for the Application
     * @param groupName Group name (if relevant)
     * @param cartridgeContextList cartridgeContextList
     * @return Map [subscription alias -> ClusterDataHolder]
     * 
     * @throws ApplicationDefinitionException
     */
    private Map<String, ClusterDataHolder> parseLeafLevelSubscriptions(
    		String appId, int tenantId, String key, String groupName,
            List<CartridgeContext> cartridgeContextList) throws ApplicationDefinitionException {
    	
    	 Map<String, ClusterDataHolder> clusterDataMap = new HashMap<String, ClusterDataHolder>();

    	 for (CartridgeContext cartridgeContext : cartridgeContextList) {
	        
    		 String cartridgeType = cartridgeContext.getType();
    		 String subscriptionAlias = cartridgeContext.getSubscribableInfoContext().getAlias();
    		 
    		 // check if a cartridgeInfo with relevant type is already deployed. else, can't continue
             CartridgeInfo cartridgeInfo =  getCartridge(cartridgeType);
             if (cartridgeInfo == null) {
                 handleError("No deployed Cartridge found with type [ " + cartridgeType +
                         " ] for Composite Application");
             }
    		 
    		// get hostname and cluster id
             ClusterInformation clusterInfo;
             assert cartridgeInfo != null;
             if (cartridgeInfo.getMultiTenant()) {
                 clusterInfo = new MTClusterInformation();
             } else {
                 clusterInfo = new STClusterInformation();
             }

             String hostname = clusterInfo.getHostName(subscriptionAlias, cartridgeInfo.getHostName());
             String clusterId = clusterInfo.getClusterId(subscriptionAlias, cartridgeType);

             // create and collect this cluster's information
             ApplicationClusterContext appClusterCtxt = createApplicationClusterContext(appId, groupName, cartridgeInfo,
                     key, tenantId, cartridgeContext.getSubscribableInfoContext().getRepoUrl(), subscriptionAlias,
                     clusterId, hostname, cartridgeContext.getSubscribableInfoContext().getDeploymentPolicy(), false, 
                     cartridgeContext.getSubscribableInfoContext().getDependencyAliases(), 
                     cartridgeContext.getSubscribableInfoContext().getProperties());

             appClusterCtxt.setAutoscalePolicyName(cartridgeContext.getSubscribableInfoContext().getAutoscalingPolicy());
             appClusterCtxt.setProperties(cartridgeContext.getSubscribableInfoContext().getProperties());
             this.applicationClusterContexts.add(appClusterCtxt);

             // add relevant information to the map
             ClusterDataHolder clusterDataHolder = new ClusterDataHolder(cartridgeType, clusterId);
             clusterDataHolder.setMinInstances(cartridgeContext.getCartridgeMin());
             clusterDataHolder.setMaxInstances(cartridgeContext.getCartridgeMax());
             clusterDataMap.put(subscriptionAlias, clusterDataHolder);
    		 
        }
    	 

         return clusterDataMap;
    }

	/**
     * Validates terminationBehavior. The terminationBehavior should be one of the following:
     *      1. terminate-none
     *      2. terminate-dependents
     *      3. terminate-all
     *
     * @throws ApplicationDefinitionException if terminationBehavior is different to what is
     * listed above
     */
    private static void validateTerminationBehavior (String terminationBehavior) throws ApplicationDefinitionException {

        if (!(terminationBehavior == null || "terminate-none".equals(terminationBehavior) ||
                "terminate-dependents".equals(terminationBehavior) || "terminate-all".equals(terminationBehavior))) {
            throw new ApplicationDefinitionException("Invalid Termination Behaviour specified: [ " +
                    terminationBehavior + " ], should be one of 'terminate-none', 'terminate-dependents', " +
                    " 'terminate-all' ");
        }
    }

    /**
     * Parse Group information
     *
     * @param appId Application id
     * @param tenantId tenant id of tenant which deployed the Application
     * @param key Generated key for the Application
     * @param groupCtxts  Group information
     * @param subscribableInformation Subscribable Information
     * @return Map [alias -> Group]
     *
     * @throws ApplicationDefinitionException if an error occurs in parsing Group Information
     */
    private Map<String, Group> parseGroups (String appId, int tenantId, String key, List<GroupContext> groupCtxts,
                                           Map<String, SubscribableInfoContext> subscribableInformation)
            throws ApplicationDefinitionException {

        Map<String, Group> groupAliasToGroup = new HashMap<String, Group>();
        
        for (GroupContext groupCtxt : groupCtxts) {
        	ServiceGroup serviceGroup  = getServiceGroup(groupCtxt.getName());        	
            Group group = parseGroup(appId, tenantId, key, groupCtxt, subscribableInformation, serviceGroup);
            groupAliasToGroup.put(group.getAlias(), group);
        }

        Set<Group> nestedGroups = new HashSet<Group>();
        getNestedGroupContexts(nestedGroups, groupAliasToGroup.values());
        filterDuplicatedGroupContexts(groupAliasToGroup.values(), nestedGroups);

        return groupAliasToGroup;
    }

    /**
     * Extracts nested Group information recursively
     *
     * @param nestedGroups Nested Groups set to be populated recursively
     * @param groups Collection of Groups
     */
    private void getNestedGroupContexts (Set<Group> nestedGroups, Collection<Group> groups) {

        if (groups != null) {
            for (Group group : groups) {
                if (group.getGroups() != null) {
                    nestedGroups.addAll(group.getGroups());
                    getNestedGroupContexts(nestedGroups, group.getGroups());
                }
            }
        }
    }

    /**
     * Filters duplicated Groups from top level
     *
     * @param topLevelGroups Top level Groups
     * @param nestedGroups nested Groups
     */
    private void filterDuplicatedGroupContexts (Collection<Group> topLevelGroups, Set<Group> nestedGroups) {

        for (Group nestedGroup : nestedGroups) {
            filterNestedGroupFromTopLevel(topLevelGroups, nestedGroup);
        }
    }

    private void filterNestedGroupFromTopLevel (Collection<Group> topLevelGroups, Group nestedGroup) {

        Iterator<Group> parentIterator = topLevelGroups.iterator();
        while (parentIterator.hasNext()) {
            Group parentGroup = parentIterator.next();
            // if there is an exactly similar nested Group Context and a top level Group Context
            // it implies that they are duplicates. Should be removed from top level.
            if (parentGroup.equals(nestedGroup)) {
                parentIterator.remove();
            }
        }
    }

    /**
     * Parses an individual Group
     *
     * @param appId Application id
     * @param tenantId tenant id of tenant which deployed the Application
     * @param key Generated key for the Application
     * @param groupCtxt Group definition information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @return  Group object
     *
     * @throws ApplicationDefinitionException if unable to parse
     */
    private Group parseGroup (String appId, int tenantId, String key, GroupContext groupCtxt,
                             Map<String, SubscribableInfoContext> subscribableInfoCtxts,
                             ServiceGroup serviceGroup)
            throws ApplicationDefinitionException {

        Group group = new Group(appId, groupCtxt.getName(), groupCtxt.getAlias());
        group.setGroupScalingEnabled(isGroupScalingEnabled(groupCtxt.getName(),serviceGroup));
        group.setGroupMinInstances(groupCtxt.getGroupMinInstances());
        group.setGroupMaxInstances(groupCtxt.getGroupMaxInstances());
        group.setGroupScalingEnabled(groupCtxt.isGroupScalingEnabled());
        DependencyOrder dependencyOrder = new DependencyOrder();
        // create the Dependency Ordering
        String []  startupOrders = getStartupOrderForGroup(groupCtxt.getName(),serviceGroup);
        if (startupOrders != null) {
            dependencyOrder.setStartupOrders(ParserUtils.convert(startupOrders, groupCtxt));
        }
        dependencyOrder.setTerminationBehaviour(getKillbehaviour(groupCtxt.getName(),serviceGroup));
        group.setDependencyOrder(dependencyOrder);

        Map<String, ClusterDataHolder> clusterDataMap;

        // get group level CartridgeContexts
        if (groupCtxt.getCartridgeContexts() != null) {
            clusterDataMap = parseLeafLevelSubscriptions(appId, tenantId, key, groupCtxt.getName(),
                    Arrays.asList(groupCtxt.getCartridgeContexts()));
            group.setClusterData(clusterDataMap);
        }

        // get nested groups
        if (groupCtxt.getGroupContexts() != null) {
            Map<String, Group> nestedGroups = new HashMap<String, Group>();
            // check sub groups
            for (GroupContext subGroupCtxt : groupCtxt.getGroupContexts()) {
                // get the complete Group Definition                
				if (subGroupCtxt != null) {
					Group nestedGroup = parseGroup(appId, tenantId, key,
					        subGroupCtxt, subscribableInfoCtxts,
					        serviceGroup);
					nestedGroups.put(nestedGroup.getAlias(), nestedGroup);
				}
            }

            group.setGroups(nestedGroups);
        }

        return group;
    }

    /**
     * Find the startup order
     *
     * @param groupContext GroupContext with Group defintion information
     * @return Set of Startup Orders which are defined in the Group
     *
     * @throws ApplicationDefinitionException
     */
    private String [] getStartupOrderForGroup(String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedServiceGroup = getNestedServiceGroup(serviceGroupName, serviceGroup);

        if (nestedServiceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }
        
        if (log.isDebugEnabled()) {
        	log.debug("parsing application ... getStartupOrderForGroup: " + serviceGroupName);
        }

        assert nestedServiceGroup != null;
        if (nestedServiceGroup.getDependencies() != null) {
        	if (log.isDebugEnabled()) {
            	log.debug("parsing application ... getStartupOrderForGroup: dependencies != null " );
            }
            if (nestedServiceGroup.getDependencies().getStartupOrders() != null) {
            	
            	String [] startupOrders = nestedServiceGroup.getDependencies().getStartupOrders();
            	if (log.isDebugEnabled()) {
                	log.debug("parsing application ... getStartupOrderForGroup: startupOrders != null # of: " +  startupOrders.length);
                }
                return startupOrders;
            }
        }

        return null;
    }

    /**
     * Get kill behaviour related to a Group
     *
     * @param serviceGroupName Group name
     * @return String indicating the kill behavior
     *
     * @throws ApplicationDefinitionException if an error occurs
     */
    private String getKillbehaviour(String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedServiceGroup = getNestedServiceGroup(serviceGroupName,serviceGroup);

        if (nestedServiceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        assert nestedServiceGroup != null;
        if (nestedServiceGroup.getDependencies() != null) {
            return nestedServiceGroup.getDependencies().getTerminationBehaviour();
        }

        return null;

    }

    /**
     * Checks if group scaling is enabled for Service Group with name serviceGroupName
     *
     * @param serviceGroupName name of the Service Group
     * @return true if group scaling is enabled, else false
     * @throws ApplicationDefinitionException if no Service Group found for the given serviceGroupName
     */
    private boolean isGroupScalingEnabled (String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedGroup = getNestedServiceGroup(serviceGroupName, serviceGroup);

        if (nestedGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);        	
        }

        return nestedGroup.isGroupscalingEnabled();
    }
    
    private ServiceGroup getNestedServiceGroup (String serviceGroupName, ServiceGroup serviceGroup) {
    	if(serviceGroup.getName().equals(serviceGroupName)) {
    		return serviceGroup;
    	} else if (serviceGroup.getGroups() != null) {
    		ServiceGroup[] groups = serviceGroup.getGroups();
    		for (ServiceGroup sg : groups) {
    			return getNestedServiceGroup(serviceGroupName, sg);
            }
    	}
    	return null;
    	
    }

    /**
     * Retrieves deployed Service Group instance
     *
     * @param serviceGroupName name of the Service Group
     * @return ServiceGroup instance if exists
     * @throws ApplicationDefinitionException if no Service Group found for the given serviceGroupName
     */
    private ServiceGroup getServiceGroup (String serviceGroupName) throws ApplicationDefinitionException {

        try {
            return RegistryManager.getInstance().getServiceGroup(serviceGroupName);
        } catch (Exception e) {
            String errorMsg = "Error in getting Service Group Definition for [ " + serviceGroupName +
                    " ] from Registry";
            log.error(errorMsg, e);
            throw new ApplicationDefinitionException(errorMsg, e);
        }
    }

    
    /**
     * Creates a ApplicationClusterContext object to keep information related to a Cluster in this Application
     *
     * @param appId Application id
     * @param groupName Group name
     * @param cartridgeInfo Cartridge information
     * @param subscriptionKey Generated key for the Application
     * @param tenantId Tenant Id of the tenant which deployed the Application
     * @param repoUrl Repository URL
     * @param alias alias specified for this Subscribable in the Application Definition
     * @param clusterId Cluster id
     * @param hostname Hostname
     * @param deploymentPolicy Deployment policy used
     * @param isLB if this cluster is an LB
     * @return ApplicationClusterContext object with relevant information
     *
     * @throws ApplicationDefinitionException If any error occurs
     */
    private ApplicationClusterContext createApplicationClusterContext (String appId, String groupName, CartridgeInfo cartridgeInfo,
                                                                       String subscriptionKey, int tenantId, String repoUrl,
                                                                       String alias, String clusterId, String hostname,
                                                                       String deploymentPolicy, boolean isLB, String[] dependencyAliases, Properties properties)
            throws ApplicationDefinitionException {

        // Create text payload
        PayloadData payloadData = ApplicationUtils.createPayload(appId, groupName, cartridgeInfo, subscriptionKey, tenantId, clusterId,
                hostname, repoUrl, alias, null, dependencyAliases, properties);
        //TOD payloadData.add("TOKEN", createToken(appId));
        String textPayload = payloadData.toString();

        return new ApplicationClusterContext(cartridgeInfo.getType(), clusterId, hostname, textPayload, deploymentPolicy, isLB);
    }

    /*
    public String  createToken(String appid) throws AutoScalerException {
        String token = null;
        String ouathAppName = appid + Math.random();
        String serviceProviderName = ouathAppName;

        try {
            oAuthAdminServiceClient.getServiceClient().registerOauthApplication(ouathAppName);
        } catch (RemoteException e) {
            throw new AutoScalerException(e);
        } catch (OAuthAdminServiceException e) {
            throw new AutoScalerException(e);
        }
        try {
            token = IdentityApplicationManagementServiceClient.getServiceClient().createServiceProvider(ouathAppName, serviceProviderName, appid);
        } catch (RemoteException e) {
            throw new AutoScalerException(e);
        } catch (OAuthAdminServiceException e) {
            e.printStackTrace();
        } catch (OAuthProblemException e) {
            throw new AutoScalerException(e);
        } catch (OAuthSystemException e) {
            throw new AutoScalerException(e);
        }

        return token;
    }
    */
    private CartridgeInfo getCartridge (String cartridgeType) throws ApplicationDefinitionException {

        try {
            return CloudControllerClient.getInstance().getCartrdgeInformation(cartridgeType);
        } catch (CartridgeInformationException e) {
            throw new ApplicationDefinitionException(e);
        }
    }

    private void handleError (String errorMsg) throws ApplicationDefinitionException {
        log.error(errorMsg);
        throw new ApplicationDefinitionException(errorMsg);
    }

    public Map<String, Properties> getAliasToProperties() {
        return aliasToProperties;
    }

    public void setAliasToProperties(Map<String, Properties> aliasToProperties) {
        this.aliasToProperties = aliasToProperties;
    }
    public void addProperties(String alias, Properties properties) {
        this.getAliasToProperties().put(alias, properties);
    }

    public static String encryptPassword(String repoUserPassword, String secKey) {
        String encryptPassword = "";
        SecretKey key;
        Cipher cipher;
        Base64 coder;
        key = new SecretKeySpec(secKey.getBytes(), "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
            coder = new Base64();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherText = cipher.doFinal(repoUserPassword.getBytes());
            encryptPassword = new String(coder.encode(cipherText));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptPassword;
    }
}
