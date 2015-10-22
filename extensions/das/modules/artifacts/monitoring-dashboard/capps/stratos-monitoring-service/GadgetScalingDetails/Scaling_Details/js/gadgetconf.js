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
    "id": "Scaling_Details",
    "title": "Cluster Instances",
    "type": "batch",
    "columns": [
        {
            "COLUMN_NAME": "Time",
            "LABEL_NAME": "Time",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "ClusterId",
            "LABEL_NAME": "Cluster Id",
            "DATA_TYPE": "varchar"
        },
        {
            "COLUMN_NAME": "MinInstanceCount",
            "LABEL_NAME": "Min Instance Count",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "MaxInstanceCount",
            "LABEL_NAME": "Max Instance Count",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "RIFPredicted",
            "LABEL_NAME": "RIF Predicted",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "RIFThreshold",
            "LABEL_NAME": "RIF Threshold",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "RIFRequiredInstances",
            "LABEL_NAME": "RIF Required Instances",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "MCPredicted",
            "LABEL_NAME": "MC Predicted",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "MCThreshold",
            "LABEL_NAME": "MC Threshold",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "MCRequiredInstances",
            "LABEL_NAME": "MC Required Instances",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "LAPredicted",
            "LABEL_NAME": "LA Predicted",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "LAThreshold",
            "LABEL_NAME": "LA Threshold",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "LARequiredInstances",
            "LABEL_NAME": "LA Required Instances",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "ActiveInstanceCount",
            "LABEL_NAME": "Active Instance Count",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "AdditionalInstanceCount",
            "LABEL_NAME": "Additional Instance Count",
            "DATA_TYPE": "int"
        },
        {
            "COLUMN_NAME": "ScalingReason",
            "LABEL_NAME": "Scaling Reason",
            "DATA_TYPE": "varchar"
        }
    ],
    "maxUpdateValue": 10,
    "chartConfig": {"chartType": "tabular", "xAxis": 0},
    "domain": "carbon.super"
};