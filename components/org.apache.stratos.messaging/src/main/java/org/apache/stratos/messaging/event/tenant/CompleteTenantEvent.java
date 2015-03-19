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

package org.apache.stratos.messaging.event.tenant;

import org.apache.stratos.messaging.domain.tenant.Tenant;

import java.util.List;

/**
 * This event is fired periodically with all the available tenants. It would be a
 * starting point for subscribers to initialize the list of tenants before receiving
 * other tenant events.
 */
public class CompleteTenantEvent extends TenantEvent {

    private List<Tenant> tenants;

    public CompleteTenantEvent(List<Tenant> tenants) {
        this.tenants = tenants;
    }

    public List<Tenant> getTenants() {
        return tenants;
    }
}
