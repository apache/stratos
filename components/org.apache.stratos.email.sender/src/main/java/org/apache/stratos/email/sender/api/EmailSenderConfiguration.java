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
package org.apache.stratos.email.sender.api;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The class that handles the configuration of the email sender for Stratos
 */
public class EmailSenderConfiguration {
    private static Log log = LogFactory.getLog(EmailSenderConfiguration.class);

    public final static String DEFAULT_VALUE_SUBJECT = "EmailSender";
    public final static String DEFAULT_VALUE_MESSAGE = "Sent form WSO2 Carbon";

    private String fromEmail;
    private String subject = DEFAULT_VALUE_SUBJECT;
    private String body = DEFAULT_VALUE_MESSAGE;
    private Map<String, String> customParameters;

    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters) {
        this.customParameters = customParameters;
    }


    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject.trim();
    }

    public String getBody() {
        return body;
    }

    public void setBody(String emailMessage) {
        this.body = emailMessage.trim();
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    /**
     * Loads the email sender configuration
     * @param configFilename configuration file name
     * @return EmailSenderConfiguration.
     */    
    public static EmailSenderConfiguration loadEmailSenderConfiguration(String configFilename) {
        File configFile = new File(configFilename);
        if (!configFile.exists()) {
            log.error("Email sender configuration File is not present at: " + configFilename);
            return null;
        }
        EmailSenderConfiguration config = new EmailSenderConfiguration();
        FileInputStream ip = null;
        try {
            ip = new FileInputStream(configFile);
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(ip);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement = builder.getDocumentElement();
            Iterator it = documentElement.getChildElements();
            while (it.hasNext()) {
                OMElement element = (OMElement) it.next();
                if ("subject".equals(element.getLocalName())) {
                    config.setSubject(element.getText());
                } else if ("body".equals(element.getLocalName())) {
                    config.setBody(element.getText());
                } else if ("customParameters".equals(element.getLocalName())) {
                    Map<String, String> customParameters = new HashMap<String, String>();
                    Iterator customParamIt = element.getChildElements();
                    while (customParamIt.hasNext()) {
                        OMElement customElement = (OMElement) it.next();
                        customParameters.put(customElement.getLocalName(), customElement.getText());
                    }
                    config.setCustomParameters(customParameters);
                }
            }
            return config;
        } catch (Exception e) {
            String msg = "Error in loading configuration for email verification: " +
                         configFilename + ".";
            log.error(msg, e);
            return null;
        } finally {
            if (ip != null) {
                try {
                    ip.close();
                } catch (IOException e) {
                    log.warn("Could not close InputStream for file " + configFile.getAbsolutePath());
                }
            }

        }
    }
}
