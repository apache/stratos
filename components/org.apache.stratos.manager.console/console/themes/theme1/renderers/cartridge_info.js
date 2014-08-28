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
        theme('index', {
            body: [
                {
                    partial: 'cartridge_info',
                    context: {
                        title:'Cartridges',
                        cartridgeInfo:data.cartridgeInfo.cartridge,
                        lbclusterinfo:data.lbCluster.cluster,
                        clusterinfo:data.clusterInfo.cluster,
                        host:data.cartridgeInfo.cartridge.hostName
                    }
                }
            ],
            header: [
                {
                    partial: 'header',
                    context:{
                        title:'Cartridges',
                        my_cartridges:true,
                        button:{
                            link:'/',
                            name:'Back To My Cartridges',
                            class_name:"btn-default",
                            class_icon: 'icons-arrow-left'
                        },
                        bamInfo:data.bamInfo,
                        has_help:false,
                        help:'Create cartridges like PHP, Python, Ruby etc.. Or create data cartridges with mySql, PostgreSQL. Directly install applications like Drupal, Wordpress etc..'
                    }
                }
            ],
            title:[
                {
                    partial:'title',
                    context:{
                        title:"My Cartridges"
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
                        has_help:false,
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
