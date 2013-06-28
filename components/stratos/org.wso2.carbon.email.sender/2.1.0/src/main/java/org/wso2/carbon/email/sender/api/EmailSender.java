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
package org.wso2.carbon.email.sender.api;

import org.wso2.carbon.email.sender.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.transport.mail.MailConstants;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;

import java.util.Map;
import java.util.HashMap;

/**
 * The class that handles the email sending logic for stratos.
 */
public class EmailSender{
    private static Log log = LogFactory.getLog(EmailSender.class);
    private EmailSenderConfiguration config = null;

    public EmailSender(EmailSenderConfiguration config) {
        this.config = config;
    }

    /**
     * Sends the email
     * @param toEmail Email Address (To:)
     * @param userParameters - map of user parameters
     * @throws Exception, if sending the email notification failed.
     */
    public void sendEmail(String toEmail, Map<String, String>userParameters) throws Exception {
        final String subject = getMessageTitle(userParameters);
        final String body = getMessageBody(userParameters);
        final String email = toEmail; // no change in here,
        new Thread() {
            public void run() {
                Map<String, String> headerMap = new HashMap<String, String>();
                headerMap.put(MailConstants.MAIL_HEADER_SUBJECT, subject);
                OMElement payload = OMAbstractFactory.getOMFactory().createOMElement(
                        BaseConstants.DEFAULT_TEXT_WRAPPER, null);
                payload.setText(body);

                try {
                    ServiceClient serviceClient;
                    ConfigurationContext configContext = Util.getConfigurationContext();
                    if (configContext != null) {
                        serviceClient = new ServiceClient(configContext, null);
                    } else {
                        serviceClient = new ServiceClient();
                    }
                    Options options = new Options();
                    options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
                    options.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
                    options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT,
                                        MailConstants.TRANSPORT_FORMAT_TEXT);
                    options.setTo(new EndpointReference("mailto:" + email));
                    serviceClient.setOptions(options);
                    serviceClient.fireAndForget(payload);
                    log.debug("Sending confirmation mail to " + email);
                } catch (AxisFault e) {
                    String msg = "Error in delivering the message, " +
                                "subject: " + subject + ", to: " + email + ".";
                    log.error(msg);
                }
            }
        }.start();
    }

    /**
     * gets the title of the message
     * @param userParameters - map of user parameters
     * @return the title of the message
     */
    protected String getMessageTitle(Map<String, String> userParameters) {
        return Util.replacePlaceHolders(config.getSubject(), userParameters);
    }

    /**
     * gets the body of the message
     * @param userParameters - map of user parameters
     * @return the body of the message
     */
    protected String getMessageBody(Map<String, String> userParameters) {
        return Util.replacePlaceHolders(config.getBody(), userParameters);
    }
}