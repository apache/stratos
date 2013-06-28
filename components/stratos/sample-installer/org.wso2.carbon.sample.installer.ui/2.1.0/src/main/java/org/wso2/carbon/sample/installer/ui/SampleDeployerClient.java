/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.sample.installer.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.sample.installer.stub.SampleDeployerCallbackHandler;
import org.wso2.carbon.sample.installer.stub.SampleDeployerStub;
import org.wso2.carbon.sample.installer.stub.SampleInformation;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stratos sample deployer service client.
 */
public class SampleDeployerClient {

    private static final Log log = LogFactory.getLog(SampleDeployerClient.class);
    private static final String SERVICE = "SampleDeployer";

    private SampleDeployerStub stub;
    private String epr;
    private ConfigurationContext confContext;
    //private static final String ADDRESSING_MODULE = "addressing";

    private static Map<String, Long> tracker = new ConcurrentHashMap<String, Long>();
    private static ExecutorService sampleInstallerService;
    private static InstallationRequestCleaner requestCleaner;
    public static final int MAX_WAIT_TIMESTAMP_MINS = 60;

    private static class SampleDeployerCallbackData {

        private boolean isComplete = false;
        private boolean result = false;
        private Exception exception = null;

        public void setComplete(boolean result) {
            this.result = result;
            isComplete = true;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public boolean handleCallback() throws Exception {
            result = false;
            int i = 0;
            try {
                while (!isComplete && exception == null) {
                    Thread.sleep(500);
                    i++;
                    if (i > 120 * 2400) {
                        throw new Exception("Response not received within 4 hours");
                    }
                }
                if (!isComplete) {
                    throw exception;
                }
            } finally {
                isComplete = false;
                exception = null;
            }
            return result;
        }
    }

    private SampleDeployerCallbackData callbackData = new SampleDeployerCallbackData();

    private SampleDeployerCallbackHandler callback =
            new SampleDeployerCallbackHandler(callbackData) {

                public void receiveResultdeploySample(boolean result) {
                    getData().setComplete(result);
                }

                public void receiveErrordeploySample(Exception e) {
                    getData().setException(e);
                }

                public void receiveResultuploadSample(boolean result) {
                    getData().setComplete(result);
                }

                public void receiveErroruploadSample(Exception e) {
                    getData().setException(e);
                }

                private SampleDeployerCallbackData getData() {
                    return (SampleDeployerCallbackData) getClientData();
                }
            };

    public SampleDeployerClient(String cookie, String serverURL,
                                ConfigurationContext configContext) throws CarbonException {
        epr = serverURL + SERVICE;
        confContext = configContext;
        try {
            stub = new SampleDeployerStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate SampleDeployer service client.";
            log.error(msg, axisFault);
            throw new CarbonException(msg, axisFault);
        }


    }

    public SampleDeployerClient(ServletConfig config, HttpSession session)
            throws CarbonException {

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(
                        CarbonConstants.CONFIGURATION_CONTEXT);
        epr = serverURL + SERVICE;

        try {
            stub = new SampleDeployerStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate SampleDeployer service client.";
            log.error(msg, axisFault);
            throw new CarbonException(msg, axisFault);
        }

    }


    /**
     * Client method for uploading a sample from the file system to the registry
     *
     * @param sampleName The name of the sample file to be uploaded to the registry
     *
     * @return true if the operation successfully completed.
     * @throws CarbonException Thrown if an error occurs
     */
    private boolean sampleUploader(String sampleName, String tenantDomain) throws CarbonException {

        try {
            /*stub._getServiceClient().engageModule(ADDRESSING_MODULE); // IMPORTANT
            Options options = stub._getServiceClient().getOptions();
            options.setUseSeparateListener(true);
            options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
            stub.startuploadSample(sampleName, callback);
            return callbackData.handleCallback();*/
            stub.uploadSample(sampleName, tenantDomain);
            return true;
        } catch (Exception e) {
            String msg = "Failed to upload.";
            log.error(msg, e);
            throw new CarbonException(msg, e);
        }
    }

    /**
     * Client method for fetching a sample from the registry and deploys it as a CarbonApp
     *
     * @param sampleName Name of the sample to be installed
     * @param tenantDomain  Domain of the tenant the sample is being installed
     *
     * @return true if the operation successfully completed.
     * @throws CarbonException Thrown if an error occurs
     */
    public void sampleDeployer(String sampleName, String tenantDomain) throws CarbonException {
        try {
            SampleInformation[] samples = getSampleInformation(sampleName, tenantDomain);
            if (samples != null) {
                for (SampleInformation sample : samples) {
                    if (sample.getInstallable()) {
                        if (sampleUploader(sample.getFileName(), tenantDomain)) {
                            ServerInfoBean[] servers = Util.getListenerServers();

                            String[] serviceEPRs = sample.getServiceEPRs();
                            SampleDeployerStub[] deployerStubs =
                                    new SampleDeployerStub[serviceEPRs.length];
                            for (int j = 0; j < deployerStubs.length; j++) {
                                String sampleDeployerCookie = null;
                                for (ServerInfoBean server : servers) {
                                    if (server.getServerUrl().equals(serviceEPRs[j])) {
                                        sampleDeployerCookie =
                                                Util.login(server.getServerUrl(), server.getUserName(),
                                                      server.getPassword(), confContext);
                                        break;
                                    }
                                }
                                if (sampleDeployerCookie == null) {
                                    log.warn("Unable to deploy sample on server: " +
                                             serviceEPRs[j] + ". Login Failed.");
                                    //return false;
                                }
                                deployerStubs[j] = new SampleDeployerStub(confContext,
                                        serviceEPRs[j] + SERVICE);
                                ServiceClient client = deployerStubs[j]._getServiceClient();
                                Options options = client.getOptions();
                                options.setManageSession(true);
                                options.setProperty(
                                        org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                                        sampleDeployerCookie);
                                /*client.engageModule(ADDRESSING_MODULE); // IMPORTANT
                                options.setUseSeparateListener(true);
                                options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
                                deployerStubs[j].startdeploySample(sample.getFileName(),
                                        callback);
                                if (!callbackData.handleCallback()) {
                                    log.error("An error occurred while deploying sample: " +
                                            sampleName);
                                    return false;
                                }*/
                                deployerStubs[j].deploySample(sample.getFileName(), tenantDomain);
                            }
                        } else {
                            log.error("An error occurred while uploading sample: " + sampleName);
                            //return false;
                        }
                    }
                }
            }

        } catch (Exception e) {
            String msg = "Failed to deploy sample: " + sampleName;
            log.error(msg, e);
            throw new CarbonException(msg, e);
        }

        //return true;
    }


