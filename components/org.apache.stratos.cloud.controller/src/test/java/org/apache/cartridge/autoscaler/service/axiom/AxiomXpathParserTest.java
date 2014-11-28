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
package org.apache.cartridge.autoscaler.service.axiom;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.util.AxiomXpathParserUtil;
import org.apache.stratos.cloud.controller.config.parser.CloudControllerConfigParser;

import java.io.File;
import java.util.List;

public class AxiomXpathParserTest extends TestCase {
    File xmlFile = new File("src/test/resources/cloud-controller.xml");
    OMElement docElt;

    public AxiomXpathParserTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        docElt = AxiomXpathParserUtil.parse(xmlFile);

        CloudControllerConfigParser.parse(docElt);
    }
    
    public void testGetMatchingNodes(){
        List<OMNode> list = AxiomXpathParserUtil.getMatchingNodes("/cloudController/iaasProviders/iaasProvider/provider", docElt);
        assertEquals(1, list.size());
        assertEquals(1, CloudControllerContext.getInstance().getIaasProviders().size());
    }
    
    public void testDataPublisherConfig() {
		assertEquals(true, CloudControllerContext.getInstance().getEnableBAMDataPublisher());
		assertEquals("nirmal", CloudControllerContext.getInstance().getDataPubConfig().getBamUsername());
		assertEquals("nirmal", CloudControllerContext.getInstance().getDataPubConfig().getBamPassword());
	}
    
    public void testTopologySynchParser() {
		assertNotNull(CloudControllerContext.getInstance().getTopologyConfig());
		assertEquals("1 * * * * ? *", CloudControllerContext.getInstance().getTopologyConfig().getProperty("cron"));
	}

}
