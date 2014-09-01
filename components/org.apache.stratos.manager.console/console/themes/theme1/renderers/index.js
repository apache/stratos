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
      // Re-create the data structure of the cartridges.
    var log = new Log();
    if(data.error.length == 0 ){
        if(data.mycartridges == null || data.mycartridges == undefined || data.mycartridges == ""){
          data.mycartridges = {};
        }
        var cartridges= data.mycartridges.cartridge,cartridges_new =[];
        session.remove("get-status");
        session.remove("deploy-status");

       /*
        Uncomment this to populate some dummy data to preview the UI

        for(var i=0;i<3;i++){
            var newElm =parse(stringify(cartridges[0]));
            newElm.serviceGroup = "foo";
            newElm.cartridgeAlias = newElm.cartridgeAlias + i;
            cartridges.push(newElm);
        }
        for(var i=0;i<3;i++){
            var newElm =parse(stringify(cartridges[0]));
            newElm.serviceGroup = "bar";
            newElm.cartridgeAlias = newElm.cartridgeAlias + i;
            cartridges.push(newElm);
        }
        */
        for (var i = 0; i < cartridges.length; i++) {
            if(cartridges[i].serviceGroup != undefined){
                if(!cartridges[i].done){

                    cartridges[i].done = true;
                    var newObj = {};
                    var serviceGroup = cartridges[i].serviceGroup;
                    newObj.serviceGroup = serviceGroup;
                    newObj.items = [];
                    newObj.items.push(parse(stringify(cartridges[i])));

                    for (var j = 0; j < cartridges.length; j++) {
                        if(cartridges[j].serviceGroup == serviceGroup && !cartridges[j].done){
                            cartridges[j].done =true;
                            newObj.items.push(parse(stringify(cartridges[j])));
                        }
                    }

                    cartridges_new.push(newObj);
                }
            }else {
                cartridges_new.push(cartridges[i]);
            }
        }


        /*if(cartridges_old == null) {
            cartridges_old = {};
        }

        var cartridges_new = [
            {
                kind: "Framework",
                cartridges: []}
        ];
        var cartridgesToPush;
        for (var i = 0; i < cartridges_old.length; i++) {
            if (cartridges_old[i].provider == undefined || (cartridges_old[i].provider.toLowerCase() != "application" && cartridges_old[i].provider.toLowerCase() != "data" )) {
                cartridgesToPush = null;
                for (var j = 0; j < cartridges_new.length; j++) {
                    if (cartridges_new[j].kind == "Framework") {
                        cartridgesToPush = cartridges_new[j].cartridges;
                    }
                }
                cartridgesToPush.push(cartridges_old[i]);
            } else {
                cartridgesToPush = null;
                for (var j = 0; j < cartridges_new.length; j++) {
                    if (cartridges_new[j].kind == cartridges_old[i].provider) {
                        cartridgesToPush = cartridges_new[j].cartridges;
                    }
                }
                if (cartridgesToPush == null) {
                    var kind = cartridges_old[i].provider;
                    cartridges_new.push({kind: cartridges_old[i].provider, cartridges: [cartridges_old[i]]})
                } else {
                    cartridgesToPush.push(cartridges_old[i]);
                }
            }
        }*/

        theme('index', {
            body: [
                {
                    partial: 'mycartridges',
                    context: {
                        title: 'My Cartridges',
                        mycartridges: cartridges_new
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
                        bamInfo:data.bamInfo,
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
