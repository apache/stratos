var render = function (theme, data, meta, require) {

    var create_btn_class = 'btn-important';
    var title = 'Tenant Management';
    theme('index', {
        body: [
            {
                partial: 'tenant_management',
                context: {
                    title:title,
                    tenants:data.tenants
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Tenant Management',
                    button:{
                        link:'/tenant_new.jag',
                        name:'Add New Tenant',
                        class_name:create_btn_class
                    },
                    has_help:true,
                    help:"Tenants you create has permission to view and subscribe to Cartridges. Tenants don't have permission to do Partition deployment, Policy deployment, LB Creation, and MT service deployment.",
                    tenant_mgt:true,
                    config_status:data.config_status,
                    has_action_buttons:true
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