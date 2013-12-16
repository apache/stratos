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
package org.apache.stratos.cloud.controller.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.messaging.domain.topology.Topology;

import com.google.gson.Gson;

import javax.jms.TextMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Persistence and retrieval of Topology from Registry
 */
public class TopologyManager {
    private static final Log log = LogFactory.getLog(TopologyManager.class);

    private  volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private volatile Topology topology;
    private static TopologyManager instance;
    private BlockingQueue<TextMessage> sharedTopologyDiffQueue = new LinkedBlockingQueue<TextMessage>();


    private TopologyManager() {
    }

    public static TopologyManager getInstance() {
        synchronized (TopologyManager.class) {
            if (instance == null) {
                instance = new TopologyManager();
            }
            return instance;
            
        }
    }

    public void acquireReadLock() {
        readLock.lock();
    }

    public void releaseReadLock() {
        readLock.unlock();
    }

    public void acquireWriteLock() {
        writeLock.lock();
    }

    public void releaseWriteLock() {
        writeLock.unlock();
    }

    public synchronized Topology getTopology() {
        synchronized (TopologyManager.class) {
            if(this.topology == null) {
                //need to initialize the topology
                this.topology = CloudControllerUtil.retrieveTopology();
                if (this.topology == null) {
                    if(log.isDebugEnabled()) {
                        log.debug("Creating new topology");
                    }
                    this.topology = new Topology();
                }
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("The current topology is: " + toJson());
        }
        return this.topology;
    }

    public synchronized void updateTopology(Topology topology) {
        synchronized (TopologyManager.class) {
             this.topology = topology;
             CloudControllerUtil.persistTopology(this.topology);
             if (log.isDebugEnabled()) {
                 log.debug("Topology got updated. Full Topology: "+toJson());
             }
             
        }

    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(topology);

    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public BlockingQueue<TextMessage> getSharedTopologyDiffQueue() {
        return sharedTopologyDiffQueue;
    }

    public void setSharedTopologyDiffQueue(BlockingQueue<TextMessage> sharedTopologyDiffQueue) {
        this.sharedTopologyDiffQueue = sharedTopologyDiffQueue;
    }
}

