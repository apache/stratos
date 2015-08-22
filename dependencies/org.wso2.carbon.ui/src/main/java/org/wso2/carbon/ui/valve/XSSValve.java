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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.utils.CarbonUtils;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XSSValve extends ValveBase {

    private static String XSS_VALVE_PROPERTY = "Security.XSSPreventionConfig";
    private static String ENABLED_PROPERTY = XSS_VALVE_PROPERTY + ".Enabled";
    private static String RULE_PATTERN_PROPERTY = XSS_VALVE_PROPERTY + ".Patterns.Pattern";
    private static String RULE_PROPERTY = XSS_VALVE_PROPERTY + ".Rule";
    private static String XSS_EXTENSION_FILE_NAME = "xss-patterns.properties";
    private static boolean xssEnabled = false;
    private static String RULE_ALLOW = "allow";
    private static String RULE_DENY = "deny";
    private static String[] xssURIPatternList;
    private static String xssRule;
    private static String patterPath = "";
    private static ArrayList<Pattern> patternList;

    protected static final Log log = LogFactory.getLog(XSSValve.class);


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
            Pattern.compile("alert(.*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onMouse", Pattern.CASE_INSENSITIVE),
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
        xssURIPatternList = serverConfiguration.getProperties(RULE_PATTERN_PROPERTY);
        xssRule = serverConfiguration.getFirstProperty(RULE_PROPERTY);
        patterPath = CarbonUtils.getCarbonSecurityConfigDirPath() + "/" + XSS_EXTENSION_FILE_NAME;
        buildScriptPatterns();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        if (xssEnabled) {
            String context = request.getRequestURI().substring(request.getRequestURI().indexOf("/") + 1);
            if (RULE_ALLOW.equals(xssRule) && !isContextStartWithGivenPatterns(context)) {
                validateParameters(request);
            } else if (RULE_DENY.equals(xssRule) && isContextStartWithGivenPatterns(context)) {
                validateParameters(request);
            } else if(!(RULE_ALLOW.equals(xssRule) || RULE_DENY.equals(xssRule))){
                validateParameters(request);
            }

        }
        getNext().invoke(request, response);
    }

    private void validateParameters(Request request) throws ServletException{

        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {

            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramValue != null) {
                paramValue = paramValue.replaceAll("\0", "");
                for (Pattern scriptPattern : patternList) {
                    Matcher matcher = scriptPattern.matcher(paramValue);
                    if (matcher.find()) {
                        throw new ServletException(
                                "Possible XSS Attack. Suspicious code : " + matcher.toMatchResult().group());
                    }
                }
            }
        }
    }

    /**
     * Check whether context starts with defined pattern
     *
     * @param context
     * @return
     */
    private boolean isContextStartWithGivenPatterns(String context) {

        boolean patternMatched = false;

        for (String pattern : xssURIPatternList) {
            if (context.startsWith(pattern)) {
                patternMatched = true;
                break;
            }
        }
        return patternMatched;
    }

    private void buildScriptPatterns() {
        patternList = new ArrayList<Pattern>(Arrays.asList(patterns));
        if (patterPath != null && !patterPath.isEmpty()) {
            InputStream inStream = null;
            File xssPatternConfigFile = new File(patterPath);
            Properties properties = new Properties();
            if (xssPatternConfigFile.exists()) {
                try {
                    inStream = new FileInputStream(xssPatternConfigFile);
                    properties.load(inStream);
                } catch (FileNotFoundException e) {
                    log.error("Can not load xssPatternConfig properties file ", e);
                } catch (IOException e) {
                    log.error("Can not load xssPatternConfigFile properties file ", e);
                } finally {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e) {
                            log.error("Error while closing stream ", e);
                        }
                    }
                }
            }
            if (!properties.isEmpty()) {
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    patternList.add(Pattern.compile(value, Pattern.CASE_INSENSITIVE));
                }
            }

        }
    }

}