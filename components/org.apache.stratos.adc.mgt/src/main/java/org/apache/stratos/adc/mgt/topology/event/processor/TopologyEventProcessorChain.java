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

package org.apache.stratos.adc.mgt.topology.event.processor;

import javax.jms.Message;

public class TopologyEventProcessorChain {

    private TopologyEventProcessor completeTopologyEventProcessor = null;
    private TopologyEventProcessor instanceStatusEventProcessor = null;
    private TopologyEventProcessor clusterStatusEventProcessor = null;
    private static TopologyEventProcessorChain topologyEventProcessorChain;

    private TopologyEventProcessorChain () {
        completeTopologyEventProcessor = new CompleteTopologyEventProcessor();
        instanceStatusEventProcessor = new InstanceStatusEventProcessor();
        clusterStatusEventProcessor = new ClusterStatusEventProcessor();
    }

    public static TopologyEventProcessorChain getInstance () {

        if(topologyEventProcessorChain == null) {
            synchronized (TopologyEventProcessorChain.class) {
                if(topologyEventProcessorChain == null) {
                      topologyEventProcessorChain = new TopologyEventProcessorChain();
                }
            }
        }

        return topologyEventProcessorChain;
    }

    public void initProcessorChain () {

        //if any other topology event processors are added, link them as follows
        //instanceStatusEventProcessor.setNext(nextTopologyeventProcessor);
        //nextTopologyeventProcessor.setNext(null);
        completeTopologyEventProcessor.setNext(instanceStatusEventProcessor);
        instanceStatusEventProcessor.setNext(clusterStatusEventProcessor);
        clusterStatusEventProcessor.setNext(null);
    }

    public void startProcessing (Message message) {
        completeTopologyEventProcessor.process(message);
    }


}
