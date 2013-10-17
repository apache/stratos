/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.lb.endpoint.util;

import org.apache.axis2.clustering.Member;
import org.apache.stratos.messaging.domain.topology.Port;

/**
 * Implements domain model transformation logic.
 */
public class Transformer {
    public static Member transform(org.apache.stratos.messaging.domain.topology.Member topologyMember) {
        Port httpPort = topologyMember.getPort("HTTP");
        Port httpsPort = topologyMember.getPort("HTTPS");

        Member member = new Member(topologyMember.getHostName(), httpPort.getValue());
        member.setDomain(topologyMember.getHostName());
        member.setHttpPort(httpPort.getValue());
        member.setHttpsPort(httpsPort.getValue());
        member.setActive(topologyMember.isActive());
        member.setProperties(topologyMember.getProperties());
        return  member;
    }
}
