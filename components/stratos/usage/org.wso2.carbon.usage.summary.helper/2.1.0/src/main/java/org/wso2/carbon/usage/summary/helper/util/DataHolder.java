/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.usage.summary.helper.util;

import org.wso2.carbon.ndatasource.core.DataSourceService;

import javax.sql.DataSource;

public class DataHolder {

    private static DataSourceService dataSourceService;

    private static DataSource dataSource;

    public static final String BILLING_DATA_SOURCE_NAME = "WSO2BillingDS";
    //public static final String BILLING_DATA_SOURCE_NAME = "WSO2USAGE_DS";


    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void setDataSource(DataSource dataSource) {
        DataHolder.dataSource = dataSource;
    }

    public static DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    public static void setDataSourceService(DataSourceService dataSourceService) {
        DataHolder.dataSourceService = dataSourceService;
    }
}
