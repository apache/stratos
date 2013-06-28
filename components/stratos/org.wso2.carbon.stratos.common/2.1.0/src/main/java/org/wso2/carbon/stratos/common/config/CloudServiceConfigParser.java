package org.wso2.carbon.stratos.common.config;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;

public class CloudServiceConfigParser {

    private static Log log = LogFactory.getLog(CloudServiceConfigParser.class);

    private static class SynchronizingClass {
    }

    private static final SynchronizingClass loadlock = new SynchronizingClass();

    private static CloudServicesDescConfig cloudServicesDescConfig = null;

    private static final String CONFIG_FILENAME = "cloud-services-desc.xml";

    public static CloudServicesDescConfig loadCloudServicesConfiguration() throws Exception {
        if (cloudServicesDescConfig != null) {
            return cloudServicesDescConfig;
        }

        synchronized (loadlock) {
            if (cloudServicesDescConfig == null) {
                try {
                    String configFileName = CarbonUtils.getCarbonConfigDirPath() + File.separator + 
                            StratosConstants.MULTITENANCY_CONFIG_FOLDER + File.separator + CONFIG_FILENAME;
                    OMElement configElement = CommonUtil.buildOMElement(new FileInputStream(configFileName));
                    cloudServicesDescConfig = new CloudServicesDescConfig(configElement);
                } catch (Exception e) {
                    String msg = "Error in building the cloud service configuration.";
                    log.error(msg, e);
                    throw new Exception(msg, e);
                }
            }
        }
        return cloudServicesDescConfig;
    }

}
