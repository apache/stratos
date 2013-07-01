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
package org.apache.stratos.autoscaler.service.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.stratos.autoscaler.service.impl.AutoscalerServiceImpl.IaasContextComparator;
import org.apache.stratos.autoscaler.service.impl.AutoscalerServiceImpl.Iaases;

import junit.framework.TestCase;

public class IaasContextComparatorTest extends TestCase {
    
    List<IaasContext> iaasContexts = new ArrayList<IaasContext>();
    
    @Override
    protected void setUp() throws Exception {
        IaasContext a = new IaasContext(Iaases.ec2, null);
        a.setScaleUpOrder(1);
        a.setScaleDownOrder(5);
        
        IaasContext b = new IaasContext(Iaases.openstack, null);
        b.setScaleUpOrder(3);
        b.setScaleDownOrder(0);
        
        iaasContexts.add(a);
        iaasContexts.add(b);
        
        super.setUp();
    }

    public void testSort() {
        
        // scale up order sort test
        Collections.sort(iaasContexts,
                         IaasContextComparator.ascending(
                         IaasContextComparator.getComparator(
                         IaasContextComparator.SCALE_UP_SORT)));
        
        assertEquals("ec2", iaasContexts.get(0).getName().toString());
        assertEquals("openstack", iaasContexts.get(1).getName().toString());
        
        // scale down order sort test
        Collections.sort(iaasContexts,
                         IaasContextComparator.ascending(
                         IaasContextComparator.getComparator(
                         IaasContextComparator.SCALE_DOWN_SORT)));
        
        assertEquals("openstack", iaasContexts.get(0).getName().toString());
        assertEquals("ec2", iaasContexts.get(1).getName().toString());
        
        
        IaasContext c = new IaasContext(Iaases.ec2, null);
        c.setScaleUpOrder(0);
        c.setScaleDownOrder(4);
        
        iaasContexts.add(c);
        
        // scale up order sort test
        Collections.sort(iaasContexts,
                         IaasContextComparator.ascending(
                         IaasContextComparator.getComparator(
                         IaasContextComparator.SCALE_UP_SORT)));
        
        assertEquals("ec2", iaasContexts.get(0).getName().toString());
        assertEquals("ec2", iaasContexts.get(1).getName().toString());
        
        // scale down order sort test
        Collections.sort(iaasContexts,
                         IaasContextComparator.ascending(
                         IaasContextComparator.getComparator(
                         IaasContextComparator.SCALE_DOWN_SORT)));
        
        assertEquals("openstack", iaasContexts.get(0).getName().toString());
        assertEquals("ec2", iaasContexts.get(1).getName().toString());
        
        
    }

}
