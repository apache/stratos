$(function () {
    $('#wizardNext').click(function () {
        var newStep = parseInt($(this).attr('data-step')) + 1;
        $('#nextStep').val(newStep);
        if(isValidForm()){
            $('#jsonForm').submit();
        }
    });

    $('#wizardFinish').click(function () {
        var newStep = parseInt($(this).attr('data-step')) + 1;
        $('#nextStep').val(newStep);
        if(isValidForm()){
            $('#jsonForm').submit();
        }
    });


    $('#wizardBack').click(function () {
        var newStep = parseInt($(this).attr('data-step')) - 1;
        $('#nextStep').val(newStep);
        if(isValidForm()){
            $('#jsonForm').submit();
        }
    });

    $('#policy').keydown(function(){
        isValidForm();
    });

    var isValidForm = function(){
        if(parseInt($('#items').val()) == 0){
            if($('#policy').val() == ""){
                $('#policy').addClass('error').focus();
                $('#policyError').show();
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
    $('pre').each(function () {
        var jsonStr = $(this).html();
        jsonStr = jsonStr.replace(/\'/g, '\"');
        var obj = jQuery.parseJSON(jsonStr);
        jsonStr = JSON.stringify(obj, undefined, 2);
        $('textarea', $(this).parent()).val(jsonStr);
        $(this).html(syntaxHighlight(jsonStr));
    });
});
