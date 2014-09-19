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

package org.apache.stratos.metadata.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadata.client.data.extractor.DataExtractor;
import org.apache.stratos.metadata.client.data.extractor.DefaultDataExtractor;
import org.apache.stratos.metadata.client.exception.DataExtractorException;
import org.apache.stratos.metadata.client.exception.MetaDataServiceClientExeption;
import org.apache.stratos.metadata.client.pojo.DataContext;

import java.util.Collection;

public class MetaDataServiceClientManager {

    private static final Log log = LogFactory.getLog(MetaDataServiceClientManager.class);

    private MetaDataServiceClient metaDataServiceClient;

    private DataExtractor dataExtractor;

    private String baseUrl;

    private String customDataExtractorClassName;


    public MetaDataServiceClientManager () {

        readConfigurations();
        init();
        this.metaDataServiceClient = new DefaultMetaDataServiceClient(baseUrl);
    }

    private void readConfigurations () {
        //TODO: read configurations
    }

    private void init () {

        metaDataServiceClient.initialize();
        //TODO:  load the relevant customized class
        // currently only the default DataExtractor is used
        dataExtractor = new DefaultDataExtractor();
    }

    public void addExtractedData () {

        Collection<DataContext> dataContexts = null;

        try {
            dataContexts = dataExtractor.getData();

        } catch (DataExtractorException e) {
            log.error("Unable to get extracted data", e);
        }

        for (DataContext dataContext : dataContexts) {
            if (dataContext.getPropertyValues() != null) {
                for (String propertyValue : dataContext.getPropertyValues()) {
                    try {
                        metaDataServiceClient.addProperty(dataContext.getAppId(), dataContext.getClusterId(),
                                dataContext.getPropertyKey(), propertyValue);

                    } catch (MetaDataServiceClientExeption e) {
                        log.error("Unable to add extracted data meta data service", e);
                    }
                }
            }
        }
    }

    public MetaDataServiceClient getMetaDataServiceClient() {
        return metaDataServiceClient;
    }
}
