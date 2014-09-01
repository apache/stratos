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
    session.put("configuring","false");
    var deploy_status = session.get("deploy-status");
    var list_status = session.get("get-status");
    var title;
    var err_message;
    var isErrDeply = false;
    var isErrGet = false;
    var isErr = false;
    var isSucceeded = false;
    var artifact_deploy = session.get("deploy_artifacts");
    session.remove("deploy_artifacts");
    var wizard_on_val = [];
    for(var i=0; i<6 ;i++){
        if(i <= data.wizard.step-1){
            wizard_on_val.push(true);
        }else{
            wizard_on_val.push(false);
        }
    }

    if(deploy_status == "succeeded") {
        isErrDeply = false;
        isSucceeded = true;
    } else if(deploy_status == null) {
        isErrDeply = false;
    } else {
        isErrDeply = true;
        err_message = deploy_status;
    }

    if(list_status == "succeeded") {
        isErrGet = false;
    } else if(list_status == null) {
        isErrGet = false;
    } else {
        isErrGet = true;
        if(err_message == undefined) {
                   err_message = list_status;
        } else {
            err_message = err_message + ", " + list_status;
        }
        step_data = "[]";
    }

    if(isErrDeply || isErrGet) {
     isErr = true;
    }

    session.remove("get-status");
    session.remove("deploy-status");

    var config_status = data.wizard;
    if( config_status.step == 1 ){
        title = 'Partition Deployment';
    }else if( config_status.step == 2 ){
        title = 'Auto scale Policy Deployment';
    }else if( config_status.step == 3 ){
        title = 'Deployment Policy Deployment';
    }else if( config_status.step == 4 ){
        title = 'Lb';
    }else if( config_status.step == 5 ){
        title = 'Cartridge Deployment';
    }else if( config_status.step == 6 ){
        title = 'Multi-Tenant Service Deployment';
    }else{
        title = 'Configure Stratos Wizard Finished';
    }
    
    if(step_data != null && step_data != undefined){
      for(var i=0;i<step_data.length;i++){
          step_data[i].json_string = stringify(step_data[i]);
      }
    }

    theme('index', {
        body: [
            {
                partial: 'configure_stratos_wizard',
                context: {
                    title:title,
                    step_data:data.step_data,
                    step:config_status.step,
                    wizard_on:true,
                    wizard_on_1:wizard_on_val[0],
                    wizard_on_2:wizard_on_val[1],
                    wizard_on_3:wizard_on_val[2],
                    wizard_on_4:wizard_on_val[3],
                    wizard_on_5:wizard_on_val[4],
                    wizard_on_6:wizard_on_val[5],
                    data_string:stringify(data.step_data),
                    error:data.error
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Configure Stratos',
                    button:{
                        link:'/',
                        name:'Deploy New Cartridge',
                        class_name:''
                    },
                    bamInfo:data.bamInfo,
                    has_help:false,
                    step_data:true,
                    config_status:data.config_status,
                    wizard_on:true,
                    wizard_on_1:wizard_on_val[0],
                    wizard_on_2:wizard_on_val[1],
                    wizard_on_3:wizard_on_val[2],
                    wizard_on_4:wizard_on_val[3],
                    wizard_on_5:wizard_on_val[4],
                    wizard_on_6:wizard_on_val[5],
                    step:step,
                    configure_stratos:true,
                    error:isErr,
                    deploy_status:isSucceeded,
                    error_msg:err_message,
                    type:artifact_deploy
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

};
