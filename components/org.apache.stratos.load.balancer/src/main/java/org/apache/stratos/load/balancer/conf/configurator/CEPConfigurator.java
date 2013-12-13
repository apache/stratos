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

package org.apache.stratos.load.balancer.conf.configurator;

import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;

/**
 * Complex event processor configurator to configure CEP related settings.
 */
public class CEPConfigurator {

    public static void configure(LoadBalancerConfiguration configuration) {
        // Set system properties
        System.setProperty("load.balancer.cep.stats.publisher.enabled", String.valueOf(configuration.isCepStatsPublisherEnabled()));
        System.setProperty("thrift.receiver.ip", configuration.getCepIp());
        System.setProperty("thrift.receiver.port", String.valueOf(configuration.getCepPort()));
        System.setProperty("network.partition.id", configuration.getNetworkPartitionId());
    }
}
