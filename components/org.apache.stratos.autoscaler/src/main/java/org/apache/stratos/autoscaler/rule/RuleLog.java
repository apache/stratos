package org.apache.stratos.autoscaler.rule;
/*
 *
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
 *
*/


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Drools rule log for logging inside rule files.
 */
public class RuleLog {
    private static final Log log = LogFactory.getLog(RuleLog.class);

    private static volatile RuleLog instance;

    private RuleLog() {
    }

    public static RuleLog getInstance() {
        if (instance == null) {
            synchronized (RuleLog.class) {
                if (instance == null) {
                    instance = new RuleLog();
                }
            }
        }
        return instance;
    }

    public boolean info(String value) {
        if(log.isInfoEnabled()) {
            log.info(value);
        }
        return true;
    }

    public boolean debug(String value) {
        if(log.isDebugEnabled()) {
            log.debug(value);
        }
        return true;
    }

    public boolean warn(String value) {
        if(log.isWarnEnabled()) {
            log.warn(value);
        }
        return true;
    }

    public boolean error(String value) {
        if(log.isErrorEnabled()) {
            log.error(value);
        }
        return true;
    }
}
