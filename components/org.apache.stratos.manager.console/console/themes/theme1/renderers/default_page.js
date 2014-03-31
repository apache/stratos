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

    theme('plain', {
        body: [
            {
                partial: '404',
                context: {
                    title: 'My Cartridges'
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
};
