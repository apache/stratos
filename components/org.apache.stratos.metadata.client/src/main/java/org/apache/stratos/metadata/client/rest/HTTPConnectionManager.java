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

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HTTPConnectionManager {

    private static final int MAX_TOTAL_CONNECTIONS = 100;
    private static final int DEFAULT_MAX_PER_ROUTE = 20;

    private HTTPConnectionManager() {
    }

    public static HTTPConnectionManager getInstance() {
        return InstanceHolder.instance;
    }

    public HttpClientConnectionManager getHttpConnectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // TODO: Introduce configurable variable for Max total and max per router variables.
        cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        //HttpHost localhost = new HttpHost("localhost", 80);
        //cm.setMaxPerRoute(new HttpRoute(localhost), 50);
        return cm;
    }

    private static class InstanceHolder {
        public static HTTPConnectionManager instance = new HTTPConnectionManager();
    }

}
