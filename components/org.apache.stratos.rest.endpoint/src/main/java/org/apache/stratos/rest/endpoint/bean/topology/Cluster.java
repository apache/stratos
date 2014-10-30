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
package org.apache.stratos.rest.endpoint.bean.topology;

import javax.xml.bind.annotation.XmlRootElement;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;

import java.util.List;

@XmlRootElement(name="clusters")
public class Cluster {

	public String serviceName;

    public String clusterId;

    public List<Member> member;

    public String tenantRange;

    public List<String> hostNames;

    public boolean isLbCluster;
    
    public List<PropertyBean> property;

    @Override
    public String toString() {
        return "Cluster [serviceName=" + serviceName + ", clusterId=" + clusterId + ", member=" + member
                + ", tenantRange=" + tenantRange + ", hostNames=" + hostNames + ", isLbCluster=" + isLbCluster
                + ", property=" + property + "]";
    }

    
}
