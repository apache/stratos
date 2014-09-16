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
package org.apache.stratos.metadataservice.handlers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.stratos.metadataservice.context.AuthenticationContext;
import org.apache.stratos.metadataservice.oauth2.ValidationServiceClient;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationResponseDTO;

/**
 * This class responsible for OAuth based authentication/authorization. A client
 * has to bring a valid OAuth token from a
 * a OAuth provider. This class intercept the request and calls the
 * OAuthTokenValidation endpoint of the provider.
 */
public class OAuthHandler extends AbstractAuthenticationAuthorizationHandler {
	private static Log log = LogFactory.getLog(OAuthHandler.class);
	private static String SUPPORTED_AUTHENTICATION_TYPE = "Bearer";
	private static String oauthValidationEndpoint;
	private static String username;
	private static String password;

	public void setOauthValidationEndpoint(String oauthValidationEndpoint) {
		OAuthHandler.oauthValidationEndpoint = oauthValidationEndpoint;
	}

	public void setUsername(String username) {
		OAuthHandler.username = username;
	}

	public void setPassword(String password) {
		OAuthHandler.password = password;
	}

	@Override
	public boolean canHandle(String authHeaderPrefix) {
		return SUPPORTED_AUTHENTICATION_TYPE.equals(authHeaderPrefix);
	}

	@Override
	public Response handle(Message message, ClassResourceInfo classResourceInfo) {
		try {
			OAuth2TokenValidationResponseDTO respDTO;
			ValidationServiceClient validationServiceClient =
			                                                  new ValidationServiceClient(
			                                                                              oauthValidationEndpoint,
			                                                                              username,
			                                                                              password);
			HttpHeaders httpHeaders = new HttpHeadersImpl(message);
			String header = httpHeaders.getRequestHeaders().getFirst("Authorization");
			// if the authorization token has Bearer..
			if (header.startsWith("Bearer ")) {
				String accessToken = header.substring(7).trim();
				respDTO = validationServiceClient.validateAuthenticationRequest(accessToken); // TODO
				                                                                              // :
				                                                                              // send
				                                                                              // scope
				                                                                              // params
				boolean valid = respDTO.getValid();
				if (!valid) {
					// authorization failure..
					return Response.status(Response.Status.FORBIDDEN).build();
				}
			}
		} catch (Exception e) {
			log.error("Error while validating access token", e);
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		AuthenticationContext.setAuthenticated(true);
		return null;
	}
}
