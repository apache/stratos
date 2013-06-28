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

package org.wso2.stratos.manager.integration.tests;

import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.integration.framework.utils.FrameworkSettings;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TenantMgtTestCase {

    private static String TenantMgtAdminServiceURL;
    private static String TestTenantDomain;
    public static final Log log = LogFactory.getLog(TenantMgtTestCase.class);

    @BeforeClass
    public void init() {
        log.info("****************************** TenantMgtTestCase Start ******************************");
        TestTenantDomain = "testcase.org";
        TenantMgtAdminServiceURL = "https://" + FrameworkSettings.HOST_NAME + ":" +
                                 FrameworkSettings.HTTPS_PORT + "/services/TenantMgtAdminService";
    }

    @Test(groups = {"stratos.manager"})
    public void addTenantTest() throws Exception {
        Calendar calender = new GregorianCalendar();
        calender.setTime(new Date());

        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        tenantInfoBean.setActive(true);
        tenantInfoBean.setEmail("manager-test@wso2.com");
        tenantInfoBean.setAdmin("admin");
        tenantInfoBean.setAdminPassword("admin123");
        tenantInfoBean.setTenantDomain(TestTenantDomain);
        tenantInfoBean.setCreatedDate(calender);
        tenantInfoBean.setFirstname("Fname");
        tenantInfoBean.setLastname("Lname");
        tenantInfoBean.setSuccessKey("true");
        tenantInfoBean.setUsagePlan("Demo");

        TenantMgtAdminServiceStub stub = new TenantMgtAdminServiceStub(TenantMgtAdminServiceURL);
        ServiceClient client = stub._getServiceClient();
        CarbonUtils.setBasicAccessSecurityHeaders(FrameworkSettings.USER_NAME, FrameworkSettings.PASSWORD, client);

        String result = stub.addTenant(tenantInfoBean);
        Assert.assertTrue(stub.retrieveTenants()[0].getTenantDomain().equals(TestTenantDomain));

    }

    @Test(groups = {"stratos.manager"})
    public void activateTenantTest() throws Exception {
        TenantMgtAdminServiceStub stub = new TenantMgtAdminServiceStub(TenantMgtAdminServiceURL);
        ServiceClient client = stub._getServiceClient();
        CarbonUtils.setBasicAccessSecurityHeaders(FrameworkSettings.USER_NAME, FrameworkSettings.PASSWORD, client);
        stub.activateTenant(TestTenantDomain);
        Assert.assertTrue(stub.retrieveTenants()[0].getActive());
    }

    @AfterClass
    public void end(){
        log.info("****************************** TenantMgtTestCase End ******************************");
    }

}
