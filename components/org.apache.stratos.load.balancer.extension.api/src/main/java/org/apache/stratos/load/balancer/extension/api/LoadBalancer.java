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

package org.apache.stratos.load.balancer.extension.api;

import org.apache.stratos.load.balancer.common.domain.Topology;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

/**
 * Load balancer interface for managing its lifecycle.
 */
public interface LoadBalancer {

    /**
     * Start a new load balancer instance.
     * @throws LoadBalancerExtensionException if the start operation fails
     */
    void start() throws LoadBalancerExtensionException;

    /**
     * Stop running load balancer instance.
     * @throws LoadBalancerExtensionException if the stop operation fails.
     */
    void stop() throws LoadBalancerExtensionException;

    /**
     * Configure the load balancer using the given topology.
     * @param topology latest topology to be configured
     * @return Returns true if configured correctly
     * @throws LoadBalancerExtensionException if the configuration operation fails.
     */
    boolean configure(Topology topology) throws LoadBalancerExtensionException;

    /**
     * Reload load balancer configuration using the configuration written in configure() method.
     * @throws LoadBalancerExtensionException if the reload operation fails.
     */
    void reload() throws LoadBalancerExtensionException;
}
