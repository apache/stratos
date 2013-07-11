/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.theme.mgt.ui.utils;

import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletConfig;

public class ThemeUtil {
	public static String getThemeResourceDownloadURL(String path) {
		return "../../registry/themeResourceContent?path=" + path;
	}

	public static String getThemeResourceViewAsImageURL(String path) {
		return "../../registry/themeResourceContent?path=" + path
		        + "&viewImage=1";
	}

	public static String getThumbUrl(HttpServletRequest request, String themeName) {
		String serverURL = CarbonUIUtil.getAdminConsoleURL(request);
		String serverRoot = serverURL.substring(0, serverURL.length()
		        - "carbon/".length());
		return serverRoot + "registry/resource"
		        + RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
		        + "/repository/components/org.wso2.carbon.all-themes/"
		        + themeName + "/thumb.png";
	}

	public static String getLogoURL(ServletConfig config, HttpSession session) {
		// this is to avoid the caching problem
		String randomUUID = UUIDGenerator.generateUUID();
		String serverURL = CarbonUIUtil.getServerURL(
		        config.getServletContext(), session);
		String serverRoot = serverURL.substring(0, serverURL.length() - "services/".length());
		String tenantDomain = (String) session
		        .getAttribute(MultitenantConstants.TENANT_DOMAIN);
		if (tenantDomain == null) {
			return "";
		}

        // Using relative path instead of the back-end server url.
        return  "../../../../t/" + tenantDomain + "/registry/resource"
		        + RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
		        + "/repository/theme/admin/logo.gif?thatsrnd=" + randomUUID;
	}
}
