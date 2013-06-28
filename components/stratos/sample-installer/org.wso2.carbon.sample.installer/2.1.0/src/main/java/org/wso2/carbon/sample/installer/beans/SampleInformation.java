/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.sample.installer.beans;

import java.util.Arrays;

/**
 * Contains information about a sample.
 */
@SuppressWarnings("unused")
public class SampleInformation {

    private String[] requiredServices = new String[0];
    private String[] serviceEPRs = new String[0];
    private boolean installable;
    private String sampleName;
    private String fileName;

    /**
     * Method to obtain the list of cloud services required to run this sample.
     * @return the list of cloud services required to run this sample.
     */
    public String[] getRequiredServices() {
        return Arrays.copyOf(requiredServices, requiredServices.length);
    }

    /**
     * Method to specify the list of cloud services required to run this sample.
     * @param requiredServices the list of cloud services required to run this sample.
     */
    public void setRequiredServices(String[] requiredServices) {
        if (requiredServices != null) {
            this.requiredServices = Arrays.copyOf(requiredServices, requiredServices.length);
        }
    }

    /**
     * Method to obtain the endpoints of cloud services required to run this sample.
     * @return the endpoints of cloud services required to run this sample.
     */
    public String[] getServiceEPRs() {
        return Arrays.copyOf(serviceEPRs, serviceEPRs.length);
    }

    /**
     * Method to specify the endpoints of cloud services required to run this sample.
     * @param serviceEPRs the endpoints of cloud services required to run this sample.
     */
    public void setServiceEPRs(String[] serviceEPRs) {
        if (serviceEPRs != null) {
            this.serviceEPRs = Arrays.copyOf(serviceEPRs, serviceEPRs.length);
        }
    }

    /**
     * Determines whether this sample is installable or not.
     * @return true if the sample is installable or false if not.
     */
    public boolean isInstallable() {
        return installable;
    }

    /**
     * Method to specify whether this sample is installable or not.
     * @param installable whether this sample is installable or not.
     */
    public void setInstallable(boolean installable) {
        this.installable = installable;
    }

    /**
     * Method to obtain the name of this sample.
     * @return the name of this sample.
     */
    public String getSampleName() {
        return sampleName;
    }

    /**
     * Method to specify the name of this sample.
     * @param sampleName the name of this sample.
     */
    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    /**
     * Method to obtain the file name of this sample.
     * @return the file name of this sample.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Method to specify the file name of this sample.
     * @param fileName the file name of this sample.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
