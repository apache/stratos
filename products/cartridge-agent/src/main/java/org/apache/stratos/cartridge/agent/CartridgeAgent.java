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

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public CartridgeAgent(String userDataFilePath) {
        this.userData = extractUserData(userDataFilePath);
    }

    private UserData extractUserData(String userDataFilePath) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(userDataFilePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find user data file");
        }

        try {
            String json = IOUtils.toString(fileInputStream, "UTF-8");
            if(log.isDebugEnabled()){
                log.debug(String.format("User data json: %s", json));
            }
            return new Gson().fromJson(json, UserData.class);
        } catch (IOException e) {
            if(log.isErrorEnabled()) {
                log.error(e);
            }
        }
        throw new RuntimeException("Could not read user data file");
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
            for(int port : userData.getPorts()) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Trying to connect to application socket: %s %d", userData.getIpAddress(), port));
                }

                SocketAddress httpSockaddr = new InetSocketAddress(userData.getIpAddress(), port);
                Socket socket = new Socket();
                socket.connect(httpSockaddr, 1000);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Successfully connected to socket: %s %d", userData.getIpAddress(), port));
                }
            }
            if (log.isInfoEnabled()) {
                log.info("Application is active");
            }
            return true;
        } catch (IOException e) {
        }
        if (log.isInfoEnabled()) {
            log.info("All sockets are still not active");
        }
        return false;
    }

    private void publishMemberStartedEvent(UserData userData) throws JMSException, NamingException, IOException, InterruptedException {
        MemberStartedEvent event = new MemberStartedEvent(userData.getServiceName(), userData.getClusterId(), userData.getMemberId());
        publishEvent(event);
    }

    private void publishMemberActivatedEvent(UserData userData) throws JMSException, NamingException, IOException, InterruptedException {
        MemberActivatedEvent event = new MemberActivatedEvent(userData.getServiceName(), userData.getClusterId(), userData.getMemberId());
        publishEvent(event);
    }

    private void publishEvent(Event event) throws JMSException, NamingException, IOException, InterruptedException {
        EventPublisher publisher = new EventPublisher(userData.getMbIpAddress(), userData.getMbPort(), org.apache.stratos.messaging.util.Constants.INSTANCE_STATUS_TOPIC);
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
