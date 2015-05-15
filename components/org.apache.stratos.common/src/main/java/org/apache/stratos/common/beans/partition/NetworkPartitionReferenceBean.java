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

package org.apache.stratos.common.beans.partition;

import org.apache.stratos.common.beans.PropertyBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class NetworkPartitionReferenceBean {

    private String id;
    private String provider;
    private List<PartitionReferenceBean> partitions;
    private String partitionAlgo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<PartitionReferenceBean> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<PartitionReferenceBean> partitions) {
        this.partitions = partitions;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPartitionAlgo() {
        return partitionAlgo;
    }

    public void setPartitionAlgo(String partitionAlgo) {
        this.partitionAlgo = partitionAlgo;
    }
}
