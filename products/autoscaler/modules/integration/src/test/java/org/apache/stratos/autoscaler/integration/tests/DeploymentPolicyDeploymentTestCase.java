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

package org.apache.stratos.autoscaler.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceStub;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A test case which tests Autoscaling policy deployment
 */
public class DeploymentPolicyDeploymentTestCase {
	
	private static final Log log = LogFactory.getLog(DeploymentPolicyDeploymentTestCase.class);

	@Test(groups = { "stratos.autoscaler" })
	public void deploy() throws Exception {
	  	log.info("Deploying deployment policy...");
		AutoScalerServiceStub stub = new AutoScalerServiceStub("https://localhost:9443/services/AutoScalerService");
		DeploymentPolicy deploymentPolicy = new DeploymentPolicy();
		deploymentPolicy.setId("isuruh-ec2");
		PartitionGroup group = new PartitionGroup();
		group.setId("ec2");
		group.setPartitionAlgo("one-after-another");
		Partition partition =new Partition();
		partition.setId("P1");
		partition.setPartitionMin(1);
		partition.setPartitionMax(3);
		group.setPartitions(new Partition[]{partition});
		deploymentPolicy.setPartitionGroups(new PartitionGroup[]{group});
		
		boolean deployed = stub.addDeploymentPolicy(deploymentPolicy);
		Assert.assertTrue(deployed, "Cannot deploy deployment-policy");
		
		DeploymentPolicy policy = stub.getDeploymentPolicy(deploymentPolicy.getId());
		Assert.assertNotNull(policy, "Cannot get deployed deployment-policy");
	}
}
