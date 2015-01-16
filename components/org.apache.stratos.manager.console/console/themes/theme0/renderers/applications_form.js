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

    if(data.error.length === 0 ){

        switch (data.applicationHbs) {
            case "applicationView":
                theme('index', {
                    page_meta: [
                        {
                            partial: 'index_title',
                            context: {
                                page_title: 'Apache Stratos - Application Managment',
                                page_description: 'Apache Stratos - Application Managment'
                            }
                        }
                    ],
                    header: [
                        {
                            partial: 'index_header',
                            context: {
                            }
                        }
                    ],
                    sub_header: [
                        {
                            partial: 'index_sub_header',
                            context: {
                                breadcrumbPathLevelOne: data.breadcrumbPathLevelOne,
                                breadcrumbPathLevelTwo: data.breadcrumbPathLevelTwo
                            }
                        }
                    ],
                    left_menu: [
                        {
                            partial: 'index_left_menu',
                            context: {
                                left_menu: data.left_menu
                            }
                        }
                    ],
                    right_menu_help: [
                        {
                            partial: 'index_right_menu_help',
                            context: {

                            }
                        }
                    ],
                    content: [
                        {
                            partial: 'applications_view',
                            context: {
                                applicationName: data.applicationName,
                                formContext: data.breadcrumbPathLevelTwo,
                                appName: data.appName,
                                applicationJSON: data.applicationJSON,
                                formTitle: data.formTitle
                            }
                        }

                    ]
                });
            break;

            case "applicationTopology":

            theme('index', {
                page_meta: [
                    {
                        partial: 'index_title',
                        context: {
                            page_title: 'Apache Stratos - Application Managment',
                            page_description: 'Apache Stratos - Application Managment'
                        }
                    }
                    ],
                    header: [
                        {
                            partial: 'index_header',
                            context: {
                            }
                        }
                    ],
                    sub_header: [
                        {
                            partial: 'index_sub_header',
                            context: {
                                breadcrumbPathLevelOne: data.breadcrumbPathLevelOne,
                                breadcrumbPathLevelTwo: data.breadcrumbPathLevelTwo
                            }
                        }
                    ],
                    left_menu: [
                        {
                            partial: 'index_left_menu',
                            context: {
                                left_menu: data.left_menu
                            }
                        }
                    ],
                    right_menu_help: [
                        {
                            partial: 'index_right_menu_help',
                            context: {

                            }
                        }
                    ],
                    content: [
                        {
                            partial: 'applications_topology',
                            context: {
                                formContext: data.breadcrumbPathLevelTwo,
                                appName: data.appName,
                                topologyData: data.topologyData,
                                form_action: data.form_action,
                                formHtml: data.formHtml,
                                formData: data.formData,
                                formDataRaw: data.formDataRaw,
                                formDataEdit: data.formDataEdit,
                                isForm: data.isForm,
                                isEdit: data.isEdit,
                                formTitle: data.formTitle,
                                content_body: {sections: data.list_data
                                }
                            }
                        }

                    ]
                });
            break;

            case "applicationDeploy":
                theme('index', {
                    page_meta: [
                        {
                            partial: 'index_title',
                            context: {
                                page_title: 'Apache Stratos - Application Managment',
                                page_description: 'Apache Stratos - Application Managment'
                            }
                        }
                    ],
                    header: [
                        {
                            partial: 'index_header',
                            context: {
                            }
                        }
                    ],
                    sub_header: [
                        {
                            partial: 'index_sub_header',
                            context: {
                                breadcrumbPathLevelOne: data.breadcrumbPathLevelOne,
                                breadcrumbPathLevelTwo: data.breadcrumbPathLevelTwo
                            }
                        }
                    ],
                    left_menu: [
                        {
                            partial: 'index_left_menu',
                            context: {
                                left_menu: data.left_menu
                            }
                        }
                    ],
                    right_menu_help: [
                        {
                            partial: 'index_right_menu_help',
                            context: {

                            }
                        }
                    ],
                    content: [
                        {
                            partial: 'applications_deploy',
                            context: {
                                formContext: data.breadcrumbPathLevelTwo,
                                applicationName: data.applicationName,
                                applicationJSON: data.applicationJSON,
                                editorCartridges: data.editorCartridges,
                                editorGroups:data.editorGroups,
                                form_action: data.form_action,
                                formHtml: data.formHtml,
                                formData: data.formData,
                                formDataRaw: data.formDataRaw,
                                formDataEdit: data.formDataEdit,
                                isForm: data.isForm,
                                isEdit: data.isEdit,
                                formTitle: data.formTitle

                            }
                        }

                    ]
                });
            break;

            case "applicationEditor":
                theme('index', {
                    page_meta: [
                        {
                            partial: 'index_title',
                            context: {
                                page_title: 'Apache Stratos - Application Managment',
                                page_description: 'Apache Stratos - Application Managment'
                            }
                        }
                    ],
                    header: [
                        {
                            partial: 'index_header',
                            context: {
                            }
                        }
                    ],
                    sub_header: [
                        {
                            partial: 'index_sub_header',
                            context: {
                                breadcrumbPathLevelOne: data.breadcrumbPathLevelOne,
                                breadcrumbPathLevelTwo: data.breadcrumbPathLevelTwo
                            }
                        }
                    ],
                    left_menu: [
                        {
                            partial: 'index_left_menu',
                            context: {
                                left_menu: data.left_menu
                            }
                        }
                    ],
                    right_menu_help: [
                        {
                            partial: 'index_right_menu_help',
                            context: {

                            }
                        }
                    ],
                    content: [
                        {
                            partial: 'applications_editor',
                            context: {
                                formContext: data.breadcrumbPathLevelTwo,
                                appName: data.appName,
                                editorCartridges: data.editorCartridges,
                                editorGroups:data.editorGroups,
                                form_action: data.form_action,
                                formHtml: data.formHtml,
                                formData: data.formData,
                                formDataRaw: data.formDataRaw,
                                formDataEdit: data.formDataEdit,
                                isForm: data.isForm,
                                isEdit: data.isEdit,
                                formTitle: data.formTitle

                            }
                        }

                    ]
                });
            break;

            case "applicationsGroupEditor":
                theme('index', {
                    page_meta: [
                        {
                            partial: 'index_title',
                            context: {
                                page_title: 'Apache Stratos - Application Managment',
                                page_description: 'Apache Stratos - Application Managment'
                            }
                        }
                    ],
                    header: [
                        {
                            partial: 'index_header',
                            context: {
                            }
                        }
                    ],
                    sub_header: [
                        {
                            partial: 'index_sub_header',
                            context: {
                                breadcrumbPathLevelOne: data.breadcrumbPathLevelOne,
                                breadcrumbPathLevelTwo: data.breadcrumbPathLevelTwo
                            }
                        }
                    ],
                    left_menu: [
                        {
                            partial: 'index_left_menu',
                            context: {
                                left_menu: data.left_menu
                            }
                        }
                    ],
                    right_menu_help: [
                        {
                            partial: 'index_right_menu_help',
                            context: {

                            }
                        }
                    ],
                    content: [
                        {
                            partial: 'applications_group_editor',
                            context: {
                                formContext: data.breadcrumbPathLevelTwo,
                                appName: data.appName,
                                editorCartridges: data.editorCartridges,
                                editorGroups:data.editorGroups,
                                form_action: data.form_action,
                                formHtml: data.formHtml,
                                formData: data.formData,
                                formDataRaw: data.formDataRaw,
                                formDataEdit: data.formDataEdit,
                                isForm: data.isForm,
                                isEdit: data.isEdit,
                                formTitle: data.formTitle

                            }
                        }

                    ]
                });
            break;
            default:
                theme('index', {
                    page_meta: [
                        {
                            partial: 'index_title',
                            context: {
                                page_title: 'Apache Stratos - Application Managment',
                                page_description: 'Apache Stratos - Application Managment'
                            }
                        }
                    ],
                    header:[
                        {
                            partial: 'index_header',
                            context:{
                            }
                        }
                    ],
                    sub_header:[
                        {
                            partial:'index_sub_header',
                            context:{
                                breadcrumbPathLevelOne:data.breadcrumbPathLevelOne,
                                breadcrumbPathLevelTwo:data.breadcrumbPathLevelTwo
                            }
                        }
                    ],
                    left_menu:[
                        {
                            partial:'index_left_menu',
                            context:{
                                left_menu:data.left_menu
                            }
                        }
                    ],
                    right_menu_help:[
                        {
                            partial:'index_right_menu_help',
                            context:{

                            }
                        }
                    ],
                    content: [
                        {
                            partial:'applications_form',
                            context:{
                                formContext: data.breadcrumbPathLevelTwo,
                                form_action: data.form_action,
                                formHtml: data.formHtml,
                                formData: data.formData,
                                formDataRaw: data.formDataRaw,
                                formDataEdit: data.formDataEdit,
                                formtype:data.formtype,
                                isForm: data.isForm,
                                isEdit:data.isEdit,
                                formTitle: data.formTitle,
                                content_body: {sections:
                                    data.list_data
                                }
                            }
                        }

                    ]
                });
            break;

        }

    }else{

        theme('index', {
            page_meta: [
                {
                    partial:'index_title',
                    context:{
                        page_title:'Apache Stratos Home - Error',
                        page_description:'Apache Stratos Home - Error'
                    }
                }
            ],
            header:[
                {
                    partial: 'index_header',
                    context:{
                    }
                }
            ],
            content: [

                {
                    partial: 'error_page',
                    context:{
                        error:data.error,
                        content_title:'Sorry Something went Wrong...! ',
                        content_body:{

                        }

                    }
                }
            ]
        });
    }
};