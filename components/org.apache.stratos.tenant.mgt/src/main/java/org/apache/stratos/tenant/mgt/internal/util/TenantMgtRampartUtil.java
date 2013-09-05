package org.apache.stratos.tenant.mgt.internal.util;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import java.util.Properties;

import org.apache.stratos.tenant.mgt.services.InMemoryPasswordcallbackHandler;
import org.apache.neethi.Policy;
import org.apache.rampart.policy.model.CryptoConfig;
import org.apache.rampart.policy.model.RampartConfig;
import org.wso2.carbon.base.ServerConfiguration;

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
        rampartConfig.setPwCbClass("org.apache.stratos.tenant.mgt.services.InMemoryPasswordcallbackHandler");

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

