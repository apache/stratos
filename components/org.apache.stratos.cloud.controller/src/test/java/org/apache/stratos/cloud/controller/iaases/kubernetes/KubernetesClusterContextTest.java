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

package org.apache.stratos.cloud.controller.iaases.kubernetes;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.internal.ServiceReferenceHolder;
import org.apache.stratos.common.clustering.impl.HazelcastDistributedObjectProvider;

import junit.framework.TestCase;

public class KubernetesClusterContextTest extends TestCase {
	
	private static final String CLUSTER_ID = "clusterid1";
	private static final String MASTER_IP_1 = "testhostname1";
	private static final String MASTER_IP_2 = "testhostname2";
	private static final String MASTER_PORT = "8080";
	private static final int LOWER_PORT = 2000;
	private static final int UPPER_PORT = 8000;
	
		
	public void testUpdateKubCluster() throws Exception {	

		KubernetesMaster km1 = new KubernetesMaster();
		km1.setPrivateIPAddress(MASTER_IP_1);
				
		KubernetesCluster cluster = new KubernetesCluster();
		cluster.setClusterId(CLUSTER_ID);
		cluster.setKubernetesMaster(km1);		
		
		AxisConfiguration axisConfiguration = new AxisConfiguration();
        axisConfiguration.setClusteringAgent(null);

        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(new HazelcastDistributedObjectProvider());
        ServiceReferenceHolder.getInstance().setAxisConfiguration(axisConfiguration);

        CloudControllerContext.unitTest = true;
        CloudControllerContext ctx = CloudControllerContext.getInstance();
		assertNotNull(ctx);		
		ctx.addKubernetesCluster(cluster);		
		
		KubernetesClusterContext kubClusterContext = new KubernetesClusterContext(CLUSTER_ID, MASTER_IP_1, MASTER_PORT, LOWER_PORT, UPPER_PORT);
		CloudControllerContext.getInstance().addKubernetesClusterContext(kubClusterContext);
		
		KubernetesMaster km2 = new KubernetesMaster();
		km2.setPrivateIPAddress(MASTER_IP_2);
		
		KubernetesCluster newCluster = new KubernetesCluster();
		newCluster.setClusterId(CLUSTER_ID);
		newCluster.setKubernetesMaster(km2);		
		
		// Get cluster context and update
		CloudControllerContext.getInstance().updateKubernetesCluster(newCluster);
        KubernetesClusterContext kubClusterContextUpdated = CloudControllerContext.getInstance().getKubernetesClusterContext(CLUSTER_ID);
        
        // Update necessary parameters of kubClusterContext using the updated kubCluster
        kubClusterContextUpdated.updateKubClusterContextParams(newCluster);            
        CloudControllerContext.getInstance().updateKubernetesClusterContext(kubClusterContext);
                
        // Get updated values and assert
        assertEquals("testhostname2" , CloudControllerContext.getInstance().getKubernetesClusterContext(CLUSTER_ID).getMasterIp());
        assertNotSame("testhostname1" , CloudControllerContext.getInstance().getKubernetesClusterContext(CLUSTER_ID).getMasterIp());
	}
	

}
