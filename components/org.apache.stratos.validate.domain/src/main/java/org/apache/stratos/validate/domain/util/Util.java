/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.stratos.validate.domain.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.apache.stratos.validate.domain.internal.ValidateDomainServiceComponent;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.util.CommonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Util methods for domain validationn
 */
public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    private static RegistryService registryService;
    private static RealmService realmService;

    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static UserRegistry getGovernanceSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getGovernanceSystemRegistry(tenantId);
    }

    /**
     * validate the content
     *
     * @param sourceURL - url of the source
     * @param content   - String content
     * @throws RegistryException, if validation failed.
     * @return, true if validated.
     */
    public static boolean validateContent(String sourceURL, String content)
            throws RegistryException {
        URL url;
        try {
            if (sourceURL != null && sourceURL.toLowerCase().startsWith("file:")) {
                String msg = "The source URL must not be file in the server's local file system";
                throw new RegistryException(msg);
            }
            url = new URL(sourceURL);
        } catch (MalformedURLException e) {
            String msg = "Given source URL " + sourceURL + "is not valid.";
            throw new RegistryException(msg, e);
        }

        try {
            URLConnection uc = url.openConnection();
            InputStream in = uc.getInputStream();
            byte[] inByteArr = RegistryUtils.getByteArray(in);
            String onlineContent = new String(inByteArr);
            return onlineContent.startsWith(content);

        } catch (IOException e) {

            String msg = "Could not read from the given URL: " + sourceURL;
            throw new RegistryException(msg, e);
        }
    }

    /**
     * Checks whether the domain is available to register.
     *
     * @param domainName domain name
     * @throws RegistryException, if failed.
     * @return, true if avaiable to register.
     */
    public static boolean checkDomainAvailability(String domainName) throws RegistryException {
        TenantManager tenantManager = Util.getTenantManager();
        int tenantId;
       
        try {
            tenantId = tenantManager.getTenantId(domainName);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in checking the domain availability.";
            log.error(msg);
            // we are instead send the tenant id.
            tenantId = -1;
        }
        return tenantId < 0;
    }

    /**
     * Check whether the domain name entered is valid
     *
     * @param domain tenant domain
     * @return true, if valid.
     */
    public static boolean checkDomainValidity(String domain) {
        // domains with '/' will be seen as possible characters, but they need to be avoided..
        return !(domain.contains("/") || domain.contains("\\"));
    }


    /**
     * Get Domain Validation Key without Login.
     * If the user is generating the validation key without login, we will be serving them a
     * different keys for each refreshes of the page.
     *
     * @param domain - tenant domain
     * @return generated domain validation key.
     * @throws RegistryException, if failed in generating the domain validation key.
     */
    public static String getDomainValidationKeyWithoutLogin(String domain)
            throws RegistryException {
        return generateDomainValidateKey(domain);
    }

    /**
     * Get the domain validation key with login
     * If the user is generating the validation key with login, we will be serving them the same key
     *
     * @param domain - tenant domain.
     * @return generated domain validation key.
     * @throws RegistryException, if failed in generating the domain validation key.
     */
    public static String getDomainValidationKeyWithLogin(String domain) throws RegistryException {
        // get the super tenant system registry
        UserRegistry governanceSystemRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        String domainValidatorInfoPath = StratosConstants.DOMAIN_VALIDATOR_INFO_PATH +
                                         RegistryConstants.PATH_SEPARATOR +
                                         domain + RegistryConstants.PATH_SEPARATOR +
                                         StratosConstants.VALIDATION_KEY_RESOURCE_NAME;
        if (governanceSystemRegistry.resourceExists(domainValidatorInfoPath)) {
            Resource resource = governanceSystemRegistry.get(domainValidatorInfoPath);
            Object contentObj = resource.getContent();
            if (contentObj instanceof String) {
                return (String) contentObj;
            } else if (contentObj instanceof byte[]) {
                return new String((byte[]) contentObj);
            }
        }

        // otherwise we will generate the key
        return generateDomainValidateKey(domain);
    }

    private static String generateDomainValidateKey(String domain) throws RegistryException {
        // get the super tenant system registry
        UserRegistry governanceSystemRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        String domainValidatorInfoPath = StratosConstants.DOMAIN_VALIDATOR_INFO_PATH +
                                         RegistryConstants.PATH_SEPARATOR + domain +
                                         RegistryConstants.PATH_SEPARATOR +
                                         StratosConstants.VALIDATION_KEY_RESOURCE_NAME;

        // we have to reset the domain validation key everytime 
        String domainValidationKey = UUIDGenerator.generateUUID();
        Resource resource = governanceSystemRegistry.newResource();
        resource.setContent(domainValidationKey);
        ((ResourceImpl) resource).setVersionableChange(false);
        governanceSystemRegistry.put(domainValidatorInfoPath, resource);
        CommonUtil.setAnonAuthorization(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                        domainValidatorInfoPath,
                                        governanceSystemRegistry.getUserRealm());

        return domainValidationKey;
    }

    /**
     * validate by DNS Entry.
     *
     * @param domain - tenant domain
     * @return successkey.
     * @throws RegistryException, if validation failed.
     */
    public static String validateByDNSEntry(String domain) throws RegistryException {
        if (!Util.checkDomainValidity(domain)) {
            return "false";
        }
        // get the super tenant system registry
        UserRegistry governanceSystemRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        String domainValidatorInfoPath = StratosConstants.DOMAIN_VALIDATOR_INFO_PATH +
                                         RegistryConstants.PATH_SEPARATOR +
                                         domain + RegistryConstants.PATH_SEPARATOR +
                                         StratosConstants.VALIDATION_KEY_RESOURCE_NAME;
        
        String domainValidationKey = "";
        if (!governanceSystemRegistry.resourceExists(domainValidatorInfoPath)) {
            return "false";
        }
        Resource resource = governanceSystemRegistry.get(domainValidatorInfoPath);
        Object content = resource.getContent();
        if (content instanceof String) {
            domainValidationKey = (String) content;
        } else if (content instanceof byte[]) {
            domainValidationKey = new String((byte[]) content);
        }
        
        int httpPort = CarbonUtils.getTransportPort(ValidateDomainServiceComponent.getConfigContextService(), "http");
        int httpProxyPort =
            CarbonUtils.getTransportProxyPort(ValidateDomainServiceComponent.getConfigContextService().getServerConfigContext(),
                                              "http");


        // check whether this is running on a different port
        int portValue = httpProxyPort != -1 ? httpProxyPort : httpPort;

        // we are forming the resource http url to access the domainValidationKey
        String domainValidatorInfoAbsolutePath = RegistryUtils.getAbsolutePath(governanceSystemRegistry.getRegistryContext(),
                                                                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                                                domainValidatorInfoPath);

        String sourceUrl = "http://" + domainValidationKey + "." + domain + ":"+ portValue +
                           RegistryConstants.PATH_SEPARATOR + "registry" +
                           RegistryConstants.PATH_SEPARATOR + "resource" + domainValidatorInfoAbsolutePath;
        if (Util.validateContent(sourceUrl, domainValidationKey)) {
            // the validation success, so we will create the success key
            // (this is a temporary key that we keep the session
            String successKey = UUIDGenerator.generateUUID();
            resource.setProperty("successKey", successKey);
            ((ResourceImpl) resource).setVersionableChange(false);
            governanceSystemRegistry.put(domainValidatorInfoPath, resource);
            return successKey;
        }
        return "false";
    }

    /**
     * Validate by text in root
     *
     * @param domain - tenant domain
     * @return successkey
     * @throws RegistryException, if validation failed.
     */
    public static String validateByTextInRoot(String domain) throws RegistryException {
        if (!Util.checkDomainValidity(domain)) {
            return "false";
        }

        UserRegistry governanceSystemRegistry =
                Util.getGovernanceSystemRegistry(MultitenantConstants.SUPER_TENANT_ID);
        String domainValidatorInfoPath = StratosConstants.DOMAIN_VALIDATOR_INFO_PATH +
                                         RegistryConstants.PATH_SEPARATOR +
                                         domain + RegistryConstants.PATH_SEPARATOR +
                                         StratosConstants.VALIDATION_KEY_RESOURCE_NAME;
        String domainValidationKey = "";
        if (!governanceSystemRegistry.resourceExists(domainValidatorInfoPath)) {
            return "false";
        }
        Resource resource = governanceSystemRegistry.get(domainValidatorInfoPath);
        Object content = resource.getContent();
        if (content instanceof String) {
            domainValidationKey = (String) content;
        } else if (content instanceof byte[]) {
            domainValidationKey = new String((byte[]) content);
        }


        // the filename is hard coded now, a change in this need to be reflected at
        //  validate_domain.jsp
        String sourceUrl = "http://" + domain + "/wso2multitenancy.txt";
        if (Util.validateContent(sourceUrl, domainValidationKey)) {
            // the validation success, so we will create the success key
            // this is a temporary key that we keep the session
            String successKey = UUIDGenerator.generateUUID();
            resource.setProperty("successKey", successKey);
            ((ResourceImpl) resource).setVersionableChange(false);
            governanceSystemRegistry.put(domainValidatorInfoPath, resource);
            return successKey;
        }
        return "false";
    }
}
