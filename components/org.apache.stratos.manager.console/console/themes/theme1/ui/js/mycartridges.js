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

$(function () {
    $('.un-subscribe-btn').click(function () {
        $('#alias').val($(this).attr('data-alias'));
        popbox.message({content: '<div>Un-subscribe will delete all your instances.</div><div>Are you sure you want to un-subscribe?</div>', type: 'confirm',
            okCallback: function () {
                $('#cForm').submit();
            },
            cancelCallback: function () {
            }
        });
    });

    $('.js_syncRepo').click(function(){
        var alias = $(this).attr('data-value');
        $.ajax({
            data:{alias:alias,action:"sync"},
            url:"/console/controllers/mycartridges.jag",
            success:function(data){
                alert($.parseJSON(data).msg)
            }
        })
    })
});
