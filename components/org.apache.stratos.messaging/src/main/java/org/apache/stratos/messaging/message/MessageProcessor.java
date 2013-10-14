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

import com.google.gson.stream.JsonReader;

import java.io.*;

/**
 * Message processor generic implementation.
 */
public class MessageProcessor {
    /**
     * Transform json into an object of given type.
     * @param json
     * @param type
     * @return
     */
    public Object jsonToObject(String json, Class type) {
        return (new JsonMessage(json, type)).getObject();
    }

    /**
     * Read message header and prepare an EventMessageHeader object.
     * @param json
     * @return
     */
    public EventMessageHeader readHeader(String json) {
        try {
            String eventClassName = null;
            BufferedReader bufferedReader = new BufferedReader(new StringReader(json));
            JsonReader reader = new JsonReader(bufferedReader);
            reader.beginObject();
            if(reader.hasNext()) {
                if ("header".equals(reader.nextName())) {
                    reader.beginObject();
                    String name = reader.nextName();
                    if("eventClassName".equals(name)) {
                        eventClassName = reader.nextString();
                    }
                    // Add logic to read new header properties here
                }
            }
            reader.close();

            if(eventClassName != null) {
                return new EventMessageHeader(eventClassName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not extract message header", e);
        }
        return null;
    }
}
