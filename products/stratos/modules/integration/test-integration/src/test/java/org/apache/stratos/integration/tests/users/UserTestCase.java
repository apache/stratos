/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.integration.tests.users;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.integration.common.RestConstants;
import org.apache.stratos.integration.tests.StratosIntegrationTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Handling users
 */
@Test(groups = { "users" })
public class UserTestCase extends StratosIntegrationTest {
    private static final Log log = LogFactory.getLog(UserTestCase.class);
    private static final String RESOURCES_PATH = "/user-test";

    @Test(timeOut = DEFAULT_TEST_TIMEOUT)
    public void addUser() throws Exception {
        String userId = "user-1";
        boolean addedUser1 = restClient.addEntity(RESOURCES_PATH + "/" +
                        userId + ".json",
                RestConstants.USERS, RestConstants.USERS_NAME);
        assertTrue(addedUser1);

        Type listType = new TypeToken<ArrayList<UserInfoBean>>() {
        }.getType();

        List<UserInfoBean> userInfoBeanList = (List<UserInfoBean>) restClient.listEntity(RestConstants.USERS,
                listType, RestConstants.USERS_NAME);

        UserInfoBean bean1 = null;
        for (UserInfoBean userInfoBean : userInfoBeanList) {
            if (userInfoBean.getUserName().equals(userId)) {
                bean1 = userInfoBean;
            }
        }
        assertNotNull(bean1);
            /*assertEquals(bean1.getEmail(), "foo@bar.com");
            assertEquals(bean1.getFirstName(), "Frank");
            assertEquals(bean1.getRole(), "admin");
            assertEquals(bean1.getLastName(), "Myers");
            assertEquals(bean1.getCredential(), "kim12345");*/

        boolean updatedUser1 = restClient.updateEntity(RESOURCES_PATH + "/" +
                        userId + "-v1.json",
                RestConstants.USERS, RestConstants.USERS_NAME);
        assertTrue(updatedUser1);

        userInfoBeanList = (List<UserInfoBean>) restClient.listEntity(RestConstants.USERS,
                listType, RestConstants.USERS_NAME);

        for (UserInfoBean userInfoBean : userInfoBeanList) {
            if (userInfoBean.getUserName().equals(userId)) {
                bean1 = userInfoBean;
            }
        }
        assertNotNull(bean1);
            /*assertEquals(bean1.getEmail(), "user-1@bar.com");
            assertEquals(bean1.getFirstName(), "Frankn");
            assertEquals(bean1.getRole(), "admin");
            assertEquals(bean1.getLastName(), "Myersn");
            assertEquals(bean1.getCredential(), "kim123456");*/

        boolean removedUser1 = restClient.removeEntity(RestConstants.USERS,
                userId, RestConstants.USERS_NAME);
        assertTrue(removedUser1);

        userInfoBeanList = (List<UserInfoBean>) restClient.listEntity(RestConstants.USERS,
                listType, RestConstants.USERS_NAME);

        bean1 = null;
        for (UserInfoBean userInfoBean : userInfoBeanList) {
            if (userInfoBean.getUserName().equals(userId)) {
                bean1 = userInfoBean;
            }
        }
        assertNull(bean1);
    }
}
