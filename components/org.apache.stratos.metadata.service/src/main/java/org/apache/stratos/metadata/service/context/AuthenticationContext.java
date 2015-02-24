package org.apache.stratos.metadata.service.context;

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

public class AuthenticationContext {
    // maintaining the authenticated state in threadLocal. We want to skip
    // subsequent authentication handlers
    // once a request get authenticated by a handler.
    private static final ThreadLocal<Boolean> authenticated = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static boolean isAthenticated() {
        return authenticated.get();
    }

    public static void setAuthenticated(boolean isAuthenticated) {
        authenticated.set(isAuthenticated);
    }
}
