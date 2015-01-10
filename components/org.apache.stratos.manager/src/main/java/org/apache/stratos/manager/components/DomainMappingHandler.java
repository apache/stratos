/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.components;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.exception.DomainMappingException;
import org.apache.stratos.manager.messaging.publisher.DomainMappingEventPublisher;
import org.apache.stratos.messaging.domain.domain.mapping.DomainMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Domain mapping handler.
 */
public class DomainMappingHandler {

    private static final Log log = LogFactory.getLog(DomainMappingHandler.class);

    private ApplicationSignUpHandler applicationSignUpHandler;

    public DomainMappingHandler() {
        applicationSignUpHandler = new ApplicationSignUpHandler();
    }

    public void addDomainMapping(DomainMapping domainMapping) throws DomainMappingException {
        try {
            String applicationId = domainMapping.getApplicationId();
            int tenantId = domainMapping.getTenantId();
            String domainName = domainMapping.getDomainName();

            // Check domain name availability
            checkDomainNameAvailability(domainName);

            // Add domain name to application signup
            ApplicationSignUp applicationSignUp = applicationSignUpHandler.getApplicationSignUp(applicationId, tenantId);
            DomainMapping[] domainMappingArray = applicationSignUp.getDomainMappings();
            if(domainMappingArray == null) {
                domainMappingArray = new DomainMapping[1];
                domainMappingArray[0] = domainMapping;
            } else {
                List<DomainMapping> domainMappingList = convertDomainMappingArrayToList(domainMappingArray);
                domainMappingList.add(domainMapping);
                domainMappingArray = domainMappingList.toArray(new DomainMapping[domainMappingList.size()]);
            }

            applicationSignUp.setDomainMappings(domainMappingArray);
            applicationSignUpHandler.updateApplicationSignUp(applicationSignUp);

            DomainMappingEventPublisher.publishDomainMappingAddedEvent(applicationId, tenantId,
                    domainMapping.getServiceName(), domainMapping.getClusterId(),
                    domainMapping.getDomainName(), domainMapping.getContextPath());

            if(log.isInfoEnabled()) {
                log.info(String.format("Domain mapping added successfully: [application-id] %s [tenant-id] %d " +
                        "[domain-name] %s", applicationId, tenantId, domainName));
            }
        } catch (Exception e) {
            String message = "Could not add domain mapping";
            log.error(message, e);
            throw new DomainMappingException(message, e);
        }
    }

    private List<DomainMapping> convertDomainMappingArrayToList(DomainMapping[] domainMappingArray) {
        List<DomainMapping> domainMappingList = new ArrayList<DomainMapping>();
        for(DomainMapping arrayItem : domainMappingArray) {
            if(arrayItem != null) {
                domainMappingList.add(arrayItem);
            }
        }
        return domainMappingList;
    }

    private void checkDomainNameAvailability(String domainName) throws DomainMappingException {
        if(domainNameExist(domainName)) {
            throw new RuntimeException(String.format("Domain name is already mapped: [domain-name] %s", domainName));
        }
    }

    public boolean domainNameExist(String domainName) throws DomainMappingException {
        try {
            List<ApplicationSignUp> applicationSignUps = applicationSignUpHandler.getApplicationSignUps();
            if(applicationSignUps != null) {
                for(ApplicationSignUp applicationSignUp : applicationSignUps) {
                    if(applicationSignUp.getDomainMappings() != null) {
                        for(DomainMapping domainMapping : applicationSignUp.getDomainMappings()) {
                            if((domainMapping != null) && (domainMapping.getDomainName().equals(domainName))) {
                                 return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            String message = String.format("Could not check domain name existence: [domain-name] %s", domainName);
            log.error(message, e);
            throw new DomainMappingException(message, e);
        }
        return false;
    }

    public DomainMapping[] getDomainMappings(String applicationId, int tenantId) throws DomainMappingException {
        try {
            ApplicationSignUp applicationSignUp = applicationSignUpHandler.getApplicationSignUp(applicationId, tenantId);
            if(applicationSignUp != null) {
                return applicationSignUp.getDomainMappings();
            }
            return null;
        } catch (Exception e) {
            String message = String.format("Could not get domain mappings: [application-id] %s [tenant-id] %d",
                    applicationId, tenantId);
            log.error(message, e);
            throw new DomainMappingException(message, e);
        }
    }

    public void removeDomainMapping(String applicationId, int tenantId, String domainName) throws DomainMappingException {
        try {
            // Add domain name to application signup
            ApplicationSignUp applicationSignUp = applicationSignUpHandler.getApplicationSignUp(applicationId, tenantId);
            DomainMapping[] domainMappingArray = applicationSignUp.getDomainMappings();
            List<DomainMapping> domainMappingList = convertDomainMappingArrayToList(domainMappingArray);

            Iterator<DomainMapping> iterator = domainMappingList.iterator();
            while(iterator.hasNext()) {
                DomainMapping domainMapping = iterator.next();
                if(domainMapping.getDomainName().equals(domainName)) {
                    iterator.remove();

                    domainMappingArray = domainMappingList.toArray(new DomainMapping[domainMappingList.size()]);
                    applicationSignUp.setDomainMappings(domainMappingArray);
                    applicationSignUpHandler.updateApplicationSignUp(applicationSignUp);

                    DomainMappingEventPublisher.publishDomainNameRemovedEvent(applicationId, tenantId,
                            domainMapping.getServiceName(), domainMapping.getClusterId(), domainName);

                    if(log.isInfoEnabled()) {
                        String.format("Domain mapping removed: [application-id] %s [tenant-id] %d " +
                                "[domain-name] %s", applicationId, tenantId, domainName);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            String message = String.format("Could not remove domain mappings: [application-id] %s [tenant-id] %d " +
                            "[domain-name] %s", applicationId, tenantId, domainName);
            log.error(message, e);
            throw new DomainMappingException(message, e);
        }
    }
}
