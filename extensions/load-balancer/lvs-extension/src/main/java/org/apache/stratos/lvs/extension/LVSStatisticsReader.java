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

package org.apache.stratos.lvs.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.util.CommandUtils;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;

import java.io.IOException;

/**
 * LVS statistics reader.
 */
public class LVSStatisticsReader implements LoadBalancerStatisticsReader {

	private static final Log log = LogFactory.getLog(LVSStatisticsReader.class);
	private static final String IPVSADM_STATS_COMMAND = "sudo ipvsadm -L -n |grep Route";
			//"ipvsadm -l --stats | grep TCP"

	private TopologyProvider topologyProvider;
	private String clusterInstanceId;

	public LVSStatisticsReader(TopologyProvider topologyProvider) {
		this.topologyProvider = topologyProvider;
	}

	@Override public String getClusterInstanceId() {
		return clusterInstanceId;
	}

	@Override
	public int getInFlightRequestCount(String clusterId) {
		String output;
		String[] array;
		int totalWeight = 0, weight;

		// Command to get the statistics : sudo ipvsadm -l --stats | grep TCP
		String[] command = { "/bin/sh", "-c", IPVSADM_STATS_COMMAND };
		try {
			output = CommandUtils.executeCommand(command);
			if ((output != null) && (output.length() > 0)) {
				array = output.split("\n");
				for (int i = 0; i < array.length; i++) {
					weight = Integer.parseInt(array[i].substring(46, 50).toString().trim());
					totalWeight += weight;
				}
			}
		} catch (IOException e) {
			if (log.isErrorEnabled()) {
				log.error(e);
			}
		}
		if (log.isInfoEnabled()) {
			log.info(String.format("Cluster weight found: [cluster] %s [weight] %d", clusterId,
			                       totalWeight));

		}
		return totalWeight;
	}

}
