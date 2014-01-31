var render = function (theme, data, meta, require) {
    var create_btn_class = 'btn-important';
    var title = 'Configure Stratos - Policy Deployments';
    if(data.config_status.first_use){
        create_btn_class = "btn-default js_handle_click";
        title =  'Configure Stratos';
    }
    theme('index', {
        body: [
            {
                partial: 'policy_deployments',
                context: {
                    title:title,
                    policy_deployments:data.policy_deployments,
                    policy_autoscale:data.policy_autoscale,
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
                        name:'Deploy New Policy',
                        class_name:create_btn_class
                    },
                    has_help:false,
                    policy_deployments:true,
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