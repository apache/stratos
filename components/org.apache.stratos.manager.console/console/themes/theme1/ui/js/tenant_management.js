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

$(function(){
    $('#checkAll').click(function(){
        if($(this).is(":checked")){
            $('table input.js_domainCheck').prop('checked',true);
        } else{
            $('table input.js_domainCheck').prop('checked',false);
        }
    });

    $( "#newTenantForm" ).validate({
        rules: {
            adminPassword: {required:true,maxlength:30,minlength:5},
            adminPassword_again: {
                equalTo: "#adminPassword"
            }
        }
    });

});
function manage_selected(action){
    var checked = "";
    $('table input.js_domainCheck:checked').each(function(){
        checked += $(this).val();
        checked += " - ";
    });
    if(action == "activate"){
        $('#foo').val(checked);
        $('#state').val(action);
        $('#manageTenantsForm').submit();
        //alert('activate-' + checked);
    } else if(action == "deactivate"){
        $('#foo').val(checked);
        $('#state').val(action);
        $('#manageTenantsForm').submit();
        //alert('deactivate-' + checked);
    } else if(action == "delete"){
        $('#foo').val(checked);
        $('#state').val(action);
        $('#manageTenantsForm').submit();
        //alert('delete-' + checked);
    }
}
function manage_one(action,obj){

    if(action == "activate"){
        //alert('activate-' + $(obj).attr('data-domain'));
        $('#foo').val($(obj).attr('data-domain'));
        $('#state').val(action);
        $('#manageTenantsForm').submit();
    } else if(action == "deactivate"){
        //alert('deactivate-' + $(obj).attr('data-domain'));
         $('#foo').val($(obj).attr('data-domain'));
         $('#state').val(action);
        $('#manageTenantsForm').submit();
    }
}

function checkAvailability() {
    var domain = $('#tenantDomain').val();
    if(domain.trim() == ""){
        $('#domainMessage').show().html('Domain should not be empty.').addClass('noDomain').removeClass('hasDomain');
        return;
    }
    $.ajax({
        data:{domain:domain},
        url:"/console/controllers/checkAvailability.jag",
        success:function(data){
            if(data=="false"){
                $('#domainMessage').show().html('Domain is not available').addClass('noDomain').removeClass('hasDomain');
            }else{
                $('#domainMessage').show().html('Domain is available').addClass('hasDomain').removeClass('noDomain');
            }
        }
    })
}
