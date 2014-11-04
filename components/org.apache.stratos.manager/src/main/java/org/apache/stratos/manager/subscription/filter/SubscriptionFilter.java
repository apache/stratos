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
package org.apache.stratos.manager.subscription.filter;

import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.common.xsd.Properties;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.subscription.SubscriptionData;

/**
 * Intercepts the flow of Cartridge Subscription.
 * Implementations of this class would get executed before making the real subscription.
 */
public interface SubscriptionFilter {

	/**
	 * Do some pre-processing on a subscription request.
	 * @param cartridgeInfo {@link CartridgeInfo} 
	 * @param subscriptionData {@link SubscriptionData}
	 * @return {@link Properties}, if there are any.
	 * @throws ADCException on a failure while processing.
	 */
	public Properties execute(CartridgeInfo cartridgeInfo, SubscriptionData subscriptionData) throws ADCException ;
}
