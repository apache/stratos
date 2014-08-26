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
    session.remove("get-status");
    session.remove("deploy-status");
    var create_btn_class = 'btn-important';
    var title = 'Tenant Management';
    if(data.error.length == 0 ){
        theme('index', {
            body: [
                {
                    partial: 'tenant_management',
                    context: {
                        title:title,
                        tenants:data.tenants.tenantInfoBean
                    }
                }
            ],
            header: [
                {
                    partial: 'header',
                    context:{
                        title:'Tenant Management',
                        button:{
                            link:'/tenant_new.jag',
                            name:'Add New Tenant',
                            class_name:create_btn_class
                        },
                        bamInfo:data.bamInfo,
                        has_help:false,
                        help:"Tenants you create has permission to view and subscribe to Cartridges. Tenants don't have permission to do Partition deployment, Policy deployment, LB Creation, and MT service deployment.",
                        tenant_mgt:true,
                        has_action_buttons:true
                    }
                }
            ],
            title:[
                {
                    partial:'title',
                    context:{
                        title:title
                    }
                }
            ]
        });
    }else{
        theme('index', {
            body: [
                {
                    partial: 'error_page',
                    context: {
                        title:'Error',
                        error:data.error
                    }
                }
            ],
            header: [
                {
                    partial: 'header',
                    context: {
                        title: 'My Cartridges',
                        my_cartridges: true,
                        button: {
                            link: '/cartridges.jag',
                            name: 'Subscribe to Cartridge',
                            class_name: 'btn-important'
                        },
                        bamInfo:data.bamInfo,
                        has_help: false,
                        help: 'Create cartridges like PHP, Python, Ruby etc.. Or create data cartridges with mySql, PostgreSQL. Directly install applications like Drupal, Wordpress etc..'
                    }
                }
            ],
            title: [
                {
                    partial: 'title',
                    context: {
                        title: "My Cartridges"
                    }
                }
            ]
        });
    }
};
