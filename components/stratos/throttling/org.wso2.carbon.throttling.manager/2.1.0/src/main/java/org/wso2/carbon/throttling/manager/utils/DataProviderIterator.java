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
package org.wso2.carbon.throttling.manager.utils;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.throttling.manager.conf.ThrottlingTaskDataProviderConfiguration;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;

// we are dynamically initializing handlers, so uses an iterator to iteration
public class DataProviderIterator implements Iterator {
    private static final Log log = LogFactory.getLog(DataProviderIterator.class);
    private ThreadLocal<Integer> index = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    List<ThrottlingTaskDataProviderConfiguration> dataProviderConfigs;
    
    public DataProviderIterator(List<ThrottlingTaskDataProviderConfiguration> dataProviderConfigs) {
        this.dataProviderConfigs = dataProviderConfigs;
        reset();
    }

    public boolean hasNext() {
        int i = index.get();
        return (i < dataProviderConfigs.size());
    }

    public Object next() {
        int i = index.get();
        ThrottlingTaskDataProviderConfiguration handlerConfig = dataProviderConfigs.get(i++);
        index.set(i);
        try {
            return handlerConfig.getDataProvider();
        } catch (ThrottlingException e) {
            String msg = "DataProvider for the dataProviderConfigs config is null. " +
            		"dataProviderConfigs index: " + (i - 1) + ".";
            log.error(msg);                        
        }
        return null;
    }

    public void remove() {
        // doesn't need to be remove
    }

    // additional method
    public void reset() {
        index.set(0);
    }
}
