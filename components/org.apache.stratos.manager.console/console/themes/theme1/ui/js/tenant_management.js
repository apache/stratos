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
            adminPassword: "required",
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
    console.info(domain);
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