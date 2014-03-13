var render = function (theme, data, meta, require) {
    session.remove("get-status");
    session.remove("deploy-status");
    var create_btn_class = 'btn-important';
    var title = 'Tenant Management';
    if(data.error.length == 0 ){
        theme('index', {
            body: [
                {
                    partial: 'tenant_management',
                    context: {
                        title:title,
                        tenants:data.tenants.tenantInfoBean
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
    }else{
        theme('index', {
            body: [
                {
                    partial: 'error_page',
                    context: {
                        title:'Error',
                        error:data.error
                    }
                }
            ],
            header: [
                {
                    partial: 'header',
                    context: {
                        title: 'My Cartridges',
                        my_cartridges: true,
                        button: {
                            link: '/cartridges.jag',
                            name: 'Subscribe to Cartridge',
                            class_name: 'btn-important'
                        },
                        has_help: true,
                        help: 'Create cartridges like PHP, Python, Ruby etc.. Or create data cartridges with mySql, PostgreSQL. Directly install applications like Drupal, Wordpress etc..'
                    }
                }
            ],
            title: [
                {
                    partial: 'title',
                    context: {
                        title: "My Cartridges"
                    }
                }
            ]
        });
    }
};