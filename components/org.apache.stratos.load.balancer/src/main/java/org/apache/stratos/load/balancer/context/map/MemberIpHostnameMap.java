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

package org.apache.stratos.load.balancer.context.map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Member-ip -> hostname map for maintaining cluster hostnames of all members against their ip addresses.
 */
public class MemberIpHostnameMap {
    @SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(MemberIpHostnameMap.class);

    private ConcurrentHashMap<String, String> concurrentHashMap;

    public MemberIpHostnameMap() {
        concurrentHashMap = new ConcurrentHashMap<String, String>();
    }

    public void put(String ip, String hostname) {
        concurrentHashMap.put(ip, hostname);
    }

    public boolean contains(String ip) {
        return concurrentHashMap.containsKey(ip);
    }

    public String get(String ip) {
        if(contains(ip)) {
            return concurrentHashMap.get(ip);
        }
        return null;
    }

    public void remove(String ip) {
        if(contains(ip)) {
            concurrentHashMap.remove(ip);
        }
    }

    public void clear() {
        concurrentHashMap.clear();
    }
}
