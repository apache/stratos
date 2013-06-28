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

package org.wso2.carbon.tenant.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TenantRegistryDataDeletionUtil {
    public static final Log log = LogFactory.getLog(TenantRegistryDataDeletionUtil.class);
    
    /**
     * Delete all tenant information related to tenant stored in REG tables
     * @param tenantId id of tenant whose data should be deleted
     * @param conn database connection object
     * @throws SQLException thrown if an error occurs while executing the queries 
     */
    public static void deleteTenantRegistryData(int tenantId, Connection conn) throws Exception {
        try {
            conn.setAutoCommit(false);
            String deleteClusterLockSql = "DELETE FROM REG_CLUSTER_LOCK WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteClusterLockSql, tenantId);

            String deleteLogSql = "DELETE FROM REG_LOG WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteLogSql, tenantId);

            String deleteAssociationSql = "DELETE FROM REG_ASSOCIATION WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteAssociationSql, tenantId);

            String deleteSnapshotSql = "DELETE FROM REG_SNAPSHOT WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteSnapshotSql, tenantId);

            String deleteResourceCommentSql = "DELETE FROM REG_RESOURCE_COMMENT WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteResourceCommentSql, tenantId);

            String deleteCommentSql = "DELETE FROM REG_COMMENT WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteCommentSql, tenantId);

            String deleteResourceRatingSql = "DELETE FROM REG_RESOURCE_RATING WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteResourceRatingSql, tenantId);

            String deleteRatingSql = "DELETE FROM REG_RATING WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteRatingSql, tenantId);

            String deleteResourceTagSql = "DELETE FROM REG_RESOURCE_TAG WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteResourceTagSql, tenantId);

            String deleteTagSql = "DELETE FROM REG_TAG WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteTagSql, tenantId);

            String deleteResourcePropertySql = "DELETE FROM REG_RESOURCE_PROPERTY WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteResourcePropertySql, tenantId);

            String deletePropertySql = "DELETE FROM REG_PROPERTY WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deletePropertySql, tenantId);

            String deleteResourceHistorySql = "DELETE FROM REG_RESOURCE_HISTORY WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteResourceHistorySql, tenantId);

            String deleteContentHistorySql = "DELETE FROM REG_CONTENT_HISTORY WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteContentHistorySql, tenantId);

            String deleteResourceSql = "DELETE FROM REG_RESOURCE WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteResourceSql, tenantId);

            String deleteContentSql = "DELETE FROM REG_CONTENT WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteContentSql, tenantId);

            String deletePathSql = "DELETE FROM REG_PATH WHERE REG_TENANT_ID = ?";
            executeDeleteQuery(conn, deletePathSql, tenantId);

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            String errorMsg = "An error occurred while deleting registry data for tenant: " + tenantId;
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        } finally {
            conn.close();
        }
    }

    private static void executeDeleteQuery(Connection conn, String query, int tenantId)
            throws Exception {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(query);
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errMsg = "Error executing query " + query + " for tenant: " + tenantId;
            log.error(errMsg, e);
            throw new Exception(errMsg, e);
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }
}
