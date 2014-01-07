var render = function (theme, data, meta, require) {
    for(var i=0;i<data.policy_deployments.length;i++){
        data.policy_deployments[i].key = data.policy_deployments[i].name.replace(/ /g,'');
    }
    theme('index', {
        body: [
            {
                partial: 'policy_deployments',
                context: {
                    title:'Configure Stratos - Policy Deployments',
                    policy_deployments:data.policy_deployments
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
                    policy_deployments:true
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