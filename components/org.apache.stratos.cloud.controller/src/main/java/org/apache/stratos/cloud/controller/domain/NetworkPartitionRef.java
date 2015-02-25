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

package org.apache.stratos.cloud.controller.domain;

import javax.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.Arrays;

@XmlRootElement
public class NetworkPartitionRef implements Serializable{

	private static final long serialVersionUID = 3725971214092010720L;

    private String id;

	private String partitionAlgo;

    private PartitionRef[] partitions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

   	public String getPartitionAlgo() {
		return partitionAlgo;
	}

	public void setPartitionAlgo(String partitionAlgo) {
		this.partitionAlgo = partitionAlgo;
	}

	public PartitionRef[] getPartitions() {
		return partitions;
	}

	public void setPartitions(PartitionRef[] partitions) {
		this.partitions = partitions;
	}
	
	/**
	 * Get partition reference by partition id
	 * @param partitionId
	 * @return {@link PartitionRef}
	 */
	public PartitionRef getPartitionRef(String partitionId) {
		if (partitions != null && partitions.length != 0) {
			for (PartitionRef partitionRef : partitions) {
				if (partitionRef.getId().equals(partitionId)) {
					return partitionRef;
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return String.format("{ network-partition-id : %s, partition-algo : %s, partitions : %s }", id, partitionAlgo, Arrays.toString(partitions));
	}
}
