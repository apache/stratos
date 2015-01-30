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

package org.apache.stratos.autoscaler.applications.parser;


import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.common.Properties;
import org.apache.stratos.messaging.domain.application.Application;

import java.util.List;
import java.util.Map;

/**
 * Parses the Application Definition
 */
public interface ApplicationParser {

    /**
     * Parses the Application Definition
     *
     * @param applicationContext Application context definition
     * @return Application structure denoting the parsed Application
     * @throws ApplicationDefinitionException If the Application Definition is invalid
     */
    public Application parse (ApplicationContext applicationContext) throws ApplicationDefinitionException;

    /**
     * Returns a set of ApplicationClusterContext which will comprise of cluster related information
     * extracted from the Application definition
     *
     * @return  Set of ApplicationClusterContext objects
     * @throws ApplicationDefinitionException if any error occurs
     */
    public List<ApplicationClusterContext> getApplicationClusterContexts() throws ApplicationDefinitionException;

    public Map<String, Properties> getAliasToProperties();
}
