/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
var gadgetConfig = {
    "id": "Member_Information",
    "title": "Member Information",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "MemberId",
            "LABEL_NAME": "Member Id",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "InstanceType",
            "LABEL_NAME": "Instance Type",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "ImageId",
            "LABEL_NAME": "Image Id",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "HostName",
            "LABEL_NAME": "Host Name",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "PrivateIPAddresses",
            "LABEL_NAME": "Private IP Addresses",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "PublicIPAddresses",
            "LABEL_NAME": "Public IP Addresses",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "Hypervisor",
            "LABEL_NAME": "Hypervisor",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "CPU",
            "LABEL_NAME": "CPU",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "RAM",
            "LABEL_NAME": "RAM",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "OSName",
            "LABEL_NAME": "OS Name",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "OSVersion",
            "LABEL_NAME": "OS Version",
            "DATA_TYPE": "varchar"
        }
    ],
    "maxUpdateValue": 10,
    "chartConfig": {
        "chartType": "tabular", "xAxis": 0
    }
    ,
    "domain": "carbon.super"
};