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
package org.apache.stratos.cloud.controller.functions;

import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.kubernetes.client.model.Pod;
import com.google.common.base.Function;

/**
 * Is responsible for converting a {@link Pod} object to a
 * {@link MemberContext} Object.
 */
public class PodToMemberContext implements Function<Pod, MemberContext> {

    @Override
    public MemberContext apply(Pod pod) {

        if (pod == null) {
            return null;
        }
        MemberContext memberContext = new MemberContext();
        memberContext.setMemberId(pod.getId());
        memberContext.setPrivateIpAddress(pod.getCurrentState().getHostIP());
        memberContext.setPublicIpAddress(pod.getCurrentState().getHostIP());
        memberContext.setInitTime(System.currentTimeMillis());
        
        return memberContext;
    }

}
