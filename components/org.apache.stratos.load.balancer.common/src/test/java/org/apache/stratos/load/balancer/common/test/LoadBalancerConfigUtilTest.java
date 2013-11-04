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
package org.apache.stratos.load.balancer.common.test;

import java.util.Arrays;
import java.util.List;

import org.apache.stratos.load.balancer.common.conf.util.LoadBalancerConfigUtil;

import junit.framework.TestCase;

public class LoadBalancerConfigUtilTest extends TestCase {

    private List<Integer> tenantList1 = Arrays.asList(1,2,3);
    private List<Integer> tenantList2 = Arrays.asList(1,6,3,4);
    private List<Integer> tenantList3 = Arrays.asList(43);
    private List<Integer> tenantList4 = Arrays.asList(0);
    
    @Override
    protected void setUp() throws Exception {

    }
    
    public final void testGetTenantIds() {
        
        assertEquals(tenantList1, LoadBalancerConfigUtil.getTenantIds("1-3"));
        assertEquals(tenantList2, LoadBalancerConfigUtil.getTenantIds("1,6,3,4"));
        assertEquals(tenantList3, LoadBalancerConfigUtil.getTenantIds("43"));
        assertEquals(tenantList4, LoadBalancerConfigUtil.getTenantIds("*"));
    }

}
