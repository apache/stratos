/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.ui;

import org.osgi.framework.Bundle;

import java.util.HashMap;
import java.util.Map;

public class BundleResourcePathRegistry {

    private Map<String, Bundle> bundleResourceMap;
    //This map needs to be synchronized
    public BundleResourcePathRegistry(){
        bundleResourceMap = new HashMap<String, Bundle>();
    }

    public Bundle getBundle(String bundleResourcePath){
        return bundleResourceMap.get(bundleResourcePath);
    }

    public void addBundleResourcePath(String bundleResourcePath, Bundle bundle){
        bundleResourceMap.put(bundleResourcePath, bundle);
    }
}
