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
package org.apache.stratos.rest.endpoint.mock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dto.Cartridge;
import org.apache.stratos.adc.mgt.dto.SubscriptionInfo;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import javax.ws.rs.*;

@Path("/admin/")
public class StratosTestAdmin {
    private static Log log = LogFactory.getLog(StratosTestAdmin.class);



    @POST
    @Path("/cartridge/definition/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deployCartridgeDefinition (CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {
           log.info("Cartridge definition : " + cartridgeDefinitionBean.toString());
           MockContext.getInstance().addCartirdgeDefinition(cartridgeDefinitionBean);
    }


    @GET
    @Path("/cartridge/tenanted/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableMultiTenantCartridges() throws ADCException {
          return MockContext.getInstance().getAvailableMultiTenantCartridges();
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableSingleTenantCartridges() throws ADCException {
         return MockContext.getInstance().getAvailableSingleTenantCartridges();
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getSubscribedCartridges() throws ADCException {
         return MockContext.getInstance().getSubscribedCartridges();
    }

    @POST
    @Path("/cartridge/subscribe")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public SubscriptionInfo subscribe(CartridgeInfoBean cartridgeInfoBean) {
          return MockContext.getInstance().subscribeToCartridge(cartridgeInfoBean);
    }





    @POST
    @Path("/cartridge/unsubscribe")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public void unsubscribe(String alias){
          MockContext.getInstance().unsubscribe(alias);
    }
    /*
    @POST
    @Path("/tenant")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public String addTenant(TenantInfoBean tenantInfoBean) throws Exception {

    }


    @PUT
    @Path("/tenant")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void updateTenant(TenantInfoBean tenantInfoBean) throws Exception {

    }

    @GET
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean getTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {

    }

    @DELETE
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deleteTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {

    }


    @GET
    @Path("/tenant/list")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrieveTenants() throws Exception {

    }

    @GET
    @Path("tenant/search/{domain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrievePartialSearchTenants(@PathParam("domain")String domain) throws Exception {

    }


    @POST
    @Path("tenant/activate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void activateTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {


    }

    @POST
    @Path("tenant/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deactivateTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {

    }*/

}
