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

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.messaging.domain.topology.Topology;

import javax.jms.TextMessage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * this will manage to get and update the whole topology  from the file and to the file
 */
public class TopologyManager {
    private static final Log log = LogFactory.getLog(TopologyManager.class);

    private  volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private File topologyFile = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH);
	private File backup = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH + ".back");
    private volatile Topology topology;
    private static TopologyManager instance;
    private BlockingQueue<TextMessage> sharedTopologyDiffQueue = new LinkedBlockingQueue<TextMessage>();


    private TopologyManager() {
    }

    public static TopologyManager getInstance() {
        if (instance == null) {
            instance = new TopologyManager();
        }
        return instance;
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
        String currentContent = null;
        synchronized (TopologyManager.class) {
            if(this.topology == null) {
                //need to initialize the topology
                if(this.topologyFile.exists()) {
                    try {
                        currentContent = FileUtils.readFileToString(this.topologyFile);
                        Gson gson = new Gson();
                        this.topology = gson.fromJson(currentContent, Topology.class);
                        if(log.isDebugEnabled()) {
                            log.debug("The current topology is: " + currentContent);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        throw new CloudControllerException(e.getMessage(), e);
                    }
                } else {
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
            if (this.topologyFile.exists()) {
                this.backup.delete();
                this.topologyFile.renameTo(backup);
            }
            Gson gson = new Gson();
            String message = gson.toJson(topology);
            // overwrite the topology file
            try {
                FileUtils.writeStringToFile(this.topologyFile, message);
                if(log.isDebugEnabled()) {
                    log.debug("The updated topology is: " + message);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new CloudControllerException(e.getMessage(), e);
            }
        }

    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public String toJson() {
        Gson gson = new Gson();
        return  gson.toJson(topology);

    }

    public BlockingQueue<TextMessage> getSharedTopologyDiffQueue() {
        return sharedTopologyDiffQueue;
    }

    public void setSharedTopologyDiffQueue(BlockingQueue<TextMessage> sharedTopologyDiffQueue) {
        this.sharedTopologyDiffQueue = sharedTopologyDiffQueue;
    }
}

