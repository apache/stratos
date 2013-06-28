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
package org.wso2.carbon.billing.test;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.*;

public class TestUtils {

    private static final String RULE_COMPONENT_CONF = "rule-component.conf";

    public static void deleteAllTables() throws BillingException {
        DataAccessObject dataAccessObject = BillingManager.getInstance().getDataAccessObject();
        boolean succeeded = false;
        try {
            dataAccessObject.beginTransaction();
            /*dataAccessObject.deleteInvoiceSubscriptionItems();
            dataAccessObject.deleteInvoiceSubscriptions();
            dataAccessObject.deletePaymentSubscriptions();
            dataAccessObject.deletePayments();
            dataAccessObject.deleteInvoice();
            dataAccessObject.deleteSubscribers();
            dataAccessObject.deleteItems();
            dataAccessObject.deleteCustomers();
            */
            succeeded = true;
        } finally {
            if (succeeded) {
                dataAccessObject.commitTransaction();
            } else {
                dataAccessObject.rollbackTransaction();
            }
        }
    }

    public static OMElement loadConfigXML() throws Exception {

        String carbonHome = System.getProperty(ServerConstants.CARBON_CONFIG_DIR_PATH);
        String path = carbonHome + File.separator + RULE_COMPONENT_CONF;
        BufferedInputStream inputStream = null;
        try {
            File file = new File(path);
            if (!file.exists()) {
                inputStream = new BufferedInputStream(
                        new ByteArrayInputStream("<RuleServer/>".getBytes()));
            } else {
                inputStream = new BufferedInputStream(new FileInputStream(file));
            }
            XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            return builder.getDocumentElement();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }
}
