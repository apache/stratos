/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.wso2.carbon.ui;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ServerConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class CarbonConnection extends URLConnection {

    private static final Log log = LogFactory.getLog(CarbonConnection.class);

    private byte[] buf = null;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url     the specified URL.
     * @param context BundleContext
     */
    protected CarbonConnection(URL url, BundleContext context) throws Exception {
        super(url);
        ConfigurationContext configContext;
        configContext = CarbonUIServiceComponent
                .getConfigurationContextService().getServerConfigContext();

        TransportInDescription httpsInDescription = configContext.getAxisConfiguration().
                getTransportIn(ServerConstants.HTTPS_TRANSPORT);
        Parameter proxyParameter = httpsInDescription.getParameter("proxyPort");
        String httpsProxyPort = null;
        if (proxyParameter != null && !"-1".equals(proxyParameter.getValue())) {
            httpsProxyPort = (String) proxyParameter.getValue();
        }

        TransportInDescription httpInDescription = configContext.getAxisConfiguration().
                getTransportIn(ServerConstants.HTTP_TRANSPORT);
        if (httpInDescription != null) {
            proxyParameter = httpInDescription.getParameter("proxyPort");
        }
        String httpProxyPort = null;
        if (proxyParameter != null && !"-1".equals(proxyParameter.getValue())) {
            httpProxyPort = (String) proxyParameter.getValue();
        }
        try {
            String servicePath = configContext.getServicePath();
            String contextRoot = configContext.getContextRoot();
            contextRoot = contextRoot.equals("/") ? "" : contextRoot;
            String httpsPort;
            if (httpsProxyPort != null) {
                httpsPort = httpsProxyPort;
            } else {
                httpsPort =
                        CarbonUtils.
                                getTransportPort(CarbonUIServiceComponent.getConfigurationContextService(),
                                                 "https") + "";
            }
            String httpPort;
            if (httpProxyPort != null) {
                httpPort = httpProxyPort;
            } else {
                if (httpInDescription != null) {
                    httpPort =
                            CarbonUtils.
                                    getTransportPort(CarbonUIServiceComponent.getConfigurationContextService(),
                                                     "http") + "";
                } else {
                    httpPort = "-1";
                }
            }
            buf = ("var SERVICE_PATH=\"" + servicePath + "\";\n" +
                   "var ROOT_CONTEXT=\"" + contextRoot + "\";\n" +
                   "var HTTP_PORT=" + httpPort + ";\n" +
                   "var HTTPS_PORT=" + httpsPort + ";\n").getBytes();
        } catch (Exception e) {
            String msg = "Error occurred while getting connection properties";
            log.error(msg, e);
        }
    }

    public void connect() throws IOException {
        //Ignore; no need to have this
    }

    public InputStream getInputStream() throws IOException {
//        String s = getURL().getPath();
        return new ByteArrayInputStream(buf);
    }

    public String getContentType() {
        return "text/javascript";
    }

    public int getContentLength() {
        return buf.length;
    }


}
