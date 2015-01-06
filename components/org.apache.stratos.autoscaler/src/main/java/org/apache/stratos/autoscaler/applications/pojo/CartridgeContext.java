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
package org.apache.stratos.autoscaler.applications.pojo;

import java.io.Serializable;


public class CartridgeContext implements Serializable {

    private static final long serialVersionUID = 7782017881026018352L;

	private String type;
	private int cartridgeMin;
	private int cartridgeMax;
	private SubscribableInfoContext subscribableInfoContext;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getCartridgeMin() {
		return cartridgeMin;
	}

	public void setCartridgeMin(int cartridgeMin) {
		this.cartridgeMin = cartridgeMin;
	}

	public int getCartridgeMax() {
		return cartridgeMax;
	}

	public void setCartridgeMax(int cartridgeMax) {
		this.cartridgeMax = cartridgeMax;
	}

	public SubscribableInfoContext getSubscribableInfoContext() {
		return subscribableInfoContext;
	}

	public void setSubscribableInfoContext(
	        SubscribableInfoContext subscribableInfoContext) {
		this.subscribableInfoContext = subscribableInfoContext;
	}
	
}
