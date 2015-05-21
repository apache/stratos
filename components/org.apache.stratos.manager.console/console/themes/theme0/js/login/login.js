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

$(document).ready(function () {
    $('#username').focus(); //set user cursor into username
    var username = $('#username'), // Get the username field
        password = $('#password'), // Get the password field
        form_error = $('.form-error');

    //handle form valiadation
    var isValidForm = function () {
        if (username.val() == "" || password.val() == "") {
            var error = "Enter your username";
            if (username.val() == "") {
                username.parent().addClass('has-error has-feedback');
                username.parent().append('<span class="glyphicon glyphicon-remove form-control-feedback"></span>');
            } else {
                username.parent().removeClass('has-error has-feedback');
                username.parent().find('span').empty().remove();
                error = "";
            }

            if (password.val() == "") {
                password.parent().addClass('has-error has-feedback');
                password.parent().append('<span class="glyphicon glyphicon-remove form-control-feedback"></span>');

                if (error == "") {
                    error = "Enter your password";
                } else {
                    error = "Enter your username and password";
                }
            } else {
                password.parent().removeClass('has-error has-feedback');
                password.parent().find('span').empty().remove();
            }
            form_error.html(error).show();
            return false;
        } else {
            username.parent().removeClass('has-error has-feedback');
            password.parent().removeClass('has-error has-feedback');
            password.parent().find('span').empty().remove();
            username.parent().find('span').empty().remove();
            form_error.hide();
            return true;
        }
    };

    //handle login ajax request
    function loginCheck(btn) {
        btn.button('loading');
        if (!isValidForm()) {
            btn.button('reset');
        } else {
            $.ajax({
                type: "POST",
                url: caramel.context + "/controllers/login/login.jag",
                dataType: 'json',
                data: {username: username.val(), password: password.val()},
                success: function (data) {
                    if (data.status === 1) {
                        window.location = caramel.context;
                    } else {
                        form_error.show().html(data.message);
                    }
                }
            })
                .always(function () {
                    btn.button('reset');
                });
        }
    }

    username.keyup(function (e) {
        if(e.keyCode == 13) {
            return;
        }
        isValidForm();
    });
    password.keyup(function (e) {
        if(e.keyCode == 13) {
            return;
        }
        isValidForm();
    });

    // handle the submit button click event
    $('#submit-btn').click(function () {
        var btn = $(this); //get current clicked button
        loginCheck(btn);
    });// end of submit button

    //handle key "Enter" press
    $(document).keypress(function (e) {
        if (e.keyCode == 13) {
            var btn = $('#submit-btn');
            loginCheck(btn);
        }
    });
});