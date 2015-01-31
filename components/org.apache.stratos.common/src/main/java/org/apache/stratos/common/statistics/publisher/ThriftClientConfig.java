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

package org.apache.stratos.common.statistics.publisher;


/**
 * Thrift Client configuration.
 */
public class ThriftClientConfig {

    public static final String THRIFT_CLIENT_CONFIG_FILE_PATH = "thrift.client.config.file.path";

    private static volatile ThriftClientConfig instance;
    private ThriftClientInfo thriftClientInfo;

    /*
    * A private Constructor prevents any other
    * class from instantiating.
    */
    ThriftClientConfig(){}

    public static ThriftClientConfig getInstance() {
        if (instance == null) {
            synchronized (ThriftClientConfig.class) {
                if (instance == null) {
                    String configFilePath = System.getProperty(THRIFT_CLIENT_CONFIG_FILE_PATH);
                    instance = ThriftClientConfigParser.parse(configFilePath);
                }
            }
        }
        return instance;
    }

    /**
     * Returns an ThriftClientInfo Object that stores the credential information.
     * CEP credential information can be found under thrift-client-config.xml file
     * These credential information then get parsed and assigned into ThriftClientInfo
     * Object.
     * <p>
     * This method is used to return the assigned values in ThriftClientInfo Object
     *
     * @return   ThriftClientInfo object which consists of username,password,ip and port values
     */
    public ThriftClientInfo getThriftClientInfo() {
        return thriftClientInfo;
    }

    /**
     * Parsed values will be assigned to ThriftClientInfo object. Required fields will be taken
     * from thrift-client-config.xml file.
     *
     * @param thriftClientInfo Object of the ThriftClientInfo
     */
    public void setThriftClientInfo(ThriftClientInfo thriftClientInfo) {
        this.thriftClientInfo = thriftClientInfo;
    }
}
