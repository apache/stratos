///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.apache.stratos.cloud.controller.application;
//
//import org.apache.stratos.cloud.controller.application.parser.DefaultApplicationParser;
//import org.apache.stratos.cloud.controller.exception.ApplicationDefinitionException;
//import org.apache.stratos.cloud.controller.interfaces.ApplicationParser;
//import org.apache.stratos.cloud.controller.pojo.Cartridge;
//import org.apache.stratos.cloud.controller.pojo.Dependencies;
//import org.apache.stratos.cloud.controller.pojo.ServiceGroup;
//import org.apache.stratos.cloud.controller.pojo.application.*;
//import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//public class CompositeApplicationParseTest {
//
//    private static FasterLookUpDataHolder dataHolder = null;
//
//    @BeforeClass
//    public static void setUpBeforeClass() {
//
//        dataHolder =  FasterLookUpDataHolder.getInstance();
//        // add cartridges
//        // add php cartridge
//        dataHolder.addCartridge(new Cartridge("php", "stratos.com", "php-provider" , "1.0.0", false));
//        // add tomcat cartridge
//        dataHolder.addCartridge(new Cartridge("tomcat", "stratos.com", "apache" , "1.0.0", false));
//        // add mysql cartridge
//        dataHolder.addCartridge(new Cartridge("mysql", "stratos.com", "apache" , "1.0.0", false));
//
//        // add groups
//        // add group1
//        ServiceGroup group1 = new ServiceGroup();
//        group1.setName("group1");
//        group1.setCartridges(new String[]{"mysql"});
//        Dependencies group1Dependencies = new Dependencies();
//        group1Dependencies.setKillBehaviour("terminate-none");
//        group1.setDependencies(group1Dependencies);
//        dataHolder.addServiceGroup(group1);
//        // add group2
//        ServiceGroup group2 = new ServiceGroup();
//        group2.setName("group2");
//        group2.setCartridges(new String[]{"php"});
//        group2.setSubGroups(new String[]{"group1"});
//        Dependencies group2Dependencies = new Dependencies();
//        group2Dependencies.setStartupOrders(new String[]{"group.group1,cartridge.php"});
//        group2Dependencies.setKillBehaviour("terminate-dependents");
//        group2.setDependencies(group2Dependencies);
//        dataHolder.addServiceGroup(group2);
//    }
//
//    @Test
//    public void testParseSimpleApplication () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleApp");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        simpleAppSubscribableContext.setType("php");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test(expected = ApplicationDefinitionException.class)
//    public void testParseSimpleApplicationWithNullAppId () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // no app id
//        simpleAppCtxt.setAlias("simpleAppAlias");
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test(expected = ApplicationDefinitionException.class)
//    public void testParseSimpleApplicationWithEmptyAppId () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // empty app id
//        simpleAppCtxt.setApplicationId("");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test(expected = ApplicationDefinitionException.class)
//    public void testParseSimpleApplicationWithInvalidCartridge() throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleInvalidApp");
//        simpleAppCtxt.setAlias("simpleInvalidAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        // give invalid cartridge type
//        simpleAppSubscribableContext.setType("php1");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test(expected = ApplicationDefinitionException.class)
//    public void testParseSimpleApplicationWithoutSubcriptionInformation() throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleInvalidApp");
//        simpleAppCtxt.setAlias("simpleInvalidAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        // give invalid cartridge type
//        simpleAppSubscribableContext.setType("php1");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // invalid Subscription information
//        SubscribableInfoContext simpleAppEmptySubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppEmptySubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test
//    public void testParseSimpleApplicationWithMultipleSubsriptions () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleInvalidApp");
//        simpleAppCtxt.setAlias("simpleInvalidAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        SubscribableContext simpleAppPhpSubscribableContext = new SubscribableContext();
//        simpleAppPhpSubscribableContext.setType("php");
//        simpleAppPhpSubscribableContext.setAlias("myphp");
//        SubscribableContext simpleAppMySqlSubscribableContext = new SubscribableContext();
//        simpleAppMySqlSubscribableContext.setType("mysql");
//        simpleAppMySqlSubscribableContext.setAlias("mysql1");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppPhpSubscribableContext,
//                simpleAppMySqlSubscribableContext});
//
//        DependencyContext simpleAppDependecyCtxt = new DependencyContext();
//        simpleAppDependecyCtxt.setTerminationBehaviour("terminate-dependents");
//        simpleAppDependecyCtxt.setStartupOrdersContexts(new String[]{"cartridge.mysql1,cartridge.myphp"});
//        simpleAppComponentCtxt.setDependencyContext(simpleAppDependecyCtxt);
//
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//
//        // subscribable information
//        SubscribableInfoContext simpleAppPhpSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppPhpSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppPhpSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppPhpSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppPhpSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppPhpSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppPhpSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppPhpSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        SubscribableInfoContext simpleAppMySqlSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppMySqlSubscribableInfoCtxt.setAlias("mysql1");
//        simpleAppMySqlSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_2");
//        simpleAppMySqlSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_2");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppPhpSubscribableInfoCtxt,
//                simpleAppMySqlSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test(expected = ApplicationDefinitionException.class)
//    public void testParseSimpleApplicationWithMultipleSubsriptionsInvalidStartupOrder () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleInvalidApp");
//        simpleAppCtxt.setAlias("simpleInvalidAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        SubscribableContext simpleAppPhpSubscribableContext = new SubscribableContext();
//        simpleAppPhpSubscribableContext.setType("php");
//        simpleAppPhpSubscribableContext.setAlias("myphp");
//        SubscribableContext simpleAppMySqlSubscribableContext = new SubscribableContext();
//        simpleAppMySqlSubscribableContext.setType("mysql");
//        simpleAppMySqlSubscribableContext.setAlias("mysql1");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppPhpSubscribableContext,
//                simpleAppMySqlSubscribableContext});
//
//        DependencyContext simpleAppDependecyCtxt = new DependencyContext();
//        simpleAppDependecyCtxt.setTerminationBehaviour("terminate-dependents");
//        // startup order is invalid, without prefix 'cartridge.' for mysql1
//        simpleAppDependecyCtxt.setStartupOrdersContexts(new String[]{"mysql1,cartridge.myphp"});
//        simpleAppComponentCtxt.setDependencyContext(simpleAppDependecyCtxt);
//
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//
//        // subscribable information
//        SubscribableInfoContext simpleAppPhpSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppPhpSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppPhpSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppPhpSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppPhpSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppPhpSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppPhpSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppPhpSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        SubscribableInfoContext simpleAppMySqlSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppMySqlSubscribableInfoCtxt.setAlias("mysql1");
//        simpleAppMySqlSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_2");
//        simpleAppMySqlSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_2");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppPhpSubscribableInfoCtxt,
//                simpleAppMySqlSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test
//    public void testAppWithOneCartridgeAndOneGroup () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleApp");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        // top level cartridges
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        simpleAppSubscribableContext.setType("php");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        // top level group
//        GroupContext simpleGroupContext = new GroupContext();
//        simpleGroupContext.setName("group1");
//        simpleGroupContext.setAlias("mygroup1");
//        simpleGroupContext.setAutoscalingPolicy("group_autoscaling_policy_1");
//        simpleGroupContext.setDeploymentPolicy("group_deployment_policy_1");
//        SubscribableContext groupSubscribableCtxt = new SubscribableContext();
//        groupSubscribableCtxt.setType("mysql");
//        groupSubscribableCtxt.setAlias("mygroup1mysql");
//        simpleGroupContext.setSubscribableContexts(new SubscribableContext[]{groupSubscribableCtxt});
//        simpleAppComponentCtxt.setGroupContexts(new GroupContext[]{simpleGroupContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        SubscribableInfoContext simpleAppGroupSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppGroupSubscribableInfoCtxt.setAlias("mygroup1mysql");
//        simpleAppGroupSubscribableInfoCtxt.setAutoscalingPolicy("mysql_autoscaling_policy");
//        simpleAppGroupSubscribableInfoCtxt.setDeploymentPolicy("mysql_deployment_policy");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt,
//                simpleAppGroupSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test (expected = ApplicationDefinitionException.class)
//    public void testAppWithOneCartridgeAndInvalidGroup () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleApp");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        // top level cartridges
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        simpleAppSubscribableContext.setType("php");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        // top level group
//        GroupContext simpleGroupContext = new GroupContext();
//        // invalid Group 'group11'
//        simpleGroupContext.setName("group11");
//        simpleGroupContext.setAlias("mygroup1");
//        simpleGroupContext.setAutoscalingPolicy("group_autoscaling_policy_1");
//        simpleGroupContext.setDeploymentPolicy("group_deployment_policy_1");
//        SubscribableContext groupSubscribableCtxt = new SubscribableContext();
//        groupSubscribableCtxt.setType("mysql");
//        groupSubscribableCtxt.setAlias("mygroup1mysql");
//        simpleGroupContext.setSubscribableContexts(new SubscribableContext[]{groupSubscribableCtxt});
//        simpleAppComponentCtxt.setGroupContexts(new GroupContext[]{simpleGroupContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        SubscribableInfoContext simpleAppGroupSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppGroupSubscribableInfoCtxt.setAlias("mygroup1mysql");
//        simpleAppGroupSubscribableInfoCtxt.setAutoscalingPolicy("mysql_autoscaling_policy");
//        simpleAppGroupSubscribableInfoCtxt.setDeploymentPolicy("mysql_deployment_policy");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt,
//                simpleAppGroupSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test
//    public void testAppWithOneCartridgeAndOneGroupWithDependencies () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleApp");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        // top level cartridges
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        simpleAppSubscribableContext.setType("php");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        // top level group
//        GroupContext simpleGroupContext = new GroupContext();
//        simpleGroupContext.setName("group1");
//        simpleGroupContext.setAlias("mygroup1");
//        simpleGroupContext.setAutoscalingPolicy("group_autoscaling_policy_1");
//        simpleGroupContext.setDeploymentPolicy("group_deployment_policy_1");
//        SubscribableContext groupSubscribableCtxt = new SubscribableContext();
//        groupSubscribableCtxt.setType("mysql");
//        groupSubscribableCtxt.setAlias("mygroup1mysql");
//        simpleGroupContext.setSubscribableContexts(new SubscribableContext[]{groupSubscribableCtxt});
//        simpleAppComponentCtxt.setGroupContexts(new GroupContext[]{simpleGroupContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // dependencies
//        DependencyContext simpleAppDependecyCtxt = new DependencyContext();
//        simpleAppDependecyCtxt.setTerminationBehaviour("terminate-dependents");
//        simpleAppDependecyCtxt.setStartupOrdersContexts(new String[]{"group.group1,cartridge.myphp"});
//        simpleAppComponentCtxt.setDependencyContext(simpleAppDependecyCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        SubscribableInfoContext simpleAppGroupSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppGroupSubscribableInfoCtxt.setAlias("mygroup1mysql");
//        simpleAppGroupSubscribableInfoCtxt.setAutoscalingPolicy("mysql_autoscaling_policy");
//        simpleAppGroupSubscribableInfoCtxt.setDeploymentPolicy("mysql_deployment_policy");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt,
//                simpleAppGroupSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test (expected = ApplicationDefinitionException.class)
//    public void testAppWithOneCartridgeAndOneGroupWithInvalidDependencies () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleApp");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        // top level cartridges
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        simpleAppSubscribableContext.setType("php");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        // top level group
//        GroupContext simpleGroupContext = new GroupContext();
//        simpleGroupContext.setName("group1");
//        simpleGroupContext.setAlias("mygroup1");
//        simpleGroupContext.setAutoscalingPolicy("group_autoscaling_policy_1");
//        simpleGroupContext.setDeploymentPolicy("group_deployment_policy_1");
//        SubscribableContext groupSubscribableCtxt = new SubscribableContext();
//        groupSubscribableCtxt.setType("mysql");
//        groupSubscribableCtxt.setAlias("mygroup1mysql");
//        simpleGroupContext.setSubscribableContexts(new SubscribableContext[]{groupSubscribableCtxt});
//        simpleAppComponentCtxt.setGroupContexts(new GroupContext[]{simpleGroupContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // dependencies
//        DependencyContext simpleAppDependecyCtxt = new DependencyContext();
//        simpleAppDependecyCtxt.setTerminationBehaviour("terminate-dependents");
//        // invalid startup order, starting with 'group1'
//        simpleAppDependecyCtxt.setStartupOrdersContexts(new String[]{"group1.group1,cartridge.myphp"});
//        simpleAppComponentCtxt.setDependencyContext(simpleAppDependecyCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        SubscribableInfoContext simpleAppGroupSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppGroupSubscribableInfoCtxt.setAlias("mygroup1mysql");
//        simpleAppGroupSubscribableInfoCtxt.setAutoscalingPolicy("mysql_autoscaling_policy");
//        simpleAppGroupSubscribableInfoCtxt.setDeploymentPolicy("mysql_deployment_policy");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt,
//                simpleAppGroupSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @Test (expected = ApplicationDefinitionException.class)
//    public void testAppWithOneCartridgeAndOneGroupWithNoSubcribableInfoForGroup () throws ApplicationDefinitionException {
//
//        ApplicationParser applicationParser = new DefaultApplicationParser();
//        ApplicationContext simpleAppCtxt = new ApplicationContext();
//        // app id
//        simpleAppCtxt.setApplicationId("simpleApp");
//        simpleAppCtxt.setAlias("simpleAppAlias");
//        // tenant info
//        simpleAppCtxt.setTenantId(-1234);
//        // components
//        ComponentContext simpleAppComponentCtxt = new ComponentContext();
//        // top level cartridges
//        SubscribableContext simpleAppSubscribableContext = new SubscribableContext();
//        simpleAppSubscribableContext.setType("php");
//        simpleAppSubscribableContext.setAlias("myphp");
//        simpleAppComponentCtxt.setSubscribableContexts(new SubscribableContext[]{simpleAppSubscribableContext});
//        // top level group
//        GroupContext simpleGroupContext = new GroupContext();
//        simpleGroupContext.setName("group1");
//        simpleGroupContext.setAlias("mygroup1");
//        simpleGroupContext.setAutoscalingPolicy("group_autoscaling_policy_1");
//        simpleGroupContext.setDeploymentPolicy("group_deployment_policy_1");
//        SubscribableContext groupSubscribableCtxt = new SubscribableContext();
//        groupSubscribableCtxt.setType("mysql");
//        groupSubscribableCtxt.setAlias("mygroup1mysql");
//        simpleGroupContext.setSubscribableContexts(new SubscribableContext[]{groupSubscribableCtxt});
//        simpleAppComponentCtxt.setGroupContexts(new GroupContext[]{simpleGroupContext});
//        simpleAppCtxt.setComponents(simpleAppComponentCtxt);
//        // subscribable information
//        SubscribableInfoContext simpleAppSubscribableInfoCtxt = new SubscribableInfoContext();
//        simpleAppSubscribableInfoCtxt.setAlias("myphp");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("deployment_policy_1");
//        simpleAppSubscribableInfoCtxt.setAutoscalingPolicy("autoscale_policy_1");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("www.mygit.com/myphp.git");
//        simpleAppSubscribableInfoCtxt.setPrivateRepo(true);
//        simpleAppSubscribableInfoCtxt.setRepoUsername("admin");
//        simpleAppSubscribableInfoCtxt.setRepoUrl("admin123");
//
//        simpleAppCtxt.setSubscribableInfoContext(new SubscribableInfoContext[]{simpleAppSubscribableInfoCtxt});
//
//        // parse
//        applicationParser.parse(simpleAppCtxt);
//    }
//
//    @AfterClass
//    public static void tearDownAfterClass() {
//
//        dataHolder = null;
//    }
//}
