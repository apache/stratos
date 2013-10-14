/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.publish;

/**
 * Represents the template of a message publisher in Stratos.
 * All the message publisher should extend this class.
 * 
 * @author nirmal
 * 
 */
public abstract class MessagePublisher {

	private String name;

	public MessagePublisher(String name) {
		this.name = name;
	}

	/**
	 * Get the name of this publisher; if the publisher is a topic publisher,
	 * name would be the name of the topic.
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * This operation get triggered when a message is ready to be published.
	 * It is up to the publisher to decide, whether this message is an eligible
	 * one
	 * from its perspective.
	 * 
	 * @param message
	 *            POJO to be published.
	 */
	public abstract void publish(Object message);
}
