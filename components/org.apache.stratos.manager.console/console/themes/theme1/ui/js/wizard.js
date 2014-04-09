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
    var isValidForm = function(){
        if(parseInt($('#items').val()) == 0 || $('#wizard_on').val() == "false"){
            if($('#policy').val() == ""){
                $('#policy').addClass('error').focus();
                $('#policyError').html("Required").show();
                return false;
            }else{
                $('#policy').removeClass('error');
                $('#policyError').hide();
                return true;
            }
        }else{
            return true;
        }

    };
    var isValidPolicy = function(){
        var policy = $('#policy').val();
        var error;
        if(policy != ""){
            try {
                var c = $.parseJSON(policy);
            }
            catch (err) {
                error = err;
            }
        }

        if(error == undefined){
            $('#policy').removeClass('error');
            $('#policyError').hide();
            return true;
        }else{
            $('#policy').addClass('error').focus();
            $('#policyError').html(error).show();
            return false;
        }
    };
    $('#wizardNext').click(function () {
        var newStep = parseInt($(this).attr('data-step')) + 1;
        $('#nextStep').val(newStep);
        if(isValidForm() && isValidPolicy()){
            $('#jsonForm').submit();
        }
    });

    $('#wizardSkip').click(function () {
        var newStep = parseInt($(this).attr('data-step')) + 1;
        $('#skip').val("true");
        $('#nextStep').val(newStep);
        $('#jsonForm').submit();
    });

    $('#wizardFinish').click(function () {
        var newStep = parseInt($(this).attr('data-step')) + 1;
        $('#nextStep').val(newStep);
        //if(isValidForm()){
        $('#jsonForm').submit();
        //}
    });


    $('#wizardBack').click(function () {
        var newStep = parseInt($(this).attr('data-step')) - 1;
        $('#nextStep').val(newStep);
        // if(isValidForm()){
        $('#jsonForm').submit();
        // }
    });
    var thisStep = $('#thisStep').val();

    $('#policy').keyup(function(){
        if(parseInt(thisStep)==4){
            if($(this).val() != ""){
                $('#wizardNext').removeClass('btn-default').addClass('btn-primary');
                $('#wizardSkip').removeClass('btn-primary').addClass('btn-default');
            }else{
                $('#wizardSkip').removeClass('btn-default').addClass('btn-primary');
                $('#wizardNext').removeClass('btn-primary').addClass('btn-default');
            }
        }else{
            isValidForm();
            isValidPolicy();
        }
    });
    $('pre').each(function () {
        var jsonStr = $(this).html();
        jsonStr = jsonStr.replace(/\'/g, '\"');
        var obj = jQuery.parseJSON(jsonStr);
        jsonStr = JSON.stringify(obj, undefined, 2);
        $('textarea', $(this).parent()).val(jsonStr);
        $(this).html(syntaxHighlight(jsonStr));
    });

    $('.js_undeploy').click(function(){
        var $btn = $(this);
        var type = $(this).attr('data-type');
        if(type == null || type == "" || type == undefined){
            return;
        }
        $.ajax({
            data:{type:type,action:"undeploy"},
            url:"/console/controllers/wizardSubmit.jag",
            success:function(data){
                data = jQuery.parseJSON(data);
                if(data.Error != undefined){
                    $btn.prev().html('<button aria-hidden="true" data-dismiss="alert" class="close" type="button">×</button> ' + data.Error.errorMessage).show();
                }else{
                    $btn.closest(".panel-default")
                        .empty()
                        .removeClass("panel")
                        .removeClass("panel-default")
                        .addClass("alert alert-success alert-dismissable")
                        .html('<button aria-hidden="true" data-dismiss="alert" class="close" type="button">×</button> Cartridge undeployed successfully');
                }
            }
        })
    })
});