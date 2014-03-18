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
package org.apache.stratos.autoscaler.deployment.policy;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.partition.deployers.PartitionReader;
import org.apache.stratos.cloud.controller.stub.deployment.partition.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author nirmal
 *
 */
public class PartitionDeployerTest {
    
    PartitionReader reader, reader1;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        reader = new PartitionReader(new File("src/test/resources/partitions.xml"));
        reader1 = new PartitionReader(new File("src/test/resources/partition.xml"));
    }

    @Test
    public void testPartitionCount() throws InvalidPolicyException {
        
        assertEquals(2, reader.getPartitionList().size());
    }
    
    @Test
    public void testPartition() throws InvalidPolicyException {
        
        assertEquals(1, reader1.getPartitionList().size());
        
        Partition p = reader1.getPartitionList().get(0);
        assertEquals("P1", p.getId());
        assertEquals(3, p.getPartitionMax());
        assertEquals(2, p.getProperties().getProperties().length);
    }

}