    /**
     * Client method to obtain information about samples.
     *
     * @param sampleName Name of the sample to be installed
     *
     * @return a record containing information on each sample available on the system.
     * @throws CarbonException Thrown if an error occurs
     */
    public SampleInformation[] getSampleInformation(String sampleName, String tenantDomain) throws CarbonException {
        try {
            SampleInformation[] samples = stub.getSampleInformation(tenantDomain);
            if (sampleName.equals("all")) {
                return samples;
            } else {
                for (SampleInformation sample : samples) {
                    if (sample.getFileName().equalsIgnoreCase(sampleName)) {
                        SampleInformation[] sampleInfo = new SampleInformation[1];
                        sampleInfo[0] = sample;
                        return sampleInfo;
                    }
                }
            }
        } catch (RemoteException e) {
            String msg = "Failed to get sample information";
            log.error(msg, e);
            throw new CarbonException(msg, e);
        }
        return null;
    }

    public void setPolicyPermission() throws RemoteException {
        stub.setPolicyPermission();
    }

    public String addToQueue(String sampleName, HttpSession session) {
        // Initialize the cached thread pool of not initialized yet
        if (sampleInstallerService == null) {
            sampleInstallerService = Executors.newCachedThreadPool();
        }
        // Initialize the worker thread that cleans up tha tracker, if not started yet 
        if (requestCleaner == null) {
            requestCleaner = new InstallationRequestCleaner();
            requestCleaner.start();
        }

        String tenantDomain = (String) session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
        // throttle tenant requests only (not super-tenant) 
        if (tenantDomain != null) {
            if (tracker.containsKey(tenantDomain)) {
                Long timestampMins = ((((System.nanoTime() - tracker.get(tenantDomain)) / 1000) / 1000) / 1000) / 60;
                // Check if the sample has been installed within 1 hour
                if (timestampMins < MAX_WAIT_TIMESTAMP_MINS) {
                    return "waiting"; // return message asking to try in an hour
                }
            }
            // If the map does not contain the tenant key or if the time stamp is greater than 1Hr, schedule for installation
            // tracker.put(tenantDomain, System.nanoTime());
            // Add the tenant information to tracker after the installation trigger completes 
        }
        SampleInstallerWorker installerWorker = new SampleInstallerWorker(tenantDomain, sampleName);
        sampleInstallerService.execute(installerWorker);
        
        return "success";
    }

    private class SampleInstallerWorker extends Thread {
        private String tenantDomain;
        private String sampleName;

        public SampleInstallerWorker(String tenantDomain, String sampleName) {
            this.tenantDomain = tenantDomain;
            this.sampleName = sampleName;
        }

        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("******* Starting the sample installer worker for " +
                          this.tenantDomain + " *******");
            }

            try {
                sampleDeployer(this.sampleName, this.tenantDomain);
                tracker.put(this.tenantDomain, System.nanoTime());
            } catch (CarbonException e) {
                String msg = "An error occurred while installing " + this.sampleName +
                             " for tenant " + this.tenantDomain;
                log.error(msg, e);
            }
        }

    }

    private class InstallationRequestCleaner extends Thread {
        public void run() {
            while (true) {
                long maxSleepInterval = 0;
                if (log.isDebugEnabled()) {
                    log.debug("Running InstallationRequestCleaner");
                }
                // If there are entries in the tracker, check if there are entries that are older that 1 hour
                if (!tracker.isEmpty()) {
                    for (String tenantDomain : tracker.keySet()) {
                        Long timeStamp = ((((System.nanoTime() - tracker.get(tenantDomain)) / 1000) / 1000) / 1000) / 60;
                        maxSleepInterval = maxSleepInterval < timeStamp ? timeStamp : maxSleepInterval;
                        if(timeStamp >= MAX_WAIT_TIMESTAMP_MINS) {
                            if (log.isDebugEnabled()) {
                                log.debug("InstallationRequestCleaner removed " + tenantDomain);
                            }
                            tracker.remove(tenantDomain);
                        }
                    }
                    // Once we have traversed the tracker, wait for an interval 
                    try {
                        sleep((MAX_WAIT_TIMESTAMP_MINS - maxSleepInterval) * 60 * 1000); // sleep in milli seconds
                    } catch (InterruptedException e) {
                        // Occur during system shutdown, hence ignored
                    }
                } else {
                    try {
                        sleep(MAX_WAIT_TIMESTAMP_MINS * 60 * 1000); // If the tracker is empty safely sleep for 1 Hr
                    } catch (InterruptedException e) {
                        // Occur during system shutdown, hence ignored
                    }
                }
            }
        }
    }
    
}
