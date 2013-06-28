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
package org.wso2.carbon.lb.common.cache;

import org.wso2.carbon.lb.common.util.DomainMapping;
import java.util.Map;

public class URLMappingCache {
    private Map<String,DomainMapping> validMappings;

    private static URLMappingCache instance = null;
    protected URLMappingCache(int maxValidKeys) {
        validMappings = new LRUCache<String, DomainMapping>(maxValidKeys);
    }

    public void addValidMapping(String hostName, DomainMapping mapping) {
        validMappings.put(hostName, mapping);
    }

    public DomainMapping getMapping(String hostName) {
        return validMappings.get(hostName);
    }

    public static URLMappingCache getInstance(int maxValidKeys) {
      if(instance == null) {

         instance = new URLMappingCache(maxValidKeys);
      }
      return instance;
   }
}
