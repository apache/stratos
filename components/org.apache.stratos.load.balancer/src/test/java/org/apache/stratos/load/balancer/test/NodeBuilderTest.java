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
package org.apache.stratos.load.balancer.test;

import junit.framework.TestCase;

import org.apache.stratos.load.balancer.conf.structure.Node;
import org.apache.stratos.load.balancer.conf.structure.NodeBuilder;

public class NodeBuilderTest extends TestCase {

    String content;

    public void setUp() throws Exception {
    }

    public final void testBuildNode() {

        // Testing a node only has properties
        Node a = new Node();
        a.setName("loadbalancer");

        content =
            "securityGroups      stratos-appserver-lb;\ninstanceType        m1.large;\n"
                + "instances           1;\nelasticIP           ${ELASTIC_IP};\n"
                + "availabilityZone    us-east-1c;\npayload             /mnt/payload.zip;";

        a = NodeBuilder.buildNode(a, content);

        assertEquals("loadbalancer", a.getName());
        assertEquals("stratos-appserver-lb", a.getProperty("securityGroups"));
        assertEquals("${ELASTIC_IP}", a.getProperty("elasticIP"));
        assertEquals("/mnt/payload.zip", a.getProperty("payload"));
        assertNull(a.getProperty("payloader"));

        // Testing a node has sub nodes and properties
        a = new Node();
        a.setName("appserver");

        content =
            "hosts                   appserver.cloud-test.wso2.com,as.cloud-test.wso2.com;\n"
                + "domains   {\n" + "wso2.as1.domain {\n" + "tenant_range    1-100;\n" + "}\n"
                + "wso2.as2.domain {\n" + "tenant_range    101-200;\n" + "}\n"
                + "wso2.as3.domain { # domain\n" + "tenant_range    *;\n" + "}\n" + "}\n"
                + "# line comment \n"
                + "payload                 resources/cluster_node.zip;# payload\n"
                + "availability_zone       us-east-1c;\n";

        a = NodeBuilder.buildNode(a, content);

        assertEquals("appserver", a.getName());
        assertEquals(1, a.getChildNodes().size());
        assertEquals("domains", a.getChildNodes().get(0).getName());
        assertEquals("appserver.cloud-test.wso2.com,as.cloud-test.wso2.com",
                            a.getProperty("hosts"));
        assertEquals("resources/cluster_node.zip", a.getProperty("payload"));
        assertEquals(null, a.getProperty("payloader"));

        Node b = a.getChildNodes().get(0);

        assertEquals(3, b.getChildNodes().size());
        assertEquals(null, b.getProperty("payload"));

        Node c = b.getChildNodes().get(0);

        assertEquals(0, c.getChildNodes().size());
        assertEquals("1-100", c.getProperty("tenant_range"));

        c = b.getChildNodes().get(2);

        assertEquals(0, c.getChildNodes().size());
        assertEquals("*", c.getProperty("tenant_range"));
        
        String nodeStr = "appserver {\n" +
                "\thosts\tappserver.cloud-test.wso2.com,as.cloud-test.wso2.com;\n" +
                "\tpayload\tresources/cluster_node.zip;\n" +
        		"\tavailability_zone\tus-east-1c;\n" +
        		"\tdomains {\n" +
        		"\t\twso2.as1.domain {\n" +
        		"\t\t\ttenant_range\t1-100;\n" +
        		"\t\t}\n" +
        		"\t\twso2.as2.domain {\n" +
        		"\t\t\ttenant_range\t101-200;\n" +
        		"\t\t}\n" +
        		"\t\twso2.as3.domain {\n" +
        		"\t\t\ttenant_range\t*;\n" +
        		"\t\t}\n" +
        		"\t}\n" +
        		"}";
        
        assertEquals(nodeStr, a.toString());
        
        // test equals method
        assertEquals(true, a.equals(a));
        assertEquals(false, a.equals(b));
        assertEquals(false, c.equals(b));
        
        // test buildNode(String)
        c = NodeBuilder.buildNode(nodeStr);
        
        assertEquals(c.getName(), "appserver");
        assertEquals(c.getChildNodes().size(), 1);
        assertEquals(c.getProperty("availability_zone"), "us-east-1c");

    }

}
