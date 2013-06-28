package org.wso2.carbon.gapp.registration.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.rampart.RampartMessageData;
import org.apache.rampart.policy.model.CryptoConfig;
import org.apache.rampart.policy.model.RampartConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.stratos.common.config.CloudServiceConfig;
import org.wso2.carbon.stratos.common.config.CloudServiceConfigParser;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.stratos.common.packages.stub.PackageInfo;
import org.wso2.carbon.stratos.common.packages.stub.PackageInfoServiceStub;
import org.wso2.carbon.tenant.register.gapp.stub.GAppTenantRegistrationServiceStub;
import org.wso2.carbon.tenant.register.gapp.stub.xsd.TenantInfoBean;
import org.wso2.carbon.utils.CarbonUtils;

public class GoolgeAppsRegistrationClient {

    private static final Log log = LogFactory.getLog(GoolgeAppsRegistrationClient.class);
    
    
    private static final String GAPP_PROPERTIES_FILE = "gapp.setup.properties";
    private static Properties consumerKeys = new Properties();
    
    public GoolgeAppsRegistrationClient() {
        
    }
       
    public void registerTenantForGAppDomain(String backendServerURL, 
    		                                ConfigurationContext configContext,
    		                                String domain,
    		                                String email, 
    		                                String adminFirstName, 
    		                                String adminLastName,
    		                                String usagePlan) throws Exception {
    	try {
    	    Map<String, CloudServiceConfig> cloudServiceConfigs = CloudServiceConfigParser.
                                                                                          loadCloudServicesConfiguration().
                                                                                          getCloudServiceConfigs();
            String managerHomepageURL =
                                        cloudServiceConfigs.get(StratosConstants.CLOUD_MANAGER_SERVICE).
                                                            getLink();
        	String serviceURL = managerHomepageURL + "/services/GAppTenantRegistrationService";
        	GAppTenantRegistrationServiceStub stub = new GAppTenantRegistrationServiceStub(configContext, serviceURL);
            ServiceClient client = stub._getServiceClient();
            // Engage rampart as we are going to sign requests to Relying Party Service
            client.engageModule("rampart");
            // Get a RampartConfig with default crypto information
            Policy rampartConfig = getDefaultRampartConfig();
            Policy signOnly = getSignOnlyPolicy();
            Policy mergedPolicy = signOnly.merge(rampartConfig);
            Options option = client.getOptions();
            option.setProperty(RampartMessageData.KEY_RAMPART_POLICY, mergedPolicy);
            option.setManageSession(true);
       
            String username = email.substring(0, email.indexOf("@"));
            TenantInfoBean bean = new TenantInfoBean();
            bean.setEmail(email);
            bean.setFirstname(adminFirstName);
            bean.setLastname(adminLastName);
            bean.setTenantDomain(domain);
            bean.setAdmin(username);
            bean.setUsagePlan(usagePlan);
            bean.setCreatedDate(Calendar.getInstance());
            stub.registerGoogleAppsTenant(bean);
        } catch (Exception e) {
            String msg = "Failed to create domain because " + e.getMessage();
            log.error(msg, e);
            throw new Exception(msg);
        }
        
    }

    public JSONArray buildPackageInfoJSONArray(PackageInfo[] packages) throws Exception {
        JSONArray packageInfo = new JSONArray();
        try {
            for (PackageInfo pak : packages) {
                JSONObject obj = new JSONObject();
                obj.put("name", pak.getName());
                obj.put("users", pak.getUsersLimit());
                packageInfo.put(obj);
            }
            return packageInfo;
        } catch (JSONException e) {
            String msg = "Failed to build JSON list " + e.getMessage();
            log.error(msg, e);
            throw new Exception(msg);
        }
    }
    
    public PackageInfo[] getPackageInfo(String backendServerURL,
            ConfigurationContext configContext) throws Exception {
    	String packageServiceEpr = backendServerURL + "PackageInfoService";
    	PackageInfoServiceStub packageStub = null;
    	PackageInfo[] packageInfo = new PackageInfo[0];
        try {
        	packageStub = new PackageInfoServiceStub(configContext, packageServiceEpr);
            PackageInfo[] packages = packageStub.getPackageInfos();
            int i = 0;
            packageInfo = new PackageInfo[packages.length];
            for (PackageInfo pack : packages ) {
            	packageInfo[i] = new PackageInfo();
                packageInfo[i].setName(pack.getName());
                packageInfo[i].setUsersLimit(pack.getUsersLimit());
                i++;
            }
            return packageInfo;
        } catch (Exception e) {
            String msg = "Failed to retrieve package list " + e.getMessage();
            log.error(msg, e);
            throw new Exception(msg);
        }
    } 
     
