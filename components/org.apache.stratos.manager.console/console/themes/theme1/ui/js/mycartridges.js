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