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

package org.wso2.carbon.sample.installer;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.application.upload.CarbonAppUploader;
import org.wso2.carbon.application.upload.UploadedFileItem;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.feature.mgt.services.CompMgtConstants;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.utils.RegistryClientUtils;
import org.wso2.carbon.sample.installer.beans.SampleInformation;
import org.wso2.carbon.sample.installer.utils.Util;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import javax.activation.DataHandler;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Stratos sample deployer Admin service.
 */
@SuppressWarnings("unused")
public class SampleDeployer extends CarbonAppUploader {

    private static final Log log = LogFactory.getLog(SampleDeployer.class);

    private static final String REGISTRY_SAMPLE_LOCATION = "/samples/";
    private static final String APP_ARCHIVE_EXTENSION = ".car";

    /**
     * Fetches a sample from the registry and deploys it as a CarbonApp
     *
     * @param sampleName Name of the sample to be installed
     *
     * @return true if the operation successfully completed.
     * @throws AxisFault         Thrown if and error occurs while uploading the sample
     * @throws RegistryException Thrown if an error occurs while accessing the Registry
     */
    public boolean deploySample(String sampleName, String tenantDomain) throws AxisFault,
                                                                               RegistryException {
        try {
            /*PrivilegedCarbonContext carbonContext =
                    PrivilegedCarbonContext.getThreadLocalCarbonContext();
            PrivilegedCarbonContext carbonContextOnMessageContext =
                    PrivilegedCarbonContext.getCurrentContext(
                            MessageContext.getCurrentMessageContext());
            carbonContextOnMessageContext.setTenantDomain(tenantDomain, true);
            BundleContext bundleContext = Util.getBundleContext();

            if (bundleContext != null) {
                ServiceTracker tracker =
                        new ServiceTracker(bundleContext,
                                           TenantRegistryLoader.class.getName(), null);
                tracker.open();
                Object[] services = tracker.getServices();
                if (services != null) {
                    for (Object service : services) {
                        ((TenantRegistryLoader) service).loadTenantRegistry(
                                carbonContextOnMessageContext.getTenantId());
                    }
                }
                tracker.close();
            }
            carbonContext.setTenantDomain(tenantDomain, true);*/

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getCurrentContext().setTenantDomain(tenantDomain, true);
            PrivilegedCarbonContext.getCurrentContext().getTenantId(true);

            Registry registry = (Registry) PrivilegedCarbonContext.getCurrentContext().getRegistry(RegistryType.SYSTEM_GOVERNANCE);

            int tenantId = Util.getRealmService().getTenantManager().getTenantId(tenantDomain);
            String pathToAuthorize = "/_system/governance/policies/policy_service.xml";
            Util.getRealmService().getTenantUserRealm(tenantId).
                    getAuthorizationManager().authorizeRole("wso2.anonymous.role", pathToAuthorize, ActionConstants.GET);

            Resource sampleFile = (Resource) registry.get(getSamplePath(sampleName));
            UploadedFileItem[] fileItems = new UploadedFileItem[1];
            fileItems[0] = new UploadedFileItem();
            fileItems[0].setDataHandler(
                    new DataHandler(new ByteArrayDataSource((byte[]) sampleFile.getContent(),
                                                            "application/octet-stream")));
            fileItems[0].setFileName(sampleName + APP_ARCHIVE_EXTENSION);
            fileItems[0].setFileType("jar");
            uploadApp(fileItems);
            return true;
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            throw new RegistryException(e.getMessage(), e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("Failed to set permission", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();  
        }

        return false;
    }

    public void setPolicyPermission() {
           try {
               log.info("Setting policy permission for tenant " + getTenantDomain());
               getUserRealm().getAuthorizationManager().
                       authorizeRole("wso2.anonymous.role", "/_system/governance/policies",
                                     ActionConstants.GET);
           } catch (UserStoreException e) {
               e.printStackTrace();
           }
    }


    private HttpSession getSession() {
        return (HttpSession) MessageContext.getCurrentMessageContext().getProperty(
                CompMgtConstants.COMP_MGT_SERVELT_SESSION);
    }

    /**
     * Upload a sample from the file system to the registry
     *
     * @param sampleName The name of the sample file to be uploaded to the registry
     * @return true if the operation successfully completed.
     * @throws RegistryException Thrown if an error occurs while accessing the Registry
     */
    public boolean uploadSample(String sampleName, String tenantDomain) throws RegistryException {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getCurrentContext().setTenantDomain(tenantDomain, true);
            PrivilegedCarbonContext.getCurrentContext().getTenantId(true);

            Registry registry = (Registry) PrivilegedCarbonContext.getCurrentContext().getRegistry(RegistryType.SYSTEM_GOVERNANCE);
            try {
                if (registry.resourceExists(getSamplePath(sampleName))) {
                    return true;
                }
            } catch (Exception ignored) {
                // Ignore any exceptions that may occur in the process, and try to re-upload the sample
                // assuming that it is not already on the registry.
                // Basically continue even though an exception does occur.
            }
            String path = CarbonUtils.getCarbonHome() + File.separator + "samples" + File.separator +
                          "bin" + File.separator + sampleName + APP_ARCHIVE_EXTENSION;
            String tenantSpecificPath;
            try {
                long start = System.nanoTime();
                tenantSpecificPath = Util.generateAppArchiveForTenant(path, getSession());
                if (log.isInfoEnabled()) {
                    log.info("Generated Sample for Tenant in " +
                             (((System.nanoTime() - start) / 1000) / 1000) + "ms");
                }
            } catch (IOException e) {
                log.error("Failed to generate sample for tenant", e);
                return false;
            }
            try {
                File sampleFile = new File(tenantSpecificPath);
                RegistryClientUtils.importToRegistry(sampleFile, REGISTRY_SAMPLE_LOCATION, registry);
            } finally {
                if (!path.equals(tenantSpecificPath)) {
                    try {
                        FileUtils.deleteDirectory(new File(tenantSpecificPath).getParentFile());
                    } catch (IOException e) {
                        log.warn("Unable to delete temporary file", e);
                    }
                }
            }
        } finally {
            // Ultimately cleanup the tenant information before exiting the thread.
            PrivilegedCarbonContext.endTenantFlow();
        }
        return true;
    }

    /**
     * Method to obtain information about samples.
     *
     * @return a record containing information on each sample available on the system.
     */
    public SampleInformation[] getSampleInformation(String tenantDomain) {
        SampleInformation[] samples = Util.getSampleInformation();

        for (SampleInformation sample : samples) {
            String[] services = sample.getServiceEPRs();
            List<String> serviceList = new LinkedList<String>();
            for (String service : services) {
                serviceList.add(service + "/services/");
            }
            sample.setServiceEPRs(serviceList.toArray(new String[serviceList.size()]));
            String[] serviceNames = sample.getRequiredServices();
            boolean installable = true;
            if (serviceNames.length > 0) {
                int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId();
                for (String serviceName : serviceNames) {
                    try {
                        if (!Util.isCloudServiceActive(serviceName, tenantId)) {
                            installable = false;
                            break;
                        }
                    } catch (Exception ignored) {
                        // If we are unable to determine whether a cloud service is active, we treat
                        // it as inactive.
                        installable = false;
                        break;
                    }
                }
            }
            sample.setInstallable(installable);
        }
        return samples;
    }

    // Utility Method to obtain the path at which samples are deployed.
    private String getSamplePath(String sampleName) {
        return REGISTRY_SAMPLE_LOCATION + sampleName + APP_ARCHIVE_EXTENSION;
    }

}
