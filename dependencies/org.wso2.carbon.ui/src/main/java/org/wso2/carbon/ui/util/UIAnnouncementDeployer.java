/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.ui.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.ui.UIAnnouncement;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class UIAnnouncementDeployer {

    private static Log log = LogFactory.getLog(UIAnnouncementDeployer.class);
    private static ServiceTracker uiAnnouncementTracker = null;

    public static String getAnnouncementHtml(HttpSession session, ServletConfig config) {
        UIAnnouncement uiAnnouncement = (UIAnnouncement) uiAnnouncementTracker.getService();
        if (uiAnnouncement == null) {
            return ""; // empty htmls
        }
        return uiAnnouncement.getAnnouncementHtml(session, config);
    }

    public static void deployNotificationSources() {
        BundleContext bundleContext = CarbonUIUtil.getBundleContext();
        uiAnnouncementTracker = new ServiceTracker(bundleContext,
                UIAnnouncement.class.getName(), null);
        uiAnnouncementTracker.open();
    }

    public static void undeployNotificationSources() {
        uiAnnouncementTracker.close();
    }
}
