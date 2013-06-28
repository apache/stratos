/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.lb.common.test;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.wso2.carbon.lb.common.conf.structure.Node;
import org.wso2.carbon.lb.common.conf.structure.NodeBuilder;

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

        Assert.assertEquals("loadbalancer", a.getName());
        Assert.assertEquals("stratos-appserver-lb", a.getProperty("securityGroups"));
        Assert.assertEquals("${ELASTIC_IP}", a.getProperty("elasticIP"));
        Assert.assertEquals("/mnt/payload.zip", a.getProperty("payload"));
        Assert.assertNull(a.getProperty("payloader"));

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

        Assert.assertEquals("appserver", a.getName());
        Assert.assertEquals(1, a.getChildNodes().size());
        Assert.assertEquals("domains", a.getChildNodes().get(0).getName());
        Assert.assertEquals("appserver.cloud-test.wso2.com,as.cloud-test.wso2.com",
                            a.getProperty("hosts"));
        Assert.assertEquals("resources/cluster_node.zip", a.getProperty("payload"));
        Assert.assertEquals(null, a.getProperty("payloader"));

        Node b = a.getChildNodes().get(0);

        Assert.assertEquals(3, b.getChildNodes().size());
        Assert.assertEquals(null, b.getProperty("payload"));

        Node c = b.getChildNodes().get(0);

        Assert.assertEquals(0, c.getChildNodes().size());
        Assert.assertEquals("1-100", c.getProperty("tenant_range"));

        c = b.getChildNodes().get(2);

        Assert.assertEquals(0, c.getChildNodes().size());
        Assert.assertEquals("*", c.getProperty("tenant_range"));
        
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
