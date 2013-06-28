/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.billing.mgt.api;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.dataobjects.Cash;
import org.wso2.carbon.billing.mgt.dataobjects.MultitenancyPackage;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reads the multitenancy-packages.xml and populates the
 * multitenancy packages list
 */
public class MultitenancyBillingInfo {
    private static Log log = LogFactory.getLog(MultitenancyBillingInfo.class);
    private static final String PACKAGE_DESCRIPTION_CONFIG_FILENAME = "multitenancy-packages.xml";
    private static final String PACKAGE_DESCRIPTION_CONFIG_NS =
            "http://wso2.com/carbon/multitenancy/billing/pacakges";
    
    List<MultitenancyPackage> multitenancyPackages = new ArrayList<MultitenancyPackage>();

    public MultitenancyBillingInfo() throws BillingException {
        // this should be only available to the super tenants..
        multitenancyPackages = deserializePackageDescriptionConfig();
    }

    public List<MultitenancyPackage> getMultitenancyPackages() {
        return multitenancyPackages;
    }

    /*
     * Deserialize following XML
    <packages xmlns="http://wso2.com/carbon/multitenancy/billing/pacakges">
        <package name="multitenancy-free">
            <!--<subscriptionCharge>0</subscriptionCharge>--> <!-- $ per month -->
            <users>
                <limit>5</limit>
                <charge>0</charge> <!-- charge per month -->
            </users>
            <resourceVolume>
                <limit>10</limit> <!--mb per user -->
            </resourceVolume>
            <bandwidth>
                <limit>1000</limit> <!-- mb per user -->
                <overuseCharge>0</overuseCharge> <!-- $ per user per month -->
            </bandwidth>
        </package>
        <package name="multitenancy-small">
            ...
        </package>
    </packages>
     */
    private List<MultitenancyPackage> deserializePackageDescriptionConfig() throws BillingException {
        String configFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator +
                PACKAGE_DESCRIPTION_CONFIG_FILENAME;
        
        OMElement packageConfigs;
        try {
            packageConfigs = CommonUtil.buildOMElement(new FileInputStream(configFilePath));
        } catch (Exception e) {
            String msg = "Error in deserializing the packageConfigs file: " + configFilePath + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        Iterator packageConfigsChildsIt = packageConfigs.getChildElements();
        while (packageConfigsChildsIt.hasNext()) {
            OMElement packageConfigEle = (OMElement) packageConfigsChildsIt.next();
            if (!new QName(PACKAGE_DESCRIPTION_CONFIG_NS, "package").equals(
                    packageConfigEle.getQName())) {
                continue;
            }
            
            MultitenancyPackage multitenancyPackage = new MultitenancyPackage();
            String packageName = packageConfigEle.getAttributeValue(new QName("name"));
            String subscriptionCharge = getPackageConfigValue("subscriptionCharge", packageConfigEle);
            String usersLimit = getPackageConfigValue("users.limit", packageConfigEle);
            String usersCharge = getPackageConfigValue("users.charge", packageConfigEle);

            String resourceVolumeLimit =
                    getPackageConfigValue("resourceVolume.limit", packageConfigEle);
            String resourceVolumeOveruseCharge =
                    getPackageConfigValue("resourceVolume.overuseCharge", packageConfigEle);
            String bandwidthLimit = getPackageConfigValue("bandwidth.limit", packageConfigEle);
            String bandwidthOveruseCharge =
                    getPackageConfigValue("bandwidth.overuseCharge", packageConfigEle);
            String cartridgeCPUHourLimit =
                    getPackageConfigValue("cartridge.hourLimit", packageConfigEle);
            String cartridgeCPUHourOverUsageCharge =
                    getPackageConfigValue("cartridge.overUsageCharge", packageConfigEle);
            int usersLimitInt = -1;
            if (!usersLimit.equals("unlimited")) {
                usersLimitInt = Integer.parseInt(usersLimit);
            }
            int resourceVolumeLimitInt = -1;
            if (!resourceVolumeLimit.equals("unlimited")) {
                resourceVolumeLimitInt = Integer.parseInt(resourceVolumeLimit);
            }
            int bandwidthLimitInt = -1;
            if (!bandwidthLimit.equals("unlimited")) {
                bandwidthLimitInt = Integer.parseInt(bandwidthLimit);
            }
            
            int cartridgeCPUHourLimitInt = -1;
            if(cartridgeCPUHourLimit!=null && !cartridgeCPUHourLimit.equals("unlimited")){
                cartridgeCPUHourLimitInt = Integer.parseInt(cartridgeCPUHourLimit);
            }


            multitenancyPackage.setName(packageName);
            multitenancyPackage.setSubscriptionCharge(new Cash(subscriptionCharge));
            multitenancyPackage.setUsersLimit(usersLimitInt);
            multitenancyPackage.setChargePerUser(new Cash(usersCharge));
            multitenancyPackage.setResourceVolumeLimit(resourceVolumeLimitInt);
            multitenancyPackage.setResourceVolumeOveruseCharge(new Cash(resourceVolumeOveruseCharge));
            multitenancyPackage.setBandwidthLimit(bandwidthLimitInt);
            multitenancyPackage.setBandwidthOveruseCharge(new Cash(bandwidthOveruseCharge));
            multitenancyPackage.setCartridgeCPUHourLimit(cartridgeCPUHourLimitInt);
            multitenancyPackage.setCartridgeCPUOveruseCharge(new Cash(cartridgeCPUHourOverUsageCharge));

            
            multitenancyPackages.add(multitenancyPackage);
        }
        return multitenancyPackages;
    }

    private String getPackageConfigValue(String key, OMElement packageNode) throws BillingException {
        String qualifiedKey = "ns:" + key.replaceAll("\\.", "/ns:");
        AXIOMXPath xpathExpression;
        try {
            xpathExpression = new AXIOMXPath(qualifiedKey);
            xpathExpression.addNamespace("ns", PACKAGE_DESCRIPTION_CONFIG_NS);
            List valueNodes = xpathExpression.selectNodes(packageNode);
            if (valueNodes.isEmpty()) {
                if (log.isDebugEnabled()) {
                    String msg = "No results found parsing package configuration for key: " + 
                            qualifiedKey + ".";
                    log.debug(msg);
                }
                return null;
            }
            OMElement valueNode = (OMElement) valueNodes.get(0);
            return valueNode.getText();
        } catch (JaxenException e) {
            String msg = "Error in retrieving the key: " + qualifiedKey + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }
    }
}
