/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.autoscaler.service.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.apache.stratos.autoscaler.service.impl.AutoscalerServiceImpl.Iaases;
import org.apache.stratos.lb.common.conf.util.Constants;

import junit.framework.TestCase;

public class IaasContextTest extends TestCase {

    IaasContext ctx;
    NodeMetadata node1, node2, node3, node4;
    
    String[] domains = {"wso2.a", "wso2.b", "wso2.c"};
    String[] subDomains = {"mgt", "worker"};
    String[] nodeIds = {"1", "2", "3", "4", "5"};
    String[] ips = {"192.168.1.2", "192.168.1.3", "192.168.1.4"};

    public IaasContextTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();

        node1 = new NodeMetadataBuilder().id("1")
                                         .status(org.jclouds.compute.domain.NodeMetadata.Status.RUNNING)
                                         .publicAddresses(new ArrayList<String>(Arrays.asList("192.168.1.2")))
                                         .build();
        node2 = new NodeMetadataBuilder().id("2")
                                         .status(org.jclouds.compute.domain.NodeMetadata.Status.RUNNING)
                                         .build();
        node3 = new NodeMetadataBuilder().id("3")
                                         .status(org.jclouds.compute.domain.NodeMetadata.Status.RUNNING)
                                         .build();
        node4 = new NodeMetadataBuilder().id("4")
                                         .status(org.jclouds.compute.domain.NodeMetadata.Status.RUNNING)
                                         .build();

        ctx = new IaasContext(Iaases.ec2, null);
        
        
        ctx.addInstanceContext(new InstanceContext(domains[0], subDomains[0], null));
        ctx.addInstanceContext(new InstanceContext(domains[1], subDomains[1], null));
        ctx.addInstanceContext(new InstanceContext(domains[2], subDomains[0], null));
        ctx.addInstanceContext(new InstanceContext(domains[2], Constants.DEFAULT_SUB_DOMAIN, null));
        
        ctx.addNodeDetails(domains[0], subDomains[0], nodeIds[0], "");
        ctx.addNodeDetails(domains[0], subDomains[0], nodeIds[1], ips[0]);
        ctx.addNodeDetails(domains[1], subDomains[1], nodeIds[2], ips[1]);
        ctx.addNodeDetails(domains[2], subDomains[0], nodeIds[3], ips[2]);
        ctx.addNodeDetails(domains[2], Constants.DEFAULT_SUB_DOMAIN, nodeIds[4], "");
        
        
//        ctx.addNodeIdToDomainMap(node1.getId(), "wso2.a");
//        ctx.addPublicIpToDomainMap("192.168.1.2", "wso2.a");
//        ctx.addPublicIpToNodeIdMap("192.168.1.2", node1.getId());
//        ctx.addNodeIdToDomainMap(node2.getId(), "wso2.b");
//        ctx.addNodeIdToDomainMap(node3.getId(), "wso2.a");
//        ctx.addPublicIpToDomainMap("192.168.1.3", "wso2.a");
//        ctx.addPublicIpToNodeIdMap("192.168.1.3", node3.getId());
//        ctx.addNodeIdToDomainMap(node4.getId(), "wso2.c");
    }

    public final void testGetLastMatchingNode() {

        assertEquals(nodeIds[1], ctx.getLastMatchingNode(domains[0], subDomains[0]));
        ctx.removeNodeId(nodeIds[1]);
        assertEquals(nodeIds[0], ctx.getLastMatchingNode(domains[0], subDomains[0]));
        ctx.addNodeDetails(domains[0], subDomains[0], nodeIds[1], ips[0]);
    }

    public final void testGetFirstMatchingNode() {
        assertEquals(nodeIds[0], ctx.getFirstMatchingNode(domains[0], subDomains[0]));
    }
    
    public final void testGetLastMatchingPublicIp() {
        assertEquals(ips[0], ctx.getLastMatchingPublicIp(domains[0], subDomains[0]));
        assertEquals(null, ctx.getLastMatchingPublicIp(domains[2], Constants.DEFAULT_SUB_DOMAIN));
    }

    public final void testGetNodeWithPublicIp() {
        assertEquals(nodeIds[3], ctx.getNodeWithPublicIp(ips[2]));
    }

    public final void testGetNodeIds() {
        assertEquals(new ArrayList<String>(Arrays.asList(nodeIds[0], nodeIds[1])), ctx.getNodeIds(domains[0], subDomains[0]));
        assertEquals(new ArrayList<String>(Arrays.asList(nodeIds[2])), ctx.getNodeIds(domains[1], subDomains[1]));
    }

}
