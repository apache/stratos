/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.ui.menu.stratos.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.ui.menu.stratos.Util;

/**
 * @scr.component name="org.wso2.carbon.ui.menu.general.internal.StratosUIGeneralMenuDSComponent"
 * immediate="true"
 */
public class StratosUIGeneralMenuDSComponent {
    private static Log log = LogFactory.getLog(StratosUIGeneralMenuDSComponent.class);

    protected void activate(ComponentContext ctxt) {
        try {
            // read the Manager Homepage URL from the config. files.
            Util.readManagerURLFromConfig();
            if (log.isDebugEnabled()) {
                log.debug("Stratos UI General Menu bundle is activated.");
            }
        } catch (StratosException igonre) {
            // ignore this exception, as the default value is used if it fails to read it from here.
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if(log.isDebugEnabled()){
            log.debug("Stratos UI General Menu bundle is deactivated.");
        }
    }
    
}
