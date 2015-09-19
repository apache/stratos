/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stratos.integration.common.rest;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.net.URI;

/**
 * Rest client to handle rest requests
 */
public class RestClient {
    private static final Log log = LogFactory.getLog(RestClient.class);
    private DefaultHttpClient httpClient;
    private String endPoint;
    private String userName;
    private String password;

    public RestClient() {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 50
        cm.setDefaultMaxPerRoute(50);

        httpClient = new DefaultHttpClient(cm);
        httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);
    }

    public RestClient(String endPoint, String userName, String password) {
        this();
        this.endPoint = endPoint;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Handle http post request. Return String
     *
     * @param resourcePath    This should be REST endpoint
     * @param jsonParamString The json string which should be executed from the post request
     * @return The HttpResponse
     * @throws Exception if any errors occur when executing the request
     */
    public HttpResponse doPost(URI resourcePath, String jsonParamString) throws Exception {
        HttpPost postRequest = null;
        try {
            postRequest = new HttpPost(resourcePath);
            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            String userPass = getUsernamePassword();
            String basicAuth =
                    "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            postRequest.addHeader("Authorization", basicAuth);

            return httpClient.execute(postRequest, new HttpResponseHandler());
        }
        finally {
            releaseConnection(postRequest);
        }
    }

    /**
     * Handle http get request. Return String
     *
     * @param resourcePath This should be REST endpoint
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *                                                        if any errors occur when executing the request
     */
    public HttpResponse doGet(URI resourcePath) throws Exception {
        HttpGet getRequest = null;
        try {
            getRequest = new HttpGet(resourcePath);
            getRequest.addHeader("Content-Type", "application/json");
            String userPass = getUsernamePassword();
            String basicAuth =
                    "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            getRequest.addHeader("Authorization", basicAuth);

            return httpClient.execute(getRequest, new HttpResponseHandler());
        }
        finally {
            releaseConnection(getRequest);
        }
    }

    public HttpResponse doDelete(URI resourcePath) throws Exception {
        HttpDelete httpDelete = null;
        try {
            httpDelete = new HttpDelete(resourcePath);
            httpDelete.addHeader("Content-Type", "application/json");
            String userPass = getUsernamePassword();
            String basicAuth =
                    "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            httpDelete.addHeader("Authorization", basicAuth);
            return httpClient.execute(httpDelete, new HttpResponseHandler());
        }
        finally {
            releaseConnection(httpDelete);
        }
    }

    public HttpResponse doPut(URI resourcePath, String jsonParamString) throws Exception {

        HttpPut putRequest = null;
        try {
            putRequest = new HttpPut(resourcePath);

            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            putRequest.setEntity(input);
            String userPass = getUsernamePassword();
            String basicAuth =
                    "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
            putRequest.addHeader("Authorization", basicAuth);
            return httpClient.execute(putRequest, new HttpResponseHandler());
        }
        finally {
            releaseConnection(putRequest);
        }
    }

    private void releaseConnection(HttpRequestBase request) {
        if (request != null) {
            request.releaseConnection();
        }
    }

    public boolean addEntity(String filePath, String resourcePath, String entityName) throws Exception {
        String content = getJsonStringFromFile(filePath);
        URI uri = new URIBuilder(this.endPoint + resourcePath).build();

        HttpResponse response = doPost(uri, content);
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return true;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                if (errorResponse != null) {
                    throw new RuntimeException(errorResponse.getErrorMessage());
                }
            }
        }
        throw new Exception("Null response received. Could not add entity [entity name] " + entityName);
    }

    public boolean deployEntity(String resourcePath, String entityName) throws Exception {
        URI uri = new URIBuilder(this.endPoint + resourcePath).build();

        HttpResponse response = doPost(uri, "");
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return true;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                if (errorResponse != null) {
                    throw new RuntimeException(errorResponse.getErrorMessage());
                }
            }
        }
        throw new Exception("Null response received. Could not deploy entity [entity name] " + entityName);
    }

    public boolean undeployEntity(String resourcePath, String entityName) throws Exception {
        URI uri = new URIBuilder(this.endPoint + resourcePath).build();

        HttpResponse response = doPost(uri, "");
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return true;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                if (errorResponse != null) {
                    throw new RuntimeException(errorResponse.getErrorMessage());
                }
            }
        }
        throw new Exception("Null response received. Could not undeploy entity [entity name] " + entityName);
    }

    public Object getEntity(String resourcePath, String identifier, Class responseJsonClass,
                            String entityName) throws Exception {
        URI uri = new URIBuilder(this.endPoint + resourcePath + "/" + identifier).build();
        HttpResponse response = doGet(uri);
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return gson.fromJson(response.getContent(), responseJsonClass);
            } else if (response.getStatusCode() == 404) {
                return null;
            } else {
                ErrorResponse errorResponse = gson.fromJson(response.getContent(),
                        ErrorResponse.class);
                if (errorResponse != null) {
                    throw new RuntimeException(errorResponse.getErrorMessage());
                }
            }
        }
        throw new Exception("Null response received. Could not get entity [entity name] " + entityName);
    }

    public Object listEntity(String resourcePath, Type type, String entityName) throws Exception {
        URI uri = new URIBuilder(this.endPoint + resourcePath).build();
        HttpResponse response = doGet(uri);
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return gson.fromJson(response.getContent(), type);
            } else if (response.getStatusCode() == 404) {
                return null;
            } else {
                ErrorResponse errorResponse = gson.fromJson(response.getContent(),
                        ErrorResponse.class);
                if (errorResponse != null) {
                    throw new RuntimeException(errorResponse.getErrorMessage());
                }
            }
        }
        throw new Exception("Null response received. Could not get entity [entity name] " + entityName);
    }

    public boolean removeEntity(String resourcePath, String identifier, String entityName) throws Exception {
        URI uri = new URIBuilder(this.endPoint + "/" + resourcePath + "/" + identifier).build();
        HttpResponse response = doDelete(uri);
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return true;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                ErrorResponse errorResponse = gson.fromJson(response.getContent(),
                        ErrorResponse.class);
                log.error("Error response while removing entity [identifier] " + identifier + ", [entity name] " +
                        entityName + ", [error] " + errorResponse.getErrorMessage() + ", [error code] " + errorResponse
                        .getErrorCode());
                return false;
            }
        }
        throw new Exception("Null response received. Could not remove entity [entity name] " + entityName);
    }

    public boolean updateEntity(String filePath, String resourcePath, String entityName) throws Exception {
        String content = getJsonStringFromFile(filePath);
        URI uri = new URIBuilder(this.endPoint + resourcePath).build();

        HttpResponse response = doPut(uri, content);
        if (response != null) {
            if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                return true;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                ErrorResponse errorResponse = gson.fromJson(response.getContent(),
                        ErrorResponse.class);
                if (errorResponse != null) {
                    throw new RuntimeException(errorResponse.getErrorMessage());
                }
            }
        }
        throw new Exception("Null response received. Could not update entity [entity name] " + entityName);
    }

    /**
     * Get the json string from the artifacts directory
     *
     * @param filePath path of the artifacts
     * @return json string of the relevant artifact
     * @throws FileNotFoundException
     */
    public String getJsonStringFromFile(String filePath) throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        Object object = parser.parse(new FileReader(getResourcesFolderPath() + filePath));
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        return gson.toJson(object);
    }

    /**
     * Get resources folder path
     *
     * @return the resource path
     */
    private String getResourcesFolderPath() {
        String path = getClass().getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }

    /**
     * Get the username and password
     *
     * @return username:password
     */
    private String getUsernamePassword() {
        return this.userName + ":" + this.password;
    }
}