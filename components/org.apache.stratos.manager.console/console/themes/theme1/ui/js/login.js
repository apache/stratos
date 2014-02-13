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