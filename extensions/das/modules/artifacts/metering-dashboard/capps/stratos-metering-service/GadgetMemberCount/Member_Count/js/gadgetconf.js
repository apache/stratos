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
    "id": "Member_Count",
    "title": "Member_Count",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "Time",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "CreatedInstanceCount",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "InitializedInstanceCount",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "ActiveInstanceCount",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "TerminatedInstanceCount",
            "DATA_TYPE": "int"
        }
    ],
    "maxUpdateValue": 10,
    "chartConfig": {
        "chartType": "line",
        "yAxis": [1, 2, 3, 4],
        "xAxis": 0,
        "interpolationMode": "line"
    },
    "domain": "carbon.super"
};