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
package org.apache.stratos.autoscaler.service.xml;

import java.util.List;
import org.apache.stratos.autoscaler.service.util.IaasProvider;
import junit.framework.TestCase;

public class ElasticScalerConfigFileReaderTest extends TestCase {

    public void testGetIaasProvidersListy() throws Exception {
        
        String file = "src/test/resources/elastic-scaler-config.xml";
        ElasticScalerConfigFileReader reader = new ElasticScalerConfigFileReader(file);
        
        List<IaasProvider> list =reader.getIaasProvidersList();
        
        assertEquals(2, list.size());
        
        assertEquals("ec2", list.get(0).getType());
        //assertEquals("cdcd", list.get(0).getIdentity());
        assertEquals(2, list.get(0).getScaleDownOrder());
        assertEquals(1, list.get(0).getScaleUpOrder());
        assertEquals("a", list.get(0).getProperties().get("A.x"));
        assertEquals("b", list.get(0).getProperties().get("B"));
        assertEquals(null, list.get(0).getProperties().get("AA"));
        
        assertEquals("openstack", list.get(1).getType());
        //assertEquals("bebbe", list.get(1).getIdentity());
        assertEquals(1, list.get(1).getScaleDownOrder());
        assertEquals(2, list.get(1).getScaleUpOrder());
        assertEquals("x", list.get(1).getProperties().get("X"));
        assertEquals("y", list.get(1).getProperties().get("Y"));
        assertEquals(null, list.get(1).getProperties().get("x"));
        
        
        List<org.apache.stratos.autoscaler.service.util.ServiceTemplate> temps =reader.getTemplates();
      
        assertEquals("wso2.as.domain", temps.get(0).getDomainName());
        assertEquals("manager,default", temps.get(0).getProperty("securityGroups"));
        
    }

}
