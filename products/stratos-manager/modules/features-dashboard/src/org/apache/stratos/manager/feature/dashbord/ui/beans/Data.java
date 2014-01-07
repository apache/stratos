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
package org.apache.stratos.manager.feature.dashbord.ui.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Data {
	private Map<String, Service> service;

	public Data() {
		this.service = new HashMap<String, Service>();
	}

	public Map<String, Service> getServices() {
		return service;
	}

	public String [] getKeys() {
		Set<String>  temp = this.getServices().keySet();
		return  temp.toArray(new String[0]);
	}
	
	public  String [] getServiceNames() {
		List <String> keyList = new ArrayList<String>();
		for (Map.Entry<String, Service> entry : service.entrySet())
		{
			Service tempService = entry.getValue();
			keyList.add(tempService.getName());
		}
		return keyList.toArray(new String[keyList.size()]);
	}

	public Service getService(String key) {
		return this.getServices().get(key);
	}

	public void addService(Service service) {
		this.getServices().put(service.getKey(), service);
	}

}
