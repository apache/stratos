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

package org.wso2.carbon.adc.mgt.utils;

/**
 *
 */
public class CartridgeAppType {
	private String appType;
	private boolean appSpecificMapping;

	public String getAppType() {
		return appType;
	}

	public void setAppType(String appType) {
		this.appType = appType;
	}

	public boolean isAppSpecificMapping() {
		return appSpecificMapping;
	}

	public void setAppSpecificMapping(boolean appSpecificMapping) {
		this.appSpecificMapping = appSpecificMapping;
	}
}