	private Policy getSignOnlyPolicy() {
		URL url = null;
		InputStream inStream;
		try {
			inStream = null;
			if ((url = this.getClass().getClassLoader().getResource("signonly-policy.xml")) != null) {
			    inStream = url.openStream();
			} else {
				throw new Exception("The file is not present in bundle");
			}
			Policy policy = org.apache.neethi.PolicyEngine.getPolicy(inStream);
			return policy;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error while creating policy");
		}
	}

    public boolean isRegisteredAsGoogleAppDomain(ConfigurationContext configContext,
                                                 String domain) throws Exception {
        
        Map<String, CloudServiceConfig> cloudServiceConfigs = CloudServiceConfigParser.
                                                                                      loadCloudServicesConfiguration().
                                                                                      getCloudServiceConfigs();
        String managerHomepageURL =
            cloudServiceConfigs.get(StratosConstants.CLOUD_MANAGER_SERVICE).
                                getLink();
        String serviceURL = managerHomepageURL + "/services/GAppTenantRegistrationService";

        GAppTenantRegistrationServiceStub stub = new GAppTenantRegistrationServiceStub(configContext, serviceURL);
        ServiceClient client = stub._getServiceClient();
        // Engage rampart as we are going to sign requests to Relying Party Service
        client.engageModule("rampart");
        // Get a RampartConfig with default crypto information
        Policy rampartConfig = getDefaultRampartConfig();
        Policy signOnly = getSignOnlyPolicy();
        Policy mergedPolicy = signOnly.merge(rampartConfig);
        Options option = client.getOptions();
        option.setProperty(RampartMessageData.KEY_RAMPART_POLICY, mergedPolicy);
        option.setManageSession(true);
   
        return stub.isRegisteredAsGoogleAppDomain(domain);
    }
    
	public static String getGoogleAppSetupPropery(String name) throws Exception {
        try {
            if (consumerKeys.size() == 0) {
                String carbonHome = CarbonUtils.getCarbonHome();
                if (carbonHome != null) {
                    File gappSetupProperties =
                                               new File(CarbonUtils.getCarbonConfigDirPath(),
                                                        GAPP_PROPERTIES_FILE);
                    
                    FileInputStream in = new FileInputStream(gappSetupProperties);
                    consumerKeys.load(in);
                }
            }
            return (String)consumerKeys.get(name);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage(), e);
        }
	}

	private Policy getDefaultRampartConfig() {
        //Extract the primary keystore information from server configuration
        ServerConfiguration serverConfig = ServerConfiguration.getInstance();
        String keyStore = serverConfig.getFirstProperty("Security.KeyStore.Location");
        String keyStoreType = serverConfig.getFirstProperty("Security.KeyStore.Type");
        String keyStorePassword = serverConfig.getFirstProperty("Security.KeyStore.Password");
        String privateKeyAlias = serverConfig.getFirstProperty("Security.KeyStore.KeyAlias");
        String privateKeyPassword = serverConfig.getFirstProperty("Security.KeyStore.KeyPassword");

        //Populate Rampart Configuration
        RampartConfig rampartConfig = new RampartConfig();
        rampartConfig.setUser(privateKeyAlias);
        rampartConfig.setPwCbClass("org.wso2.carbon.gapp.registration.ui.InMemoryPasswordcallbackHandler");

        //Set the private key alias and private key password in the password callback handler
        InMemoryPasswordcallbackHandler.addUser(privateKeyAlias, privateKeyPassword);

        CryptoConfig sigCrypto = new CryptoConfig();
        Properties props = new Properties();
        sigCrypto.setProvider("org.apache.ws.security.components.crypto.Merlin");
        props.setProperty("org.apache.ws.security.crypto.merlin.keystore.type", keyStoreType);
        props.setProperty("org.apache.ws.security.crypto.merlin.file", keyStore);
        props.setProperty("org.apache.ws.security.crypto.merlin.keystore.password", keyStorePassword);
        sigCrypto.setProp(props);

        rampartConfig.setSigCryptoConfig(sigCrypto);
        Policy policy = new Policy();
        policy.addAssertion(rampartConfig);

        return policy;

    }
}
