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
    "datasource": "MEMBER_INFORMATION",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "MemberId",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "InstanceType",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "ImageId",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "HostName",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "PrivateIPAddresses",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "PublicIPAddresses",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "Hypervisor",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "CPU",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "RAM",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "OSName",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "OSVersion",
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