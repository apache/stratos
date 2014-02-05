var render = function (theme, data, meta, require) {
    for(var i=0;i<data.mt_service_deployments.length;i++){
        data.mt_service_deployments[i].key = data.mt_service_deployments[i].name.replace(/ /g,'');
    }
    var create_btn_class = 'btn-important js_handle_click';
    var title = 'Configure Stratos - Multi-Tenant Service Deployments';
    if(data.config_status.first_use){
        create_btn_class = "btn-default js_handle_click";
        title =  'Configure Stratos';
    }
    theme('index', {
        body: [
            {
                partial: 'mt_service_deployments',
                context: {
                    title:title,
                    mt_service_deployments:data.mt_service_deployments,
                    config_status:data.config_status
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Configure Stratos',
                    button:{
                        link:'/',
                        name:'Deploy New Multi-Tenant Service',
                        class_name:create_btn_class
                    },
                    has_help:false,
                    mt_service_deployments:true,
                    configure_stratos:true,
                    config_status:data.config_status
                }
            }
        ],
        title:[
            {
                partial:'title',
                context:{
                    title:title
                }
            }
        ]
    });
};