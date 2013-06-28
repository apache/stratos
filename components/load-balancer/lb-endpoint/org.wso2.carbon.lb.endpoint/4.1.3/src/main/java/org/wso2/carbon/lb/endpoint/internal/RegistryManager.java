/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.lb.endpoint.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;
import org.wso2.carbon.lb.common.util.DomainMapping;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class RegistryManager {
    UserRegistry governanceRegistry = ConfigHolder.getInstance().getGovernanceRegistry();
    private static final Log log = LogFactory.getLog(RegistryManager.class);
    /**
     *
     */
    private Resource resource = null;
    public static final String HOST_INFO = "hostinfo/";
    public static final String ACTUAL_HOST = "actual.host";

    public DomainMapping getMapping(String hostName) {
        DomainMapping domainMapping;
        try {
            if (governanceRegistry.resourceExists(HOST_INFO + hostName)) {
                resource = governanceRegistry.get(HOST_INFO + hostName);
                domainMapping = new DomainMapping(hostName);
                domainMapping.setActualHost(resource.getProperty(ACTUAL_HOST));
                return domainMapping;
            }
        } catch (RegistryException e) {
            log.info("Error while getting registry resource");
            throw new RuntimeException(e);
        }
        return null;
    }
}
