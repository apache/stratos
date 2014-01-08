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
package org.apache.stratos.manager.dto;

import java.util.Arrays;
import java.util.List;

import org.wso2.carbon.utils.Pageable;

/**
 * This class holds paginated information about cartridges
 */
public final class CartridgeWrapper implements Pageable {

	private Cartridge[] cartridges;
	private int numberOfPages;

	public CartridgeWrapper() {
	}

	public int getNumberOfPages() {
		return numberOfPages;
	}

	public void setNumberOfPages(int numberOfPages) {
		this.numberOfPages = numberOfPages;
	}

	public <T> void set(List<T> items) {
		this.cartridges = items.toArray(new Cartridge[items.size()]);
	}

	public Cartridge[] getCartridges() {
		return cartridges != null ? Arrays.copyOf(cartridges, cartridges.length) : null;
	}
}
