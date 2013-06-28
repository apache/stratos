/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.wso2.carbon.lb.common.conf.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for Autoscale mediator
 */
public final class LoadBalancerConfigUtil {

    private static final Log log = LogFactory.getLog(LoadBalancerConfigUtil.class);

    private LoadBalancerConfigUtil() {
    }

    /**
     * handles the exception
     * 
     * @param msg
     *            exception message
     */
    public static void handleException(String msg) {
        log.error(msg);
        throw new RuntimeException(msg);
    }

    /**
     * handles the exception
     * 
     * @param msg
     *            exception message
     * @param e
     *            exception
     */
    public static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Replaces the variables
     * 
     * @param text
     *            input string
     * @return output String
     */
    public static String replaceVariables(String text) {
        int indexOfStartingChars;
        int indexOfClosingBrace;

        // The following condition deals with properties.
        // Properties are specified as ${system.property},
        // and are assumed to be System properties
        if ((indexOfStartingChars = text.indexOf("${")) != -1 &&
            (indexOfClosingBrace = text.indexOf("}")) != -1) { // Is a property used?
            String var = text.substring(indexOfStartingChars + 2, indexOfClosingBrace);

            String propValue = System.getProperty(var);
            if (propValue == null) {
                propValue = System.getenv(var);
            }
            if (propValue != null) {
                text =
                       text.substring(0, indexOfStartingChars) + propValue +
                               text.substring(indexOfClosingBrace + 1);
            }
        }
        return text;
    }

    public static String getUserData(String payloadFileName) {
        String userData = null;
        try {
            File file = new File(payloadFileName);
            if (!file.exists()) {
                handleException("Payload file " + payloadFileName + " does not exist");
            }
            if (!file.canRead()) {
                handleException("Payload file " + payloadFileName + " does cannot be read");
            }
            byte[] bytes = LoadBalancerConfigUtil.getBytesFromFile(file);
            if (bytes != null) {
                BASE64Encoder encoder = new BASE64Encoder();
                userData = encoder.encode(bytes);
            }
        } catch (Exception e) {
            LoadBalancerConfigUtil.handleException("Cannot read data from payload file " +
                                                   payloadFileName, e);

        }
        return userData;
    }

    /**
     * Returns the contents of the file in a byte array
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
     * @deprecated
     *             Extract the domain part given a string which is in
     *             &lt;sub_domain&gt;#&lt;domain&gt; format.
     * @param str
     *            in &lt;sub_domain&gt;#&lt;domain&gt; format.
     * @return the domain part. If # is not present this will return the trimmed
     *         input string.
     */
    public static String getDomain(String str) {
        str = str.trim();
        if (!str.contains(Constants.SUB_DOMAIN_DELIMITER)) {
            return str;
        }
        return str.substring(str.indexOf(Constants.SUB_DOMAIN_DELIMITER) + 1);
    }

    /**
     * @deprecated
     *             Extract the sub_domain part given a string which is in
     *             &lt;sub_domain&gt;#&lt;domain&gt; format.
     * @param str
     *            in &lt;sub_domain&gt;#&lt;domain&gt; format.
     * @return the sub_domain part. If # is not present this will return <code>null</code>.
     */
    public static String getSubDomain(String str) {
        str = str.trim();
        if (!str.contains(Constants.SUB_DOMAIN_DELIMITER)) {
            return null;
        }
        return str.substring(0, str.indexOf(Constants.SUB_DOMAIN_DELIMITER));
    }

    // public static EC2InstanceManager createEC2InstanceManager(String accessKey,
    // String secretKey,
    // String instanceMgtEPR) {
    // AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    // AmazonEC2Client ec2Client = new AmazonEC2Client(awsCredentials);
    // ec2Client.setEndpoint(instanceMgtEPR);
    // return new EC2InstanceManager(ec2Client);
    // }

    public static List<TenantDomainContext> getTenantDomainContexts(String tenantRange, String domain, String subDomain) {
        
        List<TenantDomainContext> ctxts = new ArrayList<TenantDomainContext>();
        List<Integer> tenantIds = getTenantIds(tenantRange);
        
        // iterate through all tenant ids under this host
        for (Integer tId : tenantIds) {

            // create a new TenantDomainContext
            TenantDomainContext tenantCtxt =
                                             new TenantDomainContext(
                                                                     tId,
                                                                     domain,
                                                                     subDomain);
            // add it to the list
            ctxts.add(tenantCtxt);
        }
        
        return ctxts;
        
    }
    
    /**
     * This method will read the tenant range string and return a list of tenant ids
     * which is derived from tenant range string.
     * 
     * @param tenantRange
     * @return list of tenant ids.
     */
    public static List<Integer> getTenantIds(String tenantRange) {

        List<Integer> tenantIds = new ArrayList<Integer>();

        String[] parsedLine = tenantRange.trim().split("-");

        if (parsedLine[0].equalsIgnoreCase("*")) {
            tenantIds.add(0);

        } else if (parsedLine.length == 1) {
            try {
                int tenantId = Integer.parseInt(tenantRange);
                tenantIds.add(tenantId);

            } catch (NumberFormatException e) {
                String msg = "Invalid tenant range is specified : " + tenantRange;
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        } else if (parsedLine.length == 2) {
            try {

                int startIndex = Integer.parseInt(parsedLine[0]);
                int endIndex = Integer.parseInt(parsedLine[1]);

                for (int tenantId = startIndex; tenantId <= endIndex; tenantId++) {

                    tenantIds.add(tenantId);
                }

            } catch (NumberFormatException e) {
                String msg = "Invalid tenant range is specified : " + tenantRange;
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }

        } else {
            String msg = "Invalid tenant range is specified : " + tenantRange;
            log.error(msg);
            throw new RuntimeException(msg);
        }

        return tenantIds;
    }

}