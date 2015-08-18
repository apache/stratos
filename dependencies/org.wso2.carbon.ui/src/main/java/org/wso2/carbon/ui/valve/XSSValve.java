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
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XSSValve extends ValveBase {

    private static String XSS_VALVE_PROPERTY = "Security.XSSPreventionConfig";
    private static String ENABLED_PROPERTY = XSS_VALVE_PROPERTY + ".Enabled";
    private static boolean xssEnabled = false;

    private static Pattern[] patterns = new Pattern[] {
            Pattern.compile("<input", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<body", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<link", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<link", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<img", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        loadConfiguration();
    }

    /**
     * Load configuration
     */
    private void loadConfiguration() {

        ServerConfiguration serverConfiguration = ServerConfiguration.getInstance();
        if (serverConfiguration.getFirstProperty(ENABLED_PROPERTY) != null && Boolean.parseBoolean(
                serverConfiguration.getFirstProperty(ENABLED_PROPERTY))) {
            xssEnabled = true;
        }
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        if (xssEnabled) {
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {

                String paramName = parameterNames.nextElement();
                String paramValue = request.getParameter(paramName);
                if (paramValue != null) {
                    paramValue = paramValue.replaceAll("\0", "");
                    for (Pattern scriptPattern : patterns) {
                        Matcher matcher = scriptPattern.matcher(paramValue);
                        if (matcher.find()) {
                            throw new ServletException(
                                    "Possible XSS Attack. Suspicious code : " + matcher.toMatchResult().group());
                        }
                    }
                }
            }
        }
        getNext().invoke(request, response);
    }
}