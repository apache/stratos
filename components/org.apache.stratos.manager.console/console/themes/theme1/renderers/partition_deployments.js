var render = function (theme, data, meta, require) {

    var create_btn_class = 'btn-important js_handle_click';
    var title = 'Configure Stratos - Partition Deployments';
    if(data.config_status.first_use){
        create_btn_class = "btn-default js_handle_click";
        title =  'Configure Stratos';
    }
    theme('index', {
        body: [
            {
                partial: 'partition_deployments',
                context: {
                    title:title,
                    partition_deployments:data.partition_deployments,
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
                        name:'Deploy New Partition',
                        class_name:create_btn_class
                    },
                    has_help:false,
                    partition_deployments:true,
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