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
package org.wso2.carbon.validate.domain.services;

import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.validate.domain.util.Util;

/**
 * The service allow users to validate domains before sign in.
 */
public class ValidateDomainAdminService extends AbstractAdmin {

    public String getDomainValidationKey(String domain) throws Exception {
        return Util.getDomainValidationKeyWithLogin(domain);
    }

    public String validateByDNSEntry(String domain)  throws Exception {
        return Util.validateByDNSEntry(domain);
    }

    public String validateByTextInRoot(String domain) throws Exception {
        return Util.validateByTextInRoot(domain);
    }
}