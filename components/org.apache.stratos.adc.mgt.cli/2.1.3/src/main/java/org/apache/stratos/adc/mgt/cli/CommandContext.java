/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.adc.mgt.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

public class CommandContext extends Observable {

	/**
	 * Properties for the context.
	 */
	private Map<String, Object> properties;

	/**
	 * The application
	 */
	private final CommandLineApplication<? extends CommandContext> application;

	public CommandContext(CommandLineApplication<? extends CommandContext> application) {
		properties = new HashMap<String, Object>();
		this.application = application;
	}

	public final CommandLineApplication<? extends CommandContext> getApplication() {
		return application;
	}

	/**
	 * Set property in the context
	 * 
	 * @param key
	 *            The key
	 * @param o
	 *            The value for the key
	 * @return The previous value or null
	 */
	public Object put(String key, Object o) {
		Object previous = properties.put(key, o);
		setChanged();
		notifyObservers();
		return previous;
	}

	/**
	 * Get property value from the context
	 * 
	 * @param key
	 *            The key
	 * @return The value
	 */
	public Object getObject(String key) {
		return properties.get(key);
	}

	/**
	 * Get the string value, or null
	 * 
	 * @param key
	 *            The key
	 * @return The string value, or null.
	 */
	public String getString(String key) {
		Object o = getObject(key);
		if (o instanceof String) {
			return (String) o;
		}
		return null;
	}

}
