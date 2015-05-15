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

package org.apache.stratos.mock.iaas.config;

import org.apache.stratos.mock.iaas.statistics.generator.MockHealthStatisticsPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock health statistics configuration.
 */
public class MockHealthStatisticsConfig {

    private List<MockHealthStatisticsPattern> statisticsPatternList;

    /**
     * Default constructor
     */
    public MockHealthStatisticsConfig() {
        statisticsPatternList = new ArrayList<MockHealthStatisticsPattern>();
    }

    /**
     * Add statistics pattern
     *
     * @param statisticsPattern statistics pattern
     */
    public void addStatisticsPattern(MockHealthStatisticsPattern statisticsPattern) {
        statisticsPatternList.add(statisticsPattern);
    }

    /**
     * Get statistics patterns
     *
     * @return a list of statistics pattern objects
     */
    public List<MockHealthStatisticsPattern> getStatisticsPatterns() {
        return statisticsPatternList;
    }
}
