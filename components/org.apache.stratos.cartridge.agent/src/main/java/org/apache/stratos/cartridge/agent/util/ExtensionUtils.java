/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.common.util.CommandUtils;

import java.io.File;

/**
 * Cartridge agent extension utility methods.
 */
public class ExtensionUtils {
    private static final Log log = LogFactory.getLog(ExtensionUtils.class);

    private static String getExtensionsDir() {
        String extensionsDir = System.getProperty(CartridgeAgentConstants.EXTENSIONS_DIR);
        if(StringUtils.isBlank(extensionsDir)) {
            throw new RuntimeException(String.format("System property not found: %s", CartridgeAgentConstants.EXTENSIONS_DIR));
        }
        return extensionsDir;
    }

    public static String prepareCommand(String scriptFile) {
        String extensionsDir = getExtensionsDir();
        return (extensionsDir.endsWith(File.separator)) ?
                extensionsDir + scriptFile:
                extensionsDir + File.separator + scriptFile;
    }

    public static void executeStartServersExtension() {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Executing start servers extension");
            }
            String command = prepareCommand(CartridgeAgentConstants.START_SERVERS_SH);
            CommandUtils.executeCommand(command);
        }
        catch (Exception e) {
            log.error("Could not execute start servers extension", e);
        }
    }

    public static void executeCleanupExtension() {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Executing start servers extension");
            }
            String command = prepareCommand(CartridgeAgentConstants.CLEAN_UP_SH);
            CommandUtils.executeCommand(command);
        }
        catch (Exception e) {
            log.error("Could not execute start servers extension", e);
        }
    }

    public static void executeInstanceStartedExtension() {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Executing instance started extension");
            }
            String command = prepareCommand(CartridgeAgentConstants.INSTANCE_STARTED_SH);
            CommandUtils.executeCommand(command);
        }
        catch (Exception e) {
            log.error("Could not execute instance started extension", e);
        }
    }

    public static void executeInstanceActivatedExtension() {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Executing instance activated extension");
            }
            String command = prepareCommand(CartridgeAgentConstants.INSTANCE_ACTIVATED_SH);
            CommandUtils.executeCommand(command);
        }
        catch (Exception e) {
            log.error("Could not execute instance activated extension", e);
        }
    }

    public static void executeArtifactsUpdatedExtension() {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Executing artifacts updated extension");
            }
            String command = prepareCommand(CartridgeAgentConstants.ARTIFACTS_UPDATED_SH);
            CommandUtils.executeCommand(command);
        }
        catch (Exception e) {
            log.error("Could not execute artifacts updated extension", e);
        }
    }

    /*
    This will execute the volume mounting script which format and mount the
    persistance volumes.
     */
    public static void executeVolumeMountExtension(String persistanceMappingsPayload) {
        try {
            if(log.isDebugEnabled()) {
                    log.debug("Executing volume mounting extension");
            }
            String command = prepareCommand(CartridgeAgentConstants.MOUNT_VOLUMES_SH);
            //String payloadPath = System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH);
            // add payload file path as argument so inside the script we can source
            // it  to get the env variables set by the startup script
            CommandUtils.executeCommand(command + " " + persistanceMappingsPayload);
        }
        catch (Exception e) {
                log.error("Could not execute volume mounting extension", e);
        }
    }
}
