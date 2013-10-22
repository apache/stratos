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

package org.apache.stratos.autoscaler.rule.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Defines cluster context properties.
 */
public class ClusterContext {

    private String serviceName;

    private String clusterId;

    private String autoscalePolicyName;

    private float averageRequestsInFlight;

    private float requestsInFlightSecondDerivative;

    private float requestsInFlightGradient;

    private Properties properties;

    private Map<String, MemberContext> memberContextMap;

    public ClusterContext(String serviceName, String clusterId) {

        this.serviceName = serviceName;
        this.clusterId = clusterId;
        memberContextMap = new HashMap<String, MemberContext>();
    }

    public String getServiceName() {

        return serviceName;
    }

    public String getClusterId() {

        return clusterId;
    }

    public Properties getProperties() {

        return properties;
    }

    public void setProperties(Properties properties) {

        this.properties = properties;
    }

    public float getAverageRequestsInFlight() {
        return averageRequestsInFlight;
    }

    public void setAverageRequestsInFlight(float averageRequestsInFlight) {

        this.averageRequestsInFlight = averageRequestsInFlight;
    }

    public float getRequestsInFlightSecondDerivative() {

        return requestsInFlightSecondDerivative;
    }

    public void setRequestsInFlightSecondDerivative(float requestsInFlightSecondDerivative) {

        this.requestsInFlightSecondDerivative = requestsInFlightSecondDerivative;
    }

    public float getRequestsInFlightGradient() {

        return requestsInFlightGradient;
    }

    public void setRequestsInFlightGradient(float requestsInFlightGradient) {

        this.requestsInFlightGradient = requestsInFlightGradient;
    }

    public String getAutoscalePolicyName() {

        return autoscalePolicyName;
    }

    public void setAutoscalePolicyName(String autoscalePolicyName) {

        this.autoscalePolicyName = autoscalePolicyName;
    }

    /**
     *
     * @param memberContext will be added to map
     */
    public void addMemberContext(MemberContext memberContext) {

        memberContextMap.put(memberContext.getMemberId(), memberContext );
    }

    /**
     * {@link MemberContext} which carries memberId will be removed from map
     * @param memberId
     */
    public void removeMemberContext(String memberId){

        memberContextMap.remove(memberId);
    }
}
