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
package org.apache.carbon.throttling.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.stratos.throttling.manager.conf.ThrottlingConfiguration;
import org.apache.stratos.throttling.manager.tasks.Task;
import org.apache.stratos.throttling.manager.utils.Util;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistryService;
import org.wso2.carbon.registry.core.jdbc.InMemoryEmbeddedRegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.apache.stratos.common.constants.StratosConstants;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDataAccessManager;

public class BaseTestCase extends TestCase {

    protected static RegistryContext ctx;
    protected EmbeddedRegistryService registryService;
    protected Registry registry; // an admin registry
    protected RealmService realmService;

    public void setUp() throws Exception {
        if (System.getProperty("carbon.home") == null) {
            File file = new File("src/test/resources/carbon-home");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
        }

        InputStream is;
        try {
            is = new FileInputStream("src/test/resources/registry.xml");
        } catch (Exception e) {
            is = null;
        }
        /*
         * RealmService realmService = new InMemoryRealmService();
         * RegistryContext registryContext = RegistryContext.getBaseInstance(is,
         * realmService); registryContext.setSetup(true);
         * registryContext.selectDBConfig("h2-db");
         */

        registryService = new InMemoryEmbeddedRegistryService(is);
        Util.setRegistryService(registryService);
        org.apache.stratos.usage.util.Util.setRegistryService(registryService);
        org.apache.stratos.usage.agent.util.Util.initializeAllListeners();
//    TODO    org.apache.stratos.usage.agent.util.Util.setRegistryService(registryService);

        realmService = RegistryContext.getBaseInstance().getRealmService();
        Util.setRealmService(realmService);
        DataSource registryDataSource =
                ((JDBCDataAccessManager) RegistryContext.getBaseInstance().getDataAccessManager())
                        .getDataSource();
        UserRegistry superTenantGovernanceRegistry = registryService.getGovernanceSystemRegistry();
//   TODO      Util.setTenantUsageRetriever(new TenantUsageRetriever(registryDataSource,
//                superTenantGovernanceRegistry, registryService));

        /*RuleServerManager ruleServerManager = new RuleServerManager();
        RuleServerConfiguration configuration =
                new RuleServerConfiguration(new JSR94BackendRuntimeFactory());
        ruleServerManager.init(configuration);
        Util.setRuleEngineConfigService(ruleServerManager);
        */
    }

    public Task getThrottlingTask(String configFile, String ruleFile) throws Exception {
        saveTrottlingRules(ruleFile);
        ThrottlingConfiguration throttlingConfiguration = new ThrottlingConfiguration(configFile);
        List<Task> throttlingTasks = throttlingConfiguration.getThrottlingTasks();
        return throttlingTasks.get(0);
    }
    
    public void saveTrottlingRules(String throttlingRuleFile) throws Exception {
        UserRegistry systemRegistry = registryService.getGovernanceSystemRegistry();

        byte[] content = CarbonUtils.getBytesFromFile(new File(throttlingRuleFile));
        Resource ruleResource = systemRegistry.newResource();
        ruleResource.setContent(content);
        systemRegistry.put(StratosConstants.THROTTLING_RULES_PATH, ruleResource);
    }

    public void putEmptyResource() throws Exception {
        Resource r = (Resource) registry.newResource();
        registry.put("/empty", r);
    }

}
