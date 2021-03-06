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

package org.apache.stratos.cloud.controller.statistics.publisher;

import org.apache.stratos.cloud.controller.domain.InstanceMetadata;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.wso2.carbon.databridge.commons.StreamDefinition;

/**
 * Member Information Publisher interface.
 */
public abstract class MemberInformationPublisher extends ThriftStatisticsPublisher {

    public MemberInformationPublisher(StreamDefinition streamDefinition, String thriftClientName) {
        super(streamDefinition, thriftClientName);
    }

    /**
     * Publishing member information.
     *
     * @param memberId          Member Id
     * @param scalingDecisionId Scaling Decision Id
     * @param metadata          InstanceMetadata
     */
    public abstract void publish(String memberId, String scalingDecisionId, InstanceMetadata metadata);

}
