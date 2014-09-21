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

package org.apache.stratos.metadata.client.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadata.client.DefaultMetaDataServiceClient;
import org.apache.stratos.metadata.client.MetaDataServiceClient;
import org.apache.stratos.metadata.client.config.MetaDataClientConfig;
import org.apache.stratos.metadata.client.data.extractor.DataExtractor;
import org.apache.stratos.metadata.client.exception.DataExtractorException;
import org.apache.stratos.metadata.client.factory.MetaDataExtractorFactory;
import org.apache.stratos.metadata.client.pojo.DataContext;

import java.util.Collection;

public class MetaDataServiceClientSample {

    private static final Log log = LogFactory.getLog(MetaDataServiceClientSample.class);

    private MetaDataServiceClient metaDataServiceClient;

    private DataExtractor dataExtractor;

    private MetaDataClientConfig metaDataClientConfig;


    public MetaDataServiceClientSample() {
        initialize();
    }

    private void initialize() {

        metaDataClientConfig = MetaDataClientConfig.getInstance();
        metaDataServiceClient = new DefaultMetaDataServiceClient(metaDataClientConfig.
                getMetaDataServiceBaseUrl());
        metaDataServiceClient.initialize();

        if (MetaDataClientConfig.getInstance().getDataExtractorClass() != null) {
            dataExtractor = MetaDataExtractorFactory.getMetaDataServiceClient(metaDataClientConfig.
                getDataExtractorClass());
            dataExtractor.initialize();
        }
    }

    public Collection<DataContext> getAllData (Object someObj) {

        Collection<DataContext> dataContexts = null;

        try {
            dataContexts = dataExtractor.getAllData(someObj);

        } catch (DataExtractorException e) {
            log.error("Unable to get extracted data", e);
        }

        return dataContexts;
    }

    public DataContext getData (Object someObj) {

        DataContext dataContext = null;

        try {
            dataContext = dataExtractor.getData(someObj);

        } catch (DataExtractorException e) {
            log.error("Unable to get extracted data", e);
        }

        return dataContext;
    }

}
