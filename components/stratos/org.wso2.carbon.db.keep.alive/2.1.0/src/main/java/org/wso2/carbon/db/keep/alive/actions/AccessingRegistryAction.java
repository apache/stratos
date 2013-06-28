/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.db.keep.alive.actions;

import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;

import java.util.Date;

public class AccessingRegistryAction {
    private static final String KEEP_ALIVE_RESOURCE =
            "/repository/components/org.wso2.carbon.keep-alive-resource";
    Registry registry;
    public AccessingRegistryAction(Registry registry) {
        this.registry = registry;
    }
    public void execute() throws Exception {
        Collection c;
        if (registry.resourceExists(KEEP_ALIVE_RESOURCE)) {
            c = (Collection)registry.get(KEEP_ALIVE_RESOURCE);
        } else {
            c = registry.newCollection();
        }
        long currentTime = new Date().getTime();
        c.setProperty("currentTime", String.valueOf(currentTime));
        registry.put(KEEP_ALIVE_RESOURCE, c);
    }
}
