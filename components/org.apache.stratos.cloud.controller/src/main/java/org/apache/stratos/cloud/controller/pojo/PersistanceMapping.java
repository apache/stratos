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
package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;

public class PersistanceMapping implements Serializable{
	
	private static final long serialVersionUID = 3455721979991902731L;
	private boolean persistanceRequired;
	private int size;
	private String device;
	private boolean removeOntermination;		

    public String toString () {
        return "Persistance Required: " + isPersistanceRequired() + ", Size: " + size + ", device: " + device + " remove on termination " + removeOntermination;
    }

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return the device
	 */
	public String getDevice() {
		return device;
	}

	/**
	 * @param device the device to set
	 */
	public void setDevice(String device) {
		this.device = device;
	}

	/**
	 * @return the removeOntermination
	 */
	public boolean isRemoveOntermination() {
		return removeOntermination;
	}

	/**
	 * @param removeOntermination the removeOntermination to set
	 */
	public void setRemoveOntermination(boolean removeOntermination) {
		this.removeOntermination = removeOntermination;
	}

    public boolean isPersistanceRequired() {
        return persistanceRequired;
    }

    public void setPersistanceRequired(boolean persistanceRequired) {
        this.persistanceRequired = persistanceRequired;
    }
}
