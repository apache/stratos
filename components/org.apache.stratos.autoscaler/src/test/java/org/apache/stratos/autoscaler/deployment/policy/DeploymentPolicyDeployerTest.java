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
package org.apache.stratos.autoscaler.deployment.policy;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.stratos.autoscaler.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.policy.deployers.DeploymentPolicyReader;
import org.apache.stratos.cloud.controller.deployment.policy.DeploymentPolicy;
import org.junit.Before;
import org.junit.Test;

/**
 * @author nirmal
 *
 */
public class DeploymentPolicyDeployerTest {
    
    DeploymentPolicyReader reader;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        reader = new DeploymentPolicyReader(new File("src/test/resources/deployment-policy.xml"));
    }

    @Test
    public void test() throws InvalidPolicyException {
        
        DeploymentPolicy policy = reader.read();
    }

}
