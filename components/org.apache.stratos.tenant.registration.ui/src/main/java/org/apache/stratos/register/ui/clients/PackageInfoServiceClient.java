package org.apache.stratos.register.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.apache.stratos.common.packages.stub.PackageInfoServiceStub;
import org.apache.stratos.common.packages.stub.PackageInfo;

/**
 * PackageInfoService client
 */
public class PackageInfoServiceClient {

    private static Log log = LogFactory.getLog(PackageInfoServiceClient.class);

    private PackageInfoServiceStub stub;
    private String epr;

    public PackageInfoServiceClient(
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws Exception {

        epr = backendServerURL + "PackageInfoService";

        try {
            stub = new PackageInfoServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate PackageInfoService client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public PackageInfo[] getBillingPackages() throws Exception {

        try {
            return stub.getPackageInfos();
        } catch (Exception e) {
            String msg = "Failed to get package information: " + e.getMessage();
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    public JSONArray getBillingPackagesJsonArray() throws Exception {

        try {
            PackageInfo[] packageInfoArray = stub.getPackageInfos();
            JSONArray jsonPackageInfoArray = new JSONArray();
            for (PackageInfo packageInfo : packageInfoArray) {
                JSONObject packageInfoObj = new JSONObject();
                packageInfoObj.put("name", packageInfo.getName());
                //TODO https://wso2.org/jira/browse/STRATOS-1819
                packageInfoObj.put("subscriptionCharge", packageInfo.getSubscriptionCharge());
                jsonPackageInfoArray.put(packageInfoObj);
            }
            return jsonPackageInfoArray;
        } catch (Exception e) {
            String msg = "Failed to get package information: " + e.getMessage();
            log.error(msg, e);
            throw new Exception(msg, e);
        }

    }
}
