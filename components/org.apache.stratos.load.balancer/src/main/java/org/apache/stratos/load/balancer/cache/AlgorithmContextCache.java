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

package org.apache.stratos.load.balancer.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.util.Constants;

/**
 * Algorithm context cache manages algorithm context values in a clustered environment.
 */
public class AlgorithmContextCache {
    private static final Log log = LogFactory.getLog(AlgorithmContextCache.class);

    private static String prepareKey(String serviceName, String clusterId) {
        return String.format("%s-%s", serviceName, clusterId);
    }

    public static void putCurrentMemberIndex(String serviceName, String clusterId, int currentMemberIndex) {
        String key = prepareKey(serviceName, clusterId);
        LoadBalancerCache.putInteger(Constants.ALGORITHM_CONTEXT_CACHE, key, currentMemberIndex);
    }

    public static int getCurrentMemberIndex(String serviceName, String clusterId) {
        String key = prepareKey(serviceName, clusterId);
        return LoadBalancerCache.getInteger(Constants.ALGORITHM_CONTEXT_CACHE, key);
    }
}
