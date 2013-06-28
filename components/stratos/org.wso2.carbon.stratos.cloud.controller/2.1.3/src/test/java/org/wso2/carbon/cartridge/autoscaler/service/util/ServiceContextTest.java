/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.autoscaler.service.util;

import org.wso2.carbon.stratos.cloud.controller.util.ServiceContext;
import junit.framework.TestCase;

public class ServiceContextTest extends TestCase {
    
    ServiceContext ctxt = new ServiceContext();

    public ServiceContextTest(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        ctxt.setProperty("min_app_instances", "2");
        ctxt.setProperty("max_app_instances", "5");
        ctxt.setProperty("public_ip", null);
    }

    public final void testPropertiesToNginx() throws Exception {
    	assertEquals(true, ctxt.propertiesToNginx().contains("min_app_instances 2;\n"));
    	assertEquals(true, ctxt.propertiesToNginx().contains("max_app_instances 5;\n"));
    	assertEquals(true, ctxt.propertiesToNginx().contains("public_ip ;\n"));
    }
    
    public final void testPropertiesToXml() throws Exception {
    	System.out.println(ctxt.propertiesToXml());
    	assertEquals(true, ctxt.propertiesToXml().contains("name=\"min_app_instances\" value=\"2\""));
    }
   
}
