var render = function (theme, data, meta, require) {

    var create_btn_class = 'btn-default';
    var title = 'Tenant Management - Add New Tenant';
    theme('index', {
        body: [
            {
                partial: 'tenant_new',
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
                    title:title,
                    button:{
                        link:'/tenant_management.jag',
                        name:'Tenant Management',
                        class_name:create_btn_class,
                        class_icon:'fa fa-arrow-left'
                    },
                    has_help:true,
                    help:"Tenants you create has permission to view and subscribe to Cartridges. Tenants don't have permission to do Partition deployment, Policy deployment, LB Creation, and MT service deployment.",
                    tenant_mgt:true,
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