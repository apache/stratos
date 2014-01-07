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
import org.apache.stratos.autoscaler.stub.AutoScalerServiceStub;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Property;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A test case which tests partitions deployment
 */
public class PartitionDeploymentTestCase {
	
	private static final Log log = LogFactory.getLog(PartitionDeploymentTestCase.class);
	
	  @Test(groups = {"stratos.autoscaler"})
	    public void deploy() throws Exception {
		  	log.info("Deploying partition...");
			AutoScalerServiceStub stub = new AutoScalerServiceStub("https://localhost:9443/services/AutoScalerService");
			Partition partition = new Partition();
			partition.setId("P1");
			partition.setPartitionMax(3);
			partition.setPartitionMin(1);
			Properties properties = new Properties();
			Property region = new Property();
			region.setName("region");
			region.setValue("ap-southeast-1");
			properties.setProperties(new Property[]{region});
			partition.setProperties(properties);
			partition.setProvider("ec2");
			
			boolean deployed = stub.addPartition(partition);
			Assert.assertTrue(deployed, "Cannot deploy partition");
			Partition[] allAvailablePartitions = stub.getAllAvailablePartitions();
			
			//FIXME: getting deployed partition using stub.getPartition rather than
			// iterates the partitions
			for (Partition part : allAvailablePartitions) {
				if(partition.getId().equals(part.getId()))
					return;
			}
			Assert.fail("Cannot get deployed partition");
			
			//Partition deployedPartition = stub.getPartition(partition.getId());
			//Assert.assertNotNull(deployedPartition, "Cannot get deployed partition");
	  }

}
