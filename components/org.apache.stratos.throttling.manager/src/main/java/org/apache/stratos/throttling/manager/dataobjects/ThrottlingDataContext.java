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
package org.apache.stratos.throttling.manager.dataobjects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ThrottlingDataContext {
    int tenantId;
    String userName;
    Map<String, ThrottlingDataEntry> data;
    boolean async = false;
    String taskName = null;

    private ThrottlingAccessValidation accessValidation;
    boolean processingComplete;

    public ThrottlingDataContext(int tenantId) {
        this.tenantId = tenantId;
        this.data = new HashMap<String, ThrottlingDataEntry>();
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    public ThrottlingAccessValidation getAccessValidation() {
        return accessValidation;
    }

    public void setAccessValidation(ThrottlingAccessValidation accessValidation) {
        this.accessValidation = accessValidation;
    }

    public boolean isProcessingComplete() {
        return processingComplete;
    }

    public void setProcessingComplete(boolean processingComplete) {
        this.processingComplete = processingComplete;
    }

    public Collection<ThrottlingDataEntry> getData() {
        return data.values();
    }

    public void addDataString(String key, String value) {
        ThrottlingDataEntry dataEntry = new ThrottlingDataEntry(key);
        dataEntry.setStringValue(value);
        data.put(key, dataEntry);
    }

    public void addDataLong(String key, long value) {
        ThrottlingDataEntry dataEntry = new ThrottlingDataEntry(key);
        dataEntry.setLongValue(value);
        data.put(key, dataEntry);
    }

    public void addDataInt(String key, int value) {
        ThrottlingDataEntry dataEntry = new ThrottlingDataEntry(key);
        dataEntry.setIntValue(value);
        data.put(key, dataEntry);
    }

    public void addDataObject(String key, Object value) {
        ThrottlingDataEntry dataEntry = new ThrottlingDataEntry(key);
        dataEntry.setObjectValue(value);
        data.put(key, dataEntry);
    }

    public String getDataString(String key) {
        ThrottlingDataEntry dataEntry = data.get(key);
        if (dataEntry == null) {
            return null;
        }
        return dataEntry.getStringValue();
    }

    public long getDataLong(String key) {
        ThrottlingDataEntry dataEntry = data.get(key);
        if (dataEntry == null) {
            return 0;
        }
        return dataEntry.getLongValue();
    }

    public int getDataInt(String key) {
        ThrottlingDataEntry dataEntry = data.get(key);
        if (dataEntry == null) {
            return 0;
        }
        return dataEntry.getIntValue();
    }

    public Object getDataObject(String key) {
        ThrottlingDataEntry dataEntry = data.get(key);
        if (dataEntry == null) {
            return null;
        }
        return dataEntry.getObjectValue();
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
}
