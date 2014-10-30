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

package org.apache.stratos.metadata.client.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.stratos.metadata.client.config.MetaDataClientConfig;
import org.apache.stratos.metadata.client.exception.RestClientException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class DefaultRestClient implements RestClient {

    private static final String APPLICATION_JSON = "application/json";
    private static final Log log = LogFactory.getLog(DefaultRestClient.class);

    private final String username;
    private final String password;

    private HttpClient httpClient;

    public DefaultRestClient(String username, String password) throws RestClientException {
        this.username = username;
        this.password = password;

        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslConnectionFactory;
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslConnectionFactory = new SSLConnectionSocketFactory(builder.build());
        } catch (NoSuchAlgorithmException e) {
            throw new RestClientException(e);
        } catch (KeyManagementException e) {
            throw new RestClientException(e);
        } catch (KeyStoreException e) {
            throw new RestClientException(e);
        }
        CloseableHttpClient closableHttpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionFactory)
                .setConnectionManager(HTTPConnectionManager.getInstance().getHttpConnectionManager()).build();
        this.httpClient = closableHttpClient;
    }

    public HttpResponse doPost(String resourcePath, Object payload) throws RestClientException {

        HttpPost post = new HttpPost(resourcePath);
        addPayloadJsonString(payload, post);
        setAuthHeader(post);
        try {
            return httpClient.execute(post);

        } catch (IOException e) {
            String errorMsg = "Error while executing POST statement";
            log.error(errorMsg, e);
            throw new RestClientException(errorMsg, e);
        } finally {
            post.releaseConnection();
        }
    }

    private void setAuthHeader(HttpRequestBase post) {
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            return;
        }
        String identity = username + ":" + password;
        String encoding = new String(Base64.encodeBase64(identity.getBytes()));
        post.setHeader("Authorization", "Basic " + encoding);
    }

    private String getUsername() {
        return MetaDataClientConfig.getInstance().getUsername();
    }

    private String getPassword() {
        return MetaDataClientConfig.getInstance().getPassword();
    }

    private void addPayloadJsonString(Object payload, HttpPost post) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        String payloadText = gson.toJson(payload, payload.getClass());
        addStringPayload(post, payloadText);
    }

    public HttpResponse doGet(String resourcePath) throws RestClientException {

        HttpGet get = new HttpGet(resourcePath);
        setAuthHeader(get);

        try {
            return httpClient.execute(get);

        } catch (IOException e) {
            String errorMsg = "Error while executing GET statement";
            log.error(errorMsg, e);
            throw new RestClientException(errorMsg, e);
        } finally {
            get.releaseConnection();
        }
    }

    public HttpResponse doDelete(String resourcePath) throws RestClientException {

        HttpDelete delete = new HttpDelete(resourcePath);
        setAuthHeader(delete);

        try {
            return httpClient.execute(delete);

        } catch (IOException e) {
            String errorMsg = "Error while executing DELETE statement";
            log.error(errorMsg, e);
            throw new RestClientException(errorMsg, e);
        } finally {
            delete.releaseConnection();
        }
    }


    private void addStringPayload(HttpPost post, String payloadText) {
        if (org.apache.commons.lang.StringUtils.isEmpty(payloadText)) {
            throw new IllegalArgumentException("Payload text can not be null or empty");
        }
        StringEntity stringEntity = null;
        try {
            stringEntity = new StringEntity(payloadText);
            stringEntity.setContentType(APPLICATION_JSON);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        post.setEntity(stringEntity);
    }
}
