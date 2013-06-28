/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.status.monitor.core;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.status.monitor.core.beans.AuthConfigBean;
import org.wso2.carbon.status.monitor.core.beans.SampleTenantConfigBean;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.exception.StatusMonitorException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Builds the status monitor configurations from the configuration file, status-monitor-config.xml.
 */
public class StatusMonitorConfigurationBuilder {
    private static final Log log = LogFactory.getLog(StatusMonitorConfigurationBuilder.class);
    private static BasicDataSource dataSource;
    private static AuthConfigBean authConfigBean;
    private static SampleTenantConfigBean sampleTenantConfigBean;


    public StatusMonitorConfigurationBuilder(String statusConfigFile) throws StatusMonitorException {
        try {
            OMElement statusConfig = buildOMElement(new FileInputStream(statusConfigFile));
            deserialize(statusConfig);
            if (log.isDebugEnabled()) {
                log.debug("********Status Monitor Configuration Builder**********" + statusConfigFile);
            }
        } catch (FileNotFoundException e) {
            String msg = "Unable to find the file responsible for status monitor configs: "
                            + statusConfigFile;
            log.error(msg, e);
            throw new StatusMonitorException(msg, e);
        }
    }

    private OMElement buildOMElement(InputStream inputStream) throws StatusMonitorException {
        XMLStreamReader parser;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            String msg = "Error in initializing the parser to build the OMElement.";
            log.error(msg, e);
            throw new StatusMonitorException(msg, e);
        }

