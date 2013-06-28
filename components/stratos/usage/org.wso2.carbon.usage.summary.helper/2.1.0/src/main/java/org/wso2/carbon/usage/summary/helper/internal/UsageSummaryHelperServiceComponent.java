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

package org.wso2.carbon.usage.summary.helper.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.analytics.hive.service.HiveExecutorService;
import org.wso2.carbon.ndatasource.core.DataSourceService;
import org.wso2.carbon.usage.summary.helper.util.DataHolder;

import javax.sql.DataSource;

/**
 *
 * @scr.component name="org.wso2.carbon.usage.summary" immediate="true"
 * @scr.reference name="hive.executor.service"
 * interface="org.wso2.carbon.analytics.hive.service.HiveExecutorService" cardinality="1..1"
 * policy="dynamic" bind="setHiveExecutorService" unbind="unsetHiveExecutorService"
 * @scr.reference name="datasources.service"
 * interface="org.wso2.carbon.ndatasource.core.DataSourceService"
 * cardinality="1..1" policy="dynamic"
 * bind="setDataSourceService" unbind="unsetDataSourceService"
 */
public class UsageSummaryHelperServiceComponent {
    
    private static Log log = LogFactory.getLog(UsageSummaryHelperServiceComponent.class);

    protected void activate(ComponentContext context){

        log.info("Stratos usage summary helper bundle started");
        /*try{

        }catch (Throwable t){
            log.error("Error occurred while activating the usage summary helper bundle..", t);
        }*/
    }

    protected void deactivate(){
        log.debug("Usage summary helper bundle was deactivated..");
    }

    protected void setHiveExecutorService(HiveExecutorService executorService){
        //DataHolder.setExecutorService(executorService);
    }

    protected void unsetHiveExecutorService(HiveExecutorService executorService){
        //DataHolder.setExecutorService(null);
    }
    
    protected void setDataSourceService(DataSourceService dataSourceService){
        DataHolder.setDataSourceService(dataSourceService);
        try {
            DataHolder.setDataSource((DataSource)dataSourceService.getDataSource(DataHolder.BILLING_DATA_SOURCE_NAME).getDSObject());
            log.info("Data source set to data holder");
        } catch (Exception e) {
            log.error("Error occurred while retrieving the data source: " + DataHolder.BILLING_DATA_SOURCE_NAME, e); //To change body of catch statement use File | Settings | File Templates.
        }
    }

    protected void unsetDataSourceService(DataSourceService dataSourceService){
        DataHolder.setDataSourceService(null);
    }


}
