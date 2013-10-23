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
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.messaging.domain.topology.Topology;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * this will manage to get and update the whole topology  from the file and to the file
 */
public class TopologyManager {
    private static final Log log = LogFactory.getLog(TopologyManager.class);

    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private static File topologyFile = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH);
	private static File backup = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH + ".back");

    public static void acquireReadLock() {
        readLock.lock();
    }

    public static void releaseReadLock() {
        readLock.unlock();
    }

    public static void acquireWriteLock() {
        writeLock.lock();
    }

    public static void releaseWriteLock() {
        writeLock.unlock();
    }

    public static synchronized Topology getTopology() {
        Topology topology;
        String currentContent = null;
        synchronized (TopologyManager.class) {
            topology = FasterLookUpDataHolder.getInstance().getTopology();
            if(topology == null) {
                //need to initialize the topology
                if(topologyFile.exists()) {
                    try {
                        currentContent = FileUtils.readFileToString(topologyFile);
                        Gson gson = new Gson();
                        topology = gson.fromJson(currentContent, Topology.class);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        throw new CloudControllerException(e.getMessage(), e);
                    }
                } else {
                    topology = new Topology();
                }
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("The current topology is: " + currentContent);
        }
        return topology;
    }

    public static synchronized void updateTopology(Topology topology) {
        synchronized (TopologyManager.class) {
            FasterLookUpDataHolder.getInstance().setTopology(topology);
            if (topologyFile.exists()) {
                backup.delete();
                topologyFile.renameTo(backup);
            }
            Gson gson = new Gson();
            String message = gson.toJson(topology);
            // overwrite the topology file
            try {
                FileUtils.writeStringToFile(topologyFile, message);
                if(log.isDebugEnabled()) {
                    log.debug("The updated topology is: " + message);
                }
            } catch (IOException e)     {
                log.error(e.getMessage());
                throw new CloudControllerException(e.getMessage(), e);
            }
        }

    }
}

