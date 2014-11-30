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

package org.apache.stratos.autoscaler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.stratos.cloud.controller.stub.domain.MemberContext;

/**
 * Helper class to keep the state of the consequence of a rule.
 *
 *
 */
public class TestDelegator {
    private static boolean isMinRuleFired;
    private static Map<String, MemberContext> obsoletedMembers = new ConcurrentHashMap<String, MemberContext>();

    public static boolean isMinRuleFired() {
        return isMinRuleFired;
    }

    public static void setMinRuleFired(boolean isMinRuleFired) {
        TestDelegator.isMinRuleFired = isMinRuleFired;
    }

    public static Map<String, MemberContext> getObsoletedMembers() {
        return obsoletedMembers;
    }

    public static void setObsoletedMembers(Map<String, MemberContext> obsoletedMembers) {
        TestDelegator.obsoletedMembers = obsoletedMembers;
    }
    
    public static void addObsoleteMember(MemberContext ctxt) {
    	TestDelegator.obsoletedMembers.put(ctxt.getMemberId(), ctxt);
    }

    
}
