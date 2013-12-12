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

package org.apache.stratos.adc.mgt.payload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class Payload implements Serializable {

    private static Log log = LogFactory.getLog(Payload.class);

    protected StringBuilder payloadBuilder;
    protected String payloadFilePath;
    protected PayloadArg payloadArg;

    /**
     * Constructor
     *
     * @param payloadFilePath Full path at which the payload file is created
     */
    public Payload(String payloadFilePath) {
        this.payloadFilePath = payloadFilePath;
        payloadBuilder = new StringBuilder();
    }

    /**
     * Pupulates the Payload subscription with relevant parameters and values given in PayloadArg subscription
     *
     * @param payloadArg PayloadArg subscription with relevant values
     */
    public void populatePayload(PayloadArg payloadArg) {

        this.payloadArg = payloadArg;
        payloadBuilder.append("HOST_NAME=" + payloadArg.getHostName());
        payloadBuilder.append(",");
        payloadBuilder.append("TENANT_ID=" + payloadArg.getTenantId());
        payloadBuilder.append(",");
        payloadBuilder.append("TENANT_RANGE=" + payloadArg.getTenantRange());
        payloadBuilder.append(",");
        payloadBuilder.append("TENANT_CONTEXT=" + payloadArg.getTenantDomain()); // No need to send those now
        payloadBuilder.append(",");
        payloadBuilder.append("CARTRIDGE_ALIAS=" + payloadArg.getCartridgeAlias());
        payloadBuilder.append(",");
        payloadBuilder.append("MB_IP=" + System.getProperty(CartridgeConstants.MB_IP));  // No need to send those now, will get from Puppet
        payloadBuilder.append(",");
        payloadBuilder.append("MB_PORT=" + System.getProperty(CartridgeConstants.MB_PORT)); // No need to send those now, will get from Puppet
        payloadBuilder.append(",");
        payloadBuilder.append("CEP_IP=" + System.getProperty(CartridgeConstants.CEP_IP)); // No need to send those now, will get from Puppet
        payloadBuilder.append(",");
        payloadBuilder.append("CEP_PORT=" + System.getProperty(CartridgeConstants.CEP_PORT)); // No need to send those now, will get from Puppet
        payloadBuilder.append(",");
        payloadBuilder.append("CLUSTER_ID=" + payloadArg.getServiceDomain());
        payloadBuilder.append(",");
        payloadBuilder.append("CARTRIDGE_KEY=" + payloadArg.getSubscriptionKey());
        payloadBuilder.append(",");
        payloadBuilder.append("DEPLOYMENT=" + "default"); // hard coded to default
        payloadBuilder.append(",");
        payloadBuilder.append("PUPPET_IP=" + System.getProperty(CartridgeConstants.PUPPET_IP));       
        
        
        if(payloadArg.getCartridgeInfo() != null) {
            payloadBuilder.append(",");
            payloadBuilder.append("SERVICE_NAME=" + payloadArg.getCartridgeInfo().getType());
        }

        //add the user defined payload String (if any)
        //this should be of the format <key_1>=<value_1>,<key_2>=<value_2>,....<key_n>=<value_n>
        if (payloadArg.getUserDefinedPayload() != null && !payloadArg.getUserDefinedPayload().trim().isEmpty()) {

            if(!payloadBuilder.toString().endsWith(",")) {
                payloadBuilder.append(",");
            }
            payloadBuilder.append(payloadArg.getUserDefinedPayload());
        }
    }

    /**
     * Add the user defined payload String (if any). This should be of the format
     * <key_1>=<value_1>,<key_2>=<value_2>,....<key_n>=<value_n>
     *
     * @param payloadString String object with payload information
     */
    public void populatePayload (String payloadString) {

        if(payloadBuilder.toString().isEmpty()) {
            if(payloadString.startsWith(",")) {
                payloadBuilder.append(payloadString.substring(1));
            }
            else {
                payloadBuilder.append(payloadString);
            }
        } else {
            if(!payloadBuilder.toString().endsWith(",") && !payloadString.startsWith(",")) {
                payloadBuilder.append(",");
                payloadBuilder.append(payloadString);
            }
            else if (payloadBuilder.toString().endsWith(",") && payloadString.startsWith(",")) {
                payloadBuilder.append(payloadString.substring(1));
            }
            else {
                payloadBuilder.append(payloadString);
            }
        }
    }

    /**
     * Create the actual payload in the file system
     *
     * @return DataHandler subscription with payload
     * @throws ADCException in case of an error
     */
    public StringBuilder createPayload () throws ADCException {

        if(payloadBuilder.length() == 0) {
            log.warn("Payload string length is zero. Create payload failed");
            return null;
        }

        File payloadFile = new File(getPayloadFilePath());
        if(payloadFile.exists()) {
            payloadFile.delete();
        }

        log.info("** Payload ** " + payloadBuilder.toString());

        return payloadBuilder;
    }

    /**
     * Adds content to a zip file
     *
     * @param dir Name of directory
     * @param fileName Name of file to add
     * @param zos ZipOutputStream subscription to write
     * @throws ADCException in an error
     */
    private void addToZipFile(String dir, String fileName, ZipOutputStream zos) throws ADCException {

        log.info("Writing '" + fileName + "' to zip file");

        File file = new File(dir + File.separator + fileName);
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);

        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            throw new ADCException(e.getMessage(), e);
        }

        ZipEntry zipEntry = new ZipEntry(fileName);
        try {
            zos.putNextEntry(zipEntry);

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ADCException(e.getMessage(), e);
        }

        byte[] bytes = new byte[1024];
        int length;

            try {
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new ADCException(e.getMessage(), e);
            }

        try {
            zos.closeEntry();
            fis.close();

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ADCException(e.getMessage(), e);
        }
    }

    public String getPayloadFilePath() {
        return payloadFilePath;
    }

    public void setPayloadFilePath(String payloadFilePath) {
        this.payloadFilePath = payloadFilePath;
    }

    public boolean delete () {
        return new File(payloadFilePath).delete();
    }

    public PayloadArg getPayloadArg() {
        return payloadArg;
    }

    public void setPayloadArg(PayloadArg payloadArg) {
        this.payloadArg = payloadArg;
    }
}