        StAXOMBuilder builder = new StAXOMBuilder(parser);
        return builder.getDocumentElement();
    }

    /*
       Deserialize the following
       <billingConfig xmlns="http://wso2.com/carbon/status.monitor.config">
           <dbConfig>
                ...
           </dbConfig>
        </billingConfig>
     */
    private void deserialize(OMElement statusMonitorConfigEle) throws StatusMonitorException {
        Iterator statusMonitorConfigChildIt = statusMonitorConfigEle.getChildElements();

        while (statusMonitorConfigChildIt.hasNext()) {
            OMElement statusMonitorConfigChildEle = (OMElement) statusMonitorConfigChildIt.next();

            if (new QName(StatusMonitorConstants.CONFIG_NS, StatusMonitorConstants.DB_CONFIG,
                    StatusMonitorConstants.NS_PREFIX).equals(statusMonitorConfigChildEle.getQName())) {
                //element is "dbConfig"
                initDataSource(statusMonitorConfigChildEle);
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.AUTH_CONFIG,
                    StatusMonitorConstants.NS_PREFIX).equals(statusMonitorConfigChildEle.getQName())) {
                //element is "authConfig"
                initAuthentication(statusMonitorConfigChildEle);
            } else if (new QName(StatusMonitorConstants.CONFIG_NS, StatusMonitorConstants.PS_CONFIG,
                    StatusMonitorConstants.NS_PREFIX).equals(statusMonitorConfigChildEle.getQName())) {
                //element is "psConfig"
                initSampleServicesMonitoring(statusMonitorConfigChildEle);
            } else {
                String msg = "Unknown element in Status Monitor Configuration: " +
                                statusMonitorConfigChildEle.getQName().getLocalPart();
                log.warn(msg);
            }
        }
    }

    /*
     * Deserialise dbConfigElement (Given below) and initialize data source
        <dbConfig>
            <url>jdbc:mysql://localhost:3306/stratos_stat</url>
            <userName>monitor</userName>
            <password>monitor</password>
            <driverName>com.mysql.jdbc.Driver</driverName>
            <maxActive>80</maxActive>
            <maxWait>60000</maxWait>
            <minIdle>5</minIdle>
            <validationQuery>SELECT 1</validationQuery>
        </dbConfig>
     */
    private void initDataSource(OMElement dbConfigEle) throws StatusMonitorException {
        // initializing the data source and load the database configurations
        Iterator dbConfigChildIt = dbConfigEle.getChildElements();
        dataSource = new BasicDataSource();

        while (dbConfigChildIt.hasNext()) {

            OMElement dbConfigChildEle = (OMElement) dbConfigChildIt.next();
            if (new QName(StatusMonitorConstants.CONFIG_NS, StatusMonitorConstants.DBCONFIG_URL,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setUrl(dbConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_USER_NAME,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setUsername(dbConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_PASSWORD,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setPassword(dbConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_DRIVER_NAME,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setDriverClassName(dbConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_MAX_ACTIVE,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setMaxActive(Integer.parseInt(dbConfigChildEle.getText()));
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_MAX_WAIT,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setMaxWait(Integer.parseInt(dbConfigChildEle.getText()));
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_MIN_IDLE,
                    StatusMonitorConstants.NS_PREFIX).equals(dbConfigChildEle.getQName())) {
                dataSource.setMinIdle(Integer.parseInt(dbConfigChildEle.getText()));
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.DBCONFIG_VALIDATION_QUERY,
                    StatusMonitorConstants.NS_PREFIX)
                    .equals(dbConfigChildEle.getQName())) {
                dataSource.setValidationQuery(dbConfigChildEle.getText());
            } else {
                String msg = "Unknown element in DBConfig of Status Monitor Configuration: " +
                                dbConfigChildEle.getQName().getLocalPart();
                log.warn(msg);
            }
        }
    }

    /*
     * Deserialise authConfigElement (Given below) and initializes authConfigBean
    <authConfig>
         <jksLocation>/home/carbon/automation/projects/src/resources/wso2carbon.jks</jksLocation>
         <userName>admin@wso2-heartbeat-checker.org</userName>
         <password>password123</password>
     </authConfig>
     */
    private void initAuthentication(OMElement authConfigEle) throws StatusMonitorException {
        // initializing the and loading the authentication configurations
        Iterator authConfigChildIt = authConfigEle.getChildElements();
        authConfigBean = new AuthConfigBean();

        while (authConfigChildIt.hasNext()) {
            OMElement authConfigChildEle = (OMElement) authConfigChildIt.next();
            if (new QName(StatusMonitorConstants.CONFIG_NS, StatusMonitorConstants.JKS_LOCATION,
                    StatusMonitorConstants.NS_PREFIX).equals(authConfigChildEle.getQName())) {
                authConfigBean.setJksLocation(authConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.AUTHCONFIG_USER_NAME,
                    StatusMonitorConstants.NS_PREFIX).equals(authConfigChildEle.getQName())) {
                authConfigBean.setUserName(authConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.AUTHCONFIG_PASSWORD,
                    StatusMonitorConstants.NS_PREFIX).equals(authConfigChildEle.getQName())) {
                authConfigBean.setPassword(authConfigChildEle.getText());
            } else if (new QName(StatusMonitorConstants.CONFIG_NS,
                    StatusMonitorConstants.AUTHCONFIG_TENANT,
                    StatusMonitorConstants.NS_PREFIX).equals(authConfigChildEle.getQName())) {
                authConfigBean.setTenant(authConfigChildEle.getText());
            } else {
                String msg = "Unknown element in AuthConfig of Status Monitor Configuration: " +
                                authConfigChildEle.getQName().getLocalPart();
                log.warn(msg);
            }
        }
    }

    /**
     <platformSample>
              <tenantDomain>wso2.org</tenantDomain>
     </platformSample>
    */
    private void initSampleServicesMonitoring (OMElement psConfigEle) throws StatusMonitorException {
        // initializing the and loading the authentication configurations
        Iterator psConfigChildIt = psConfigEle.getChildElements();
        sampleTenantConfigBean = new SampleTenantConfigBean();

        while (psConfigChildIt.hasNext()) {
            OMElement psConfigChildEle = (OMElement) psConfigChildIt.next();
            if (new QName(StatusMonitorConstants.CONFIG_NS, StatusMonitorConstants.PSCONFIG_TENANT,
                    StatusMonitorConstants.NS_PREFIX).equals(psConfigChildEle.getQName())) {
                sampleTenantConfigBean.setTenant(psConfigChildEle.getText());
            } else {
                String msg = "Unknown element in PSConfig of Status Monitor Configuration: " +
                                psConfigChildEle.getQName().getLocalPart();
                log.warn(msg);
            }
        }
    }

    public static BasicDataSource getDataSource() {
        return dataSource;
    }

    public static AuthConfigBean getAuthConfigBean() {
        return authConfigBean;
    }

    public static SampleTenantConfigBean getSampleTenantConfigBean() {
        return sampleTenantConfigBean;
    }
}
