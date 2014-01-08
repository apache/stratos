var render = function (theme, data, meta, require) {
    for(var i=0;i<data.cartridge_deployments.length;i++){
        data.cartridge_deployments[i].key = data.cartridge_deployments[i].name.replace(/ /g,'');
    }
    var create_btn_class = 'btn-important js_handle_click';
    var title = 'Configure Stratos - Partition Deployments';
    if(data.config_status.first_use){
        create_btn_class = "btn-default js_handle_click";
        title =  'Configure Stratos';
    }
    theme('index', {
        body: [
            {
                partial: 'cartridge_deployments',
                context: {
                    title:title,
                    cartridge_deployments:data.cartridge_deployments,
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
                        name:'Deploy New Cartridge',
                        class_name:create_btn_class
                    },
                    has_help:false,
                    cartridge_deployments:true,
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