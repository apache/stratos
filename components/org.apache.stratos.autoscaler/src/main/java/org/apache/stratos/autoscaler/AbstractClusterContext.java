/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.policy.model.RequestsInFlight;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.common.constants.StratosConstants;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/*
 * It holds the runtime data of a service cluster
 */
public class AbstractClusterContext implements Serializable {

    private static final Log log = LogFactory.getLog(AbstractClusterContext.class);


    // cluster id
    protected String clusterId;
    private String serviceId;

    public AbstractClusterContext(String clusterId, String serviceId){
        this.clusterId = clusterId;
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }
}
