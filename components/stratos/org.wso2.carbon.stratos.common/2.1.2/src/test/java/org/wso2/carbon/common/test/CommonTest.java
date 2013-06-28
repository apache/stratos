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
package org.wso2.carbon.stratos.common.test;

import junit.framework.TestCase;
import org.wso2.carbon.stratos.common.util.CommonUtil;

public class CommonTest extends TestCase {
    public void testEmailValidation() throws Exception {
        try {
            CommonUtil.validateEmail("damn@right.com");
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }

        try {
            CommonUtil.validateEmail("damn@right].com");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("Wrong characters in the email.", e.getMessage());
        }

        try {
            CommonUtil.validateEmail("damn@right@wrong.com");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
            assertEquals("Invalid email address is provided.", e.getMessage());
        }
    }
}
