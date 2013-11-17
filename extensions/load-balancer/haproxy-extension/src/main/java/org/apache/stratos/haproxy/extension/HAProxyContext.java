/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.haproxy.extension;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HAProxy context to read and store system properties.
 */
public class HAProxyContext {
    private static final Log log = LogFactory.getLog(HAProxyContext.class);
    private static volatile HAProxyContext context;

    private String executableFilePath;
    private String processIdFilePath;
    private String templatePath;
    private String templateName;
    private String scriptsPath;
    private String confFilePath;
    private String statsSocketFilePath;

    private HAProxyContext() {
        this.executableFilePath = System.getProperty("executable.file.path");
        this.templatePath = System.getProperty("templates.path");
        this.templateName = System.getProperty("templates.name");
        this.scriptsPath = System.getProperty("scripts.path");
        this.confFilePath = System.getProperty("conf.file.path");
        this.statsSocketFilePath = System.getProperty("stats.socket.file.path");

        if (log.isDebugEnabled()) {
            log.debug("executable.file.path = " + executableFilePath);
            log.debug("templates.path = " + templatePath);
            log.debug("templates.name = " + templateName);
            log.debug("scripts.path = " + scriptsPath);
            log.debug("conf.file.path = " + confFilePath);
            log.debug("stats.socket.file.path = " + statsSocketFilePath);
        }
    }

    public static HAProxyContext getInstance() {
        if (context == null) {
            synchronized (HAProxyContext.class) {
                if (context == null) {
                    context = new HAProxyContext();
                }
            }
        }
        return context;
    }

    public void validate() {
        if ((StringUtils.isEmpty(executableFilePath)) || (StringUtils.isEmpty(templatePath)) || (StringUtils.isEmpty(templateName)) ||
                (StringUtils.isEmpty(scriptsPath)) || (StringUtils.isEmpty(confFilePath)) || (StringUtils.isEmpty(statsSocketFilePath))) {
            throw new RuntimeException("Required system properties were not found: executable.file.path, templates.path, templates.name, scripts.path, conf.file.path, stats.socket.file.path");
        }
    }

    public String getExecutableFilePath() {
        return executableFilePath;
    }

    public String getProcessIdFilePath() {
        return processIdFilePath;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getScriptsPath() {
        return scriptsPath;
    }

    public String getConfFilePath() {
        return confFilePath;
    }

    public String getStatsSocketFilePath() {
        return statsSocketFilePath;
    }
}
