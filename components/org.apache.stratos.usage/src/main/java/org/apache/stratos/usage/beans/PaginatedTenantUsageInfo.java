/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.apache.stratos.usage.beans;

import java.util.Arrays;

public class PaginatedTenantUsageInfo {
    private TenantUsage[] tenantUsages;
    private int pageNumber;
    private int numberOfPages;
    
    public TenantUsage[] getTenantUsages() {
        return Arrays.copyOf(tenantUsages, tenantUsages.length);
    }
    public void setTenantUsages(TenantUsage[] tenantUsages) {
        this.tenantUsages = Arrays.copyOf(tenantUsages, tenantUsages.length);
    }
    public int getPageNumber() {
        return pageNumber;
    }
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
    public int getNumberOfPages() {
        return numberOfPages;
    }
    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }
    
}
