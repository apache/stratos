/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.nginx.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;

import java.io.IOException;

/**
 * Nginx statistics reader.
 */
public class NginxStatisticsReader implements LoadBalancerStatisticsReader {

    private static final Log log = LogFactory.getLog(NginxStatisticsReader.class);

    private String scriptsPath;
    private String statsSocketFilePath;
    private TopologyProvider topologyProvider;

    public NginxStatisticsReader(TopologyProvider topologyProvider) {
        this.scriptsPath = NginxContext.getInstance().getScriptsPath();
        this.statsSocketFilePath = NginxContext.getInstance().getStatsSocketFilePath();
        this.topologyProvider = topologyProvider;
    }

    @Override
    public int getInFlightRequestCount(String clusterId) {
        return 0;
    }

    @Override
    public int getServedRequestCount(String clusterId) {
        return 0;
    }

    @Override
    public int getActiveInstancesCount(Cluster cluster) {
        return 0;
    }
}
