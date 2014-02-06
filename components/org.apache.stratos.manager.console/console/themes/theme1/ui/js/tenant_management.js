$(function(){
    $('#checkAll').click(function(){
        if($(this).is(":checked")){
            $('table input.js_domainCheck').prop('checked',true);
        } else{
            $('table input.js_domainCheck').prop('checked',false);
        }
    })
});
function manage_selected(action){
    var checked = "";
    $('table input.js_domainCheck:checked').each(function(){
        checked += $(this).val();
        checked += " - ";
    });
    if(action == "activate"){
        alert('activate-' + checked);
    }else if(action == "deactivate"){
        alert('deactivate-' + checked);
    }else if(action == "delete"){
        alert('delete-' + checked);
    }
}
function manage_one(action,obj){
    if(action == "activate"){
        $('#foo').val($(obj).attr('data-domain'));
        $('#state').val(action);
        $('#manageTenantsForm').submit();
        //alert('activate-' + $(obj).attr('data-domain'));
    }else if(action == "deactivate"){
        //alert('deactivate-' + $(obj).attr('data-domain'));
         $('#foo').val($(obj).attr('data-domain'));
         $('#state').val(action);
        $('#manageTenantsForm').submit();
    }
}