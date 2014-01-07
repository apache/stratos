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
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.LoadThresholds;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.policy.model.RequestsInFlight;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceStub;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A test case which tests Autoscaling policy deployment
 */
public class AutoscalingPolicyDeploymentTestCase {
	
	private static final Log log = LogFactory.getLog(AutoscalingPolicyDeploymentTestCase.class);
	
	  @Test(groups = {"stratos.autoscaler"})
	    public void deploy() throws Exception {
		log.info("Deploying autoscaling policy...");
		AutoScalerServiceStub stub = new AutoScalerServiceStub("https://localhost:9443/services/AutoScalerService");
		
		AutoscalePolicy autoscalePolicy = new AutoscalePolicy();
		autoscalePolicy.setId("economyPolicy");
		LoadThresholds loadThresholds = new LoadThresholds();
		
		LoadAverage loadAverage = new LoadAverage();
		loadAverage.setAverage(6000);
		loadAverage.setGradient(0);
		loadAverage.setSecondDerivative(0);
		loadAverage.setScaleDownMarginOfGradient(1);
		loadAverage.setScaleDownMarginOfSecondDerivative(0.2f);
		loadThresholds.setLoadAverage(loadAverage);
		
		MemoryConsumption memoryConsumption = new MemoryConsumption();
		memoryConsumption.setAverage(6000);
		memoryConsumption.setGradient(0);
		memoryConsumption.setSecondDerivative(0);
		memoryConsumption.setScaleDownMarginOfGradient(1);
		memoryConsumption.setScaleDownMarginOfSecondDerivative(0.2f);
		loadThresholds.setMemoryConsumption(memoryConsumption);
		
		RequestsInFlight requestsInFlight = new RequestsInFlight();
		requestsInFlight.setAverage(6000);
		requestsInFlight.setGradient(0);
		requestsInFlight.setSecondDerivative(0);
		requestsInFlight.setScaleDownMarginOfGradient(1);
		requestsInFlight.setScaleDownMarginOfSecondDerivative(0.2f);
		loadThresholds.setRequestsInFlight(requestsInFlight);
		
		autoscalePolicy.setLoadThresholds(loadThresholds);
		boolean deployed = stub.addAutoScalingPolicy(autoscalePolicy);
		Assert.assertTrue(deployed, "Cannot deploy autoscaling-policy");

		AutoscalePolicy policy = stub.getAutoscalingPolicy(autoscalePolicy.getId());
		Assert.assertNotNull(policy, "Cannot get deployed autoscaling-policy");
		  
	  }

}
