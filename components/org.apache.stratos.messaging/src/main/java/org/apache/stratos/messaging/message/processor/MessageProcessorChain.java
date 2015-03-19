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

package org.apache.stratos.messaging.message.processor;

import org.apache.stratos.messaging.listener.EventListener;

import java.util.LinkedList;

/**
 * Message processor chain definition.
 */
public abstract class MessageProcessorChain {

    private LinkedList<MessageProcessor> list;

    public MessageProcessorChain() {
        list = new LinkedList<MessageProcessor>();
        initialize();
    }

    protected abstract void initialize();

    public abstract void addEventListener(EventListener eventListener);

    public void add(MessageProcessor messageProcessor) {
        if (list.size() > 0) {
            list.getLast().setNext(messageProcessor);
        }
        list.add(messageProcessor);
    }

    public void removeLast() {
        list.removeLast();
        if (list.size() > 0) {
            list.getLast().setNext(null);
        }
    }

    public boolean process(String type, String message, Object object) {
        MessageProcessor root = list.getFirst();
        if (root == null) {
            throw new RuntimeException("Message processor chain is not initialized");
        }
        return root.process(type, message, object);
    }
}
