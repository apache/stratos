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

package org.apache.stratos.cartridge.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.status.MemberActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.MemberStartedEvent;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Cartridge agent lifecycle implementation.
 */
public class CartridgeAgent implements Runnable {
    private static final Log log = LogFactory.getLog(CartridgeAgent.class);

    private UserData userData;

    public CartridgeAgent(UserData userData) {
        this.userData = userData;
    }

    @Override
    public void run() {
        try {
            if (log.isInfoEnabled()) {
                log.info("\nCartridge agent started");
            }
            publishMemberStartedEvent(userData);

            // Wait for the application to become active
            while (!isApplicationActive(userData)) {
                Thread.sleep(1000);
            }

            publishMemberActivatedEvent(userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isApplicationActive(UserData userData) {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Trying to connect to application socket: %s %d", userData.getIpAddress(), userData.getPort()));
            }

            SocketAddress httpSockaddr = new InetSocketAddress(userData.getIpAddress(), userData.getPort());
            new Socket().connect(httpSockaddr, 1000);

            if (log.isInfoEnabled()) {
                log.info("Successfully connected to socket");
            }
            return true;
        } catch (IOException e) {
        }
        if (log.isInfoEnabled()) {
            log.info("Socket is not available");
        }
        return false;
    }

    private void publishMemberStartedEvent(UserData userData) throws JMSException, NamingException, IOException, InterruptedException {
        MemberStartedEvent event = new MemberStartedEvent();
        event.setServiceName(userData.getServiceName());
        event.setClusterId(userData.getClusterId());
        event.setMemberId(userData.getMemberId());
        publishEvent(event);
    }

    private void publishMemberActivatedEvent(UserData userData) throws JMSException, NamingException, IOException, InterruptedException {
        MemberActivatedEvent event = new MemberActivatedEvent();
        event.setServiceName(userData.getServiceName());
        event.setClusterId(userData.getClusterId());
        event.setMemberId(userData.getMemberId());
        publishEvent(event);
    }

    private void publishEvent(Event event) throws JMSException, NamingException, IOException, InterruptedException {
        EventPublisher publisher = new EventPublisher(org.apache.stratos.messaging.util.Constants.INSTANCE_STATUS_TOPIC);
        try {
            publisher.connect();
            publisher.publish(event);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            publisher.close();
        }
    }
}
