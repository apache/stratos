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
package org.wso2.carbon.billing.core.conf;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingConstants;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.ndatasource.common.DataSourceException;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class reads the billing-config.xml file and
 * keeps the billing task configurations in a map.
 * There are two configurations, one for scheduled bill generation
 * and the other for on-demand bill generation
 */
public class BillingConfiguration {
    private static final Log log = LogFactory.getLog(BillingConfiguration.class);
    DataSource dataSource;
    Map<String, BillingTaskConfiguration> billingTaskConfigs = new HashMap<String, BillingTaskConfiguration>();

    public BillingConfiguration(String billingConfigFile) throws BillingException {
        try {
            dataSource = (DataSource) Util.getDataSourceService().getDataSource(BillingConstants.WSO2_BILLING_DS).getDSObject();
            OMElement billingConfig = buildOMElement(new FileInputStream(billingConfigFile));
            deserialize(billingConfig);
        } catch (FileNotFoundException e) {
            String msg = "Unable to find the file responsible for billing task configs: "
                            + billingConfigFile;
            log.error(msg, e);
            throw new BillingException(msg, e);
        } catch (DataSourceException e) {
            String msg = "Error retrieving Billing datasource from master-datasources.xml configuration.";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }
    }

    private OMElement buildOMElement(InputStream inputStream) throws BillingException {
        XMLStreamReader parser;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            String msg = "Error in initializing the parser to build the OMElement.";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        StAXOMBuilder builder = new StAXOMBuilder(parser);
        return builder.getDocumentElement();
    }
    
    /*
       Deserialize the following
       <billingConfig xmlns="http://wso2.com/carbon/multitenancy/billing/config">
           <dbConfig>
                ...
           </dbConfig>
           <tasks>
                <task id="multitenancyScheduledTask">
                    ...
                </task>
                <task id="multitenancyViewingTask">
                    ...
                </task>
            </tasks>
        </billingConfig>
     */
    private void deserialize(OMElement billingConfigEle) throws BillingException {
        Iterator billingConfigChildIt = billingConfigEle.getChildElements();
        
        while (billingConfigChildIt.hasNext()) {
            OMElement billingConfigChildEle = (OMElement) billingConfigChildIt.next();
            
            if (new QName(BillingConstants.CONFIG_NS, BillingConstants.TASKS,
                    BillingConstants.NS_PREFIX).equals(billingConfigChildEle.getQName())) {
                //element is "tasks"
                Iterator taskConfigChildIt = billingConfigChildEle.getChildElements();
                while (taskConfigChildIt.hasNext()) {
                    OMElement taskConfigEle = (OMElement) taskConfigChildIt.next();
                    String id = taskConfigEle.getAttributeValue(new QName(BillingConstants.ATTR_ID));
                    BillingTaskConfiguration taskConfig =
                            new BillingTaskConfiguration(id, taskConfigEle);
                    billingTaskConfigs.put(id, taskConfig);
                }
            } else {
                String msg = "Unknown element in Billing Configuration: " +
                                billingConfigChildEle.getQName().getLocalPart();
                log.error(msg);
                throw new BillingException(msg);
            }
        }
    }

    /*
     * Deserialise dbConfigElement (Given below) and initialize data source
        <dbConfig>
            <url>jdbc:mysql://localhost:3306/billing</url>
            <userName>billing</userName>
            <password>billing</password>
            <driverName>com.mysql.jdbc.Driver</driverName>
            <maxActive>80</maxActive>
            <maxWait>60000</maxWait>
            <minIdle>5</minIdle>
            <validationQuery>SELECT 1</validationQuery>
        </dbConfig>
     */
    /*private void initDataSource(OMElement dbConfigEle) throws BillingException {
        // initializing the data source and load the database configurations
        Iterator dbConfigChildIt = dbConfigEle.getChildElements();
        dataSource = new BasicDataSource();
        
        while (dbConfigChildIt.hasNext()) {
            
            OMElement dbConfigChildEle = (OMElement) dbConfigChildIt.next();
            if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_URL,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setUrl(dbConfigChildEle.getText());
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_USER_NAME,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setUsername(dbConfigChildEle.getText());
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_PASSWORD,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setPassword(dbConfigChildEle.getText());
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_DRIVER_NAME,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setDriverClassName(dbConfigChildEle.getText());
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_MAX_ACTIVE,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setMaxActive(Integer.parseInt(dbConfigChildEle.getText()));
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_MAX_WAIT,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setMaxWait(Integer.parseInt(dbConfigChildEle.getText()));
            } else if (new QName(BillingConstants.CONFIG_NS, BillingConstants.DBCONFIG_MIN_IDLE,
                    BillingConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setMinIdle(Integer.parseInt(dbConfigChildEle.getText()));
            } else if (new QName(BillingConstants.CONFIG_NS, 
                    BillingConstants.DBCONFIG_VALIDATION_QUERY, BillingConstants.NS_PREFIX)
                    .equals(dbConfigChildEle.getQName())) {
                dataSource.setValidationQuery(dbConfigChildEle.getText());
            } else {
                String msg = "Unknown element in DBConfig of Billing Configuration: " +
                                dbConfigChildEle.getQName().getLocalPart();
                log.error(msg);
                throw new BillingException(msg);
            }
        }
    }*/

    public Map<String, BillingTaskConfiguration> getBillingTaskConfigs() {
        return billingTaskConfigs;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
