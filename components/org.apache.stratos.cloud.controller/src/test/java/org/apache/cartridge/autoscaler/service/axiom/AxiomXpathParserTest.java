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
import org.apache.axiom.om.OMNode;
import org.apache.stratos.cloud.controller.axiom.AxiomXpathParser;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;

import java.io.File;
import java.util.List;

public class AxiomXpathParserTest extends TestCase {
    AxiomXpathParser parser;
    File xmlFile = new File("src/test/resources/cloud-controller.xml");

    public AxiomXpathParserTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        parser = new AxiomXpathParser(xmlFile);
        parser.parse();
    }
    
    public void testGetMatchingNodes(){
        List<OMNode> list = parser.getMatchingNodes("/cloudController/iaasProviders/iaasProvider/provider");
        assertEquals(1, list.size());
        parser.setIaasProvidersList();
        assertEquals(1, FasterLookUpDataHolder.getInstance().getIaasProviders().size());
    }
    
    public void testDataPublisherConfig() {
		parser.setDataPublisherRelatedData();
		assertEquals(true, FasterLookUpDataHolder.getInstance().getEnableBAMDataPublisher());
		assertEquals("nirmal", FasterLookUpDataHolder.getInstance().getBamUsername());
		assertEquals("nirmal", FasterLookUpDataHolder.getInstance().getBamPassword());
	}
    
    public void testTopologySynchParser() {
		parser.setTopologySyncRelatedData();
		assertNotNull(FasterLookUpDataHolder.getInstance().getTopologyConfig());
		assertEquals("1 * * * * ? *", FasterLookUpDataHolder.getInstance().getTopologyConfig().getProperty("cron"));
	}

}
