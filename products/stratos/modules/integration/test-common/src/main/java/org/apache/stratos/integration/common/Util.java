/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
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

package org.apache.stratos.integration.common;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class Util {
    public static final String CARBON_ZIP_KEY = "carbon.zip";
    public static final String ACTIVEMQ_BIND_ADDRESS = "activemq.bind.address";
    public static final String CARBON_CONF_PATH = "repository" + File.separator + "conf";
    public static final String BASE_PATH = Util.class.getResource(File.separator).getPath();
    public static final int MIN_PORT_NUMBER = 1;
    public static final int MAX_PORT_NUMBER = 65535;
    public static final int SUPER_TENANT_ID = -1234;

    public static final String STRATOS_SECURE_DYNAMIC_PORT_PLACEHOLDER = "STRATOS_SECURE_DYNAMIC_PORT";
    public static final String ACTIVEMQ_DYNAMIC_PORT_PLACEHOLDER = "ACTIVEMQ_DYNAMIC_PORT";
    public static final String THRIFT_DYNAMIC_PORT_PLACEHOLDER = "THRIFT_DYNAMIC_PORT";
    public static final String STRATOS_DYNAMIC_PORT_PLACEHOLDER = "STRATOS_DYNAMIC_PORT";
    public static final String THRIFT_SECURE_DYNAMIC_PORT_PLACEHOLDER = "THRIFT_SECURE_DYNAMIC_PORT";

    public static final int THRIFT_DEFAULT_PORT = 7611;
    public static final int THRIFT_DEFAULT_SECURE_PORT = 7711;
    public static final int STRATOS_DEFAULT_PORT = 9763;
    public static final int STRATOS_DEFAULT_SECURE_PORT = 9443;
    public static final int STRATOS_DEFAULT_RMI_REGISTRY_PORT = 9999;
    public static final int STRATOS_DEFAULT_RMI_SERVER_PORT = 11111;

    /**
     * Get resources folder path
     *
     * @return path to resources folder
     */
    public static String getCommonResourcesFolderPath() {
        return BASE_PATH + ".." + File.separator + ".." + File.separator + "src" + File.separator + "test" +
                File.separator + "resources" + File.separator + "common";
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    public static boolean isPortAvailable(int port) {
        if (port < Util.MIN_PORT_NUMBER || port > Util.MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            return true;
        }
        catch (IOException ignored) {
        }
        finally {
            if (ss != null) {
                try {
                    ss.close();
                }
                catch (IOException ignored) {
                }
            }
        }
        return false;
    }
}