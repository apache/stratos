/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadata.service.oauth2;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.wso2.carbon.identity.oauth2.stub.OAuth2TokenValidationServiceStub;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.utils.CarbonUtils;

import java.rmi.RemoteException;

/**
 * Service class wrapper for OAuthTokenValidation endpoint.
 */
public class ValidationServiceClient {
    private static final Log log = LogFactory.getLog(OAuth2TokenValidationServiceStub.class);
    private OAuth2TokenValidationServiceStub stub = null;

    public ValidationServiceClient(String backendServerURL, String username, String password)
            throws Exception {
        String serviceURL = backendServerURL + "OAuth2TokenValidationService";
        try {
            stub = new OAuth2TokenValidationServiceStub(serviceURL);
            CarbonUtils.setBasicAccessSecurityHeaders(username, password, true,
                    stub._getServiceClient());
        } catch (AxisFault e) {
            log.error("Error initializing OAuth2 Client");
            throw new Exception("Error initializing OAuth Client", e);
        }
    }

    public OAuth2TokenValidationResponseDTO validateAuthenticationRequest(String accessToken)
            throws Exception {
        OAuth2TokenValidationRequestDTO oauthReq = new OAuth2TokenValidationRequestDTO();
        oauthReq.setAccessToken(accessToken);
        oauthReq.setTokenType(OAuthConstants.BEARER_TOKEN_TYPE);
        try {
            return stub.validate(oauthReq);
        } catch (RemoteException e) {
            log.error("Error while validating OAuth2 request");
            throw new Exception("Error while validating OAuth2 request", e);
        }
    }

}
