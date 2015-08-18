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
package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.ui.deployment.beans.Component;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.wso2.carbon.ui.deployment.ComponentBuilder.build;

/**
 * Engine that process plugins
 */
public class DeploymentEngine {

    /**
     *
     */
    private Map<Long, Component> componentMap = new ConcurrentHashMap<Long, Component>();

    /**
     *
     */
    private ComponentDeployer componentDeployer;

    /**
     *
     */
    private static Log log = LogFactory.getLog(DeploymentEngine.class);

    /**
     *
     */
    private Bundle componentBundle;

    public DeploymentEngine(Bundle componentBundle) {
        this.componentBundle = componentBundle;
        this.componentDeployer = new ComponentDeployer(componentBundle);
    }


    public void process(Bundle registeredBundle) {
        try {
            URL url = registeredBundle.getEntry("META-INF/component.xml");
            if (url != null) {
                //found a Carbon OSGi bundle that should amend for admin UI
                Dictionary headers = registeredBundle.getHeaders();
                String bundleName = (String) headers.get("Bundle-Name");
                String bundleVersion = (String) headers.get("Bundle-Version");
                InputStream inputStream = url.openStream();
                Component component = build(inputStream,
                                            bundleName,
                                            bundleVersion,
                                            registeredBundle.getBundleContext());
                componentMap.put(registeredBundle.getBundleId(), component);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("", e);
        } catch (XMLStreamException e) {
            e.printStackTrace();
            log.error("", e);
        } catch (CarbonException e) {
            e.printStackTrace();
            log.error("", e);
        }
    }

    public void layout() throws CarbonException {
        componentDeployer.layout(componentMap);
    }

}
