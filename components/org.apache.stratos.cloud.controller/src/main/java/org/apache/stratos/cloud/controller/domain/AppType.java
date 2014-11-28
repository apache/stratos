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
package org.apache.stratos.cloud.controller.domain;

import java.io.Serializable;

/**
 * domain mapping related data.
 *
 */
public class AppType implements Serializable{
	
    private static final long serialVersionUID = 3550489774139807168L;
	private String name;
	private boolean appSpecificMapping = true;
	
	public AppType(){
		
	}
	
	public AppType(String name){
		this.setName(name);
	}
	
	public AppType(String name, boolean appSpecificMapping){
		this.setName(name);
		this.setAppSpecificMapping(appSpecificMapping);
	}

	public String getName() {
	    return name;
    }

	public void setName(String name) {
	    this.name = name;
    }

	public boolean isAppSpecificMapping() {
	    return appSpecificMapping;
    }

	public void setAppSpecificMapping(boolean appSpecificMapping) {
	    this.appSpecificMapping = appSpecificMapping;
    }

}
