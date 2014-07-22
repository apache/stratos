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

var render = function (theme, data, meta, require) {

    var create_btn_class = 'btn-default';
    var title = 'Tenant Management - Add New Tenant';

    theme('index', {
        body: [
            {
                partial: 'tenant_new',
                context: {
                    title: title,
                    tenants: data.tenants
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context: {
                    title: title,
                    button: {
                        link: '/tenant_management.jag',
                        name: 'Tenant Management',
                        class_name: create_btn_class,
                        class_icon: 'icons-arrow-left'
                    },
                    bamInfo:data.bamInfo,
                    has_help: true,
                    help: "Tenants you create has permission to view and subscribe to Cartridges. Tenants don't have permission to do Partition deployment, Policy deployment, LB Creation, and MT service deployment.",
                    tenant_mgt: true
                }
            }
        ],
        title: [
            {
                partial: 'title',
                context: {
                    title: title
                }
            }
        ]
    });

};
