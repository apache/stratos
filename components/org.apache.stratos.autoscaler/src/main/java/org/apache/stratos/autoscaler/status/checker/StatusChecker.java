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
package org.apache.stratos.autoscaler.status.checker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.EventListener;

import java.util.Observable;

/**
 * This will be used to evaluate the status of a group
 * and notify the interested parties about the status changes.
 */
public abstract class StatusChecker extends Observable implements Runnable {

    private static final Log log = LogFactory.getLog(StatusChecker.class);

    public void addObserver(EventListener eventListener) {
        addObserver(eventListener);
    }

    public void notifyObservers(Monitor monitor) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Notifying the observers: [monitor] %s", monitor.getClass().getName()));
        }
        setChanged();
        notifyObservers(monitor);
    }
}
