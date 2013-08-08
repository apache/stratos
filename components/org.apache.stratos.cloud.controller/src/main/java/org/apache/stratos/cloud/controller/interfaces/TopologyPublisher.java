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
package org.apache.stratos.cloud.controller.interfaces;

/**
 * All custom implementations of Topology Publisher should extend this abstract class.
 */
public abstract class TopologyPublisher {
    
    /**
     * This operation will be called once in order to initialize this publisher.
     */
    public abstract void init();
    
    /**
     * When a message is ready to be published to a certain topic, this operation will be called.
     * @param topicName name of the topic to be published.
     * @param message message to be published.
     */
    public abstract void publish(String topicName, String message);
    
    /**
     * Cron expression which explains the frequency that the topology publishing happens.
     * @return cron expression
     */
    public abstract String getCron();
    
}
