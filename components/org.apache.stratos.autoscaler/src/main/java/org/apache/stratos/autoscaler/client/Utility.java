/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.client;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.Header;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;

import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

public class Utility {
    public static final String SIGNED_JWT_AUTH_USERNAME = "Username";
    public static final String BEARER = "Bearer";
    private static final Log log = LogFactory.getLog(Utility.class);

    /**
     * Set Auth headers to service client. Singed JWT authentication handler expect username
     * as a claim in order to validate the user. This is an alternative to mutual auth.
     *
     * @param serviceClient Service client.
     * @param username      username which is set in header.
     */

    public static void setAuthHeaders(ServiceClient serviceClient, String username) {
        List headerList = new ArrayList();
        Header header = new Header();
        header.setName(HTTPConstants.HEADER_AUTHORIZATION);
        header.setValue(getAuthHeader(username));
        headerList.add(header);
        serviceClient.getOptions().setProperty(HTTPConstants.HTTP_HEADERS, headerList);
    }

    public static String getAuthHeader(String username) throws RuntimeException {

        KeyStoreManager keyStoreManager;
        keyStoreManager = KeyStoreManager.getInstance(MultitenantConstants.SUPER_TENANT_ID);
        try {
            keyStoreManager.getDefaultPrimaryCertificate();
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) keyStoreManager.getDefaultPrivateKey());
            JWTClaimsSet claimsSet = new JWTClaimsSet();
            claimsSet.setClaim(SIGNED_JWT_AUTH_USERNAME, username);
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS512), claimsSet);
            signedJWT.sign(signer);

            return BEARER + " " + Base64Utils.encode(signedJWT.serialize().getBytes());
        } catch (SignatureException e) {
            String msg = "Failed to sign with signature instance";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to get primary default certificate";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
