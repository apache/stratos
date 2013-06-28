package org.wso2.carbon.tenant.mgt.internal.util;

import java.util.Properties;

import org.apache.neethi.Policy;
import org.apache.rampart.policy.model.CryptoConfig;
import org.apache.rampart.policy.model.RampartConfig;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.tenant.mgt.services.InMemoryPasswordcallbackHandler;

public class TenantMgtRampartUtil {
	
	public static Policy getDefaultRampartConfig() {
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
        rampartConfig.setPwCbClass("org.wso2.carbon.tenant.mgt.services.InMemoryPasswordcallbackHandler");

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

