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
    "id": "Member_Status",
    "title": "Member Status",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "MemberId",
            "LABEL_NAME": "Member Id",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "Time",
            "LABEL_NAME": "Time",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "MemberStatus",
            "LABEL_NAME": "Member Status",
            "DATA_TYPE": "varchar"
        }
    ],
    "maxUpdateValue": 10,
    "chartConfig": {
        "chartType": "tabular",
        "xAxis": 1
    }
    ,
    "domain": "carbon.super"
};