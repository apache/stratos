/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.ui.taglibs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

public class JSi18n extends BodyTagSupport {
    private static final Log log = LogFactory.getLog(JSi18n.class);

    private static final String ORG_WSO2_CARBON_I18N_JSRESOURCES =
            "org.wso2.carbon.i18n.JSResources";

    private String resourceBundle;
    private HttpServletRequest request;
    private String i18nObjectName = "jsi18n";
    private String namespace = null;

    public String getI18nObjectName() {
        if (namespace != null) {
            return namespace + "_" + i18nObjectName;
        }
        return i18nObjectName;
    }

    public void setI18nObjectName(String i18nObjectName) {
        this.i18nObjectName = i18nObjectName;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getResourceBundle() {
        return resourceBundle;
    }

    public void setResourceBundle(String resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public int doEndTag() throws JspException {
        if (request != null) {
            // Retrieve locale from either session or request headers
            Locale locale = getLocaleFromPageContext(pageContext);

            String jsString =
                    "<script type=\"text/javascript\" src='../yui/build/utilities/utilities.js'></script>" +
                            "<script type=\"text/javascript\" src='../yui/build/yahoo/yahoo-min.js'></script>" +
                            "<script type=\"text/javascript\" src='../yui/build/json/json-min.js'></script>" +
                            "<script type=\"text/javascript\"> var " +
                            ((getNamespace() != null) ? getNamespace() + "_" : "") + "tmpPairs = '{";

            boolean firstPair = true;

            // Retrieve the default carbon JS resource bundle
            ResourceBundle defaultBunndle = ResourceBundle.getBundle(
                    ORG_WSO2_CARBON_I18N_JSRESOURCES, locale);

            // Retrieve keys from the default bundle
            for (Enumeration e = defaultBunndle.getKeys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String value = defaultBunndle.getString(key);
                if (firstPair) {
                    jsString = jsString + "\"" + key + "\":\"" + value + "\"";
                    firstPair = false;
                } else {
                    jsString = jsString + ",\"" + key + "\":\"" + value + "\"";
                }
            }

            if (resourceBundle != null) {
                // Retrieve the resource bundle
                ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle, locale);

                // Retrieve keys from the user defined bundle
                for (Enumeration e = bundle.getKeys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    String value = bundle.getString(key);
                    if (firstPair) {
                        jsString = jsString + "\"" + key + "\":\"" + value + "\"";
                        firstPair = false;
                    } else {
                        jsString = jsString + ",\"" + key + "\":\"" + value + "\"";
                    }
                }
            }

            jsString = jsString + "}'; var " + getI18nObjectName() +
                    " = YAHOO.lang.JSON.parse(" +
                            ((getNamespace() != null) ? getNamespace() + "_" : "") + "tmpPairs);</script>";

            StringBuffer content = new StringBuffer();
            content.append(jsString);

            // Write to output
            JspWriter writer = pageContext.getOut();
            try {
                writer.write(content.toString());
            } catch (IOException e) {
                String msg = "Cannot write i18n tag content";
                log.error(msg, e);

                try {
                    //exit gracefully
                    writer.write("");
                } catch (IOException e1) {/*do nothing*/}
            }

        }

        return 0;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        namespace = namespace.replace('.', '_');
        this.namespace = namespace;
    }

    public static Locale getLocaleFromPageContext(PageContext pageContext)
    {
        if (pageContext.getSession().getAttribute(CarbonUIUtil.SESSION_PARAM_LOCALE) != null) {
            return CarbonUIUtil.toLocale(pageContext.getSession().getAttribute(CarbonUIUtil.SESSION_PARAM_LOCALE).toString());
        }else{
            return pageContext.getRequest().getLocale();
        }
    }
}
