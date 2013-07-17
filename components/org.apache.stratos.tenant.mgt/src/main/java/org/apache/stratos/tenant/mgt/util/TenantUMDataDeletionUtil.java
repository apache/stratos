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
package org.apache.stratos.tenant.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TenantUMDataDeletionUtil {
    public static final Log log = LogFactory.getLog(TenantUMDataDeletionUtil.class);

    /**
     * Delete all tenant information related to tenant stored in UM tables
     * @param tenantId id of tenant whose data should be deleted
     * @param conn database connection object
     * @throws SQLException thrown if an error occurs while executing the queries
     */
    public static void deleteTenantUMData(int tenantId, Connection conn) throws Exception {
        try {
            conn.setAutoCommit(false);
            String deleteUserPermissionSql = "DELETE FROM UM_USER_PERMISSION WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteUserPermissionSql, tenantId);

            String deleteRolePermissionSql = "DELETE FROM UM_ROLE_PERMISSION WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteRolePermissionSql, tenantId);

            String deletePermissionSql = "DELETE FROM UM_PERMISSION WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deletePermissionSql, tenantId);

            String deleteClaimBehaviourSql = "DELETE FROM UM_CLAIM_BEHAVIOR WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteClaimBehaviourSql, tenantId);

            String deleteProfileConfigSql = "DELETE FROM UM_PROFILE_CONFIG WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteProfileConfigSql, tenantId);

            String deleteClaimSql = "DELETE FROM UM_CLAIM WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteClaimSql, tenantId);

            String deleteDialectSql = "DELETE FROM UM_DIALECT WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteDialectSql, tenantId);

            String deleteUserAttributeSql = "DELETE FROM UM_USER_ATTRIBUTE WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteUserAttributeSql, tenantId);

            String deleteHybridUserRoleSql = "DELETE FROM UM_HYBRID_USER_ROLE WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteHybridUserRoleSql, tenantId);

            String deleteHybridRoleSql = "DELETE FROM UM_HYBRID_ROLE WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteHybridRoleSql, tenantId);

            String deleteHybridRememberMeSql = "DELETE FROM UM_HYBRID_REMEMBER_ME WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteHybridRememberMeSql, tenantId);

            String deleteUserRoleSql = "DELETE FROM UM_USER_ROLE WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteUserRoleSql, tenantId);

            String deleteRoleSql = "DELETE FROM UM_ROLE WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteRoleSql, tenantId);

            String deleteUserSql = "DELETE FROM UM_USER WHERE UM_TENANT_ID = ?";
            executeDeleteQuery(conn, deleteUserSql, tenantId);

            String deleteTenantSql = "DELETE FROM UM_TENANT WHERE UM_ID = ?";
            executeDeleteQuery(conn, deleteTenantSql, tenantId);

            conn.commit();
        } catch (Exception e) {
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