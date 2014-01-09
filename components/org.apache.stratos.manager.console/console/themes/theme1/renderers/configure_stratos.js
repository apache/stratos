var render = function (theme, data, meta, require) {
    for(var i=0;i<data.partition_deployment.length;i++){
        data.partition_deployment[i].key = data.partition_deployment[i].name.replace(/ /g,'');
    }

    theme('index', {
        body: [
            {
                partial: 'configure_stratos',
                context: {
                    title:'Configure Stratos',
                    partition_deployment:data.partition_deployment,
                    config_status:data.config_status
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Configure Stratos',
                    configure_stratos:true,
                    button:{
                        link:'/',
                        name:'Deploy New Partition',
                        class_name:"btn-important"
                    },
                    has_help:false,
                    pa_deploy:true
                }
            }
        ],
        title:[
            {
                partial:'title',
                context:{
                    title:"My Cartridges"
                }
            }
        ]
    });
};