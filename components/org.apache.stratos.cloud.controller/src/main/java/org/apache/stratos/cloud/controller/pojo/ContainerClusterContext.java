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
package org.apache.stratos.cloud.controller.pojo;

import org.apache.stratos.common.Properties;

import java.io.Serializable;

/**
 * Holds information about a container cluster to be started.
 *
 */
public class ContainerClusterContext implements Serializable {

    private static final long serialVersionUID = -388327475844701869L;
    // cluster id this Pod belongs to
    private String clusterId;
    // properties
    private Properties properties;
    
    public ContainerClusterContext() {
    }
    
    public ContainerClusterContext(String clusterId) {
        this.clusterId = clusterId;
    }
    
    public String getClusterId() {
        return clusterId;
    }
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContainerClusterContext other = (ContainerClusterContext) obj;
        if (clusterId == null) {
            if (other.clusterId != null)
                return false;
        } else if (!clusterId.equals(other.clusterId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ContainerClusterContext [clusterId=" + clusterId + ", properties=" + properties
                + "]";
    }

}
