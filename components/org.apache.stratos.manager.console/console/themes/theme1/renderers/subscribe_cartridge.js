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
    if(data.error.length == 0 ){
        var cartridge = data.cartridge.cartridge;
        if(cartridge == undefined){
            cartridge = data.cartridge;
        }
        theme('index', {
            body: [
                {
                    partial: 'subscribe_cartridge',
                    context: {
                        autoScalePolicies:data.autoScalePolicies.autoscalePolicy,
                        deploymentPolicies:data.deploymentPolicies.deploymentPolicy,
                        cartridge:cartridge,
                        cartridgeType:meta.request.getParameter('cartridgeType')
                    }
                }
            ],
            header: [
                        {
                            partial: 'header',
                            context:{
                                title:'Subscribe Cartridge',
                                my_cartridges:true,
                                button:{
                                    link: '/cartridges.jag',
                                    name: 'Select different Cartridge',
                                    class_name: "btn-default",
                                    class_icon: "icon-arrow-left"
                                },
                                has_help:true,
                                help:'Create cartridges like PHP, Python, Ruby etc.. Or create data cartridges with mySql, PostgreSQL. Directly install applications like Drupal, Wordpress etc..'
                            }
                        }
                    ],
            title:[
                {
                    partial:'title',
                    context:{
                        title:"Subscribe Cartridge -" + cartridge.cartridgeType + " " + cartridge.version + " Cartridge",
                        cartridge:cartridge
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
                        has_help: true,
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
