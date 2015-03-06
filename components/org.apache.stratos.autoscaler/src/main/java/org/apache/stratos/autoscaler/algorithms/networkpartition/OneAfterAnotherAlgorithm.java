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
package org.apache.stratos.autoscaler.algorithms.networkpartition;

import java.util.ArrayList;
import java.util.List;

import org.apache.stratos.autoscaler.algorithms.NetworkPartitionAlgorithm;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;

public class OneAfterAnotherAlgorithm implements NetworkPartitionAlgorithm{

	@Override
	public List<String> getNextNetworkPartitions(NetworkPartitionAlgorithmContext networkPartitionAlgorithmContext) {
		
		if (networkPartitionAlgorithmContext == null) {
			return null;
		}
		
		ApplicationPolicy applicationPolicy = networkPartitionAlgorithmContext.getApplicationPolicy();
		if (applicationPolicy == null) {
			return null;
		}
		
		String[] networkPartitions = applicationPolicy.getNetworkPartitions();
		if (networkPartitions == null || networkPartitions.length == 0) {
			return null;
		}
		
		int totalNetworkPartitions = networkPartitions.length;
		int currentPartitionIndex = networkPartitionAlgorithmContext.getCurrentNetworkPartitionIndex().intValue();
		if (currentPartitionIndex >= totalNetworkPartitions) {
			return null;
		}
		
		int selectedIndex = networkPartitionAlgorithmContext.getCurrentNetworkPartitionIndex().incrementAndGet();
		List<String> nextNetworkPartitions = new ArrayList<String>();
		nextNetworkPartitions.add(networkPartitions[selectedIndex-1]);
		
		return nextNetworkPartitions;
	}
}
