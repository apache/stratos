/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.jcloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.stratos.cloud.controller.util.IaasProvider;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * This class is responsible for creating a JClouds specific ComputeService object.
 */
public class ComputeServiceBuilderUtil {
    
    private static final Log log = LogFactory.getLog(ComputeServiceBuilderUtil.class);
    
    public static byte[] getUserData(String payloadFileName) {
        // String userData = null;
        byte[] bytes = null;
        try {
            File file = new File(payloadFileName);
            if (!file.exists()) {
                handleException("Payload file " + payloadFileName + " does not exist");
            }
            if (!file.canRead()) {
                handleException("Payload file " + payloadFileName + " does cannot be read");
            }
            bytes = getBytesFromFile(file);

        } catch (IOException e) {
            handleException("Cannot read data from payload file " + payloadFileName, e);
        }
        return bytes;
    }

    
    public static void buildDefaultComputeService(IaasProvider iaas) {

        Properties properties = new Properties();

        // load properties
        for (Map.Entry<String, String> entry : iaas.getProperties().entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        // set modules
        Iterable<Module> modules =
            ImmutableSet.<Module> of(new SshjSshClientModule(), new SLF4JLoggingModule(),
                                     new EnterpriseConfigurationModule());

        // build context
        ContextBuilder builder =
            ContextBuilder.newBuilder(iaas.getProvider())
                          .credentials(iaas.getIdentity(), iaas.getCredential()).modules(modules)
                          .overrides(properties);

        // set the compute service object
        iaas.setComputeService(builder.buildView(ComputeServiceContext.class).getComputeService());
    }
    
    public static String extractRegion(IaasProvider iaas) {
        String region;
        // try to find region
        if ((region = iaas.getProperty(CloudControllerConstants.REGION_PROPERTY)) == null) {
            // if the property, isn't specified, try to obtain from the image id
            // image id can be in following format - {region}/{UUID}
            region = iaas.getImage().contains("/") ? iaas.getImage().split("/")[0] : null;
        }

        return region;
    }
    
    /** Returns the contents of the file in a byte array
     *
     * @param file
     *            - Input File
     * @return Bytes from the file
     * @throws java.io.IOException
     *             , if retrieving the file contents failed.
     */
    public static byte[] getBytesFromFile(File file) throws IOException {
        if (!file.exists()) {
            log.error("Payload file " + file.getAbsolutePath() + " does not exist");
            return null;
        }
        InputStream is = new FileInputStream(file);
        byte[] bytes;

        try {
            // Get the size of the file
            long length = file.length();
            
            // You cannot create an array using a long type.
            // It needs to be an int type.
            // Before converting to an int type, check
            // to ensure that file is not larger than Integer.MAX_VALUE.
            if (length > Integer.MAX_VALUE) {
                if (log.isDebugEnabled()) {
                    log.debug("File is too large");
                }
            }

            // Create the byte array to hold the data
            bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead;
            while (offset < bytes.length &&
                (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } finally {
            // Close the input stream and return bytes
            is.close();
         }
        
        return bytes;
    }
    
    /**
     * handles the exception
     * 
     * @param msg
     *            exception message
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * handles the exception
     * 
     * @param msg
     *            exception message
     * @param e
     *            exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
    
    
}
