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
package org.apache.stratos.metadataservice.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.Principal;

/**
 * {@link StratosSecurityContext} make use of principal instance. Here with Stratos
 * authentication/authorization framework we only need username as the principal details
 */
public class StratosPrincipal implements Principal {
    private Log log = LogFactory.getLog(StratosPrincipal.class);
    private String userName;

    public StratosPrincipal(String userName) {
        this.userName = userName;
    }

    public boolean equals(Object another) {
      return userName.equals((another));
    }

    public String toString() {
        return userName.toString();
    }

    public int hashCode() {
        return userName.hashCode();
    }

    public String getName() {
        return userName;
    }
}
