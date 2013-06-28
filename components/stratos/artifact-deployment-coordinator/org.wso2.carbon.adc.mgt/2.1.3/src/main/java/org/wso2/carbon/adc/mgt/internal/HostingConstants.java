/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.internal;

/**
 * Web Application Constants
 */
public final class HostingConstants {
	public static final String WEBAPP_PREFIX = "webapps";
	public static final String WEBAPP_DEPLOYMENT_FOLDER = "webapps";
	public static final String WEBAPP_EXTENSION = "war";

	public static final class WebappState {
		public static final String STARTED = "started";
		public static final String STOPPED = "stopped";

		private WebappState() {
		}
	}

	private HostingConstants() {
	}
}
