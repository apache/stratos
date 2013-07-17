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
package org.apache.stratos.validate.domain.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.validate.domain.util.Util;

/**
 * The service allow users to validate domains before sign in.
 */
public class ValidateDomainService {
    private static final Log log = LogFactory.getLog(ValidateDomainService.class);

    /**
     * get domain validation key
     *
     * @param domain - tenant domain
     * @return domain validation key
     * @throws Exception, if getting the validation key failed.
     */
    public String getDomainValidationKey(String domain) throws Exception {
        // for the non admin case, we are not issuing validation keys if the domain already exists.

        // Now we register first, before coming to the domain validation.
//        if (!Util.checkDomainAvailability(domain)) {
//            String msg = "Domain " + domain + " is already taken.";
//            log.info(msg);
//            return "unavailable";
//        }
        return Util.getDomainValidationKeyWithoutLogin(domain);
    }

    /**
     * Validate by DNS Entry
     *
     * @param domain - tenant domain
     * @throws Exception, if validation failed.
     * @return, successkey
     */
    public String validateByDNSEntry(String domain) throws Exception {
//        if (!Util.checkDomainAvailability(domain)) {
//            String msg = "Domain " + domain + " is already taken.";
//            log.info(msg);
//            return "unavailable";
//        }
        return Util.validateByDNSEntry(domain);
    }

    /**
     * Validate by Text in root
     *
     * @param domain, tenant domain
     * @throws Exception, if validation failed.
     * @return, successkey
     */
    public String validateByTextInRoot(String domain) throws Exception {
        if (!Util.checkDomainAvailability(domain)) {
            String msg = "Domain " + domain + " is already taken.";
            log.info(msg);
            return "unavailable";
        }
        return Util.validateByTextInRoot(domain);
    }
}


