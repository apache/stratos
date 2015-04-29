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

package org.apache.stratos.cloud.controller.exception;

/**
 * Exception class for handling invalid Kubernetes cluster
 */
public class KubernetesClusterAlreadyExistsException extends Exception {

    private String message;

    public KubernetesClusterAlreadyExistsException(String message, Exception exception) {
        super(message, exception);
        this.message = message;
    }

    public KubernetesClusterAlreadyExistsException(Exception exception) {
        super(exception);
    }

    public KubernetesClusterAlreadyExistsException(String msg) {
        super(msg);
        this.message = msg;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
