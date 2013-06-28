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
package org.wso2.carbon.sample.installer.ui;

import org.wso2.carbon.authenticator.proxy.AuthenticationAdminClient;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Util {
    private static final Log log = LogFactory.getLog(Util.class);

    private static String TENANT_REG_AGENT_CONF_FILENAME = "tenant-reg-agent.xml";

    private static List<ServerInfoBean> listenerServers = new ArrayList<ServerInfoBean>();


    public static String login(String serverUrl, String userName, 
                               String password, ConfigurationContext confContext) throws UserStoreException {
        String sessionCookie = null;
        try {
            AuthenticationAdminClient client =
                    new AuthenticationAdminClient(confContext, serverUrl, null, null, false);
            //TODO : get the correct IP
            boolean isLogin = client.login(userName, password, "127.0.0.1");
            if (isLogin) {
                sessionCookie = client.getAdminCookie();
            }
        } catch (Exception e) {
            throw new UserStoreException("Error in login to the server server: " + serverUrl +
                                         "username: " + userName + ".", e);
        }
        return sessionCookie;
    }

    private static void loadConfig() throws Exception {
        String configFilename = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator + TENANT_REG_AGENT_CONF_FILENAME;
        OMElement agentConfigEle = buildOMElement(new FileInputStream(configFilename));

        Iterator configChildren = agentConfigEle.getChildElements();
        while (configChildren.hasNext()) {
            Object configObj = configChildren.next();
            if (!(configObj instanceof OMElement)) {
                continue;
            }
            OMElement serverConfigEle = (OMElement) configObj;

            Iterator serverConfigChildren = serverConfigEle.getChildElements();
            ServerInfoBean server = new ServerInfoBean();
            while (serverConfigChildren.hasNext()) {
                Object childObj = serverConfigChildren.next();
                if (!(childObj instanceof OMElement)) {
                    continue;
                }

                OMElement childEle = (OMElement) childObj;

                if (new QName(null, "serverUrl").equals(childEle.getQName())) {
                    String serverUrl = childEle.getText();
                    server.setServerUrl(serverUrl);
                } else if (new QName(null, "userName").equals(childEle.getQName())) {
                    String userName = childEle.getText();
                    server.setUserName(userName);
                } else if (new QName(null, "password").equals(childEle.getQName())) {
                    String password = childEle.getText();
                    server.setPassword(password);
                }
            }
            listenerServers.add(server);
        }
    }

    

    private static OMElement buildOMElement(InputStream inputStream) throws Exception {
        XMLStreamReader parser;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            String msg = "Error in initializing the parser to build the OMElement.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        //create the builder
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        //get the root element (in this case the envelope)

        return builder.getDocumentElement();
    }

    public static ServerInfoBean[] getListenerServers() throws Exception {
        loadConfig();
        return listenerServers.toArray(new ServerInfoBean[listenerServers.size()]);
    }
}
