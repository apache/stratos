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

package org.apache.stratos.cartridge.agent.event.publisher;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Cartridge agent lifecycle implementation.
 */
public class EventPublisherClient {
    private static final Log log = LogFactory.getLog(EventPublisherClient.class);

    private String mbIpAddress;
    private int mbPort;
    private String eventClassName;
    private String jsonFilePath;

    public EventPublisherClient(String mbIpAddress, int mbPort, String eventClassName, String jsonFilePath) {
        this.mbIpAddress = mbIpAddress;
        this.mbPort = mbPort;
        this.eventClassName = eventClassName;
        this.jsonFilePath = jsonFilePath;
    }

    private Event jsonToEvent(String jsonFilePath) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(jsonFilePath));
            String json = IOUtils.toString(fileInputStream, "UTF-8");
            if (log.isDebugEnabled()) {
                log.debug(String.format("Json: %s", json));
            }
            Class eventClass = Class.forName(eventClassName);
            return (Event) new Gson().fromJson(json, eventClass);
        } catch (FileNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            throw new RuntimeException(String.format("Could not find json file %s", jsonFilePath));
        } catch (ClassNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            throw new RuntimeException(String.format("Event class name %s is not valid", eventClassName));
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            throw new RuntimeException(String.format("Could not read json file %s", jsonFilePath));
        }
    }

    public void run() throws JMSException, NamingException, IOException {
        if (log.isInfoEnabled()) {
            log.info("\nEvent publisher started");
        }
        Event event = jsonToEvent(jsonFilePath);
        publishEvent(event);
    }


    private void publishEvent(Event event) throws JMSException, NamingException, IOException {
        EventPublisher publisher = new EventPublisher(mbIpAddress, mbPort, org.apache.stratos.messaging.util.Constants.INSTANCE_STATUS_TOPIC);
        try {
            publisher.connect();
            publisher.publish(event);
            if (log.isInfoEnabled()) {
                log.info(String.format("Event %s published", eventClassName));
            }
        } finally {
            publisher.close();
        }
    }
}
