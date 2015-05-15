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

package org.apache.stratos.common.partition;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The model class for NetworkPartition information passed in deployment policy.
 */
public class NetworkPartitionRef implements Serializable {

    private static final long serialVersionUID = -8043298009352097370L;

    private String id;
    private PartitionRef[] partitionRefs;
    private String partitionAlgo;

    public void setPartitionRefs(PartitionRef[] partitionRefs) {
        if (partitionRefs == null) {
            this.partitionRefs = partitionRefs;
        } else {
            this.partitionRefs = Arrays.copyOf(partitionRefs, partitionRefs.length);
        }
    }

    /**
     * Gets the value of the partitionRefs.
     */
    public PartitionRef[] getPartitionRefs() {
        if (partitionRefs == null) {
            partitionRefs = new PartitionRef[0];
        }
        return this.partitionRefs;
    }

    /**
     * Gets the value of the id.
     */
    public String getId() {
        return id;
    }

    /**
     * sets the value of the id.
     */
    public void setId(String id) {
        this.id = id;
    }

    public PartitionRef getPartitionRef(String partitionId) {
        for (PartitionRef partitionRef : partitionRefs) {
            if (partitionRef.getId().equals(partitionId)) {
                return partitionRef;
            }
        }
        return null;
    }

    public String getPartitionAlgo() {
        return partitionAlgo;
    }

    public void setPartitionAlgo(String partitionAlgo) {
        this.partitionAlgo = partitionAlgo;
    }
}
