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
    var $username = $('#username');
    var $password = $('#password');
    var $loginForm = $('#loginForm');
    var isValidForm = function(){
        if($username.val() == "" || $password.val() == ""){
            var error = "Enter your username";
            if($username.val() == ""){
                $username.addClass('error');
            }else{
                $username.removeClass('error');
                error = "";
            }

            if($password.val() == ""){
                $password.addClass('error');
                if(error == ""){
                    error = "Enter your password";
                }else{
                    error = "Enter your username and password";
                }
            }else{
                $password.removeClass('error');
            }
            $('#loginError').html(error).show();
            return false;
        }else{
            $username.removeClass('error');
            $password.removeClass('error');
            $('#loginError').hide();
            return true;
        }

    };
    var submitForm = function(){
        if(isValidForm()){
            $loginForm.submit();
        }else{
            if($password.val() == ""){
                $password.focus();
            }
            if($username.val() == ""){
                $username.focus();
            }

            $username.keyup(function(){
                isValidForm();
            });
            $password.keyup(function(){
                isValidForm();
            });
        }
    }
    $('#loginButton').click(function(){
        submitForm();
    });
    $username.keyup(function(e){
          if(e.which == "13"){
              submitForm();
          }
    });
    $password.keyup(function(e){
          if(e.which == "13"){
              submitForm();
          }
    });
    $username.focus();
});
