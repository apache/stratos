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

package org.apache.stratos.adc.mgt.instance.cache;

public class CartridgeInstanceCacheKey {

    private int tenantId;
    private String cartridgeAlias;

    public CartridgeInstanceCacheKey (int tenantId, String cartridgeAlias) {

        this.tenantId = tenantId;
        this.cartridgeAlias = cartridgeAlias;
    }

    public int getTenantId() {
        return tenantId;
    }

    public String getCartridgeInstanceAlias() {
        return cartridgeAlias;
    }

    public boolean equals (Object object) {

        if(object == this) {
            return true;
        }
        else if (object == null || !(object instanceof CartridgeInstanceCacheKey)) {
            return false;
        }

        CartridgeInstanceCacheKey subscriptionKey = (CartridgeInstanceCacheKey)object;
        return this.getCartridgeInstanceAlias().equals(subscriptionKey.getCartridgeInstanceAlias()) &&
                this.getTenantId() == subscriptionKey.getTenantId();
    }

    public int hashCode () {

        return getCartridgeInstanceAlias().hashCode() + Integer.toString(getTenantId()).hashCode();
    }
}
