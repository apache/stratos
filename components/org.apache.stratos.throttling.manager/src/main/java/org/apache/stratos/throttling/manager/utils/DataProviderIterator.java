/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.throttling.manager.utils;

import java.util.Iterator;
import java.util.List;

import org.apache.stratos.throttling.manager.conf.ThrottlingTaskDataProviderConfiguration;
import org.apache.stratos.throttling.manager.exception.ThrottlingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
