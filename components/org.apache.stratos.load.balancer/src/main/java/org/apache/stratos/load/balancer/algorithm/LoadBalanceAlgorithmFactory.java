/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.algorithm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Load balance algorithm factory to create the algorithm
 */
public class LoadBalanceAlgorithmFactory {
    private static final Log log = LogFactory.getLog(LoadBalanceAlgorithmFactory.class);


    public static LoadBalanceAlgorithm createAlgorithm(String className) {
        try {
            Class algorithmClass = Class.forName(className);
            try {
                Object instance = algorithmClass.getConstructor().newInstance();
                if (instance instanceof LoadBalanceAlgorithm) {
                    return (LoadBalanceAlgorithm) instance;
                } else {
                    throw new RuntimeException(String.format("Class %s is not a valid load balance algorithm implementation"));
                }
            } catch (NoSuchMethodException e) {
                if (log.isErrorEnabled()) {
                    log.error(e);
                }
            } catch (InstantiationException e) {
                if (log.isErrorEnabled()) {
                    log.error(e);
                }
            } catch (IllegalAccessException e) {
                if (log.isErrorEnabled()) {
                    log.error(e);
                }
            } catch (InvocationTargetException e) {
                if (log.isErrorEnabled()) {
                    log.error(e);
                }
            }

        } catch (ClassNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
        }
        return null;
    }
}
