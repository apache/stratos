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
import java.util.Arrays;
/**
 * Had to wrap {@link NetworkInterface} array using a class, since there's a bug in current 
 * stub generation.
 * 
 * @author Jeffrey Nguyen
 *
 */
public class NetworkInterfaces implements Serializable {

	private static final long serialVersionUID = -8435710709813227055L;
	private NetworkInterface[] networkInterfaces;

	/**
	 * @return the networkInterfaces
	 */
	public NetworkInterface[] getNetworkInterfaces() {
		return networkInterfaces;
	}

	/**
	 * @param networkInterfaces the networkInterfaces to set
	 */
	public void setNetworkInterfaces(NetworkInterface[] networkInterfaces) {
        if(networkInterfaces == null) {
            this.networkInterfaces = new NetworkInterface[0];
        } else {
            this.networkInterfaces = Arrays.copyOf(networkInterfaces, networkInterfaces.length);
        }
	}

	@Override
    public String toString() {
        return "NetworkInterfaces [network interfaces=" + Arrays.toString(networkInterfaces) + "]";
    }
}
