/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.ui.valve;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.wso2.carbon.base.ServerConfiguration;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

public class CSRFValve extends ValveBase {

    private static String REFERER_HEADER = "referer";
    private static String CSRF_VALVE_PROPERTY = "Security.CSRFPreventionConfig";
    private static String ENABLED_PROPERTY = CSRF_VALVE_PROPERTY + ".Enabled";
    private static String WHITE_LIST_PROPERTY = CSRF_VALVE_PROPERTY + ".WhiteList.Url";
    private static String RULE_PATTERN_PROPERTY = CSRF_VALVE_PROPERTY + ".Patterns.Pattern";
    private static String RULE_PROPERTY = CSRF_VALVE_PROPERTY + ".Rule";
    private static String RULE_ALLOW = "allow";
    private static String RULE_DENY = "deny";
    private static String[] patternList;
    private static String[] whiteList;
    private static String rule;
    private static boolean csrfEnabled = false;

    /**
     * Load configuration
     */
    private void loadConfiguration() {

        ServerConfiguration serverConfiguration = ServerConfiguration.getInstance();
        whiteList = serverConfiguration.getProperties(WHITE_LIST_PROPERTY);
        patternList = serverConfiguration.getProperties(RULE_PATTERN_PROPERTY);
        rule = serverConfiguration.getFirstProperty(RULE_PROPERTY);
        if (whiteList.length > 0 && patternList.length > 0 && rule != null
                && serverConfiguration.getFirstProperty(ENABLED_PROPERTY) != null && Boolean
                .parseBoolean(serverConfiguration.getFirstProperty(ENABLED_PROPERTY))) {
            csrfEnabled = true;
        }
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        loadConfiguration();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        if (csrfEnabled) {
            validatePatterns(request);
        }
        getNext().invoke(request, response);

    }

    /**
     * Validate request context with pattern
     * @param request  Http Request
     * @throws ServletException
     */
    private void validatePatterns(Request request) throws ServletException {

        String context;

        if (request.getRequestURI().indexOf("/", 2) == -1) {
            context = request.getRequestURI().substring(request.getRequestURI().indexOf("/") + 1);
        } else {
            context = request.getRequestURI()
                    .substring(request.getRequestURI().indexOf("/") + 1, request.getRequestURI().indexOf(
                            "/", 2));
        }

        if (RULE_ALLOW.equals(rule)) {
            if (!Arrays.asList(patternList).contains(context)) {
                validateRefererHeader(request);
            }
        } else if (RULE_DENY.equals(rule)) {
            if (Arrays.asList(patternList).contains(context)) {
                validateRefererHeader(request);
            }
        }
    }

    /**
     * Validate referer header
     * @param request Http Request
     * @throws ServletException
     */
    private void validateRefererHeader(Request request) throws ServletException {

        String refererHeader = request.getHeader(REFERER_HEADER);

        boolean allow = false;
        if (refererHeader != null) {
            for (String ip : whiteList) {
                if (refererHeader.startsWith(ip)) {
                    allow = true;
                    break;
                }
            }
            if (!allow) {
                throw new ServletException("Possible CSRF attack. Refer header : " + refererHeader);
            }
        }
    }
}