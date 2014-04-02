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

var popbox = popbox || {};

(function () {
popbox.messageDisplay = function (params) {
    $('#messageModal').html($('#confirmation-data').html());
    if(params.title == undefined){
        $('#messageModal h3.modal-title').html('API Store');
    }else{
        $('#messageModal h3.modal-title').html(params.title);
    }
    $('#messageModal div.modal-body').html(params.content);
    if(params.buttons != undefined){
        $('#messageModal a.btn-primary').hide();
        $('#messageModal div.modal-footer').html('');
        for(var i=0;i<params.buttons.length;i++){
            $('#messageModal div.modal-footer').append($('<a class="btn '+params.buttons[i].cssClass+'">'+params.buttons[i].name+'</a>').click(params.buttons[i].cbk));
        }
    }else{
        $('#messageModal a.btn-primary').html('OK').click(function() {
            $('#messageModal').modal('hide');
        });
    }
    $('#messageModal a.btn-other').hide();
    $('#messageModal').modal();
};
/*
 usage
 Show info dialog
 popbox.message({content:'foo',type:'info', cbk:function(){alert('Do something here.')} });

 Show warning
 dialog popbox.message({content:'foo',type:'warning', cbk:function(){alert('Do something here.')} });

 Show error dialog
 popbox.message({content:'foo',type:'error', cbk:function(){alert('Do something here.')} });

 Show confirm dialog
 popbox.message({content:'foo',type:'confirm',okCallback:function(){},cancelCallback:function(){}});
 */
popbox.message = function(params){
    if(params.type == "custom"){
        popbox.messageDisplay(params);
        return;
    }
    if(params.type == "confirm"){
        if( params.title == undefined ){ params.title = "API Store"}
        popbox.messageDisplay({content:params.content,title:params.title ,buttons:[
            {name:"Yes",cssClass:"btn btn-primary",cbk:function() {
                $('#messageModal').modal('hide');
                if(typeof params.okCallback == "function") {params.okCallback()};
            }},
            {name:"No",cssClass:"btn",cbk:function() {
                $('#messageModal').modal('hide');
                if(typeof params.cancelCallback  == "function") {params.cancelCallback()};
            }}
        ]
        });
        return;
    }
    var iconClass = "fa fa-times-circle-o";
    if(params.type == "info"){ iconClass = "info fa fa-info-circle"}
    if(params.type == "warning"){ iconClass = "warning fa fa-exclamation-circle"}
    if(params.type == "error"){ iconClass = "error fa fa-times-circle-o"}
    if(params.type == "confirm"){ iconClass = "confirm fa fa-question-circle"}
    params.content = '<table class="msg-table"><tr><td class="imageCell"><i class="'+iconClass+'"></i></td><td><span class="messageText">'+params.content+'</span></td></tr></table>';
    var type = "";
    if(params.title == undefined){
        if(params.type == "info"){ type = "Notification"}
        if(params.type == "warning"){ type = "Warning"}
        if(params.type == "error"){ type = "Error"}
    }
    popbox.messageDisplay({content:params.content,title:"API Store - " + type,buttons:[
        {name:"OK",cssClass:"btn btn-primary",cbk:function() {
            $('#messageModal').modal('hide');
            if(params.cbk && typeof params.cbk == "function")
                params.cbk();
        }}
    ]
    });
};
}());
