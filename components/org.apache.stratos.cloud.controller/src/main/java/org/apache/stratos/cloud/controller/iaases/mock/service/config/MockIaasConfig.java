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

package org.apache.stratos.cloud.controller.iaases.mock.service.config;

/**
 * Mock iaas configuration.
 */
public class MockIaasConfig {
    private static final String MOCK_IAAS_CONFIG_FILE_NAME = "mock-iaas.xml";
    private static final String CARBON_HOME = "carbon.home";
    private static final String REPOSITORY_CONF = "/repository/conf/";

    private static volatile MockIaasConfig instance;

    private boolean enabled;
    private MockHealthStatisticsConfig mockHealthStatisticsConfig;
    
    public static MockIaasConfig getInstance() {
        if (instance == null) {
            synchronized (MockIaasConfig.class) {
                if (instance == null) {
                    String confPath = System.getProperty(CARBON_HOME) + REPOSITORY_CONF;
                    instance = MockIaasConfigParser.parse(confPath + MOCK_IAAS_CONFIG_FILE_NAME);
                }
            }
        }
        return instance;
    }

    MockIaasConfig() {
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setMockHealthStatisticsConfig(MockHealthStatisticsConfig mockHealthStatisticsConfig) {
        this.mockHealthStatisticsConfig = mockHealthStatisticsConfig;
    }

    public MockHealthStatisticsConfig getMockHealthStatisticsConfig() {
        return mockHealthStatisticsConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
