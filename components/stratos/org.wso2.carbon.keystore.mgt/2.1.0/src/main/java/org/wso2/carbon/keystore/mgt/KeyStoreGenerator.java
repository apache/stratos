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
package org.wso2.carbon.keystore.mgt;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.keystore.mgt.util.RealmServiceHolder;
import org.wso2.carbon.keystore.mgt.util.RegistryServiceHolder;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.security.SecurityConstants;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This class is used to generate a key store for a tenant and store it in the governance registry.
 */
public class KeyStoreGenerator {

    private static Log log = LogFactory.getLog(KeyStoreGenerator.class);
    private UserRegistry govRegistry;
    private int tenantId;
    private String tenantDomain;
    private String password;


    public KeyStoreGenerator(int  tenantId) throws KeyStoreMgtException {
        try {
            this.tenantId = tenantId;
            this.tenantDomain = getTenantDomainName();
            this.govRegistry = RegistryServiceHolder.getRegistryService().
                    getGovernanceSystemRegistry(tenantId);
            if(govRegistry == null){
                log.error("Governance registry instance is null");
                throw new KeyStoreMgtException("Governance registry instance is null");
            }
        } catch (RegistryException e) {
            String errorMsg = "Error while obtaining the governance registry for tenant : " +
                      tenantId;
            log.error(errorMsg, e);
            throw new KeyStoreMgtException(errorMsg, e);
        }
    }


    /**
     * This method first generates the keystore, then persist it in the gov.registry of that tenant
     *
     * @throws KeyStoreMgtException Error when generating or storing the keystore
     */
    public void generateKeyStore() throws KeyStoreMgtException {
        try {
            password = generatePassword();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, password.toCharArray());
            X509Certificate pubCert = generateKeyPair(keyStore);
            persistKeyStore(keyStore, pubCert);
        } catch (Exception e) {
            String msg = "Error while instantiating a keystore";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }

    /**
     * This method generates the keypair and stores it in the keystore
     *
     * @param keyStore A keystore instance
     * @return Generated public key for the tenant
     * @throws KeyStoreMgtException Error when generating key pair
     */
    private X509Certificate generateKeyPair(KeyStore keyStore) throws KeyStoreMgtException {
        try {
            CryptoUtil.getDefaultCryptoUtil();
            //generate key pair
            KeyPairGenerator keyPairGenerator = null;
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Common Name and alias for the generated certificate
            String commonName = "CN=" + tenantDomain + ", OU=None, O=None L=None, C=None";

            //generate certificates
            X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
            v3CertGen.setSerialNumber(BigInteger.valueOf(new SecureRandom().nextInt()));
            v3CertGen.setIssuerDN(new X509Principal(commonName));
            v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
            v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
            v3CertGen.setSubjectDN(new X509Principal(commonName));
            v3CertGen.setPublicKey(keyPair.getPublic());
            v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");
            X509Certificate PKCertificate = v3CertGen.generateX509Certificate(keyPair.getPrivate());

            //add private key to KS
            keyStore.setKeyEntry(tenantDomain, keyPair.getPrivate(), password.toCharArray(),
                                 new java.security.cert.Certificate[]{PKCertificate});
            return PKCertificate;
        } catch (Exception ex) {
            String msg = "Error while generating the certificate for tenant :" +
                         tenantDomain + ".";
            log.error(msg, ex);
            throw new KeyStoreMgtException(msg, ex);
        }

    }

    /**
     * Persist the keystore in the gov.registry
     *
     * @param keyStore created Keystore of the tenant
     * @param PKCertificate pub. key of the tenant
     * @throws KeyStoreMgtException Exception when storing the keystore in the registry
     */
    private void persistKeyStore(KeyStore keyStore, X509Certificate PKCertificate)
            throws KeyStoreMgtException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keyStore.store(outputStream, password.toCharArray());
            outputStream.flush();
            outputStream.close();

            String keyStoreName = generateKSNameFromDomainName();
            // Use the keystore using the keystore admin
            KeyStoreAdmin keystoreAdmin = new KeyStoreAdmin(tenantId, govRegistry);
            keystoreAdmin.addKeyStore(outputStream.toByteArray(), keyStoreName,
                                      password, " ", "JKS", password);
            
            //Create the pub. key resource
            Resource pubKeyResource = govRegistry.newResource();
            pubKeyResource.setContent(PKCertificate.getEncoded());
            pubKeyResource.addProperty(SecurityConstants.PROP_TENANT_PUB_KEY_FILE_NAME_APPENDER,
                                       generatePubKeyFileNameAppender());

            govRegistry.put(RegistryResources.SecurityManagement.TENANT_PUBKEY_RESOURCE, pubKeyResource);

            //associate the public key with the keystore
            govRegistry.addAssociation(RegistryResources.SecurityManagement.KEY_STORES + "/" + keyStoreName,
                                       RegistryResources.SecurityManagement.TENANT_PUBKEY_RESOURCE,
                                       SecurityConstants.ASSOCIATION_TENANT_KS_PUB_KEY);

        } catch (RegistryException e) {
            String msg = "Error when writing the keystore/pub.cert to registry";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
        catch (Exception e) {
            String msg = "Error when processing keystore/pub. cert to be stored in registry";
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }

    /**
     * This method is used to generate a random password for the generated keystore
     *
     * @return generated password
     */
    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        String randString = new BigInteger(130, random).toString(12);
        return randString.substring(randString.length() - 10, randString.length());
    }

    /**
     * This method is used to generate a file name appender for the pub. cert, e.g.
     * example-com-343743.cert
     * @return generated string to be used as a file name appender
     */
    private String generatePubKeyFileNameAppender(){
        String uuid = UUIDGenerator.getUUID();
        return uuid.substring(uuid.length() - 6, uuid.length()-1);
    }

    /**
     * This method generates the key store file name from the Domain Name
     * @return
     */
    private String generateKSNameFromDomainName(){
        String ksName = tenantDomain.trim().replace(".", "-");
        return (ksName + ".jks" );
    }

    private String getTenantDomainName() throws KeyStoreMgtException {
        RealmService realmService = RealmServiceHolder.getRealmService();
        if (realmService == null) {
            String msg = "Error in getting the domain name, realm service is null.";
            log.error(msg);
            throw new KeyStoreMgtException(msg);
        }
        try {
            return realmService.getTenantManager().getDomain(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in getting the domain name for the tenant id: " + tenantId;
            log.error(msg, e);
            throw new KeyStoreMgtException(msg, e);
        }
    }
}
