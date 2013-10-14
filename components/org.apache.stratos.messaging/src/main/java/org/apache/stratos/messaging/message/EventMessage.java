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

package org.apache.stratos.messaging.message;

import org.apache.stratos.messaging.event.Event;

import java.io.Serializable;

/**
 * Defines event messages sent between Stratos modules.
 */
public class EventMessage implements Serializable {
    private EventMessageHeader header;
    private String body;

    public EventMessage(EventMessageHeader header, String body) throws ClassNotFoundException {
        this.header = header;
        this.body = body;
    }

    public EventMessage(Event event) {
        this.header = new EventMessageHeader(event.getClass().getName());
        this.body = (new JsonMessage(event)).getText();
    }

    public EventMessageHeader getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }

    public String getJson() {
        return (new JsonMessage(this)).getText();
    }

    public Event getEvent() throws ClassNotFoundException {
        return (Event) new JsonMessage(body, Class.forName(getHeader().getEventClassName()).getClass()).getObject();
    }
}
