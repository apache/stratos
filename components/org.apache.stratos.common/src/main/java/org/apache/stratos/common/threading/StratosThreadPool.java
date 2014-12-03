/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements. See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership. The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.stratos.common.threading;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for Stratos thread pool
 */
public class StratosThreadPool {

	private static HashMap<String, ExecutorService> mapExecutorService = new HashMap<String, ExecutorService>();
	private static Object mutex = new Object();

	/**
	 * Return the executor service based on the identifier and thread pool size
	 *
	 * @param identifier     Component identifier name
	 * @param threadPoolSize Thread pool size
	 * @return ExecutorService
	 */
	public static ExecutorService getExecutorService(String identifier, int threadPoolSize) {
		ExecutorService executorService = mapExecutorService.get(identifier);
		if (executorService == null) {
			synchronized (mutex) {
				if (executorService == null) {
					executorService = Executors.newFixedThreadPool(threadPoolSize);
					mapExecutorService.put(identifier, executorService);
				}
			}

		}
		return executorService;

	}

}
