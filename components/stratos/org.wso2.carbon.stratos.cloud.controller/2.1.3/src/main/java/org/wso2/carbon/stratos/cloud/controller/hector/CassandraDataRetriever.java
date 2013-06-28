/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.stratos.cloud.controller.hector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;

import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;

public class CassandraDataRetriever {

    private static final Log log = LogFactory.getLog(CassandraDataRetriever.class);
    private static FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
    private final static StringSerializer se = StringSerializer.get();
    private static Cluster cluster;
    private static Keyspace keyspace;
    private static boolean isInit;
    
    public static void init() {

        if(isInit){
            return;
        }
        getCassandraKeyspace();
        indexCounterColumn("payload_status");
        isInit = true;
    }
    
    public static void connect() {
        
        if(keyspace == null){
            handleException("Cannot find the key space.");
        }
        
        String colFamily = CloudControllerConstants.CLOUD_CONTROLLER_COL_FAMILY;
        
        CqlQuery<String,String,String> cqlQuery = new CqlQuery<String,String,String>(keyspace, se, se, se);
        cqlQuery.setQuery("select payload_nodeId from "+colFamily+" where payload_status='RUNNING'");
//        cqlQuery.setQuery("select * from "+colFamily+" where payload_domain='nirmal'");
        QueryResult<CqlRows<String,String,String>> result = cqlQuery.execute();
        
        if (result != null && result.get() != null) {
            List<Row<String, String, String>> list = result.get().getList();
            for (Row<?, ?, ?> row : list) {
                System.out.println(".");
                List<?> columns = row.getColumnSlice().getColumns();
                for (Iterator<?> iterator = columns.iterator(); iterator.hasNext();) {
                    HColumn<?, ?> column = (HColumn<?, ?>) iterator.next();
                    System.out.print(column.getName() + ":" + column.getValue()
                            + "\t");
                }
                System.out.println("");
            }
        }
        
//        ColumnQuery<String, String, String> columnQuery =
//                HFactory.createStringColumnQuery(keyspace);
//        KeyIterator<String> keyIterator = new KeyIterator<String>(keyspace, colFamily, StringSerializer.get());
//        for ( String key : keyIterator ) {
//            
//            columnQuery.setColumnFamily(colFamily).setKey(key).setName("payload_nodeId");
//            QueryResult<HColumn<String, String>> result = columnQuery.execute();
//            HColumn<String, String> hColumn = result.get();
//            System.out.println("Column: " + hColumn.getName() + " Value : " + hColumn.getValue() + "\n");
//        }
        
//        //Read Data
//        for (String key : keyList) {
//            System.out.println("\nretrieving Key " + rowKey + "From Column Family " + columnFamily + "\n");
//            for (String columnName : columnList.split(":")) {
//                //sout data
//            }
//        }
        
        
    }
    
    private static void indexCounterColumn(String idxColumnName) {

        KeyspaceDefinition keyspaceDefinition = cluster.describeKeyspace(CloudControllerConstants.DEFAULT_CASSANDRA_KEY_SPACE);

        List<ColumnFamilyDefinition> cdfs = keyspaceDefinition.getCfDefs();
        ColumnFamilyDefinition cfd = null;
        for (ColumnFamilyDefinition c : cdfs) {
            if (c.getName().equals(CloudControllerConstants.CLOUD_CONTROLLER_COL_FAMILY)) {
                System.out.println(c.getName());
                cfd = c;
                break;
            }
        }

        BasicColumnFamilyDefinition columnFamilyDefinition = new BasicColumnFamilyDefinition(cfd);

        BasicColumnDefinition bcdf = new BasicColumnDefinition();
        bcdf.setName(StringSerializer.get().toByteBuffer(idxColumnName));
        bcdf.setIndexName(idxColumnName + "index");
        bcdf.setIndexType(ColumnIndexType.KEYS);
        bcdf.setValidationClass(ComparatorType.UTF8TYPE.getClassName());

        columnFamilyDefinition.addColumnDefinition(bcdf);
        cluster.updateColumnFamily(new ThriftCfDef(columnFamilyDefinition));

    } 
    
    private static void getCassandraKeyspace() {
        if (cluster == null) {
            Map<String, String> credentials = new HashMap<String, String>();
            credentials.put("username", dataHolder.getCassandraUser());
            credentials.put("password", dataHolder.getCassandraPassword());

            cluster =
                      retrieveCassandraCluster(CloudControllerConstants.DEFAULT_CASSANDRA_CLUSTER_NAME,
                                               dataHolder.getCassandraConnUrl(), credentials);

            keyspace =
                       HFactory.createKeyspace(CloudControllerConstants.DEFAULT_CASSANDRA_KEY_SPACE,
                                               cluster);
        }
        
    }
    
    private static Cluster retrieveCassandraCluster(String clusterName, String connectionUrl,
        Map<String, String> credentials) {

        CassandraHostConfigurator hostConfigurator = new CassandraHostConfigurator(connectionUrl);
        hostConfigurator.setRetryDownedHosts(false);
        Cluster cluster = HFactory.createCluster(clusterName, hostConfigurator, credentials);
        return cluster;
    }

    private static void handleException(String msg) {

        log.error(msg);
        throw new CloudControllerException(msg);
    }
    
//    private void handleException(String msg, Exception e) {
//
//        log.error(msg, e);
//        throw new AutoscalerServiceException(msg, e);
//    }
}
