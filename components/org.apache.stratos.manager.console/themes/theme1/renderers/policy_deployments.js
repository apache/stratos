var render = function (theme, data, meta, require) {
    for(var i=0;i<data.policy_deployments.length;i++){
        data.policy_deployments[i].key = data.policy_deployments[i].name.replace(/ /g,'');
    }
    var create_btn_class = 'btn-important js_handle_click';
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