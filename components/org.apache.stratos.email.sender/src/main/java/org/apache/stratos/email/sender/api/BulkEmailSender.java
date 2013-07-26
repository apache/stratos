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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.mail.MailConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.email.sender.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class that handles the bulk email sending logic for stratos.
 */
public class BulkEmailSender extends EmailSender{
    private static Log log = LogFactory.getLog(BulkEmailSender.class);

    public BulkEmailSender(EmailSenderConfiguration config) {
        super(config);
    }

    /**
     * Sends the email
     * @param bulkEmailData List of email data holder objects
     * @throws Exception, if sending the email notification failed.
     */
    public void sendBulkEmails(List<EmailDataHolder> bulkEmailData) throws Exception {

        final List<EmailDataHolder> emailDataHolderList = bulkEmailData;

        final ServiceClient serviceClient;
        ConfigurationContext configContext = Util.getConfigurationContext();
        if (configContext != null) {
            serviceClient = new ServiceClient(configContext, null);
        } else {
            serviceClient = new ServiceClient();
        }

        new Thread() {
            public void run() {

                for(EmailDataHolder dataHolder : emailDataHolderList){

                    String subject = getMessageTitle(dataHolder.getEmailParameters());
                    String body = getMessageBody(dataHolder.getEmailParameters());
                    String email = dataHolder.getEmail(); // no change in here,

                    Map<String, String> headerMap = new HashMap<String, String>();
                    headerMap.put(MailConstants.MAIL_HEADER_SUBJECT, subject);
                    OMElement payload = OMAbstractFactory.getOMFactory().createOMElement(
                            BaseConstants.DEFAULT_TEXT_WRAPPER, null);
                    payload.setText(body);

                    try {
                        Options options = new Options();
                        options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
                        options.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
                        options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT,
                                MailConstants.TRANSPORT_FORMAT_TEXT);
                        options.setTo(new EndpointReference("mailto:" + email));
                        serviceClient.setOptions(options);
                        serviceClient.fireAndForget(payload);
                        serviceClient.cleanup();
                    } catch (AxisFault e) {
                        String msg = "Error in delivering the message, " +
                                "subject: " + subject + ", to: " + email + ".";
                        log.error(msg);
                    }
                }

            }
        }.start();
    }

}
